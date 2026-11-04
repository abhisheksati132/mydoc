package com.abhisheksati.mydoc.rendering.engine

import android.graphics.Bitmap
import android.graphics.RectF
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.cache.TileBitmapCache
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.Pixmap
import com.artifex.mupdf.fitz.StructuredText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MuPdfRenderer(
    private val documentBytes: ByteArray? = null,
    private val filePath: String? = null,
    private val mimeType: String = "application/pdf",
    private val tileCache: TileBitmapCache = TileBitmapCache()
) : DocumentRenderer {

    private val lock = java.util.concurrent.locks.ReentrantLock()
    private var document: Document? = null

    companion object {
        init {
            try {
                com.artifex.mupdf.fitz.Context.init()
            } catch (_: Throwable) {}
        }
    }

    init {
        openDocument()
    }

    private fun openDocument() {
        lock.withLock {
            try {
                com.artifex.mupdf.fitz.Context.init()
            } catch (_: Throwable) {}
            document = try {
                if (filePath != null && File(filePath).exists()) {
                    Document.openDocument(filePath)
                } else if (documentBytes != null) {
                    try {
                        Document.openDocument(documentBytes, mimeType)
                    } catch (_: Throwable) {
                        try {
                            Document.openDocument(documentBytes, "application/pdf")
                        } catch (_: Throwable) {
                            try {
                                Document.openDocument(documentBytes, "pdf")
                            } catch (_: Throwable) {
                                null
                            }
                        }
                    }
                } else {
                    null
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    override val isPasswordProtected: Boolean
        get() = lock.withLock { document?.needsPassword() == true }

    /** Fitz takes the password itself and then reads the document, so it is the engine that can. */
    override val supportsPasswordAuthentication: Boolean
        get() = true

    override fun authenticatePassword(password: String): Boolean {
        return lock.withLock {
            val doc = document ?: return@withLock false
            doc.authenticatePassword(password)
        }
    }

    private val pageSizeCache = java.util.concurrent.ConcurrentHashMap<Int, Pair<Float, Float>>()

    override val pageCount: Int
        get() = lock.withLock {
            try {
                document?.countPages() ?: 0
            } catch (_: Throwable) {
                0
            }
        }

    /**
     * MuPDF hands out native page, pixmap and structured-text objects that the garbage collector
     * does not reclaim. Every one is taken through these helpers so it is destroyed whether the
     * body returns or throws; the previous code destroyed them on the success path only, so each
     * failure on a damaged file leaked native memory until the process died.
     */
    private inline fun <R> withPage(doc: Document, index: Int, block: (Page) -> R): R {
        val page = doc.loadPage(index)
        try {
            return block(page)
        } finally {
            page.destroy()
        }
    }

    private inline fun <R> withPixmap(page: Page, matrix: Matrix, block: (Pixmap) -> R): R {
        val pixmap = page.toPixmap(matrix, ColorSpace.DeviceRGB, true)
        try {
            return block(pixmap)
        } finally {
            pixmap.destroy()
        }
    }

    private inline fun <R> withStructuredText(page: Page, block: (StructuredText) -> R): R {
        val text = page.toStructuredText()
        try {
            return block(text)
        } finally {
            text.destroy()
        }
    }

    override fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        pageSizeCache[pageIndex]?.let { return it }
        return lock.withLock {
            pageSizeCache[pageIndex]?.let { return@withLock it }
            val doc = document ?: return@withLock Pair(595f, 842f)
            if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@withLock Pair(595f, 842f)

            val size = runCatching {
                withPage(doc, pageIndex) { page ->
                    val bounds = page.bounds
                    Pair(bounds.x1 - bounds.x0, bounds.y1 - bounds.y0)
                }
            }.getOrElse {
                // Not cached: a fallback size must not outlive the failure that produced it.
                android.util.Log.w("MuPdfRenderer", "Page $pageIndex bounds unreadable; using A4", it)
                return@withLock Pair(595f, 842f)
            }
            pageSizeCache[pageIndex] = size
            size
        }
    }

    override fun getCachedPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val cached = tileCache.get(pageIndex, scale, mode)
        return if (cached != null && !cached.isRecycled) cached else null
    }

    override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val cached = tileCache.get(pageIndex, scale, mode)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return lock.withLock {
            val doc = document ?: return@withLock null
            if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@withLock null

            var baseBitmap: Bitmap? = null
            try {
                baseBitmap = withPage(doc, pageIndex) { page ->
                    val matrix = Matrix(scale)
                    try {
                        com.artifex.mupdf.fitz.android.AndroidDrawDevice.drawPage(page, matrix)
                    } catch (_: Throwable) {
                        withPixmap(page, matrix) { pixmap ->
                            val width = pixmap.width
                            val height = pixmap.height
                            val pixels = pixmap.pixels
                            for (i in pixels.indices) {
                                pixels[i] = pixels[i] or (0xFF shl 24)
                            }
                            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                        }
                    }
                }

                if (baseBitmap == null) return@withLock null

            val finalBitmap = when (mode) {
                    ReadingMode.INVERTED_DARK -> {
                        val converted = AndroidPdfRenderer.invertBitmapColors(baseBitmap)
                        if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                            baseBitmap.recycle()
                        }
                        converted
                    }
                    ReadingMode.SEPIA -> {
                        val converted = AndroidPdfRenderer.applySepiaFilter(baseBitmap)
                        if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                            baseBitmap.recycle()
                        }
                        converted
                    }
                    ReadingMode.EYE_COMFORT -> {
                        val converted = AndroidPdfRenderer.applyEyeComfortFilter(baseBitmap)
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
        region: RectF,
        pageWidthPx: Float,
        outWidth: Int,
        outHeight: Int,
        mode: ReadingMode
    ): Bitmap? = lock.withLock {
        val doc = document ?: return@withLock null
        if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0) || outWidth <= 0 || outHeight <= 0) return@withLock null

        var baseBitmap: Bitmap? = null
        try {
            val raw = withPage(doc, pageIndex) { page ->
                val bounds = page.bounds
                val srcW = (bounds.x1 - bounds.x0).coerceAtLeast(1f)
                val s = pageWidthPx / srcW
                val pageHeightPx = (bounds.y1 - bounds.y0) * s
                val matrix = Matrix(s).translate(-region.left * pageWidthPx, -region.top * pageHeightPx)
                try {
                    com.artifex.mupdf.fitz.android.AndroidDrawDevice.drawPage(page, matrix)
                } catch (_: Throwable) {
                    withPixmap(page, matrix) { pixmap ->
                        val width = pixmap.width
                        val height = pixmap.height
                        val pixels = pixmap.pixels
                        for (i in pixels.indices) {
                            pixels[i] = pixels[i] or (0xFF shl 24)
                        }
                        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                    }
                }
            }

            baseBitmap = if (raw != null && (raw.width != outWidth || raw.height != outHeight)) {
                val scaled = Bitmap.createScaledBitmap(raw, outWidth, outHeight, true)
                if (scaled !== raw && !raw.isRecycled) {
                    raw.recycle()
                }
                scaled
            } else {
                raw
            }

            if (baseBitmap == null) return@withLock null

            val finalBitmap = when (mode) {
                ReadingMode.INVERTED_DARK -> {
                    val converted = AndroidPdfRenderer.invertBitmapColors(baseBitmap)
                    if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                        baseBitmap.recycle()
                    }
                    converted
                }
                ReadingMode.SEPIA -> {
                    val converted = AndroidPdfRenderer.applySepiaFilter(baseBitmap)
                    if (converted !== baseBitmap && !baseBitmap.isRecycled) {
                        baseBitmap.recycle()
                    }
                    converted
                }
                ReadingMode.EYE_COMFORT -> {
                    val converted = AndroidPdfRenderer.applyEyeComfortFilter(baseBitmap)
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

    override fun search(query: String): List<SearchMatch> {
        if (query.isBlank()) return emptyList()

        return lock.withLock {
            val doc = document ?: return@withLock emptyList()
            val results = mutableListOf<SearchMatch>()
            val total = try { doc.countPages() } catch (_: Throwable) { 0 }

            for (p in 0 until total) {
                try {
                    withPage(doc, p) { page ->
                        val quadsList = page.search(query)
                        if (quadsList != null) {
                            for (quads in quadsList) {
                                for (q in quads) {
                                    val r = q.toRect()
                                    results.add(
                                        SearchMatch(
                                            pageIndex = p,
                                            bounds = RectF(r.x0, r.y0, r.x1, r.y1),
                                            textSnippet = query
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    android.util.Log.w("MuPdfRenderer", "Search skipped page $p", e)
                }
            }
            results
        }
    }

    override fun getOutline(): List<OutlineItem> {
        return lock.withLock {
            val doc = document ?: return@withLock emptyList()
            try {
                val rawOutlines = doc.loadOutline() ?: return@withLock emptyList()
                rawOutlines.mapNotNull { parseOutlineNode(doc, it, 0) }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    private fun parseOutlineNode(doc: Document, outline: Outline, level: Int): OutlineItem? {
        val title = outline.title ?: return null
        val pageNumber = try {
            val loc = doc.resolveLink(outline)
            if (loc != null) doc.pageNumberFromLocation(loc).coerceAtLeast(0) else 0
        } catch (_: Throwable) {
            0
        }

        val children = outline.down?.mapNotNull { parseOutlineNode(doc, it, level + 1) } ?: emptyList()

        return OutlineItem(
            title = title,
            pageIndex = pageNumber,
            level = level,
            children = children
        )
    }

    /**
     * Null means the text layer could not be read; an empty string means this page really has no
     * text. The two need different advice, so they must not share a value.
     */
    override fun getPageText(pageIndex: Int): String? = lock.withLock {
        val doc = document ?: return@withLock null
        if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@withLock null
        runCatching {
            withPage(doc, pageIndex) { page -> withStructuredText(page) { st -> dumpText(st) } }
        }.getOrElse {
            if (it is CancellationException) throw it
            android.util.Log.w("MuPdfRenderer", "Page $pageIndex text layer unreadable", it)
            null
        }
    }

    private fun dumpText(st: StructuredText): String {
        val sb = StringBuilder()
        for (b in st.blocks) {
            for (l in b.lines) {
                for (c in l.chars) sb.append(c.c.toChar())
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    /**
     * Text of every readable page. Null when nothing could be read; when only some pages failed
     * the readable part is returned and each failure is logged.
     */
    override fun getDocumentText(): String? = lock.withLock {
        val doc = document ?: return@withLock null
        val total = runCatching { doc.countPages() }.getOrElse {
            if (it is CancellationException) throw it
            android.util.Log.w("MuPdfRenderer", "Page count unreadable", it)
            return@withLock null
        }
        val sb = StringBuilder()
        var failedPages = 0
        for (p in 0 until total) {
            val pageText = runCatching {
                withPage(doc, p) { page -> withStructuredText(page) { st -> dumpText(st) } }
            }.getOrElse {
                if (it is CancellationException) throw it
                android.util.Log.w("MuPdfRenderer", "Page $p text layer unreadable", it)
                null
            }
            if (pageText == null) failedPages++ else sb.append(pageText)
        }
        if (sb.isEmpty() && failedPages > 0) null else sb.toString()
    }

    override fun selectTextAt(pageIndex: Int, point: android.graphics.PointF): TextSelectionResult? = lock.withLock {
        val doc = document ?: return@withLock null
        if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@withLock null
        runCatching {
            withPage(doc, pageIndex) { page ->
                withStructuredText(page) { st ->
                    val pt = com.artifex.mupdf.fitz.Point(point.x, point.y)
                    val quad = st.snapSelection(pt, pt, com.artifex.mupdf.fitz.StructuredText.SELECT_WORDS)
                        ?: return@withStructuredText null
                    val r = quad.toRect()
                    val copied = st.copy(
                        com.artifex.mupdf.fitz.Point(r.x0, r.y0),
                        com.artifex.mupdf.fitz.Point(r.x1, r.y1)
                    )?.trim().orEmpty()
                    if (copied.isEmpty()) null
                    else TextSelectionResult(copied, listOf(RectF(r.x0, r.y0, r.x1, r.y1)))
                }
            }
        }.getOrElse {
            if (it is CancellationException) throw it
            android.util.Log.w("MuPdfRenderer", "Word selection failed on page $pageIndex", it)
            null
        }
    }

    override fun selectTextBetween(pageIndex: Int, start: android.graphics.PointF, end: android.graphics.PointF): TextSelectionResult? = lock.withLock {
        val doc = document ?: return@withLock null
        if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@withLock null
        runCatching {
            withPage(doc, pageIndex) { page ->
                withStructuredText(page) { st ->
                    val p1 = com.artifex.mupdf.fitz.Point(start.x, start.y)
                    val p2 = com.artifex.mupdf.fitz.Point(end.x, end.y)
                    val rects = st.highlight(p1, p2)?.map { q ->
                        val r = q.toRect()
                        RectF(r.x0, r.y0, r.x1, r.y1)
                    }.orEmpty()
                    val text = st.copy(p1, p2)?.trim().orEmpty()
                    if (text.isNotEmpty() && rects.isNotEmpty()) {
                        TextSelectionResult(selectedText = text, highlightRects = rects)
                    } else null
                }
            }
        }.getOrElse {
            if (it is CancellationException) throw it
            android.util.Log.w("MuPdfRenderer", "Range selection failed on page $pageIndex", it)
            null
        }
    }

    override fun close() {
        lock.withLock {
            pageSizeCache.clear()
            tileCache.recycleAll()
            try {
                document?.destroy()
            } catch (_: Throwable) {
            }
            document = null
        }
    }
}
