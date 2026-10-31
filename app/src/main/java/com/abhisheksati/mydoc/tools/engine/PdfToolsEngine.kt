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
            val initialSizeBytes = inputFiles.sumOf { it.length() }
            val initialPages = primaryFile?.let { countDocumentPages(it) } ?: 0

            onProgress(ToolProgress(10, 100, 15, "Executing ${tool.title}...", false))

            val resultFile: File = when (tool) {
                // 1. Organize
                PdfTool.MERGE -> mergePdfs(inputFiles, options, onProgress)
                PdfTool.SPLIT -> splitPdf(primaryFile!!, options, onProgress)
                PdfTool.REMOVE_PAGES -> removePages(primaryFile!!, options, onProgress)
                PdfTool.REORDER_PAGES -> reorderPages(primaryFile!!, options, onProgress)
                PdfTool.EXTRACT_PAGES -> extractPages(primaryFile!!, options, onProgress)
                PdfTool.ROTATE -> rotatePdf(primaryFile!!, options, onProgress)
                PdfTool.CROP -> cropPdf(primaryFile!!, options, onProgress)

                // 2. Optimize
                PdfTool.COMPRESS -> compressPdf(primaryFile!!, options, onProgress)
                PdfTool.REPAIR -> repairPdf(primaryFile!!, onProgress)

                // 3. Convert (to PDF)
                PdfTool.IMAGE_TO_PDF -> imagesToPdf(inputFiles, options, onProgress)
                PdfTool.OFFICE_TO_PDF -> officeToPdf(primaryFile!!, onProgress)
                PdfTool.TEXT_TO_PDF -> textOrCsvToPdf(primaryFile!!, onProgress)

                // 4. Convert (from PDF)
                PdfTool.PDF_TO_IMAGES -> pdfToImagesZip(primaryFile!!, options, onProgress)
                PdfTool.PDF_TO_TEXT_MD -> pdfToTextOrMarkdown(primaryFile!!, onProgress)
                PdfTool.PDF_TO_OFFICE -> pdfToOfficeDocument(primaryFile!!, onProgress)
                PdfTool.PDF_TO_PDFA -> pdfToPdfA(primaryFile!!, onProgress)
                PdfTool.EXTRACT_ASSETS -> extractAssetsZip(primaryFile!!, onProgress)

                // 5. Edit
                PdfTool.ANNOTATE -> annotatePdf(primaryFile!!, options, onProgress)
                PdfTool.ADD_PAGE_NUMBERS -> addPageNumbers(primaryFile!!, options, onProgress)
                PdfTool.ADD_WATERMARK -> addWatermark(primaryFile!!, options, onProgress)
                PdfTool.FILL_FORM -> fillFormFields(primaryFile!!, options, onProgress)
                PdfTool.SIGN_PDF -> signPdf(primaryFile!!, options, onProgress)
                PdfTool.COMPARE_PDF -> comparePdfs(inputFiles, onProgress)

                // 6. Security
                PdfTool.PROTECT -> protectPdf(primaryFile!!, options, onProgress)
                PdfTool.UNLOCK -> unlockPdf(primaryFile!!, options, onProgress)
                PdfTool.REDACT -> redactPdf(primaryFile!!, options, onProgress)
                PdfTool.STRIP_METADATA -> stripMetadata(primaryFile!!, onProgress)

                // 7. Scan & OCR
                PdfTool.SCAN_TO_PDF -> imagesToPdf(inputFiles, options, onProgress)
                PdfTool.OCR_PDF -> ocrPdf(primaryFile!!, onProgress)
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

    private suspend fun mergePdfs(files: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val destDoc = PDFDocument()
        val totalFiles = files.size

        files.forEachIndexed { fileIndex, file ->
            currentCoroutineContext().ensureActive()
            val progressPercent = 20 + ((fileIndex.toFloat() / totalFiles) * 70).toInt()
            onProgress(ToolProgress(fileIndex + 1, totalFiles, progressPercent, "Merging document ${fileIndex + 1} of $totalFiles...", false))

            val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
                ?: throw IllegalArgumentException("Cannot open ${file.name} as a valid PDF")
            try {
                val pageCount = srcDoc.countPages()
                val selectedIndices = parsePageRanges(options.pageRange, pageCount)
                for (p in selectedIndices) {
                    currentCoroutineContext().ensureActive()
                    destDoc.graftPage(destDoc.countPages(), srcDoc, p)
                }
            } finally {
                srcDoc.destroy()
            }
        }

        val outFile = createTempOutputFile("merged", "pdf")
        destDoc.save(outFile.absolutePath, "compress,garbage=3")
        destDoc.destroy()
        return outFile
    }

    private suspend fun splitPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val step = options.splitEveryNPages.coerceAtLeast(1)

            // If split results in multiple parts, package into ZIP
            val partsCount = (totalPages + step - 1) / step
            if (partsCount > 1) {
                val zipFile = createTempOutputFile("split_parts", "zip")
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    var partNum = 1
                    var pageIndex = 0
                    while (pageIndex < totalPages) {
                        currentCoroutineContext().ensureActive()
                        val subDoc = PDFDocument()
                        val endIndex = (pageIndex + step).coerceAtMost(totalPages)
                        for (p in pageIndex until endIndex) {
                            subDoc.graftPage(subDoc.countPages(), srcDoc, p)
                        }

                        val partFile = createTempOutputFile("part_${partNum}", "pdf")
                        subDoc.save(partFile.absolutePath, "compress")
                        subDoc.destroy()

                        // Add to zip
                        zos.putNextEntry(ZipEntry(String.format("part_%03d.pdf", partNum)))
                        partFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                        partFile.delete()

                        partNum++
                        pageIndex = endIndex
                        val pct = 20 + ((pageIndex.toFloat() / totalPages) * 75).toInt()
                        onProgress(ToolProgress(pageIndex, totalPages, pct, "Splitting pages $pageIndex of $totalPages...", false))
                    }
                }
                return zipFile
            } else {
                // Single extracted part
                val outDoc = PDFDocument()
                val indices = parsePageRanges(options.pageRange, totalPages)
                for (p in indices) {
                    outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                }
                val outFile = createTempOutputFile("split", "pdf")
                outDoc.save(outFile.absolutePath, "compress")
                outDoc.destroy()
                return outFile
            }
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun removePages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val toRemove = parsePageRanges(options.pageRange, totalPages).toSet()
            if (toRemove.size >= totalPages) {
                throw IllegalArgumentException("Cannot remove all pages from document.")
            }

            val outDoc = PDFDocument()
            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                if (p !in toRemove) {
                    outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                }
            }
            onProgress(ToolProgress(1, 1, 85, "Saving pruned PDF...", false))
            val outFile = createTempOutputFile("pruned", "pdf")
            outDoc.save(outFile.absolutePath, "compress,clean,garbage=3")
            outDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun reorderPages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val order = if (options.reorderedPageIndices.isNotEmpty()) {
                options.reorderedPageIndices.filter { it in 0 until totalPages }
            } else {
                (0 until totalPages).toList()
            }

            val outDoc = PDFDocument()
            order.forEachIndexed { idx, p ->
                currentCoroutineContext().ensureActive()
                outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                val pct = 20 + ((idx.toFloat() / order.size) * 70).toInt()
                onProgress(ToolProgress(idx + 1, order.size, pct, "Reordering page ${idx + 1}...", false))
            }

            val outFile = createTempOutputFile("reordered", "pdf")
            outDoc.save(outFile.absolutePath, "compress,garbage=2")
            outDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun extractPages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val indices = parsePageRanges(options.pageRange, totalPages)
            if (indices.isEmpty()) {
                throw IllegalArgumentException("No valid pages specified for extraction.")
            }

            val outDoc = PDFDocument()
            indices.forEachIndexed { i, p ->
                currentCoroutineContext().ensureActive()
                outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                val pct = 20 + ((i.toFloat() / indices.size) * 70).toInt()
                onProgress(ToolProgress(i + 1, indices.size, pct, "Extracting page ${i + 1}...", false))
            }

            val outFile = createTempOutputFile("extracted", "pdf")
            outDoc.save(outFile.absolutePath, "compress")
            outDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun rotatePdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        PdfBoxHelper.ensureInitialized(context)
        val outFile = createTempOutputFile("rotated", "pdf")
        val degrees = options.rotateDegrees

        FileInputStream(file).use { input ->
            PDDocument.load(input).use { doc ->
                val totalPages = doc.numberOfPages
                val indices = if (options.pageRange.isNotBlank()) {
                    parsePageRanges(options.pageRange, totalPages).toSet()
                } else {
                    (0 until totalPages).toSet()
                }

                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    if (p in indices) {
                        val page = doc.getPage(p)
                        page.rotation = (page.rotation + degrees) % 360
                    }
                    val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Rotating page ${p + 1} of $totalPages...", false))
                }

                FileOutputStream(outFile).use { out -> doc.save(out) }
            }
        }
        return outFile
    }

    private suspend fun cropPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        PdfBoxHelper.ensureInitialized(context)
        val outFile = createTempOutputFile("cropped", "pdf")
        val marginFrac = options.cropMarginPercent.coerceIn(0.01f, 0.45f)

        FileInputStream(file).use { input ->
            PDDocument.load(input).use { doc ->
                val totalPages = doc.numberOfPages
                val indices = if (options.pageRange.isNotBlank()) {
                    parsePageRanges(options.pageRange, totalPages).toSet()
                } else {
                    (0 until totalPages).toSet()
                }

                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    if (p in indices) {
                        val page = doc.getPage(p)
                        val box = page.cropBox ?: page.mediaBox ?: continue
                        val insetX = box.width * marginFrac
                        val insetY = box.height * marginFrac
                        val newCrop = PDRectangle(
                            box.lowerLeftX + insetX,
                            box.lowerLeftY + insetY,
                            box.width - (2f * insetX),
                            box.height - (2f * insetY)
                        )
                        page.cropBox = newCrop
                    }
                    val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Cropping page ${p + 1} of $totalPages...", false))
                }

                FileOutputStream(outFile).use { out -> doc.save(out) }
            }
        }
        return outFile
    }

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
    // 3. Convert (to PDF)
    // -------------------------------------------------------------

    private suspend fun imagesToPdf(files: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val outDoc = AndroidPdfDocument()
        val total = files.size

        files.forEachIndexed { index, imgFile ->
            currentCoroutineContext().ensureActive()
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                ?: return@forEachIndexed

            val (pageWidth, pageHeight) = when (options.pageSizeFormat) {
                PageSizeFormat.A4 -> Pair(PageSizeFormat.A4.widthPt.toInt(), PageSizeFormat.A4.heightPt.toInt())
                PageSizeFormat.LETTER -> Pair(PageSizeFormat.LETTER.widthPt.toInt(), PageSizeFormat.LETTER.heightPt.toInt())
                PageSizeFormat.FIT_IMAGE -> Pair(bitmap.width, bitmap.height)
            }

            val margin = options.pageMargin.marginPt
            val availableW = (pageWidth - 2 * margin).coerceAtLeast(10f)
            val availableH = (pageHeight - 2 * margin).coerceAtLeast(10f)

            val pageInfo = AndroidPdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = outDoc.startPage(pageInfo)
            val canvas = page.canvas

            val scale = minOf(availableW / bitmap.width.toFloat(), availableH / bitmap.height.toFloat())
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val left = margin + (availableW - drawW) / 2f
            val top = margin + (availableH - drawH) / 2f

            canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), null)
            bitmap.recycle()
            outDoc.finishPage(page)

            val pct = 10 + (((index + 1).toFloat() / total) * 80).toInt()
            onProgress(ToolProgress(index + 1, total, pct, "Adding image ${index + 1} of $total...", false))
        }

        val outFile = createTempOutputFile("converted", "pdf")
        FileOutputStream(outFile).use { outDoc.writeTo(it) }
        outDoc.close()
        return outFile
    }

    private suspend fun officeToPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        onProgress(ToolProgress(1, 100, 20, "Extracting document content...", false))
        val ext = file.extension.lowercase()

        val textBlocks = mutableListOf<String>()
        if (ext == "docx") {
            val parsed = FileInputStream(file).use { DocxParser().parse(it) }
            for (b in parsed.blocks) {
                when (b) {
                    is DocxBlock.Heading -> textBlocks.add("## ${b.text}")
                    is DocxBlock.Paragraph -> textBlocks.add(b.runs.joinToString("") { it.text })
                    is DocxBlock.Bullet -> textBlocks.add("• " + b.runs.joinToString("") { it.text })
                    is DocxBlock.Table -> {
                        for (r in b.rows) textBlocks.add(r.joinToString(" | "))
                    }
                }
            }
        } else if (ext == "xlsx" || ext == "xls") {
            val parsed = FileInputStream(file).use { SpreadsheetParser().parse(it, ext) }
            for (sheet in parsed.sheets) {
                textBlocks.add("[Sheet: ${sheet.name}]")
                val maxR = sheet.maxRow.coerceAtMost(200)
                val maxC = sheet.maxCol.coerceAtMost(25)
                for (r in 0..maxR) {
                    val rowCells = mutableListOf<String>()
                    for (c in 0..maxC) {
                        val v = sheet.getCellValue(r, c)
                        if (v.isNotBlank()) rowCells.add(v)
                    }
                    if (rowCells.isNotEmpty()) {
                        textBlocks.add(rowCells.joinToString(" | "))
                    }
                }
            }
        } else if (ext == "pptx") {
            val parsed = FileInputStream(file).use { PptxParser().parse(it) }
            for (slide in parsed.slides) {
                textBlocks.add("[Slide ${slide.slideNumber}: ${slide.title}]")
                textBlocks.addAll(slide.content)
            }
        }

        // Layout extracted blocks into standard paginated PDF
        return renderTextToPdf(textBlocks, onProgress)
    }

    private suspend fun textOrCsvToPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        val lines = file.readLines()
        return renderTextToPdf(lines, onProgress)
    }

    private suspend fun renderTextToPdf(lines: List<String>, onProgress: (ToolProgress) -> Unit): File {
        val outDoc = AndroidPdfDocument()
        val pageWidth = PageSizeFormat.A4.widthPt.toInt()
        val pageHeight = PageSizeFormat.A4.heightPt.toInt()
        val margin = 54f // 0.75 in
        val printableWidth = pageWidth - (2 * margin)

        val headingPaint = Paint().apply {
            color = Color.rgb(0x1A, 0x73, 0xE8)
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        fun wrapLine(text: String, paint: Paint, maxW: Float): List<String> {
            if (text.isBlank()) return listOf("")
            if (paint.measureText(text) <= maxW) return listOf(text)

            val words = text.split("\\s+".toRegex())
            val wrapped = mutableListOf<String>()
            val cur = StringBuilder()

            for (w in words) {
                val candidate = if (cur.isEmpty()) w else "$cur $w"
                if (paint.measureText(candidate) <= maxW) {
                    cur.setLength(0)
                    cur.append(candidate)
                } else {
                    if (cur.isNotEmpty()) wrapped.add(cur.toString())
                    if (paint.measureText(w) > maxW) {
                        var chunk = w
                        while (paint.measureText(chunk) > maxW && chunk.length > 1) {
                            val splitIdx = (chunk.length * (maxW / paint.measureText(chunk))).toInt().coerceIn(1, chunk.length - 1)
                            wrapped.add(chunk.substring(0, splitIdx))
                            chunk = chunk.substring(splitIdx)
                        }
                        cur.setLength(0)
                        cur.append(chunk)
                    } else {
                        cur.setLength(0)
                        cur.append(w)
                    }
                }
            }
            if (cur.isNotEmpty()) wrapped.add(cur.toString())
            return wrapped
        }

        val effectiveLines = if (lines.isEmpty()) listOf("[Document has no extractable text content]") else lines

        var pageNum = 1
        var currentPage = outDoc.startPage(AndroidPdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = currentPage.canvas
        var currentY = margin + 18f

        val totalLines = effectiveLines.size
        effectiveLines.forEachIndexed { idx, rawLine ->
            currentCoroutineContext().ensureActive()

            val isHeading = rawLine.startsWith("##") || rawLine.startsWith("[Sheet:") || rawLine.startsWith("[Slide")
            val paint = if (isHeading) headingPaint else bodyPaint
            val lineHeight = if (isHeading) 22f else 17f

            val wrappedParts = wrapLine(rawLine, paint, printableWidth)
            for (part in wrappedParts) {
                if (currentY + lineHeight > pageHeight - margin) {
                    outDoc.finishPage(currentPage)
                    pageNum++
                    currentPage = outDoc.startPage(AndroidPdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = currentPage.canvas
                    currentY = margin + lineHeight
                }

                canvas.drawText(part, margin, currentY, paint)
                currentY += lineHeight
            }

            if (idx % 20 == 0) {
                val pct = 20 + ((idx.toFloat() / totalLines) * 70).toInt()
                onProgress(ToolProgress(idx, totalLines, pct, "Writing page $pageNum...", false))
            }
        }

        outDoc.finishPage(currentPage)
        val outFile = createTempOutputFile("text_converted", "pdf")
        FileOutputStream(outFile).use { outDoc.writeTo(it) }
        outDoc.close()
        return outFile
    }

    // -------------------------------------------------------------
    // 4. Convert (from PDF)
    // -------------------------------------------------------------

    private suspend fun pdfToImagesZip(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val isJpg = options.exportFormatJpg
            val ext = if (isJpg) "jpg" else "png"
            val compressFormat = if (isJpg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            val quality = if (isJpg) 85 else 100
            val scale = if (options.exportDpi >= 300) 3.0f else 1.5f

            if (options.exportAsLongImage) {
                val renderedBitmaps = mutableListOf<Bitmap>()
                var totalHeight = 0
                var maxWidth = 0
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    val pixmap = page.toPixmap(Matrix(scale, scale), ColorSpace.DeviceRGB, false)
                    val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                    renderedBitmaps.add(bmp)
                    maxWidth = maxOf(maxWidth, bmp.width)
                    totalHeight += bmp.height
                    pixmap.destroy()
                    page.destroy()
                    val pct = 10 + (((p + 1).toFloat() / totalPages) * 60).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Rendering page ${p + 1} of $totalPages for stitch...", false))
                }

                val stitchedBitmap = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(stitchedBitmap)
                canvas.drawColor(Color.WHITE)
                var currentY = 0f
                for (bmp in renderedBitmaps) {
                    val left = (maxWidth - bmp.width) / 2f
                    canvas.drawBitmap(bmp, left, currentY, null)
                    currentY += bmp.height
                    bmp.recycle()
                }

                val outFile = createTempOutputFile("stitched_long_image", ext)
                FileOutputStream(outFile).use { fos ->
                    stitchedBitmap.compress(compressFormat, quality, fos)
                }
                stitchedBitmap.recycle()
                return outFile
            }

            val zipFile = createTempOutputFile("rendered_images", "zip")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    val pixmap = page.toPixmap(Matrix(scale, scale), ColorSpace.DeviceRGB, false)
                    val bitmap = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)

                    zos.putNextEntry(ZipEntry(String.format("page_%03d.$ext", p + 1)))
                    bitmap.compress(compressFormat, quality, zos)
                    zos.closeEntry()

                    bitmap.recycle()
                    pixmap.destroy()
                    page.destroy()

                    val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Rendering page ${p + 1} of $totalPages...", false))
                }
            }
            return zipFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun pdfToTextOrMarkdown(file: File, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val sb = StringBuilder()
            sb.append("# ${file.nameWithoutExtension}\n\n")

            var hasAnyText = false
            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val st = page.toStructuredText()
                val pageSb = StringBuilder()

                for (block in st.blocks) {
                    for (line in block.lines) {
                        for (charItem in line.chars) {
                            pageSb.append(charItem.c.toChar())
                        }
                        pageSb.append("\n")
                    }
                    pageSb.append("\n")
                }
                st.destroy()
                page.destroy()

                val pageStr = pageSb.toString().trim()
                if (pageStr.isNotBlank()) {
                    hasAnyText = true
                    sb.append("## Page ${p + 1}\n\n").append(pageStr).append("\n\n")
                }

                val pct = 10 + (((p + 1).toFloat() / totalPages) * 70).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Extracting text from page ${p + 1}...", false))
            }

            // OCR fallback for scanned or image-only documents
            if (!hasAnyText) {
                val ocrEngine = OcrEngine(context)
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    val pixmap = page.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, false)
                    val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                    pixmap.destroy()
                    page.destroy()

                    val ocrText = runCatching { ocrEngine.recognizeText(bmp) }.getOrDefault("")
                    bmp.recycle()

                    if (ocrText.isNotBlank()) {
                        sb.append("## Page ${p + 1} (OCR)\n\n").append(ocrText).append("\n\n")
                    }
                    val pct = 70 + (((p + 1).toFloat() / totalPages) * 25).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Extracting text via OCR from page ${p + 1}...", false))
                }
            }

            val outFile = createTempOutputFile("extracted", "md")
            outFile.writeText(sb.toString())
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private fun sanitizeXml(text: String): String {
        return text.filter { ch ->
            ch == '\t' || ch == '\n' || ch == '\r' || (ch.code in 0x20..0xD7FF) || (ch.code in 0xE000..0xFFFD)
        }.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
    }

    private suspend fun pdfToOfficeDocument(file: File, onProgress: (ToolProgress) -> Unit): File {
        // Generates an offline valid OOXML .docx with structured paragraphs
        val srcDoc = Document.openDocument(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val paragraphs = mutableListOf<String>()

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val st = page.toStructuredText()
                for (b in st.blocks) {
                    val pText = StringBuilder()
                    for (l in b.lines) {
                        for (c in l.chars) pText.append(c.c.toChar())
                        pText.append(" ")
                    }
                    if (pText.isNotBlank()) paragraphs.add(pText.toString().trim())
                }
                st.destroy()
                page.destroy()

                val pct = 10 + (((p + 1).toFloat() / totalPages) * 70).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Converting page ${p + 1}...", false))
            }

            // OCR fallback for scanned or image-only PDFs
            if (paragraphs.isEmpty()) {
                val ocrEngine = OcrEngine(context)
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    val pixmap = page.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, false)
                    val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                    pixmap.destroy()
                    page.destroy()

                    val ocrText = runCatching { ocrEngine.recognizeText(bmp) }.getOrDefault("")
                    bmp.recycle()

                    for (l in ocrText.lines()) {
                        val trimmed = l.trim()
                        if (trimmed.isNotBlank()) paragraphs.add(trimmed)
                    }

                    val pct = 70 + (((p + 1).toFloat() / totalPages) * 25).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Extracting text via OCR from page ${p + 1}...", false))
                }
            }

            if (paragraphs.isEmpty()) {
                paragraphs.add("[Document has no extractable text content]")
            }

            // Create valid OOXML DOCX ZIP
            val outFile = createTempOutputFile("converted", "docx")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
                // [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray())
                zos.closeEntry()

                // _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray())
                zos.closeEntry()

                // word/document.xml
                zos.putNextEntry(ZipEntry("word/document.xml"))
                val docXml = StringBuilder()
                docXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>""")
                for (para in paragraphs) {
                    val escaped = sanitizeXml(para)
                    docXml.append("<w:p><w:r><w:t xml:space=\"preserve\">$escaped</w:t></w:r></w:p>")
                }
                docXml.append("""  </w:body>
