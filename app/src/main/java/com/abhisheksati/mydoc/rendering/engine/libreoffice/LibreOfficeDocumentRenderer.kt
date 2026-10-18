package com.abhisheksati.mydoc.rendering.engine.libreoffice

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.engine.DocumentRenderer
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.rendering.engine.SearchMatch
import org.libreoffice.kit.Document
import org.libreoffice.kit.LibreOfficeKit
import org.libreoffice.kit.Office
import java.io.File
import java.nio.ByteBuffer

class LibreOfficeDocumentRenderer(
    private val filePath: String
) : DocumentRenderer {

    companion object {
        private const val TAG = "LibreOfficeDocRenderer"
    }

    private var office: Office? = null
    private var document: Document? = null
    private var _pageCount: Int = 1

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

        val off = Office(handle)
        office = off

        val docUrl = "file://${file.absolutePath}"
        val doc = off.documentLoad(docUrl)
            ?: throw IllegalStateException("LibreOffice could not load document: ${off.error ?: "Unknown error"}")

        document = doc
        _pageCount = doc.parts.coerceAtLeast(1)
    }

    override val pageCount: Int
        get() = _pageCount

    override val isPasswordProtected: Boolean
        get() = false

    override fun authenticatePassword(password: String): Boolean {
        return true
    }

    override fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        val doc = document ?: return Pair(595f, 842f)
        return try {
            doc.part = pageIndex
            val w = doc.documentWidth.toFloat()
            val h = doc.documentHeight.toFloat()
            if (w > 0f && h > 0f) Pair(w, h) else Pair(595f, 842f)
        } catch (_: Throwable) {
            Pair(595f, 842f)
        }
    }

    override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val doc = document ?: return null
        return try {
            doc.part = pageIndex
            val baseW = doc.documentWidth.toInt().coerceAtLeast(400)
            val baseH = doc.documentHeight.toInt().coerceAtLeast(400)

            val targetW = ((baseW * scale).toInt()).coerceIn(320, 3840)
            val targetH = ((baseH * scale).toInt()).coerceIn(320, 3840)

            val buffer = ByteBuffer.allocateDirect(targetW * targetH * 4)
            doc.paintTile(buffer, targetW, targetH, 0, 0, baseW, baseH)
            buffer.rewind()

            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            applyReadingMode(bitmap, mode)
        } catch (t: Throwable) {
            Log.e(TAG, "Error rendering page $pageIndex", t)
            null
        }
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
        return output
    }

    override fun search(query: String): List<SearchMatch> {
        return emptyList()
    }

    override fun getOutline(): List<OutlineItem> {
        val doc = document ?: return emptyList()
        return try {
            (0 until doc.parts).map { idx ->
                val name = doc.getPartName(idx).ifBlank { "Section ${idx + 1}" }
                OutlineItem(title = name, pageIndex = idx)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override fun close() {
        try {
            document?.destroy()
        } catch (_: Throwable) {}
        document = null

        try {
            office?.destroy()
        } catch (_: Throwable) {}
        office = null
    }
}
