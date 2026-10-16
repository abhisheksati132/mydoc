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

    private var document: Document? = null

    init {
        openDocument()
    }

    private fun openDocument() {
        document = try {
            if (filePath != null && File(filePath).exists()) {
                Document.openDocument(filePath)
            } else if (documentBytes != null) {
                Document.openDocument(documentBytes, mimeType)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override val isPasswordProtected: Boolean
        get() = document?.needsPassword() == true

    override fun authenticatePassword(password: String): Boolean {
        val doc = document ?: return false
        return doc.authenticatePassword(password)
    }

    override val pageCount: Int
        get() = try {
            document?.countPages() ?: 0
        } catch (_: Exception) {
            0
        }

    override fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        val doc = document ?: return Pair(595f, 842f)
        if (pageIndex < 0 || pageIndex >= pageCount) return Pair(595f, 842f)

        return try {
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

    override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val doc = document ?: return null
        if (pageIndex < 0 || pageIndex >= pageCount) return null

        // Check LruCache first for instant frame rates
        val cached = tileCache.get(pageIndex, scale, mode)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return try {
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

    override fun search(query: String): List<SearchMatch> {
        val doc = document ?: return emptyList()
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<SearchMatch>()
        val total = pageCount

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
        return results
    }

    override fun getOutline(): List<OutlineItem> {
        val doc = document ?: return emptyList()
        return try {
            val rawOutlines = doc.loadOutline() ?: return emptyList()
            rawOutlines.mapNotNull { parseOutlineNode(doc, it, 0) }
        } catch (_: Exception) {
            emptyList()
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
        tileCache.clear()
        try {
            document?.destroy()
        } catch (_: Exception) {
        }
        document = null
    }
}
