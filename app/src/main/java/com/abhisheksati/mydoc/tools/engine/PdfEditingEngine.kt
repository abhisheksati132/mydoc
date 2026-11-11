package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.ToolProgress
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.abhisheksati.mydoc.tools.pdf.pagenumber.PdfPageNumberer
import com.abhisheksati.mydoc.tools.pdf.watermark.PdfWatermarker
import com.abhisheksati.mydoc.tools.security.SignatureStorage
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.PDFAnnotation
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFPage
import com.artifex.mupdf.fitz.Rect
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Specialized engine handling non-destructive document editing, stamps, annotations,
 * page numbering, watermarking, and digital signatures.
 */
class PdfEditingEngine(private val context: Context) {

    private fun createTempOutputFile(prefix: String, ext: String): File =
        PdfToolUtils.createTempOutputFile(context.cacheDir, prefix, ext)

    suspend fun annotatePdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val text = options.annotationText.ifBlank { "APPROVED" }
            val isHighlight = options.annotationType.equals("HIGHLIGHT", ignoreCase = true)
            var applied = 0

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p) as? PDFPage ?: continue

                if (isHighlight) {
                    val quads = page.search(text)
                    if (quads != null && quads.isNotEmpty()) {
                        for (hit in quads) {
                            for (q in hit) {
                                val annot = page.createAnnotation(PDFAnnotation.TYPE_HIGHLIGHT)
                                annot.rect = Rect(q.ul_x, q.ul_y, q.lr_x, q.lr_y)
                                annot.color = floatArrayOf(1.0f, 0.9f, 0.2f)
                                annot.contents = text
                                annot.update()
                                applied++
                            }
                        }
                    }
                } else {
                    if (p == 0) {
                        val b = page.bounds
                        val annot = page.createAnnotation(PDFAnnotation.TYPE_STAMP)
                        val stampW = 160f
                        val stampH = 48f
                        val left = b.x1 - stampW - 36f
                        val top = b.y0 + 36f
                        annot.rect = Rect(left, top, left + stampW, top + stampH)
                        annot.contents = text
                        annot.color = when (text.uppercase()) {
                            "REJECTED" -> floatArrayOf(0.9f, 0.1f, 0.1f)
                            "CONFIDENTIAL" -> floatArrayOf(0.85f, 0.2f, 0.2f)
                            "DRAFT" -> floatArrayOf(0.9f, 0.5f, 0.1f)
                            else -> floatArrayOf(0.1f, 0.65f, 0.25f)
                        }
                        annot.update()
                        applied++
                    }
                }
                page.destroy()
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Applying annotations...", false))
            }

            if (applied == 0) {
                throw IllegalArgumentException("No match for \"$text\" was found. Nothing was annotated.")
            }

            val outFile = createTempOutputFile("annotated", "pdf")
            srcDoc.save(outFile.absolutePath, "compress")
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun addPageNumbers(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val outFile = createTempOutputFile("numbered", "pdf")
        PdfPageNumberer.apply(
            context = context,
            sourceFile = file,
            destinationFile = outFile,
            position = options.pageNumberPosition,
            format = options.pageNumberFormat,
            startAt = options.pageNumberStart
        ) { current, total ->
            val pct = 10 + ((current.toFloat() / total) * 85).toInt()
            onProgress(ToolProgress(current, total, pct, "Stamping page number on page $current of $total...", false))
        }
        return outFile
    }

    suspend fun addWatermark(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val outFile = createTempOutputFile("watermarked", "pdf")
        PdfWatermarker.applyText(
            context = context,
            sourceFile = file,
            destinationFile = outFile,
            text = options.watermarkText,
            opacity = options.watermarkOpacity,
            rotationDegrees = options.watermarkRotation
        ) { current, total ->
            val pct = 10 + ((current.toFloat() / total) * 85).toInt()
            onProgress(ToolProgress(current, total, pct, "Applying watermark to page $current of $total...", false))
        }
        return outFile
    }

    suspend fun signPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        // A stored-but-undecryptable signature is a different problem from never having saved
        // one, and the reader needs the other instruction.
        val sigBmp = SignatureStorage.loadSignature(context)
            ?: throw IllegalStateException(SignatureStorage.problemMessage(context) ?: "No signature found.")

        try {
            PdfBoxHelper.ensureInitialized(context)
            val outFile = createTempOutputFile("signed", "pdf")

            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    val totalPages = doc.numberOfPages
                    // A negative choice means the last page; a fixed sentinel signed page 1000 of a
                    // longer document instead.
                    val targetPageIdx =
                        if (options.signaturePage < 0) totalPages - 1
                        else options.signaturePage.coerceIn(0, totalPages - 1)
                    val pdImage = LosslessFactory.createFromImage(doc, sigBmp)

                    val page = doc.getPage(targetPageIdx)
                    val box = page.cropBox ?: page.mediaBox ?: PDRectangle(0f, 0f, 612f, 792f)
                    val w = box.width
                    val h = box.height
                    val originX = box.lowerLeftX
                    val originY = box.lowerLeftY
                    val rotation = (page.rotation ?: 0) % 360

                    val isRotated = rotation == 90 || rotation == 270
                    val visW = if (isRotated) h else w
                    val visH = if (isRotated) w else h

                    val sigW = (visW * options.signatureScale).coerceIn(80f, visW * 0.85f)
                    val sigH = sigW * (sigBmp.height.toFloat() / sigBmp.width.toFloat())
                    val vx = (visW * options.signatureXPercent) - (sigW / 2f)
                    val vy = (visH * options.signatureYPercent) - (sigH / 2f)

                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        when (rotation) {
                            90 -> {
                                cs.saveGraphicsState()
                                cs.transform(com.tom_roush.pdfbox.util.Matrix(0f, 1f, -1f, 0f, originX + w - vy, originY + vx))
                                cs.drawImage(pdImage, 0f, 0f, sigW, sigH)
                                cs.restoreGraphicsState()
                            }
                            180 -> {
                                cs.saveGraphicsState()
                                cs.transform(com.tom_roush.pdfbox.util.Matrix(-1f, 0f, 0f, -1f, originX + w - vx, originY + h - vy))
                                cs.drawImage(pdImage, 0f, 0f, sigW, sigH)
                                cs.restoreGraphicsState()
                            }
                            270 -> {
                                cs.saveGraphicsState()
                                cs.transform(com.tom_roush.pdfbox.util.Matrix(0f, -1f, 1f, 0f, originX + vy, originY + h - vx))
                                cs.drawImage(pdImage, 0f, 0f, sigW, sigH)
                                cs.restoreGraphicsState()
                            }
                            else -> {
                                cs.drawImage(pdImage, originX + vx, originY + vy, sigW, sigH)
                            }
                        }
                    }

                    FileOutputStream(outFile).use { fos -> doc.save(fos) }
                }
            }
            return outFile
        } finally {
            sigBmp.recycle()
        }
    }
}
