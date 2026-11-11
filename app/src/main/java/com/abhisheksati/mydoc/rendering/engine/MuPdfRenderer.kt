package com.abhisheksati.mydoc.rendering.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    // MuPDF's needsPassword() retries an empty password and can overwrite an unlocked key.
    // Once authenticated, never probe it again during this renderer's lifetime.
    private var passwordAuthenticated = false

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
        get() = lock.withLock { !passwordAuthenticated && document?.needsPassword() == true }

    /** Fitz takes the password itself and then reads the document, so it is the engine that can. */
    override val supportsPasswordAuthentication: Boolean
        get() = true

    override fun authenticatePassword(password: String): Boolean {
        return lock.withLock {
            val doc = document ?: return@withLock false
            if (passwordAuthenticated) return@withLock true
            doc.authenticatePassword(password).also { passwordAuthenticated = it }
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

    override fun peekPageSize(pageIndex: Int): Pair<Float, Float>? = pageSizeCache[pageIndex]

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

    /**
     * A page is rendered into one bitmap, and a PDF may declare a MediaBox large enough that the
     * requested scale would ask for a multi-gigabyte allocation. The scale is capped so the page
     * still appears, smaller, instead of failing the allocation and leaving a spinner behind.
     */
    /** 24 Mpx is about 96 MB at ARGB_8888, the largest single page worth allocating. */
    private val MaxPagePixels = 24_000_000.0

    private fun budgetedScale(pageIndex: Int, scale: Float): Float {
        val (width, height) = getPageSize(pageIndex)
        val points = width.toDouble() * height.toDouble()
        if (points <= 0.0) return scale
        val ceiling = kotlin.math.sqrt(MaxPagePixels / points).toFloat()
        return if (scale > ceiling) ceiling.coerceAtLeast(0.02f) else scale
    }


    override fun getCachedPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val cached = tileCache.get(pageIndex, budgetedScale(pageIndex, scale), mode)
        return if (cached != null && !cached.isRecycled) cached else null
    }

    override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        // Bounded before the lock is taken: getPageSize locks too.
        val effectiveScale = budgetedScale(pageIndex, scale)
        val cached = tileCache.get(pageIndex, effectiveScale, mode)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return lock.withLock {
            val doc = document ?: return@withLock null
            if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@withLock null

            var baseBitmap: Bitmap? = null
            try {
                baseBitmap = withPage(doc, pageIndex) { page ->
                    val matrix = Matrix(effectiveScale)
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
                val srcH = (bounds.y1 - bounds.y0).coerceAtLeast(1f)
                val s = pageWidthPx / srcW

                // The region in page coordinates. MuPDF applies a Matrix translation in pre-scale
                // space, so the offsets here are page units: shifting by pixel values left every
                // tile showing the whole page instead of the slice that was asked for.
                val regionLeft = bounds.x0 + region.left * srcW
                val regionTop = bounds.y0 + region.top * srcH

                try {
                    val tile = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                    Canvas(tile).drawColor(Color.WHITE)
                    try {
                        val device = com.artifex.mupdf.fitz.android.AndroidDrawDevice(tile)
                        try {
                            page.run(device, Matrix(s).translate(-regionLeft, -regionTop), null)
                        } finally {
                            device.destroy()
                        }
                    } catch (t: Throwable) {
                        android.util.Log.w("MuPdfRenderer", "clipped tile render failed", t)
                        if (!tile.isRecycled) tile.recycle()
                        throw t
                    }
                    tile
                } catch (_: Throwable) {
                    // Pixmap fallback. toPixmap allocates for the transformed page bounds, so this
                    // path is only taken when the draw device itself cannot be used.
                    withPixmap(page, Matrix(s).translate(-regionLeft, -regionTop)) { pixmap ->
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

            val base = if (raw.width != outWidth || raw.height != outHeight) {
                val scaled = Bitmap.createScaledBitmap(raw, outWidth, outHeight, true)
                if (scaled !== raw && !raw.isRecycled) {
                    raw.recycle()
                }
                scaled
            } else {
                raw
            }
            baseBitmap = base

            val finalBitmap = when (mode) {
                ReadingMode.INVERTED_DARK -> {
                    val converted = AndroidPdfRenderer.invertBitmapColors(base)
                    if (converted !== base && !base.isRecycled) {
                        base.recycle()
                    }
                    converted
                }
                ReadingMode.SEPIA -> {
                    val converted = AndroidPdfRenderer.applySepiaFilter(base)
                    if (converted !== base && !base.isRecycled) {
                        base.recycle()
                    }
                    converted
                }
                ReadingMode.EYE_COMFORT -> {
                    val converted = AndroidPdfRenderer.applyEyeComfortFilter(base)
                    if (converted !== base && !base.isRecycled) {
                        base.recycle()
                    }
                    converted
                }
                ReadingMode.NORMAL -> base
            }
            finalBitmap
        } catch (_: Throwable) {
            baseBitmap?.recycle()
            null
        }
    }

    override fun search(query: String, isCancelled: () -> Boolean): List<SearchMatch> {
        if (query.isBlank()) return emptyList()

        val total = lock.withLock {
            val doc = document ?: return emptyList()
            try {
                doc.countPages()
            } catch (t: Throwable) {
                android.util.Log.w("MuPdfRenderer", "could not count pages for search", t)
                0
            }
        }

        val results = mutableListOf<SearchMatch>()
        for (p in 0 until total) {
            // The native document is not safe to touch from two threads, but the scan does not need
            // the lock while it is turning quads into RectFs or deciding whether to keep going.
            // Holding the rendering lock for the whole document used to freeze page painting for as
            // long as the search ran, which for a large PDF was seconds per keystroke.
            if (isCancelled()) return emptyList()
            val quads = lock.withLock {
                val doc = document ?: return emptyList()
                try {
                    withPage(doc, p) { page -> page.search(query) }
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    android.util.Log.w("MuPdfRenderer", "Search skipped page $p", t)
                    null
                }
            }
            if (quads == null) continue
            for (quadsInLine in quads) {
                for (quad in quadsInLine) {
                    val r = quad.toRect()
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
        return results
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
                for (c in l.chars) sb.appendCodePoint(c.c)
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
            passwordAuthenticated = false
        }
    }
}
