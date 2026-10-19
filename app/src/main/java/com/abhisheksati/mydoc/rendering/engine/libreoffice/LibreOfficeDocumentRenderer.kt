package com.abhisheksati.mydoc.rendering.engine.libreoffice

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.cache.TileBitmapCache
import com.abhisheksati.mydoc.rendering.engine.DocumentRenderer
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.rendering.engine.SearchMatch
import org.libreoffice.kit.Document
import org.libreoffice.kit.LibreOfficeKit
import org.libreoffice.kit.Office
import java.io.File
import java.nio.ByteBuffer

class LibreOfficeDocumentRenderer(
    private val filePath: String,
    private val tileCache: TileBitmapCache = TileBitmapCache()
) : DocumentRenderer {

    companion object {
        private const val TAG = "LibreOfficeDocRenderer"
    }

    private val lock = Any()
    private var office: Office? = null
    private var document: Document? = null
    private var _pageCount: Int = 1
    private var _documentType: Int = Document.DOCTYPE_OTHER

    // Reusable direct memory buffer for native tile painting (avoids continuous 10MB-40MB allocations)
    private var directBuffer: ByteBuffer? = null
    private var directBufferCapacity: Int = 0

    init {
        openDocument()
    }

    private fun openDocument() {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Document file does not exist: $filePath")
        }

        val handle = LibreOfficeKit.getLibreOfficeKitHandle()
            ?: throw IllegalStateException("LibreOfficeKit handle is null. Engine must be initialized first.")

        synchronized(lock) {
            val off = Office(handle)
            office = off

            val docUrl = "file://${file.absolutePath}"
            val doc = off.documentLoad(docUrl)
                ?: throw IllegalStateException("LibreOffice could not load document: ${off.error ?: "Unknown error"}")

            document = doc
            _pageCount = doc.parts.coerceAtLeast(1)
            _documentType = try { doc.documentType } catch (_: Throwable) { Document.DOCTYPE_OTHER }
        }
    }

    val documentType: Int
        get() = _documentType

    override val pageCount: Int
        get() = synchronized(lock) { _pageCount }

    override val isPasswordProtected: Boolean
        get() = false

    override fun authenticatePassword(password: String): Boolean {
        return true
    }

    override fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        return synchronized(lock) {
            val doc = document ?: return@synchronized Pair(595f, 842f)
            try {
                doc.part = pageIndex
                val w = doc.documentWidth.toFloat()
                val h = doc.documentHeight.toFloat()
                if (w > 0f && h > 0f) Pair(w, h) else Pair(595f, 842f)
            } catch (_: Throwable) {
                Pair(595f, 842f)
            }
        }
    }

    override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        // Fast-path: check LRU tile cache first without locking native resources
        val cached = tileCache.get(pageIndex, scale, mode)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return synchronized(lock) {
            val doc = document ?: return@synchronized null
            try {
                doc.part = pageIndex
                val baseW = doc.documentWidth.toInt().coerceAtLeast(400)
                val baseH = doc.documentHeight.toInt().coerceAtLeast(400)

                val targetW = ((baseW * scale).toInt()).coerceIn(320, 3840)
                val targetH = ((baseH * scale).toInt()).coerceIn(320, 3840)
                val requiredBytes = targetW * targetH * 4

                val buffer = getOrCreateDirectBuffer(requiredBytes)
                buffer.clear()
                buffer.limit(requiredBytes)

                doc.paintTile(buffer, targetW, targetH, 0, 0, baseW, baseH)
                buffer.rewind()

                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)

                val finalBitmap = applyReadingMode(bitmap, mode)
                tileCache.put(pageIndex, scale, mode, finalBitmap)
                finalBitmap
            } catch (t: Throwable) {
                Log.e(TAG, "Error rendering page $pageIndex", t)
                null
            }
        }
    }

    private fun getOrCreateDirectBuffer(requiredBytes: Int): ByteBuffer {
        val existing = directBuffer
        if (existing != null && directBufferCapacity >= requiredBytes) {
            return existing
        }
        val newCapacity = (requiredBytes * 1.25).toInt().coerceAtLeast(1024 * 1024)
        val newBuffer = ByteBuffer.allocateDirect(newCapacity)
        directBuffer = newBuffer
        directBufferCapacity = newCapacity
        return newBuffer
    }

    private fun applyReadingMode(src: Bitmap, mode: ReadingMode): Bitmap {
        if (mode == ReadingMode.NORMAL) return src

        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        val matrix = when (mode) {
            ReadingMode.INVERTED_DARK -> ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ReadingMode.SEPIA -> {
                val m = ColorMatrix()
                m.setScale(0.95f, 0.85f, 0.70f, 1.0f)
                m
            }
            else -> null
        }

        if (matrix != null) {
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)

        // Recycle intermediate raw bitmap to save RAM
        if (output != src && !src.isRecycled) {
            src.recycle()
        }
        return output
    }

    override fun search(query: String): List<SearchMatch> {
        if (query.isBlank()) return emptyList()
        return synchronized(lock) {
            val doc = document ?: return@synchronized emptyList()
            try {
                val matches = mutableListOf<SearchMatch>()
                val qLower = query.lowercase()
                for (idx in 0 until doc.parts) {
                    val partName = doc.getPartName(idx)
                    if (partName.lowercase().contains(qLower)) {
                        matches.add(
                            SearchMatch(
                                pageIndex = idx,
                                bounds = RectF(0f, 0f, 200f, 50f),
                                textSnippet = partName
                            )
                        )
                    }
                }
                matches
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    override fun getOutline(): List<OutlineItem> {
        return synchronized(lock) {
            val doc = document ?: return@synchronized emptyList()
            try {
                (0 until doc.parts).map { idx ->
                    val defaultPrefix = when (_documentType) {
                        Document.DOCTYPE_SPREADSHEET -> "Sheet"
                        Document.DOCTYPE_PRESENTATION -> "Slide"
                        else -> "Page"
                    }
                    val rawName = doc.getPartName(idx).trim()
                    val name = if (rawName.isNotBlank()) rawName else "$defaultPrefix ${idx + 1}"
                    OutlineItem(title = name, pageIndex = idx)
                }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            try {
                document?.destroy()
            } catch (_: Throwable) {}
            document = null

            try {
                office?.destroy()
            } catch (_: Throwable) {}
            office = null

            directBuffer = null
            directBufferCapacity = 0
            tileCache.clear()
        }
    }
}
