package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import android.net.Uri
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetParser
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import com.abhisheksati.mydoc.tools.model.CompressionLevel
import com.abhisheksati.mydoc.tools.model.PageMargin
import com.abhisheksati.mydoc.tools.model.PageNumberFormat
import com.abhisheksati.mydoc.tools.model.PageNumberPosition
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolExecutionResult
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.ToolProgress
import com.abhisheksati.mydoc.tools.pdf.pagenumber.PdfPageNumberer
import com.abhisheksati.mydoc.tools.pdf.watermark.PdfWatermarker
import com.abhisheksati.mydoc.ocr.OcrEngine
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.abhisheksati.mydoc.tools.security.SignatureStorage
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.pdmodel.common.PDMetadata
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDOutputIntent
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.PDFAnnotation
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFPage
import com.artifex.mupdf.fitz.Rect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Robust, fully offline PDF Toolkit Engine implementing all 30 tools.
 * Strict safety rules:
 * 1. Never modifies original files.
 * 2. Background processing with cooperative cancellation.
 * 3. Page-by-page streaming to avoid OOM.
 * 4. Automatic temporary file cleanup in finally blocks.
 */
class PdfToolsEngine(private val context: Context) {

    suspend fun executeTool(
        tool: PdfTool,
        inputUris: List<Uri>,
        options: ToolOptions,
        onProgress: (ToolProgress) -> Unit
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val tempFilesToDelete = mutableListOf<File>()

        try {
            if (inputUris.isEmpty()) {
                return@withContext ToolExecutionResult(
                    success = false,
                    tool = tool,
                    errorMessage = "No input file provided."
                )
            }

            onProgress(ToolProgress(0, 100, 5, "Preparing input files...", false))

            // Copy URIs to temp files safely
            val inputFiles = inputUris.map { uri ->
                currentCoroutineContext().ensureActive()
                val temp = copyUriToTemp(uri)
                tempFilesToDelete.add(temp)
                temp
            }

            val primaryFile = inputFiles.firstOrNull()
            fun requirePrimaryFile(): File = primaryFile
                ?: throw IllegalArgumentException("Primary input file is required for ${tool.title}")

            val initialSizeBytes = inputFiles.sumOf { it.length() }
            val initialPages = primaryFile?.let { countDocumentPages(it) } ?: 0

            onProgress(ToolProgress(10, 100, 15, "Executing ${tool.title}...", false))

            val resultFile: File = when (tool) {
                // 1. Organize
                PdfTool.MERGE -> mergePdfs(inputFiles, options, onProgress)
                PdfTool.SPLIT -> splitPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.REMOVE_PAGES -> removePages(requirePrimaryFile(), options, onProgress)
                PdfTool.REORDER_PAGES -> reorderPages(requirePrimaryFile(), options, onProgress)
                PdfTool.EXTRACT_PAGES -> extractPages(requirePrimaryFile(), options, onProgress)
                PdfTool.ROTATE -> rotatePdf(requirePrimaryFile(), options, onProgress)
                PdfTool.CROP -> cropPdf(requirePrimaryFile(), options, onProgress)

                // 2. Optimize
                PdfTool.COMPRESS -> compressPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.REPAIR -> repairPdf(requirePrimaryFile(), onProgress)

                // 3. Convert (to PDF)
                PdfTool.IMAGE_TO_PDF -> imagesToPdf(inputFiles, options, onProgress)
                PdfTool.TEXT_TO_PDF -> textOrCsvToPdf(requirePrimaryFile(), onProgress)

                // 4. Convert (from PDF)
                PdfTool.PDF_TO_IMAGES -> pdfToImagesZip(requirePrimaryFile(), options, onProgress)
                PdfTool.PDF_TO_TEXT_MD -> pdfToTextOrMarkdown(requirePrimaryFile(), onProgress)
                PdfTool.PDF_TO_OFFICE -> pdfToOfficeDocument(requirePrimaryFile(), onProgress)
                PdfTool.EXTRACT_ASSETS -> extractAssetsZip(requirePrimaryFile(), onProgress)

                // 5. Edit
                PdfTool.ANNOTATE -> annotatePdf(requirePrimaryFile(), options, onProgress)
                PdfTool.ADD_PAGE_NUMBERS -> addPageNumbers(requirePrimaryFile(), options, onProgress)
                PdfTool.ADD_WATERMARK -> addWatermark(requirePrimaryFile(), options, onProgress)
                PdfTool.SIGN_PDF -> signPdf(requirePrimaryFile(), options, onProgress)

                // 6. Security
                PdfTool.PROTECT -> protectPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.UNLOCK -> unlockPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.REDACT -> redactPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.STRIP_METADATA -> stripMetadata(requirePrimaryFile(), onProgress)

                // 7. Scan
                PdfTool.SCAN_TO_PDF -> imagesToPdf(inputFiles, options, onProgress)
            }

            currentCoroutineContext().ensureActive()
            onProgress(ToolProgress(100, 100, 100, "Completed.", false))

            val finalSizeBytes = resultFile.length()
            val finalPages = if (resultFile.extension.equals("pdf", ignoreCase = true)) {
                countDocumentPages(resultFile)
            } else {
                initialPages
            }

            var warningNote: String? = null
            if (tool == PdfTool.COMPRESS) {
                val savingsPercent = if (initialSizeBytes > 0) {
                    ((initialSizeBytes - finalSizeBytes).toFloat() / initialSizeBytes.toFloat()) * 100f
                } else 0f
                if (savingsPercent < 5.0f) {
                    warningNote = "Compression saved only " +
                        "${String.format(java.util.Locale.US, "%.1f", savingsPercent.coerceAtLeast(0f))}%." +
                        " Images in this document are already optimized."
                }
            } else if (tool == PdfTool.PDF_TO_OFFICE) {
                warningNote = "Best-effort offline conversion. Complex multi-column layouts and vector art will not match the original."
            }

            ToolExecutionResult(
                success = true,
                tool = tool,
                inputSizeBytes = initialSizeBytes,
                outputSizeBytes = finalSizeBytes,
                inputPages = initialPages,
                outputPages = finalPages,
                outputFile = resultFile,
                warningNote = warningNote,
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            val msg = if (t is OutOfMemoryError) {
                "Device ran out of memory while processing document. Try a smaller file or close background apps."
            } else {
                t.localizedMessage ?: "Processing error occurred."
            }
            ToolExecutionResult(
                success = false,
                tool = tool,
                errorMessage = msg,
                durationMs = System.currentTimeMillis() - startTime
            )
        } finally {
            // Safe cleanup of temporary input copies
            for (f in tempFilesToDelete) {
                try {
                    if (f.exists()) f.delete()
                } catch (_: Exception) {}
            }
        }
    }

    // -------------------------------------------------------------
    // 1. Organize
    // -------------------------------------------------------------

    private val pageOperationsEngine by lazy { PdfPageOperationsEngine(context) }

    private suspend fun mergePdfs(files: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        pageOperationsEngine.mergePdfs(files, options, onProgress)

    private suspend fun splitPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        pageOperationsEngine.splitPdf(file, options, onProgress)

    private suspend fun removePages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        pageOperationsEngine.removePages(file, options, onProgress)

    private suspend fun reorderPages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        pageOperationsEngine.reorderPages(file, options, onProgress)

    private suspend fun extractPages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        pageOperationsEngine.extractPages(file, options, onProgress)

    private suspend fun rotatePdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        pageOperationsEngine.rotatePdf(file, options, onProgress)

    private suspend fun cropPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        pageOperationsEngine.cropPdf(file, options, onProgress)

    // -------------------------------------------------------------
    // 2. Optimize
    // -------------------------------------------------------------

    private val optimizationEngine by lazy { PdfOptimizationEngine(context) }

    private suspend fun compressPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        optimizationEngine.compressPdf(file, options, onProgress)

    private suspend fun repairPdf(file: File, onProgress: (ToolProgress) -> Unit): File =
        optimizationEngine.repairPdf(file, onProgress)

    // -------------------------------------------------------------
    // 3. Convert (to & from PDF)
    // -------------------------------------------------------------

    private val conversionEngine by lazy { PdfConversionEngine(context) }

    private suspend fun imagesToPdf(files: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.imagesToPdf(files, options, onProgress)

    private suspend fun textOrCsvToPdf(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.textOrCsvToPdf(file, onProgress)

    private suspend fun pdfToImagesZip(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.pdfToImagesZip(file, options, onProgress)

    private suspend fun pdfToTextOrMarkdown(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.pdfToTextOrMarkdown(file, onProgress)

    private suspend fun pdfToOfficeDocument(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.pdfToOfficeDocument(file, onProgress)

    private suspend fun extractAssetsZip(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.extractAssetsZip(file, onProgress)


    // -------------------------------------------------------------
    // 5. Edit (Non-Destructive)
    // -------------------------------------------------------------

    private val editingEngine by lazy { PdfEditingEngine(context) }

    private suspend fun annotatePdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        editingEngine.annotatePdf(file, options, onProgress)

    private suspend fun addPageNumbers(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        editingEngine.addPageNumbers(file, options, onProgress)

    private suspend fun addWatermark(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        editingEngine.addWatermark(file, options, onProgress)

    private suspend fun signPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        editingEngine.signPdf(file, options, onProgress)

    // -------------------------------------------------------------
    // 6. Security
    // -------------------------------------------------------------

    private val securityEngine by lazy { PdfSecurityEngine(context) }

    private suspend fun protectPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        securityEngine.protectPdf(file, options, onProgress)

    private suspend fun unlockPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        securityEngine.unlockPdf(file, options, onProgress)

    private suspend fun redactPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        securityEngine.redactPdf(file, options, onProgress)

    private suspend fun stripMetadata(file: File, onProgress: (ToolProgress) -> Unit): File =
        securityEngine.stripMetadata(file, onProgress)

    // -------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------


    private fun copyUriToTemp(uri: Uri): File {
        val temp = File.createTempFile("mydoc_tool_in_", ".tmp", context.cacheDir)
        val stream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri)
            ?: context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot read URI $uri")
        stream.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        }
        return temp
    }

    private fun createTempOutputFile(prefix: String, ext: String): File {
        val toolsDir = File(context.cacheDir, "tools_output").apply { if (!exists()) mkdirs() }
        return File.createTempFile("mydoc_${prefix}_", ".$ext", toolsDir)
    }

    private fun countDocumentPages(file: File): Int {
        return try {
            val doc = Document.openDocument(file.absolutePath)
            val pages = doc.countPages()
            doc.destroy()
            pages
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            0
        }
    }

    private fun parsePageRanges(rangeStr: String, maxPages: Int): List<Int> =
        PdfToolUtils.parsePageRanges(rangeStr, maxPages)
}
