package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import android.graphics.Bitmap
import com.abhisheksati.mydoc.ocr.OcrEngine
import com.abhisheksati.mydoc.tools.model.ToolProgress
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.PDFDocument
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Specialized engine handling optical character recognition (OCR) and searchable PDF synthesis.
 */
class PdfScanOcrEngine(private val context: Context) {

    private fun createTempOutputFile(prefix: String, ext: String): File =
        PdfToolUtils.createTempOutputFile(context.cacheDir, prefix, ext)

    suspend fun ocrPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        PdfBoxHelper.ensureInitialized(context)
        val ocrEngine = OcrEngine(context)
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")

        val outFile = createTempOutputFile("ocr_searchable", "pdf")

        try {
            val totalPages = srcDoc.countPages()
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    val font = PDType1Font.HELVETICA
                    for (p in 0 until totalPages) {
                        currentCoroutineContext().ensureActive()
                        val page = srcDoc.loadPage(p)
                        val pixmap = page.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, false)
                        val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                        pixmap.destroy()
                        page.destroy()

                        val recognizedText = runCatching { ocrEngine.recognizeText(bmp) }.getOrDefault("")
                        bmp.recycle()

                        if (recognizedText.isNotBlank() && p < doc.numberOfPages) {
                            val pdPage = doc.getPage(p)
                            val box = pdPage.cropBox ?: pdPage.mediaBox ?: continue
                            val margin = 36f
                            var curY = box.upperRightY - margin

                            PDPageContentStream(doc, pdPage, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                cs.saveGraphicsState()
                                val gs = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = 0.01f }
                                cs.setGraphicsStateParameters(gs)
                                cs.beginText()
                                cs.setFont(font, 8f)

                                for (line in recognizedText.lines()) {
                                    val clean = line.replace("\t", " ").trim()
                                    if (clean.isNotBlank() && curY > box.lowerLeftY + margin) {
                                        cs.newLineAtOffset(box.lowerLeftX + margin, curY)
                                        runCatching { cs.showText(clean.take(120)) }
                                        cs.newLineAtOffset(-(box.lowerLeftX + margin), -curY)
                                        curY -= 12f
                                    }
                                }
                                cs.endText()
                                cs.restoreGraphicsState()
                            }
                        }

                        val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                        onProgress(ToolProgress(p + 1, totalPages, pct, "OCR processing page ${p + 1} of $totalPages...", false))
                    }

                    FileOutputStream(outFile).use { fos -> doc.save(fos) }
                }
            }
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }
}
