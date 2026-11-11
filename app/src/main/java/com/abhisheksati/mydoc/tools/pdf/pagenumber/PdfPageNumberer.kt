package com.abhisheksati.mydoc.tools.pdf.pagenumber

import android.content.Context
import android.graphics.Color
import android.net.Uri
import com.abhisheksati.mydoc.tools.model.PageNumberFormat
import com.abhisheksati.mydoc.tools.model.PageNumberPosition
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.min

object PdfPageNumberer {

    fun apply(
        context: Context,
        sourceFile: File,
        destinationFile: File,
        position: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,
        format: PageNumberFormat = PageNumberFormat.PAGE_X_OF_Y,
        startAt: Int = 1,
        colorArgb: Int = 0xFF444444.toInt(),
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        PdfBoxHelper.ensureInitialized(context)
        val c = Color.valueOf(colorArgb)
        val font = PDType1Font.HELVETICA

        FileInputStream(sourceFile).use { input ->
            PDDocument.load(input).use { doc ->
                val total = doc.numberOfPages
                for (i in 0 until total) {
                    val page = doc.getPage(i)
                    val box = page.cropBox ?: page.mediaBox ?: continue
                    val w = box.width
                    val h = box.height
                    val originX = box.lowerLeftX
                    val originY = box.lowerLeftY
                    val pageRotation = (page.rotation ?: 0) % 360
                    val isRotated = pageRotation == 90 || pageRotation == 270
                    val visW = if (isRotated) h else w
                    val visH = if (isRotated) w else h
                    val fontSize = (min(visW, visH) * 0.018f).coerceIn(9f, 14f)
                    val margin = fontSize * 2.2f

                    val currNum = startAt + i
                    val totalDisplay = startAt + total - 1
                    val text = when (format) {
                        PageNumberFormat.PAGE_X_OF_Y -> "Page $currNum of $totalDisplay"
                        PageNumberFormat.X_OF_Y -> "$currNum / $totalDisplay"
                        PageNumberFormat.ONLY_X -> "$currNum"
                    }

                    val textWidth = font.getStringWidth(text) / 1000f * fontSize
                    val vx = when (position) {
                        PageNumberPosition.BOTTOM_CENTER, PageNumberPosition.TOP_CENTER -> visW / 2f - textWidth / 2f
                        PageNumberPosition.BOTTOM_RIGHT, PageNumberPosition.TOP_RIGHT -> visW - margin - textWidth
                        PageNumberPosition.BOTTOM_LEFT -> margin
                    }
                    val vy = when (position) {
                        PageNumberPosition.TOP_CENTER, PageNumberPosition.TOP_RIGHT -> visH - margin
                        else -> margin - fontSize / 2f
                    }

                    val matrix = when (pageRotation) {
                        90 -> com.tom_roush.pdfbox.util.Matrix(0f, 1f, -1f, 0f, originX + w - vy, originY + vx)
                        180 -> com.tom_roush.pdfbox.util.Matrix(-1f, 0f, 0f, -1f, originX + w - vx, originY + h - vy)
                        270 -> com.tom_roush.pdfbox.util.Matrix(0f, -1f, 1f, 0f, originX + vy, originY + h - vx)
                        else -> com.tom_roush.pdfbox.util.Matrix(1f, 0f, 0f, 1f, originX + vx, originY + vy)
                    }

                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.beginText()
                        cs.setNonStrokingColor(c.red(), c.green(), c.blue())
                        cs.setFont(font, fontSize)
                        cs.setTextMatrix(matrix)
                        runCatching { cs.showText(text) }
                        cs.endText()
                    }
                    onProgress?.invoke(i + 1, total)
                }

                FileOutputStream(destinationFile).use { output ->
                    doc.save(output)
                }
            }
        }
    }

    fun apply(
        context: Context,
        sourceUri: Uri,
        destinationUri: Uri,
        position: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,
        format: PageNumberFormat = PageNumberFormat.PAGE_X_OF_Y,
        startAt: Int = 1,
        colorArgb: Int = 0xFF444444.toInt(),
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        PdfBoxHelper.ensureInitialized(context)
        val c = Color.valueOf(colorArgb)
        val font = PDType1Font.HELVETICA

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            PDDocument.load(input).use { doc ->
                val total = doc.numberOfPages
                for (i in 0 until total) {
                    val page = doc.getPage(i)
                    val box = page.cropBox ?: page.mediaBox ?: continue
                    val w = box.width
                    val h = box.height
                    val originX = box.lowerLeftX
                    val originY = box.lowerLeftY
                    val pageRotation = (page.rotation ?: 0) % 360
                    val isRotated = pageRotation == 90 || pageRotation == 270
                    val visW = if (isRotated) h else w
                    val visH = if (isRotated) w else h
                    val fontSize = (min(visW, visH) * 0.018f).coerceIn(9f, 14f)
                    val margin = fontSize * 2.2f

                    val currNum = startAt + i
                    val totalDisplay = startAt + total - 1
                    val text = when (format) {
                        PageNumberFormat.PAGE_X_OF_Y -> "Page $currNum of $totalDisplay"
                        PageNumberFormat.X_OF_Y -> "$currNum / $totalDisplay"
                        PageNumberFormat.ONLY_X -> "$currNum"
                    }

                    val textWidth = font.getStringWidth(text) / 1000f * fontSize
                    val vx = when (position) {
                        PageNumberPosition.BOTTOM_CENTER, PageNumberPosition.TOP_CENTER -> visW / 2f - textWidth / 2f
                        PageNumberPosition.BOTTOM_RIGHT, PageNumberPosition.TOP_RIGHT -> visW - margin - textWidth
                        PageNumberPosition.BOTTOM_LEFT -> margin
                    }
                    val vy = when (position) {
                        PageNumberPosition.TOP_CENTER, PageNumberPosition.TOP_RIGHT -> visH - margin
                        else -> margin - fontSize / 2f
                    }

                    val matrix = when (pageRotation) {
                        90 -> com.tom_roush.pdfbox.util.Matrix(0f, 1f, -1f, 0f, originX + w - vy, originY + vx)
                        180 -> com.tom_roush.pdfbox.util.Matrix(-1f, 0f, 0f, -1f, originX + w - vx, originY + h - vy)
                        270 -> com.tom_roush.pdfbox.util.Matrix(0f, -1f, 1f, 0f, originX + vy, originY + h - vx)
                        else -> com.tom_roush.pdfbox.util.Matrix(1f, 0f, 0f, 1f, originX + vx, originY + vy)
                    }

                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.beginText()
                        cs.setNonStrokingColor(c.red(), c.green(), c.blue())
                        cs.setFont(font, fontSize)
                        cs.setTextMatrix(matrix)
                        runCatching { cs.showText(text) }
                        cs.endText()
                    }
                    onProgress?.invoke(i + 1, total)
                }

                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    doc.save(output)
                } ?: throw IllegalStateException("Unable to write PDF output")
            }
        } ?: throw IllegalStateException("Unable to read source PDF")
    }
}