</w:document>""")
                zos.write(docXml.toString().toByteArray())
                zos.closeEntry()
            }
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun pdfToPdfA(file: File, onProgress: (ToolProgress) -> Unit): File {
        onProgress(ToolProgress(1, 100, 30, "Injecting PDF/A-1b metadata schema...", false))
        PdfBoxHelper.ensureInitialized(context)
        val boxOutFile = createTempOutputFile("pdfa_temp", "pdf")

        try {
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    val cat = doc.documentCatalog ?: throw IllegalStateException("Invalid PDF catalog")

                    val xmp = """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <rdf:Description rdf:about="" xmlns:pdfaid="http://www.aiim.org/pdfa/ns/id/">
      <pdfaid:part>1</pdfaid:part>
      <pdfaid:conformance>B</pdfaid:conformance>
    </rdf:Description>
    <rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/">
      <dc:format>application/pdf</dc:format>
      <dc:title>
        <rdf:Alt>
          <rdf:li xml:lang="x-default">${sanitizeXml(file.nameWithoutExtension)}</rdf:li>
        </rdf:Alt>
      </dc:title>
    </rdf:Description>
    <rdf:Description rdf:about="" xmlns:pdf="http://ns.adobe.com/pdf/1.3/">
      <pdf:Producer>MyDoc Archival Engine (PDF/A-1b Profile)</pdf:Producer>
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>"""

                    val metadataStream = PDMetadata(doc)
                    metadataStream.createOutputStream().use { os ->
                        os.write(xmp.toByteArray(Charsets.UTF_8))
                    }
                    cat.metadata = metadataStream

                    doc.documentInformation?.producer = "Stacks Archival Engine (PDF/A-1b Profile)"

                    FileOutputStream(boxOutFile).use { fos -> doc.save(fos) }
                }
            }

            // Compact xref table
            val srcDoc = Document.openDocument(boxOutFile.absolutePath) as? PDFDocument
            if (srcDoc != null) {
                try {
                    val finalOut = createTempOutputFile("pdfa_archival", "pdf")
                    srcDoc.save(finalOut.absolutePath, "compress,clean,garbage=3")
                    return finalOut
                } finally {
                    srcDoc.destroy()
                    try { if (boxOutFile.exists()) boxOutFile.delete() } catch (_: Exception) {}
                }
            }
            return boxOutFile
        } catch (_: Exception) {
            val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
                ?: throw IllegalArgumentException("Cannot open document")
            try {
                srcDoc.setMetaData(Document.META_INFO_PRODUCER, "Stacks Archival Engine (PDF/A-1b Profile)")
                val outFile = createTempOutputFile("pdfa_archival", "pdf")
                srcDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
                return outFile
            } finally {
                srcDoc.destroy()
            }
        }
    }

    private suspend fun extractAssetsZip(file: File, onProgress: (ToolProgress) -> Unit): File {
        onProgress(ToolProgress(1, 100, 10, "Extracting embedded images & text...", false))
        PdfBoxHelper.ensureInitialized(context)
        val zipFile = createTempOutputFile("extracted_assets", "zip")

        val srcDoc = Document.openDocument(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot open document")

        try {
            val totalPages = srcDoc.countPages()
            var extractedImagesCount = 0
            val fullText = StringBuilder()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                // Step 1: Extract real embedded PDImageXObjects via PDFBox
                runCatching {
                    FileInputStream(file).use { fis ->
                        PDDocument.load(fis).use { doc ->
                            for (p in 0 until doc.numberOfPages) {
                                currentCoroutineContext().ensureActive()
                                val page = doc.getPage(p)
                                val resources = page.resources ?: continue
                                var pageImgIdx = 1
                                for (name in resources.xObjectNames) {
                                    val xObject = resources.getXObject(name)
                                    if (xObject is PDImageXObject) {
                                        val bmp = runCatching { xObject.image }.getOrNull()
                                        if (bmp != null) {
                                            val entryName = String.format("images/page_%03d_img_%02d.png", p + 1, pageImgIdx++)
                                            zos.putNextEntry(ZipEntry(entryName))
                                            bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                                            zos.closeEntry()
                                            bmp.recycle()
                                            extractedImagesCount++
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 2: Extract text and fallback renders if no embedded XObjects found
                val ocrEngine = OcrEngine(context)
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    val st = page.toStructuredText()

                    val pageTextSb = StringBuilder()
                    for (b in st.blocks) {
                        for (l in b.lines) {
                            for (c in l.chars) pageTextSb.append(c.c.toChar())
                            pageTextSb.append("\n")
                        }
                    }
                    val pageText = pageTextSb.toString().trim()

                    fullText.append("--- PAGE ${p + 1} ---\n")
                    if (pageText.isNotBlank()) {
                        fullText.append(pageText).append("\n\n")
                    }

                    // If document had no embedded individual images (e.g. raster scan), export full page
                    if (extractedImagesCount == 0) {
                        val pixmap = page.toPixmap(Matrix(2.0f, 2.0f), ColorSpace.DeviceRGB, false)
                        val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)

                        zos.putNextEntry(ZipEntry(String.format("images/page_%03d.png", p + 1)))
                        bmp.compress(Bitmap.CompressFormat.PNG, 95, zos)
                        zos.closeEntry()

                        if (pageText.isBlank()) {
                            val ocrText = runCatching { ocrEngine.recognizeText(bmp) }.getOrDefault("")
                            if (ocrText.isNotBlank()) {
                                fullText.append(ocrText).append("\n\n")
                            }
                        }

                        bmp.recycle()
                        pixmap.destroy()
                    }

                    st.destroy()
                    page.destroy()

                    val pct = 20 + (((p + 1).toFloat() / totalPages) * 75).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Extracting page ${p + 1} assets...", false))
                }

                // Step 3: Write extracted text file
                zos.putNextEntry(ZipEntry("text/document_text.txt"))
                zos.write(fullText.toString().toByteArray())
                zos.closeEntry()
            }

            return zipFile
        } finally {
            srcDoc.destroy()
        }
    }

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

    private suspend fun protectPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            onProgress(ToolProgress(1, 100, 30, "Encrypting with AES-256...", false))
            val userPwd = options.userPassword.ifEmpty { "1234" }
            val ownerPwd = options.ownerPassword.ifEmpty { userPwd }
            val permissions = if (options.allowPrinting) "print" else "no-print"

            val outFile = createTempOutputFile("protected", "pdf")
            srcDoc.save(outFile.absolutePath, "encryption=aes-256,user-password=$userPwd,owner-password=$ownerPwd,permissions=$permissions")
            onProgress(ToolProgress(100, 100, 95, "Password encryption complete.", false))
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun unlockPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            if (srcDoc.needsPassword()) {
                val ok = srcDoc.authenticatePassword(options.userPassword)
                if (!ok) throw IllegalArgumentException("Invalid document password provided.")
            }

            onProgress(ToolProgress(1, 100, 40, "Removing encryption...", false))
            val totalPages = srcDoc.countPages()
            val destDoc = PDFDocument()
            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                destDoc.graftPage(destDoc.countPages(), srcDoc, p)
            }

            val outFile = createTempOutputFile("unlocked", "pdf")
            destDoc.save(outFile.absolutePath, "encryption=none,compress")
            destDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun redactPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val query = options.redactQuery.ifBlank { "CONFIDENTIAL" }

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p) as? PDFPage ?: continue
                val quads = srcDoc.search(p, 0, query)
                if (quads != null && quads.isNotEmpty()) {
                    for (hit in quads) {
                        for (q in hit) {
                            val annot = page.createAnnotation(PDFAnnotation.TYPE_REDACT)
                            annot.rect = Rect(q.ul_x, q.ul_y, q.lr_x, q.lr_y)
                            annot.color = floatArrayOf(0f, 0f, 0f)
                            annot.update()
                        }
                    }
                    // Burn redactions permanently
                    page.applyRedactions(true, PDFPage.REDACT_IMAGE_PIXELS, PDFPage.REDACT_LINE_ART_REMOVE_IF_COVERED, PDFPage.REDACT_TEXT_REMOVE)
                }
                page.destroy()
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Redacting page ${p + 1}...", false))
            }

            val outFile = createTempOutputFile("redacted", "pdf")
            srcDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun stripMetadata(file: File, onProgress: (ToolProgress) -> Unit): File {
        onProgress(ToolProgress(1, 100, 25, "Purging XMP streams & author tags...", false))
        PdfBoxHelper.ensureInitialized(context)
        val intermediate = createTempOutputFile("pdfbox_stripped", "pdf")

        try {
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    doc.documentCatalog?.metadata = null
                    doc.documentInformation = PDDocumentInformation()
                    FileOutputStream(intermediate).use { fos -> doc.save(fos) }
                }
            }
        } catch (_: Exception) {
            file.copyTo(intermediate, overwrite = true)
        }

        // Secondary pass via MuPDF xref cleaner to destroy unreachable objects and clear /Info
        val srcDoc = Document.openDocument(intermediate.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            srcDoc.setMetaData(Document.META_INFO_AUTHOR, "")
            srcDoc.setMetaData(Document.META_INFO_TITLE, "")
            srcDoc.setMetaData(Document.META_INFO_CREATOR, "")
            srcDoc.setMetaData(Document.META_INFO_PRODUCER, "")
            srcDoc.setMetaData(Document.META_INFO_SUBJECT, "")
            srcDoc.setMetaData(Document.META_INFO_KEYWORDS, "")

            val outFile = createTempOutputFile("sanitized", "pdf")
            srcDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
            onProgress(ToolProgress(100, 100, 95, "Metadata purged.", false))
            return outFile
        } finally {
            srcDoc.destroy()
            try { if (intermediate.exists()) intermediate.delete() } catch (_: Exception) {}
        }
    }

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

    private fun parsePageRanges(rangeStr: String, maxPages: Int): List<Int> {
        if (rangeStr.isBlank()) return (0 until maxPages).toList()
        val result = mutableSetOf<Int>()
        val parts = rangeStr.split(",")
        for (part in parts) {
            val clean = part.trim()
            if (clean.contains("-")) {
                val bounds = clean.split("-")
                val start = (bounds.getOrNull(0)?.toIntOrNull() ?: 1) - 1
                val end = (bounds.getOrNull(1)?.toIntOrNull() ?: maxPages) - 1
                for (i in start.coerceAtLeast(0)..end.coerceAtMost(maxPages - 1)) {
                    result.add(i)
                }
            } else {
                val num = clean.toIntOrNull()
                if (num != null && num in 1..maxPages) {
                    result.add(num - 1)
                }
            }
        }
        return result.sorted()
    }
}
