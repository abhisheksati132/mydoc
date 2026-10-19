package com.abhisheksati.mydoc.rendering.engine.presentation

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.cache.TileBitmapCache
import org.libreoffice.kit.Document
import org.libreoffice.kit.LibreOfficeKit
import org.libreoffice.kit.Office
import java.io.File
import java.nio.ByteBuffer

/**
 * Enterprise presentation rendering engine powered by embedded LibreOfficeKit.
 * Renders PPTX and legacy PPT slides with 100% desktop fidelity directly into high-res Bitmaps.
 */
class LibreOfficePresentationRenderer(
    private val tileCache: TileBitmapCache = TileBitmapCache()
) {

    companion object {
        private const val TAG = "LibreOfficePresRenderer"

        fun isEngineAvailable(): Boolean {
            return LibreOfficeKit.isAvailable()
        }

        fun init(activity: Activity): Boolean {
            return LibreOfficeKit.init(activity)
        }
    }

    private val lock = Any()
    private var office: Office? = null
    private var document: Document? = null
    private var _slideCount: Int = 0
    private var _aspectRatio: Float = 16f / 9f

    private var directBuffer: ByteBuffer? = null
    private var directBufferCapacity: Int = 0

    fun load(filePath: String): Boolean {
        synchronized(lock) {
            try {
                val file = File(filePath)
                if (!file.exists()) return false

                val handle = LibreOfficeKit.getLibreOfficeKitHandle() ?: return false
                val off = Office(handle)
                office = off

                val docUrl = "file://${file.absolutePath}"
                val doc = off.documentLoad(docUrl) ?: return false
                document = doc

                // Ensure slide mode
                try {
                    doc.setPartMode(Document.PART_MODE_SLIDE)
                } catch (_: Throwable) {}

                _slideCount = doc.parts.coerceAtLeast(0)

                val w = doc.documentWidth.toFloat()
                val h = doc.documentHeight.toFloat()
                _aspectRatio = if (w > 0f && h > 0f) w / h else 16f / 9f

                return _slideCount > 0
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load presentation in LibreOffice: $filePath", t)
                return false
            }
        }
    }

    fun getSlideCount(): Int = synchronized(lock) { _slideCount }

    fun getAspectRatio(): Float = synchronized(lock) { _aspectRatio }

    fun getPartName(slideIndex: Int): String = synchronized(lock) {
        val doc = document ?: return@synchronized "Slide ${slideIndex + 1}"
        try {
            val name = doc.getPartName(slideIndex).trim()
            if (name.isNotBlank()) name else "Slide ${slideIndex + 1}"
        } catch (_: Throwable) {
            "Slide ${slideIndex + 1}"
        }
    }

    fun renderSlide(
        slideIndex: Int,
        targetWidth: Int = 1280,
        targetHeight: Int = 720,
        mode: ReadingMode = ReadingMode.NORMAL
    ): Bitmap? {
        val cached = tileCache.get(slideIndex, 1.0f, mode)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return synchronized(lock) {
            val doc = document ?: return@synchronized null
            try {
                val w = targetWidth.coerceIn(320, 2560)
                val h = targetHeight.coerceIn(180, 1440)
                val requiredBytes = w * h * 4

                val buffer = getOrCreateDirectBuffer(requiredBytes)
                buffer.clear()
                buffer.limit(requiredBytes)

                doc.setPart(slideIndex)
                val docWidth = doc.documentWidth.toInt().coerceAtLeast(100)
                val docHeight = doc.documentHeight.toInt().coerceAtLeast(100)

                doc.paintTile(buffer, w, h, 0, 0, docWidth, docHeight)
                buffer.rewind()

                val rawBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                rawBitmap.copyPixelsFromBuffer(buffer)

                val finalBitmap = applyReadingMode(rawBitmap, mode)
                tileCache.put(slideIndex, 1.0f, mode, finalBitmap)
                finalBitmap
            } catch (t: Throwable) {
                Log.e(TAG, "Error rendering slide $slideIndex", t)
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

        if (output != src && !src.isRecycled) {
            src.recycle()
        }
        return output
    }

    fun close() {
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
            _slideCount = 0
        }
    }
}
