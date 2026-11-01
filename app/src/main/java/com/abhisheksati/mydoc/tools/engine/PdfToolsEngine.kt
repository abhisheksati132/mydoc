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
import com.abhisheksati.mydoc.tools.pdf.form.PdfFormService
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
                PdfTool.OFFICE_TO_PDF -> officeToPdf(requirePrimaryFile(), onProgress)
                PdfTool.TEXT_TO_PDF -> textOrCsvToPdf(requirePrimaryFile(), onProgress)

                // 4. Convert (from PDF)
                PdfTool.PDF_TO_IMAGES -> pdfToImagesZip(requirePrimaryFile(), options, onProgress)
                PdfTool.PDF_TO_TEXT_MD -> pdfToTextOrMarkdown(requirePrimaryFile(), onProgress)
                PdfTool.PDF_TO_OFFICE -> pdfToOfficeDocument(requirePrimaryFile(), onProgress)
                PdfTool.PDF_TO_PDFA -> pdfToPdfA(requirePrimaryFile(), onProgress)
                PdfTool.EXTRACT_ASSETS -> extractAssetsZip(requirePrimaryFile(), onProgress)

                // 5. Edit
                PdfTool.ANNOTATE -> annotatePdf(requirePrimaryFile(), options, onProgress)
                PdfTool.ADD_PAGE_NUMBERS -> addPageNumbers(requirePrimaryFile(), options, onProgress)
                PdfTool.ADD_WATERMARK -> addWatermark(requirePrimaryFile(), options, onProgress)
                PdfTool.FILL_FORM -> fillFormFields(requirePrimaryFile(), options, onProgress)
                PdfTool.SIGN_PDF -> signPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.COMPARE_PDF -> comparePdfs(inputFiles, onProgress)

                // 6. Security
                PdfTool.PROTECT -> protectPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.UNLOCK -> unlockPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.REDACT -> redactPdf(requirePrimaryFile(), options, onProgress)
                PdfTool.STRIP_METADATA -> stripMetadata(requirePrimaryFile(), onProgress)

                // 7. Scan & OCR
                PdfTool.SCAN_TO_PDF -> imagesToPdf(inputFiles, options, onProgress)
                PdfTool.OCR_PDF -> ocrPdf(requirePrimaryFile(), onProgress)
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
                    warningNote = "Compression saved only ${String.format("%.1f", savingsPercent.coerceAtLeast(0f))}%. Images in this document are already optimized."
                }
            } else if (tool == PdfTool.OFFICE_TO_PDF || tool == PdfTool.PDF_TO_OFFICE) {
                warningNote = "Best-effort offline conversion. Complex multi-column layouts and vector art will not match the original."
            } else if (tool == PdfTool.PDF_TO_PDFA) {
                warningNote = "PDF/A-style, not validated. Formatted for archival compatibility without online external profile verification."
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

    private suspend fun compressPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val originalSize = file.length()
        onProgress(ToolProgress(0, 100, 10, "Analyzing document streams and images...", false))

        // Strategy A: Recompress embedded image streams with PDFBox
        var totalImagesRecompressed = 0
        val boxOutFile = createTempOutputFile("compressed_box", "pdf")
        var pdfBoxSucceeded = false

        try {
            PdfBoxHelper.ensureInitialized(context)
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    val totalPages = doc.numberOfPages
                    val (maxDim, qualityFloat) = when (options.compressionLevel) {
                        CompressionLevel.LIGHT -> Pair(1600, 0.75f)
                        CompressionLevel.BALANCED -> Pair(1200, 0.60f)
                        CompressionLevel.STRONG -> Pair(800, 0.45f)
                    }

                    for (p in 0 until totalPages) {
                        currentCoroutineContext().ensureActive()
                        val page = doc.getPage(p)
                        val resources = page.resources ?: continue
                        for (name in resources.xObjectNames) {
                            if (resources.isImageXObject(name)) {
                                val xobj = resources.getXObject(name) as? PDImageXObject ?: continue
                                val origBmp = runCatching { xobj.image }.getOrNull() ?: continue
                                val w = origBmp.width
                                val h = origBmp.height

                                val scale = minOf(1.0f, maxDim.toFloat() / maxOf(w, h).toFloat())
                                val targetW = (w * scale).toInt().coerceAtLeast(1)
                                val targetH = (h * scale).toInt().coerceAtLeast(1)

                                val scaledBmp = if (targetW != w || targetH != h) {
                                    Bitmap.createScaledBitmap(origBmp, targetW, targetH, true)
                                } else {
                                    origBmp
                                }

                                val newImg = JPEGFactory.createFromImage(doc, scaledBmp, qualityFloat)
                                resources.put(name, newImg)
                                totalImagesRecompressed++

                                if (scaledBmp !== origBmp) {
                                    scaledBmp.recycle()
                                }
                            }
                        }
                        val pct = 10 + ((p.toFloat() / totalPages) * 35).toInt()
                        onProgress(ToolProgress(p + 1, totalPages, pct, "Optimizing image streams...", false))
                    }

                    if (totalImagesRecompressed > 0) {
                        FileOutputStream(boxOutFile).use { fos -> doc.save(fos) }
                        pdfBoxSucceeded = true
                    }
                }
            }
        } catch (_: Exception) {
            pdfBoxSucceeded = false
        }

        // Strategy B: Scanned / photo-heavy page compression via MuPDF & AndroidPdfDocument
        val rasterOutFile = createTempOutputFile("compressed_raster", "pdf")
        var rasterSucceeded = false

        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
        if (srcDoc != null) {
            try {
                val totalPages = srcDoc.countPages()
                val (renderScale, jpegQuality) = when (options.compressionLevel) {
                    CompressionLevel.LIGHT -> Pair(1.5f, 75)
                    CompressionLevel.BALANCED -> Pair(1.15f, 60)
                    CompressionLevel.STRONG -> Pair(0.85f, 45)
                }

                val outDoc = AndroidPdfDocument()
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    val b = page.bounds
                    val w = (b.x1 - b.x0).toInt().coerceAtLeast(1)
                    val h = (b.y1 - b.y0).toInt().coerceAtLeast(1)

                    val pixmap = page.toPixmap(Matrix(renderScale, renderScale), ColorSpace.DeviceRGB, false)
                    val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                    pixmap.destroy()
                    page.destroy()

                    val baos = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
                    bmp.recycle()

                    val pageInfo = AndroidPdfDocument.PageInfo.Builder(w, h, p + 1).create()
                    val aPage = outDoc.startPage(pageInfo)
                    val compressedBmp = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size())
                    if (compressedBmp != null) {
                        aPage.canvas.drawBitmap(compressedBmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                        compressedBmp.recycle()
                    }
                    outDoc.finishPage(aPage)

                    val pct = 45 + ((p.toFloat() / totalPages) * 45).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Compressing page ${p + 1} of $totalPages...", false))
                }

                FileOutputStream(rasterOutFile).use { outDoc.writeTo(it) }
                outDoc.close()
                rasterSucceeded = true
            } catch (_: Exception) {
                rasterSucceeded = false
            }

            // Strategy C: Pure object and xref compaction via MuPDF
            val cleanOutFile = createTempOutputFile("compressed_clean", "pdf")
            val cleanParams = when (options.compressionLevel) {
                CompressionLevel.LIGHT -> "compress,clean,garbage=2"
                CompressionLevel.BALANCED -> "compress,deflate,clean,garbage=3"
                CompressionLevel.STRONG -> "compress,deflate,clean,garbage=4"
            }
            try {
                srcDoc.save(cleanOutFile.absolutePath, cleanParams)
            } catch (_: Exception) {}
            srcDoc.destroy()

            onProgress(ToolProgress(100, 100, 95, "Selecting most efficient compression...", false))

            // Choose the smallest resulting file that is smaller than original!
            val candidates = mutableListOf<File>()
            if (pdfBoxSucceeded && boxOutFile.exists() && boxOutFile.length() > 0 && boxOutFile.length() < originalSize) {
                candidates.add(boxOutFile)
            }
            if (rasterSucceeded && rasterOutFile.exists() && rasterOutFile.length() > 0 && rasterOutFile.length() < originalSize * 0.90f) {
                candidates.add(rasterOutFile)
            }
            if (cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() < originalSize) {
                candidates.add(cleanOutFile)
            }

            val bestFile = candidates.minByOrNull { it.length() }
            if (bestFile != null) {
                if (bestFile != boxOutFile && boxOutFile.exists()) boxOutFile.delete()
                if (bestFile != rasterOutFile && rasterOutFile.exists()) rasterOutFile.delete()
                if (bestFile != cleanOutFile && cleanOutFile.exists()) cleanOutFile.delete()
                return bestFile
            }

            if (cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() <= originalSize) {
                if (boxOutFile.exists()) boxOutFile.delete()
                if (rasterOutFile.exists()) rasterOutFile.delete()
                return cleanOutFile
            }
        }

        if (boxOutFile.exists() && boxOutFile.length() > 0) return boxOutFile
        return file
    }

    private suspend fun repairPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        onProgress(ToolProgress(1, 100, 20, "Rebuilding damaged xref tables...", false))
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
        if (srcDoc != null) {
            try {
                val totalPages = srcDoc.countPages()
                val destDoc = PDFDocument()

                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    try {
                        destDoc.graftPage(destDoc.countPages(), srcDoc, p)
                    } catch (_: Exception) {
                        // Skip corrupt page gracefully
                    }
                    val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Recovering page ${p + 1}...", false))
                }

                val outFile = createTempOutputFile("repaired", "pdf")
                destDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
                destDoc.destroy()
                return outFile
            } finally {
                srcDoc.destroy()
            }
        } else {
            // Secondary PDFBox recovery fallback
            PdfBoxHelper.ensureInitialized(context)
            val outFile = createTempOutputFile("repaired_box", "pdf")
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    FileOutputStream(outFile).use { fos -> doc.save(fos) }
                }
            }
            return outFile
        }
    }
    // -------------------------------------------------------------
    // 3. Convert (to & from PDF)
    // -------------------------------------------------------------

    private val conversionEngine by lazy { PdfConversionEngine(context) }

    private suspend fun imagesToPdf(files: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.imagesToPdf(files, options, onProgress)

    private suspend fun officeToPdf(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.officeToPdf(file, onProgress)

    private suspend fun textOrCsvToPdf(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.textOrCsvToPdf(file, onProgress)

    private suspend fun renderTextToPdf(lines: List<String>, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.renderTextToPdf(lines, onProgress)

    private suspend fun pdfToImagesZip(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.pdfToImagesZip(file, options, onProgress)

    private suspend fun pdfToTextOrMarkdown(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.pdfToTextOrMarkdown(file, onProgress)

    private suspend fun pdfToOfficeDocument(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.pdfToOfficeDocument(file, onProgress)

    private suspend fun pdfToPdfA(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.pdfToPdfA(file, onProgress)

    private suspend fun extractAssetsZip(file: File, onProgress: (ToolProgress) -> Unit): File =
        conversionEngine.extractAssetsZip(file, onProgress)


    // -------------------------------------------------------------
    // 5. Edit (Non-Destructive)
    // -------------------------------------------------------------

    private suspend fun annotatePdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val text = options.annotationText.ifBlank { "APPROVED" }
            val isHighlight = options.annotationType.equals("HIGHLIGHT", ignoreCase = true)

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p) as? PDFPage ?: continue

                if (isHighlight) {
                    val quads = srcDoc.search(p, 0, text)
                    if (quads != null && quads.isNotEmpty()) {
                        for (hit in quads) {
                            for (q in hit) {
                                val annot = page.createAnnotation(PDFAnnotation.TYPE_HIGHLIGHT)
                                annot.rect = Rect(q.ul_x, q.ul_y, q.lr_x, q.lr_y)
                                annot.color = floatArrayOf(1.0f, 0.9f, 0.2f)
                                annot.contents = text
                                annot.update()
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
                    }
                }
                page.destroy()
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Applying annotations...", false))
            }

            val outFile = createTempOutputFile("annotated", "pdf")
            srcDoc.save(outFile.absolutePath, "compress")
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun addPageNumbers(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
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

    private suspend fun addWatermark(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val outFile = createTempOutputFile("watermarked", "pdf")
        val isDiagonal = kotlin.math.abs(options.watermarkRotation) > 10f
        PdfWatermarker.applyText(
            context = context,
            sourceFile = file,
            destinationFile = outFile,
            text = options.watermarkText,
            opacity = options.watermarkOpacity,
            diagonal = isDiagonal
        ) { current, total ->
            val pct = 10 + ((current.toFloat() / total) * 85).toInt()
            onProgress(ToolProgress(current, total, pct, "Applying watermark to page $current of $total...", false))
        }
        return outFile
    }

    private suspend fun fillFormFields(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val outFile = createTempOutputFile("filled", "pdf")
        onProgress(ToolProgress(10, 100, 30, "Applying form values...", false))
        PdfFormService.fill(
            context = context,
            sourceFile = file,
            destinationFile = outFile,
            values = options.formFieldValues,
            flatten = options.flattenForm
        )
        onProgress(ToolProgress(100, 100, 95, "Form saved successfully.", false))
        return outFile
    }

    private suspend fun signPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val sigBmp = SignatureStorage.loadSignature(context)
            ?: throw IllegalStateException("No signature found. Please draw or import your signature first.")

        PdfBoxHelper.ensureInitialized(context)
        val outFile = createTempOutputFile("signed", "pdf")

        FileInputStream(file).use { fis ->
            PDDocument.load(fis).use { doc ->
                val totalPages = doc.numberOfPages
                val targetPageIdx = options.signaturePage.coerceIn(0, totalPages - 1)
                val pdImage = LosslessFactory.createFromImage(doc, sigBmp)

                val page = doc.getPage(targetPageIdx)
                val box = page.cropBox ?: page.mediaBox ?: PDRectangle(0f, 0f, 612f, 792f)
                val w = box.width
                val h = box.height
                val originX = box.lowerLeftX
                val originY = box.lowerLeftY

                val sigW = (w * options.signatureScale).coerceIn(80f, w * 0.85f)
                val sigH = sigW * (sigBmp.height.toFloat() / sigBmp.width.toFloat())
                val sigX = originX + (w * options.signatureXPercent) - (sigW / 2f)
                val sigY = originY + (h * options.signatureYPercent) - (sigH / 2f)

                PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    cs.drawImage(pdImage, sigX, sigY, sigW, sigH)
                }

                FileOutputStream(outFile).use { fos -> doc.save(fos) }
            }
        }
        sigBmp.recycle()
        return outFile
    }

    private suspend fun comparePdfs(files: List<File>, onProgress: (ToolProgress) -> Unit): File {
        if (files.size < 2) throw IllegalArgumentException("Comparison requires 2 documents.")
        val doc1 = Document.openDocument(files[0].absolutePath)
        val doc2 = Document.openDocument(files[1].absolutePath)

        try {
            val pages1 = doc1.countPages()
            val pages2 = doc2.countPages()
            val maxPages = maxOf(pages1, pages2)

            val reportLines = mutableListOf<String>()
            reportLines.add("PDF Comparison Report")
            reportLines.add("Generated by Stacks")
            reportLines.add("----------------------------------------")
            reportLines.add("Document A: ${files[0].name} ($pages1 pages)")
            reportLines.add("Document B: ${files[1].name} ($pages2 pages)")
            reportLines.add("----------------------------------------")
            reportLines.add("")

            var totalDifferences = 0
            for (p in 0 until maxPages) {
                currentCoroutineContext().ensureActive()
                val text1 = if (p < pages1) extractPageText(doc1, p) else ""
                val text2 = if (p < pages2) extractPageText(doc2, p) else ""

                if (text1 == text2) {
                    reportLines.add("Page ${p + 1}: Identical")
                } else {
                    totalDifferences++
                    reportLines.add("Page ${p + 1}: Differences Detected")
                    val words1 = text1.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
                    val words2 = text2.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
                    val removed = words1 - words2
                    val added = words2 - words1

                    if (removed.isNotEmpty()) reportLines.add("  - Removed: ${removed.take(12).joinToString(", ")}")
                    if (added.isNotEmpty()) reportLines.add("  + Added: ${added.take(12).joinToString(", ")}")
                }
                reportLines.add("")

                val pct = 10 + (((p + 1).toFloat() / maxPages) * 70).toInt()
                onProgress(ToolProgress(p + 1, maxPages, pct, "Comparing page ${p + 1} of $maxPages...", false))
            }

            reportLines.add("----------------------------------------")
            reportLines.add("Summary: $totalDifferences page(s) with differences detected.")

            return renderTextToPdf(reportLines, onProgress)
        } finally {
            doc1.destroy()
            doc2.destroy()
        }
    }

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
    // 7. Scan & OCR
    // -------------------------------------------------------------

    private suspend fun ocrPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
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

    // -------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------

    private fun extractPageText(doc: Document, pageIdx: Int): String {
        return try {
            val page = doc.loadPage(pageIdx)
            val st = page.toStructuredText()
            val sb = StringBuilder()
            for (b in st.blocks) {
                for (l in b.lines) {
                    for (c in l.chars) sb.append(c.c.toChar())
                    sb.append(" ")
                }
            }
            st.destroy()
            page.destroy()
            sb.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

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
        } catch (_: Exception) {
            0
        }
    }

    private fun parsePageRanges(rangeStr: String, maxPages: Int): List<Int> =
        PdfToolUtils.parsePageRanges(rangeStr, maxPages)
}
