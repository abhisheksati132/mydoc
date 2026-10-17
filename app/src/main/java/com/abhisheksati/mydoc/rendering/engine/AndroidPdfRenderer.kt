package com.abhisheksati.mydoc.rendering.engine

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
import java.io.File

/**
 * Standard Android OS built-in PDF Renderer.
 * Zero native library dependencies, guaranteed to work on all Android devices (API 21+).
 */
class AndroidPdfRenderer(
    private val file: File,
    private val tileCache: TileBitmapCache = TileBitmapCache()
) : DocumentRenderer {

    private val lock = Any()
    private var pfd: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    init {
        synchronized(lock) {
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pfd?.let {
                    pdfRenderer = PdfRenderer(it)
                }
            } catch (_: Exception) {
            }
        }
    }

    override val isPasswordProtected: Boolean
        get() = false

    override fun authenticatePassword(password: String): Boolean = false

    override val pageCount: Int
        get() = synchronized(lock) { pdfRenderer?.pageCount ?: 0 }

    override fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        return synchronized(lock) {
            val renderer = pdfRenderer ?: return@synchronized Pair(595f, 842f)
            if (pageIndex < 0 || pageIndex >= (pdfRenderer?.pageCount ?: 0)) return@synchronized Pair(595f, 842f)
            try {
                val page = renderer.openPage(pageIndex)
                val w = page.width.toFloat()
                val h = page.height.toFloat()
                page.close()
                Pair(w, h)
            } catch (_: Exception) {
                Pair(595f, 842f)
            }
        }
    }

    override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val cached = tileCache.get(pageIndex, scale, mode)
        if (cached != null && !cached.isRecycled) return cached

        return synchronized(lock) {
            val renderer = pdfRenderer ?: return@synchronized null
            if (pageIndex < 0 || pageIndex >= (pdfRenderer?.pageCount ?: 0)) return@synchronized null

            try {
                val page = renderer.openPage(pageIndex)
                val origWidth = page.width
                val origHeight = page.height

                val scaledWidth = (origWidth * scale).toInt().coerceAtLeast(1)
                val scaledHeight = (origHeight * scale).toInt().coerceAtLeast(1)

                val baseBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(baseBitmap)
                canvas.drawColor(Color.WHITE)

                page.render(baseBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val finalBitmap = when (mode) {
                    ReadingMode.INVERTED_DARK -> invertBitmapColors(baseBitmap)
                    ReadingMode.SEPIA -> applySepiaFilter(baseBitmap)
                    ReadingMode.NORMAL -> baseBitmap
                }

                tileCache.put(pageIndex, scale, mode, finalBitmap)
                finalBitmap
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun search(query: String): List<SearchMatch> = emptyList()

    override fun getOutline(): List<OutlineItem> = emptyList()

    override fun close() {
        synchronized(lock) {
            tileCache.clear()
            try {
                pdfRenderer?.close()
            } catch (_: Exception) {
            }
            try {
                pfd?.close()
            } catch (_: Exception) {
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
    }
}
