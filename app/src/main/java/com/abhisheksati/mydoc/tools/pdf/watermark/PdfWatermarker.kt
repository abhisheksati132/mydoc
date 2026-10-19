package com.abhisheksati.mydoc.tools.pdf.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.min

object PdfWatermarker {

    fun applyText(
        context: Context,
        sourceFile: File,
        destinationFile: File,
        text: String,
        opacity: Float = 0.25f,
        diagonal: Boolean = true,
        colorArgb: Int = 0xFF8A8A8A.toInt(),
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        PdfBoxHelper.ensureInitialized(context)
        val sanitized = sanitize(text)
        val c = Color.valueOf(colorArgb)
        val alpha = opacity.coerceIn(0.05f, 1f)

        FileInputStream(sourceFile).use { input ->
            PDDocument.load(input).use { doc ->
                if (sanitized.isNotBlank()) {
                    val font = PDType1Font.HELVETICA_BOLD
                    val total = doc.numberOfPages
                    for (i in 0 until total) {
                        val page = doc.getPage(i)
                        val boxRect = page.cropBox ?: page.mediaBox ?: continue
                        val w = boxRect.width
                        val h = boxRect.height
                        val originX = boxRect.lowerLeftX
                        val originY = boxRect.lowerLeftY

                        val fontSize = (min(w, h) * 0.11f).coerceIn(18f, 120f)
                        val textWidth = font.getStringWidth(sanitized) / 1000f * fontSize
                        val cx = originX + w / 2f
                        val cy = originY + h / 2f

                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            cs.saveGraphicsState()
                            val gs = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = alpha }
                            cs.setGraphicsStateParameters(gs)
                            cs.setNonStrokingColor(c.red(), c.green(), c.blue())
                            cs.beginText()
                            cs.setFont(font, fontSize)
                            if (diagonal) {
                                cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(45.0), cx, cy))
                                cs.newLineAtOffset(-textWidth / 2f, -fontSize / 2.6f)
                            } else {
                                cs.newLineAtOffset(cx - textWidth / 2f, cy - fontSize / 2.6f)
                            }
                            runCatching { cs.showText(sanitized) }
                            cs.endText()
                            cs.restoreGraphicsState()
                        }
                        onProgress?.invoke(i + 1, total)
                    }
                }
                FileOutputStream(destinationFile).use { output ->
                    doc.save(output)
                }
            }
        }
    }

    fun applyImage(
        context: Context,
        sourceFile: File,
        destinationFile: File,
        bitmap: Bitmap,
        opacity: Float = 0.3f,
        scaleFraction: Float = 0.5f,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        PdfBoxHelper.ensureInitialized(context)
        val alpha = opacity.coerceIn(0.05f, 1f)

        FileInputStream(sourceFile).use { input ->
            PDDocument.load(input).use { doc ->
                val pdImage = LosslessFactory.createFromImage(doc, bitmap)
                val total = doc.numberOfPages
                for (i in 0 until total) {
                    val page = doc.getPage(i)
                    val boxRect = page.cropBox ?: page.mediaBox ?: continue
                    val w = boxRect.width
                    val h = boxRect.height
                    val originX = boxRect.lowerLeftX
                    val originY = boxRect.lowerLeftY

                    val targetW = w * scaleFraction.coerceIn(0.1f, 0.9f)
                    val targetH = targetW * (bitmap.height.toFloat() / bitmap.width.toFloat())
                    val x = originX + (w - targetW) / 2f
                    val y = originY + (h - targetH) / 2f

                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.saveGraphicsState()
                        val gs = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = alpha }
                        cs.setGraphicsStateParameters(gs)
                        cs.drawImage(pdImage, x, y, targetW, targetH)
                        cs.restoreGraphicsState()
                    }
                    onProgress?.invoke(i + 1, total)
                }
                FileOutputStream(destinationFile).use { output ->
                    doc.save(output)
                }
            }
        }
    }

    private fun sanitize(raw: String): String {
        return raw.map { ch -> if (ch.code in 32..126) ch else ' ' }.joinToString("").trim()
    }
}
