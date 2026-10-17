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

class MuPdfRenderer(
    private val documentBytes: ByteArray? = null,
    private val filePath: String? = null,
    private val mimeType: String = "application/pdf",
    private val tileCache: TileBitmapCache = TileBitmapCache()
) : DocumentRenderer {

    private val lock = Any()
    private var document: Document? = null

    init {
        openDocument()
    }

    private fun openDocument() {
        synchronized(lock) {
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
        get() = synchronized(lock) { document?.needsPassword() == true }

    override fun authenticatePassword(password: String): Boolean {
        return synchronized(lock) {
            val doc = document ?: return@synchronized false
            doc.authenticatePassword(password)
        }
    }

    override val pageCount: Int
        get() = synchronized(lock) {
            try {
                document?.countPages() ?: 0
            } catch (_: Exception) {
                0
            }
        }

    override fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        return synchronized(lock) {
            val doc = document ?: return@synchronized Pair(595f, 842f)
            if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@synchronized Pair(595f, 842f)

            try {
                val page = doc.loadPage(pageIndex)
                val bounds = page.bounds
                val width = bounds.x1 - bounds.x0
                val height = bounds.y1 - bounds.y0
                page.destroy()
                Pair(width, height)
            } catch (_: Exception) {
                Pair(595f, 842f)
            }
        }
    }

    override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val cached = tileCache.get(pageIndex, scale, mode)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return synchronized(lock) {
            val doc = document ?: return@synchronized null
            if (pageIndex < 0 || pageIndex >= (document?.countPages() ?: 0)) return@synchronized null

            try {
                val page = doc.loadPage(pageIndex)
                val matrix = Matrix(scale)
                val pixmap = page.toPixmap(matrix, ColorSpace.DeviceRGB, false)

                when (mode) {
                    ReadingMode.INVERTED_DARK -> {
                        // Native MuPDF luminance inversion: true AMOLED black background (#000000)
                        pixmap.invertLuminance()
                    }
                    ReadingMode.SEPIA -> {
                        // Soft warm parchment reading mode (#3A3125 text, #F4ECD8 bg)
                        pixmap.tint(0x3A3125, 0xF4ECD8)
                    }
                    ReadingMode.NORMAL -> {
                        // Standard document colors
                    }
                }

                val width = pixmap.width
                val height = pixmap.height
                val pixels = pixmap.pixels

                val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                pixmap.destroy()
                page.destroy()

                tileCache.put(pageIndex, scale, mode, bitmap)
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun search(query: String): List<SearchMatch> {
        if (query.isBlank()) return emptyList()

        return synchronized(lock) {
            val doc = document ?: return@synchronized emptyList()
            val results = mutableListOf<SearchMatch>()
            val total = try { doc.countPages() } catch (_: Exception) { 0 }

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
        return synchronized(lock) {
            val doc = document ?: return@synchronized emptyList()
            try {
                val rawOutlines = doc.loadOutline() ?: return@synchronized emptyList()
                rawOutlines.mapNotNull { parseOutlineNode(doc, it, 0) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun parseOutlineNode(doc: Document, outline: Outline, level: Int): OutlineItem? {
        val title = outline.title ?: return null
        val pageNumber = try {
            val loc = doc.resolveLink(outline)
            if (loc != null) doc.pageNumberFromLocation(loc) else 0
        } catch (_: Exception) {
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

    override fun close() {
        synchronized(lock) {
            tileCache.clear()
            try {
                document?.destroy()
            } catch (_: Exception) {
            }
            document = null
        }
    }
}
