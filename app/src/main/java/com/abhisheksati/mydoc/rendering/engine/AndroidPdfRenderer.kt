package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.cache.TileBitmapCache
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Standard Android OS built-in PDF Renderer with PDFBox textual intelligence fallback.
 * Guaranteed to work on all Android devices (API 21+).
 */
class AndroidPdfRenderer(
    private val file: File,
    private val context: Context? = null,
    private val tileCache: TileBitmapCache = TileBitmapCache()
) : DocumentRenderer {

    private val lock = ReentrantLock()
    private var pfd: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    init {
        lock.withLock {
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pfd?.let {
                    pdfRenderer = PdfRenderer(it)
                }
            } catch (_: Throwable) {
            }
        }
    }

    private val pageSizeCache = java.util.concurrent.ConcurrentHashMap<Int, Pair<Float, Float>>()

    override val isPasswordProtected: Boolean
        get() = false

    override fun authenticatePassword(password: String): Boolean = false

    override val pageCount: Int
        get() = lock.withLock { pdfRenderer?.pageCount ?: 0 }

    override fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        pageSizeCache[pageIndex]?.let { return it }
        return lock.withLock {
            pageSizeCache[pageIndex]?.let { return@withLock it }
            val renderer = pdfRenderer ?: return@withLock Pair(595f, 842f)
            if (pageIndex < 0 || pageIndex >= (pdfRenderer?.pageCount ?: 0)) return@withLock Pair(595f, 842f)
            try {
                val page = renderer.openPage(pageIndex)
                try {
                    val w = page.width.toFloat()
                    val h = page.height.toFloat()
                    val size = Pair(w, h)
                    pageSizeCache[pageIndex] = size
                    size
                } finally {
                    page.close()
                }
            } catch (_: Throwable) {
                Pair(595f, 842f)
            }
        }
    }

    override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val cached = tileCache.get(pageIndex, scale, mode)
        if (cached != null && !cached.isRecycled) return cached

        return lock.withLock {
            val renderer = pdfRenderer ?: return@withLock null
            if (pageIndex < 0 || pageIndex >= (pdfRenderer?.pageCount ?: 0)) return@withLock null

            var baseBitmap: Bitmap? = null
            try {
                val page = renderer.openPage(pageIndex)
                try {
                    val origWidth = page.width
                    val origHeight = page.height

                    val scaledWidth = (origWidth * scale).toInt().coerceAtLeast(1)
                    val scaledHeight = (origHeight * scale).toInt().coerceAtLeast(1)

                    baseBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(baseBitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(baseBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                } finally {
                    page.close()
                }

                val finalBitmap = when (mode) {
                    ReadingMode.INVERTED_DARK -> {
                        val converted = invertBitmapColors(baseBitmap)
                        if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                            baseBitmap.recycle()
                        }
                        converted
                    }
                    ReadingMode.SEPIA -> {
                        val converted = applySepiaFilter(baseBitmap)
                        if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                            baseBitmap.recycle()
                        }
                        converted
                    }
                    ReadingMode.EYE_COMFORT -> {
                        val converted = applyEyeComfortFilter(baseBitmap)
                        if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                            baseBitmap.recycle()
                        }
                        converted
                    }
                    ReadingMode.NORMAL -> baseBitmap
                }

                tileCache.put(pageIndex, scale, mode, finalBitmap)
                finalBitmap
            } catch (_: Throwable) {
                baseBitmap?.recycle()
                null
            }
        }
    }

    override fun renderRegion(
        pageIndex: Int,
        region: android.graphics.RectF,
        pageWidthPx: Float,
        outWidth: Int,
        outHeight: Int,
        mode: ReadingMode
    ): Bitmap? = lock.withLock {
        val renderer = pdfRenderer ?: return@withLock null
        if (pageIndex < 0 || pageIndex >= (pdfRenderer?.pageCount ?: 0) || outWidth <= 0 || outHeight <= 0) return@withLock null

        val page = try {
            renderer.openPage(pageIndex)
        } catch (_: Throwable) {
            return@withLock null
        }

        var baseBitmap: Bitmap? = null
        try {
            try {
                val srcW = page.width.toFloat().coerceAtLeast(1f)
                val srcH = page.height.toFloat().coerceAtLeast(1f)
                val s = pageWidthPx / srcW
                val pageHeightPx = srcH * s

                baseBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                baseBitmap.eraseColor(Color.WHITE)

                val matrix = android.graphics.Matrix().apply {
                    postScale(s, s)
                    postTranslate(-region.left * pageWidthPx, -region.top * pageHeightPx)
                }

                page.render(baseBitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            } finally {
                page.close()
            }

            val finalBitmap = when (mode) {
                ReadingMode.INVERTED_DARK -> {
                    val converted = invertBitmapColors(baseBitmap)
                    if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                        baseBitmap.recycle()
                    }
                    converted
                }
                ReadingMode.SEPIA -> {
                    val converted = applySepiaFilter(baseBitmap)
                    if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                        baseBitmap.recycle()
                    }
                    converted
                }
                ReadingMode.EYE_COMFORT -> {
                    val converted = applyEyeComfortFilter(baseBitmap)
                    if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                        baseBitmap.recycle()
                    }
                    converted
                }
                ReadingMode.NORMAL -> baseBitmap
            }
            finalBitmap
        } catch (_: Throwable) {
            baseBitmap?.recycle()
            null
        }
    }

    override fun getPageText(pageIndex: Int): String = lock.withLock {
        if (pageIndex < 0 || pageIndex >= pageCount) return@withLock ""
        try {
            if (context != null) {
                PdfBoxHelper.ensureInitialized(context)
            }
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
                stripper.getText(doc)?.trim() ?: ""
            }
        } catch (_: Throwable) {
            ""
        }
    }

    override fun getDocumentText(): String = lock.withLock {
        try {
            if (context != null) {
                PdfBoxHelper.ensureInitialized(context)
            }
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                stripper.getText(doc)?.trim() ?: ""
            }
        } catch (_: Throwable) {
            ""
        }
    }

    override fun search(query: String): List<SearchMatch> = lock.withLock {
        if (query.isBlank()) return@withLock emptyList()
        val total = pageCount
        val results = mutableListOf<SearchMatch>()
        try {
            if (context != null) {
                PdfBoxHelper.ensureInitialized(context)
            }
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                for (p in 0 until total) {
                    stripper.startPage = p + 1
                    stripper.endPage = p + 1
                    val text = stripper.getText(doc) ?: ""
                    if (text.contains(query, ignoreCase = true)) {
                        val size = getPageSize(p)
                        results.add(
                            SearchMatch(
                                pageIndex = p,
                                bounds = android.graphics.RectF(0f, 0f, size.first, size.second),
                                textSnippet = query
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {}
        results
    }

    override fun getOutline(): List<OutlineItem> = lock.withLock {
        try {
            if (context != null) {
                PdfBoxHelper.ensureInitialized(context)
            }
            PDDocument.load(file).use { doc ->
                val outline = doc.documentCatalog.documentOutline ?: return@withLock emptyList()
                parsePdfBoxOutline(doc, outline)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parsePdfBoxOutline(doc: PDDocument, node: PDOutlineNode): List<OutlineItem> {
        val items = mutableListOf<OutlineItem>()
        var current: PDOutlineItem? = node.firstChild
        while (current != null) {
            val title = current.title ?: ""
            val pageNumber = try {
                val destPage = current.findDestinationPage(doc)
                if (destPage != null) doc.pages.indexOf(destPage).coerceAtLeast(0) else 0
            } catch (_: Throwable) {
                0
            }
            val children = parsePdfBoxOutline(doc, current)
            items.add(
                OutlineItem(
                    title = title,
                    pageIndex = pageNumber,
                    level = 0,
                    children = children
                )
            )
            current = current.nextSibling
        }
        return items
    }

    override fun close() {
        lock.withLock {
            pageSizeCache.clear()
            tileCache.recycleAll()
            try {
                pdfRenderer?.close()
            } catch (_: Throwable) {
            }
            try {
                pfd?.close()
            } catch (_: Throwable) {
            }
            pdfRenderer = null
            pfd = null
        }
    }

    companion object {
        fun invertBitmapColors(src: Bitmap): Bitmap {
            val width = src.width
            val height = src.height
            val inverted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(inverted)
            val paint = Paint()
            val colorMatrix = ColorMatrix(floatArrayOf(
                -1.0f, 0f, 0f, 0f, 255f,
                0f, -1.0f, 0f, 0f, 255f,
                0f, 0f, -1.0f, 0f, 255f,
                0f, 0f, 0f, 1.0f, 0f
            ))
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(src, 0f, 0f, paint)
            return inverted
        }

        fun applySepiaFilter(src: Bitmap): Bitmap {
            val width = src.width
            val height = src.height
            val sepia = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(sepia)
            val paint = Paint()
            val matrix = ColorMatrix()
            matrix.set(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1.0f, 0f
            ))
            paint.colorFilter = ColorMatrixColorFilter(matrix)
            canvas.drawBitmap(src, 0f, 0f, paint)
            return sepia
        }

        fun applyEyeComfortFilter(src: Bitmap): Bitmap {
            val width = src.width
            val height = src.height
            val filtered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(filtered)
            val paint = Paint()
            // Gentle warm amber filter reducing blue spectrum while preserving text legibility
            val matrix = ColorMatrix(floatArrayOf(
                1.0f, 0.0f, 0.0f, 0f, 0f,
                0.0f, 0.94f, 0.0f, 0f, 0f,
                0.0f, 0.0f, 0.76f, 0f, 0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0f
            ))
            paint.colorFilter = ColorMatrixColorFilter(matrix)
            canvas.drawBitmap(src, 0f, 0f, paint)
            return filtered
        }
    }
}
