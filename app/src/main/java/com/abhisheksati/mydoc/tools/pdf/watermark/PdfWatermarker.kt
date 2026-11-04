package com.abhisheksati.mydoc.tools.pdf.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
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
        rotationDegrees: Float = 45f,
        colorArgb: Int = 0xFF8A8A8A.toInt(),
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Watermark text cannot be empty." }

        PdfBoxHelper.ensureInitialized(context)
        val alpha = opacity.coerceIn(0.05f, 1f)
        val c = Color.valueOf(colorArgb)
        val isAsciiOnly = trimmed.all { it.code in 32..126 }

        FileInputStream(sourceFile).use { input ->
            PDDocument.load(input).use { doc ->
                val total = doc.numberOfPages

                if (isAsciiOnly) {
                    val font = PDType1Font.HELVETICA_BOLD
                    for (i in 0 until total) {
                        val page = doc.getPage(i)
                        val boxRect = page.cropBox ?: page.mediaBox ?: continue
                        val w = boxRect.width
                        val h = boxRect.height
                        val originX = boxRect.lowerLeftX
                        val originY = boxRect.lowerLeftY

                        val fontSize = (min(w, h) * 0.11f).coerceIn(18f, 120f)
                        val textWidth = font.getStringWidth(trimmed) / 1000f * fontSize
                        val cx = originX + w / 2f
                        val cy = originY + h / 2f

                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            cs.saveGraphicsState()
                            val gs = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = alpha }
                            cs.setGraphicsStateParameters(gs)
                            cs.setNonStrokingColor(c.red(), c.green(), c.blue())
                            cs.beginText()
                            cs.setFont(font, fontSize)
                            val rad = Math.toRadians(rotationDegrees.toDouble())
                            cs.setTextMatrix(Matrix.getRotateInstance(rad, cx, cy))
                            cs.newLineAtOffset(-textWidth / 2f, -fontSize / 2.6f)
                            cs.showText(trimmed)
                            cs.endText()
                            cs.restoreGraphicsState()
                        }
                        onProgress?.invoke(i + 1, total)
                    }
                } else {
                    // Render Unicode text to bitmap for broad script & character support
                    val textBmp = renderUnicodeTextToBitmap(trimmed, colorArgb, 1.0f)
                    val pdImage = LosslessFactory.createFromImage(doc, textBmp)
                    textBmp.recycle()

                    for (i in 0 until total) {
                        val page = doc.getPage(i)
                        val boxRect = page.cropBox ?: page.mediaBox ?: continue
                        val w = boxRect.width
                        val h = boxRect.height
                        val originX = boxRect.lowerLeftX
                        val originY = boxRect.lowerLeftY

                        val targetW = (min(w, h) * 0.7f).coerceIn(1f, w.coerceAtLeast(1f))
                        val targetH = targetW * (pdImage.height.toFloat() / pdImage.width.toFloat())
                        val cx = originX + w / 2f
                        val cy = originY + h / 2f

                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            cs.saveGraphicsState()
                            val gs = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = alpha }
                            cs.setGraphicsStateParameters(gs)
                            val rad = Math.toRadians(rotationDegrees.toDouble())
                            cs.transform(Matrix.getRotateInstance(rad, cx, cy))
                            cs.drawImage(pdImage, -targetW / 2f, -targetH / 2f, targetW, targetH)
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

    fun applyText(
        context: Context,
        sourceFile: File,
        destinationFile: File,
        text: String,
        opacity: Float = 0.25f,
        diagonal: Boolean,
        colorArgb: Int = 0xFF8A8A8A.toInt(),
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) = applyText(
        context = context,
        sourceFile = sourceFile,
        destinationFile = destinationFile,
        text = text,
        opacity = opacity,
        rotationDegrees = if (diagonal) 45f else 0f,
        colorArgb = colorArgb,
        onProgress = onProgress
    )

    private fun renderUnicodeTextToBitmap(text: String, colorArgb: Int, alpha: Float): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            this.alpha = (alpha * 255).toInt().coerceIn(10, 255)
            textSize = 96f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val pad = 36
        val w = bounds.width() + pad * 2
        val h = bounds.height() + pad * 2
        val bitmap = Bitmap.createBitmap(maxOf(1, w), maxOf(1, h), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val xPos = canvas.width / 2f
        val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(text, xPos, yPos, paint)
        return bitmap
    }
}
