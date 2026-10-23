package com.abhisheksati.mydoc.rendering.engine

import android.graphics.Bitmap
import android.graphics.RectF
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.cache.TileBitmapCache
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
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

    override fun authenticatePassword(password: String): Boolean {
        return lock.withLock {
            val doc = document ?: return@withLock false
            doc.authenticatePassword(password)
        }
    }

    override val pageCount: Int
        get() = lock.withLock {
            try {
                document?.countPages() ?: 0
            } catch (_: Throwable) {
                0
            }
        }

    override fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        return lock.withLock {
            val doc = document ?: return@withLock Pair(595f, 842f)
            if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@withLock Pair(595f, 842f)

            try {
                val page = doc.loadPage(pageIndex)
                val bounds = page.bounds
                val width = bounds.x1 - bounds.x0
                val height = bounds.y1 - bounds.y0
                page.destroy()
                Pair(width, height)
            } catch (_: Throwable) {
                Pair(595f, 842f)
            }
        }
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
                val page = doc.loadPage(pageIndex)
                val matrix = Matrix(scale)
                baseBitmap = try {
                    com.artifex.mupdf.fitz.android.AndroidDrawDevice.drawPage(page, matrix)
                } catch (_: Throwable) {
                    val pixmap = page.toPixmap(matrix, ColorSpace.DeviceRGB, true)
                    val width = pixmap.width
                    val height = pixmap.height
                    val pixels = pixmap.pixels
                    for (i in pixels.indices) {
                        pixels[i] = pixels[i] or (0xFF shl 24)
                    }
                    val b = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                    pixmap.destroy()
                    b
                }
                page.destroy()

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
            val page = doc.loadPage(pageIndex)
            val bounds = page.bounds
            val srcW = (bounds.x1 - bounds.x0).coerceAtLeast(1f)
            val s = pageWidthPx / srcW
            val pageHeightPx = (bounds.y1 - bounds.y0) * s

            val matrix = Matrix(s).translate(-region.left * pageWidthPx, -region.top * pageHeightPx)
            baseBitmap = try {
                com.artifex.mupdf.fitz.android.AndroidDrawDevice.drawPage(page, matrix)
            } catch (_: Throwable) {
                val pixmap = page.toPixmap(matrix, ColorSpace.DeviceRGB, true)
                val width = pixmap.width
                val height = pixmap.height
                val pixels = pixmap.pixels
                for (i in pixels.indices) {
                    pixels[i] = pixels[i] or (0xFF shl 24)
                }
                val b = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                pixmap.destroy()
                b
            }
            page.destroy()

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
                    val page = doc.loadPage(p)
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
                    page.destroy()
                } catch (_: Exception) {
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
            if (loc != null) doc.pageNumberFromLocation(loc) else 0
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

    override fun getPageText(pageIndex: Int): String = lock.withLock {
        val doc = document ?: return@withLock ""
        if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@withLock ""
        try {
            val page = doc.loadPage(pageIndex)
            val st = page.toStructuredText()
            val sb = StringBuilder()
            for (b in st.blocks) {
                for (l in b.lines) {
                    for (c in l.chars) sb.append(c.c.toChar())
                    sb.append("\n")
                }
            }
            st.destroy()
            page.destroy()
            sb.toString()
        } catch (_: Throwable) {
            ""
        }
    }

    override fun getDocumentText(): String = lock.withLock {
        val doc = document ?: return@withLock ""
        val total = try { doc.countPages() } catch (_: Throwable) { 0 }
        val sb = StringBuilder()
        for (p in 0 until total) {
            try {
                val page = doc.loadPage(p)
                val st = page.toStructuredText()
                for (b in st.blocks) {
                    for (l in b.lines) {
                        for (c in l.chars) sb.append(c.c.toChar())
                        sb.append("\n")
                    }
                }
                st.destroy()
                page.destroy()
            } catch (_: Throwable) {}
        }
        sb.toString()
    }

    override fun close() {
        lock.withLock {
            tileCache.recycleAll()
            try {
                document?.destroy()
            } catch (_: Throwable) {
            }
            document = null
        }
    }
}
