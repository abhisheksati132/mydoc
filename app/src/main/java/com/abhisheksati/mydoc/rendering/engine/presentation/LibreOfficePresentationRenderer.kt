package com.abhisheksati.mydoc.rendering.engine.presentation

import android.app.Activity
import android.graphics.Bitmap
import org.libreoffice.kit.Document
import org.libreoffice.kit.LibreOfficeKit
import org.libreoffice.kit.Office
import java.io.File
import java.nio.ByteBuffer

/**
 * Enterprise presentation rendering engine powered by embedded LibreOfficeKit.
 * Renders PPTX and legacy PPT slides with 100% desktop fidelity directly into high-res Bitmaps.
 */
class LibreOfficePresentationRenderer {

    companion object {
        fun isEngineAvailable(): Boolean {
            return LibreOfficeKit.isAvailable()
        }

        fun init(activity: Activity): Boolean {
            return LibreOfficeKit.init(activity)
        }
    }

    private var office: Office? = null
    private var document: Document? = null

    fun load(filePath: String): Boolean {
        try {
            val file = File(filePath)
            if (!file.exists()) return false

            val handle = LibreOfficeKit.getLibreOfficeKitHandle() ?: return false
            office = Office(handle)
            val docUrl = "file://${file.absolutePath}"
            document = office?.documentLoad(docUrl)
            return document != null
        } catch (_: Throwable) {
            return false
        }
    }

    fun getSlideCount(): Int {
        return try {
            document?.parts ?: 0
        } catch (_: Throwable) {
            0
        }
    }

    fun renderSlide(slideIndex: Int, targetWidth: Int = 1280, targetHeight: Int = 720): Bitmap? {
        val doc = document ?: return null
        return try {
            val w = targetWidth.coerceIn(320, 2560)
            val h = targetHeight.coerceIn(180, 1440)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val buffer = ByteBuffer.allocateDirect(w * h * 4)

            doc.setPart(slideIndex)
            val docWidth = doc.documentWidth.toInt()
            val docHeight = doc.documentHeight.toInt()

            doc.paintTile(buffer, w, h, 0, 0, docWidth, docHeight)
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    fun close() {
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
