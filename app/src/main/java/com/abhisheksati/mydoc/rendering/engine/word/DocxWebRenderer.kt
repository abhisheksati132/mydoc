package com.abhisheksati.mydoc.rendering.engine.word

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Xml
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

/**
 * High-fidelity Word (.docx) renderer that executes `docx-preview` inside an offscreen WebView
 * and converts the rendered DOM into vector PDF pages using Android's Print framework.
 *
 * If the print framework package trick fails, it falls back to band slicing onto [PdfDocument].
 * Strict offline privacy is enforced: network loads are blocked, and all resources are bundled.
 */
internal object DocxWebRenderer {

    private const val TimeoutSeconds = 60L
    private const val CssDpi = 96
    private const val MaxSlicedPages = 500


    private const val A4WidthMils = 8268
    private const val A4HeightMils = 11693

    fun render(context: Context, docxBytes: ByteArray, outFile: File): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) return false

        val (widthMils, heightMils) = readPageSizeMils(docxBytes)
        val latch = CountDownLatch(1)
        val succeeded = AtomicBoolean(false)
        val main = Handler(Looper.getMainLooper())
        var webView: WebView? = null

        val finish = { ok: Boolean ->
            succeeded.set(ok)
            latch.countDown()
        }

        main.post {
            runCatching {
                val view = WebView(context)
                webView = view
                view.settings.javaScriptEnabled = true
                view.settings.blockNetworkLoads = true
                view.settings.allowFileAccess = true

                view.addJavascriptInterface(
                    object {
                        @android.webkit.JavascriptInterface
                        fun onRendered() {
                            main.post {
                                finish(
                                    runCatching { sliceToPdf(view, widthMils, heightMils, outFile) }
                                        .getOrDefault(false)
                                )
                            }
                        }

                        @android.webkit.JavascriptInterface
                        @Suppress("UNUSED_PARAMETER")
                        fun onFailed(reason: String) = finish(false)
                    },
                    "AndroidDocx"
                )

                OfflineWebContent.harden(view)
                view.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: android.webkit.WebResourceRequest
                    ): android.webkit.WebResourceResponse? =
                        OfflineWebContent.intercept(request)

                    override fun onPageFinished(view: WebView, url: String) {
                        val base64 = Base64.encodeToString(docxBytes, Base64.NO_WRAP)
                        view.evaluateJavascript("renderDocx('$base64')", null)
                    }
                }
                view.loadUrl("file:///android_asset/docx/index.html")
            }.onFailure { finish(false) }
        }

        val completed = runCatching { latch.await(TimeoutSeconds, TimeUnit.SECONDS) }.getOrDefault(false)
        main.post { runCatching { webView?.destroy() } }

        if (!completed || !succeeded.get()) {
            runCatching { if (outFile.exists()) outFile.delete() }
            return false
        }
        if (outFile.length() <= 0L) {
            runCatching { outFile.delete() }
            return false
        }
        return true
    }

    private fun sliceToPdf(view: WebView, widthMils: Int, heightMils: Int, outFile: File): Boolean {
        val pageWidth = (widthMils.toLong() * CssDpi / 1000L).toInt().coerceIn(80, 5000)
        val pageHeight = (heightMils.toLong() * CssDpi / 1000L).toInt().coerceIn(80, 5000)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val contentHeight = view.measuredHeight.coerceAtLeast(1)
        view.layout(0, 0, pageWidth, contentHeight)

        outFile.parentFile?.mkdirs()
        val document = PdfDocument()
        try {
            var y = 0
            var pageNumber = 1
            while (y < contentHeight && pageNumber <= MaxSlicedPages) {
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                page.canvas.save()
                page.canvas.translate(0f, -y.toFloat())
                view.draw(page.canvas)
                page.canvas.restore()
                document.finishPage(page)
                y += pageHeight
                pageNumber++
            }
            FileOutputStream(outFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return outFile.length() > 0
    }

    private fun readPageSizeMils(docxBytes: ByteArray): Pair<Int, Int> {
        val documentXml = runCatching {
            ZipInputStream(docxBytes.inputStream()).use { zip ->
                var found: ByteArray? = null
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name.removePrefix("/") == "word/document.xml") {
                        val baos = java.io.ByteArrayOutputStream(64 * 1024)
                        val buf = ByteArray(16384)
                        var readTotal = 0
                        val maxBytes = 15 * 1024 * 1024 // Max 15MB
                        var n: Int
                        while (zip.read(buf).also { n = it } != -1) {
                            val toWrite = minOf(n, maxBytes - readTotal)
                            baos.write(buf, 0, toWrite)
                            readTotal += toWrite
                            if (readTotal >= maxBytes) break
                        }
                        found = baos.toByteArray()
                        break
                    }
                }
                found
            }
        }.getOrNull() ?: return A4WidthMils to A4HeightMils

        return runCatching {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(documentXml.inputStream(), "UTF-8")
            }
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "w:pgSz") {
                    val w = parser.getAttributeValue(null, "w:w")?.toIntOrNull()
                    val h = parser.getAttributeValue(null, "w:h")?.toIntOrNull()
                    if (w != null && h != null && w > 0 && h > 0) {
                        return@runCatching twipsToMils(w) to twipsToMils(h)
                    }
                }
                event = parser.next()
            }
            A4WidthMils to A4HeightMils
        }.getOrDefault(A4WidthMils to A4HeightMils)
    }

    private fun twipsToMils(twips: Int): Int =
        (twips * 1000L / 1440L).toInt().coerceIn(1000, 40000)
}
