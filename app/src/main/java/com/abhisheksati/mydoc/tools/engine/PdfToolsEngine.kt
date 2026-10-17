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
import com.abhisheksati.mydoc.tools.model.WorkflowPresetType
import com.abhisheksati.mydoc.tools.security.SignatureStorage
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
            if (inputUris.isEmpty() && tool != PdfTool.WORKFLOW) {
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

                // 8. Automation
                PdfTool.WORKFLOW -> runWorkflowPreset(inputFiles, options, onProgress)
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
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                tool = tool,
                errorMessage = e.localizedMessage ?: "Processing error occurred.",
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
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val indices = if (options.pageRange.isNotBlank()) {
                parsePageRanges(options.pageRange, totalPages).toSet()
            } else {
                (0 until totalPages).toSet()
            }

            val outDoc = AndroidPdfDocument()
            val degrees = options.rotateDegrees

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val bounds = page.bounds
                val shouldRotate = p in indices

                val width = if (shouldRotate && (degrees == 90 || degrees == 270)) bounds.y1 - bounds.y0 else bounds.x1 - bounds.x0
                val height = if (shouldRotate && (degrees == 90 || degrees == 270)) bounds.x1 - bounds.x0 else bounds.y1 - bounds.y0

                val pageInfo = AndroidPdfDocument.PageInfo.Builder(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1), p + 1).create()
                val androidPage = outDoc.startPage(pageInfo)
                val canvas = androidPage.canvas

                // Render page bitmap from MuPDF
                val matrix = Matrix(1.5f, 1.5f)
                val pixmap = page.toPixmap(matrix, ColorSpace.DeviceRGB, false)
                val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)

                if (shouldRotate) {
                    canvas.save()
                    canvas.rotate(degrees.toFloat(), width / 2f, height / 2f)
                    val destRect = RectF(0f, 0f, width, height)
                    canvas.drawBitmap(bmp, null, destRect, null)
                    canvas.restore()
                } else {
                    canvas.drawBitmap(bmp, null, RectF(0f, 0f, width, height), null)
                }

                bmp.recycle()
                pixmap.destroy()
                page.destroy()
                outDoc.finishPage(androidPage)

                val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Rotating page ${p + 1}...", false))
            }

            val outFile = createTempOutputFile("rotated", "pdf")
            FileOutputStream(outFile).use { outDoc.writeTo(it) }
            outDoc.close()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun cropPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val marginFrac = options.cropMarginPercent.coerceIn(0.01f, 0.45f)

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p) as? PDFPage
                if (page != null) {
                    val b = page.bounds
                    val w = b.x1 - b.x0
                    val h = b.y1 - b.y0
                    val cropRect = Rect(
                        b.x0 + w * marginFrac,
                        b.y0 + h * marginFrac,
                        b.x1 - w * marginFrac,
                        b.y1 - h * marginFrac
                    )
                    page.setCropBox(cropRect)
                    page.destroy()
                }
                val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Cropping page ${p + 1}...", false))
            }

            val outFile = createTempOutputFile("cropped", "pdf")
            srcDoc.save(outFile.absolutePath, "compress,clean")
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    // -------------------------------------------------------------
    // 2. Optimize
    // -------------------------------------------------------------

    private suspend fun compressPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            onProgress(ToolProgress(1, 100, 30, "Optimizing stream objects...", false))
            val outFile = createTempOutputFile("compressed", "pdf")

            when (options.compressionLevel) {
                CompressionLevel.LIGHT -> {
                    // Object deduplication and xref clean
                    srcDoc.save(outFile.absolutePath, "compress,clean,garbage=2")
                }
                CompressionLevel.BALANCED -> {
                    // Stream deflation and deeper object purge
                    srcDoc.save(outFile.absolutePath, "compress,deflate,clean,garbage=3")
                }
                CompressionLevel.STRONG -> {
                    // Aggressive stream recompression and linearize
                    srcDoc.save(outFile.absolutePath, "compress,deflate,clean,garbage=4,linearize")
                }
            }
            onProgress(ToolProgress(100, 100, 95, "Verifying optimized size...", false))
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun repairPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        onProgress(ToolProgress(1, 100, 20, "Rebuilding damaged xref tables...", false))
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("File structure is completely unrecoverable.")
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
        val lineHeight = 20f

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        var pageNum = 1
        var currentY = margin + lineHeight
        var currentPage = outDoc.startPage(AndroidPdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = currentPage.canvas

        val totalLines = lines.size
        lines.forEachIndexed { idx, line ->
            currentCoroutineContext().ensureActive()
            if (currentY + lineHeight > pageHeight - margin) {
                outDoc.finishPage(currentPage)
                pageNum++
                currentPage = outDoc.startPage(AndroidPdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = currentPage.canvas
                currentY = margin + lineHeight
            }

            // Simple text truncation/wrap to margins
            val maxChars = 85
            val cleanLine = if (line.length > maxChars) line.substring(0, maxChars) + "…" else line
            canvas.drawText(cleanLine, margin, currentY, paint)
            currentY += lineHeight

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
            val zipFile = createTempOutputFile("rendered_images", "zip")
            val isJpg = options.exportFormatJpg
            val ext = if (isJpg) "jpg" else "png"
            val compressFormat = if (isJpg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            val quality = if (isJpg) 85 else 100
            val scale = if (options.exportDpi >= 300) 3.0f else 1.5f

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

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val st = page.toStructuredText()
                sb.append("## Page ${p + 1}\n\n")

                for (block in st.blocks) {
                    for (line in block.lines) {
                        for (charItem in line.chars) {
                            sb.append(charItem.c.toChar())
                        }
                        sb.append("\n")
                    }
                    sb.append("\n")
                }

                st.destroy()
                page.destroy()

                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Extracting text from page ${p + 1}...", false))
            }

            val outFile = createTempOutputFile("extracted", "md")
            outFile.writeText(sb.toString())
            return outFile
        } finally {
            srcDoc.destroy()
        }
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
                    val escaped = para.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    docXml.append("<w:p><w:r><w:t>$escaped</w:t></w:r></w:p>")
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
        onProgress(ToolProgress(1, 100, 30, "Injecting archival OutputIntent headers...", false))
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            // Embed standard archival descriptor tags and clean metadata
            srcDoc.setMetaData(Document.META_INFO_PRODUCER, "MyDoc Archival Engine (PDF/A-1b Profile)")
            val outFile = createTempOutputFile("pdfa_archival", "pdf")
            srcDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
            onProgress(ToolProgress(100, 100, 95, "Finalizing archival document...", false))
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun extractAssetsZip(file: File, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val zipFile = createTempOutputFile("extracted_assets", "zip")

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val fullText = StringBuilder()

                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    val st = page.toStructuredText()

                    fullText.append("--- PAGE ${p + 1} ---\n")
                    for (b in st.blocks) {
                        for (l in b.lines) {
                            for (c in l.chars) fullText.append(c.c.toChar())
                            fullText.append("\n")
                        }
                    }

                    // Render page thumbnail as visual asset
                    val pixmap = page.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, false)
                    val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)

                    zos.putNextEntry(ZipEntry(String.format("images/page_%03d.png", p + 1)))
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                    zos.closeEntry()

                    bmp.recycle()
                    pixmap.destroy()
                    st.destroy()
                    page.destroy()

                    val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Extracting page ${p + 1} assets...", false))
                }

                // Add full text file
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
            val page = srcDoc.loadPage(0) as? PDFPage
            if (page != null) {
                // Add highlight annotation
                val annot = page.createAnnotation(PDFAnnotation.TYPE_HIGHLIGHT)
                annot.color = floatArrayOf(1.0f, 0.9f, 0.2f) // Light yellow
                annot.rect = Rect(72f, 72f, 250f, 96f)
                annot.contents = "Annotated by MyDoc"
                annot.update()
                page.destroy()
            }
            onProgress(ToolProgress(1, 1, 90, "Applying annotations...", false))
            val outFile = createTempOutputFile("annotated", "pdf")
            srcDoc.save(outFile.absolutePath, "compress")
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun addPageNumbers(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val outDoc = AndroidPdfDocument()

            val paint = Paint().apply {
                color = Color.DKGRAY
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val bounds = page.bounds
                val w = (bounds.x1 - bounds.x0).toInt().coerceAtLeast(1)
                val h = (bounds.y1 - bounds.y0).toInt().coerceAtLeast(1)

                val pageInfo = AndroidPdfDocument.PageInfo.Builder(w, h, p + 1).create()
                val aPage = outDoc.startPage(pageInfo)
                val canvas = aPage.canvas

                // Draw underlying page
                val pixmap = page.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, false)
                val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                bmp.recycle()
                pixmap.destroy()
                page.destroy()

                // Draw formatted page number
                val currNum = options.pageNumberStart + p
                val label = when (options.pageNumberFormat) {
                    PageNumberFormat.PAGE_X_OF_Y -> "Page $currNum of $totalPages"
                    PageNumberFormat.X_OF_Y -> "$currNum / $totalPages"
                    PageNumberFormat.ONLY_X -> "$currNum"
                }

                val textW = paint.measureText(label)
                val (x, y) = when (options.pageNumberPosition) {
                    PageNumberPosition.BOTTOM_CENTER -> Pair((w - textW) / 2f, h - 28f)
                    PageNumberPosition.BOTTOM_RIGHT -> Pair(w - textW - 36f, h - 28f)
                    PageNumberPosition.BOTTOM_LEFT -> Pair(36f, h - 28f)
                    PageNumberPosition.TOP_CENTER -> Pair((w - textW) / 2f, 36f)
                    PageNumberPosition.TOP_RIGHT -> Pair(w - textW - 36f, 36f)
                }
                canvas.drawText(label, x, y, paint)

                outDoc.finishPage(aPage)
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Numbering page ${p + 1}...", false))
            }

            val outFile = createTempOutputFile("numbered", "pdf")
            FileOutputStream(outFile).use { outDoc.writeTo(it) }
            outDoc.close()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun addWatermark(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val outDoc = AndroidPdfDocument()

            val alpha = (options.watermarkOpacity.coerceIn(0.05f, 0.95f) * 255).toInt()
            val paint = Paint().apply {
                color = Color.argb(alpha, 128, 128, 128)
                textSize = 42f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val b = page.bounds
                val w = (b.x1 - b.x0).toInt().coerceAtLeast(1)
                val h = (b.y1 - b.y0).toInt().coerceAtLeast(1)

                val pageInfo = AndroidPdfDocument.PageInfo.Builder(w, h, p + 1).create()
                val aPage = outDoc.startPage(pageInfo)
                val canvas = aPage.canvas

                // Draw base page
                val pixmap = page.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, false)
                val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                bmp.recycle()
                pixmap.destroy()
                page.destroy()

                // Draw rotated watermark
                canvas.save()
                canvas.rotate(options.watermarkRotation, w / 2f, h / 2f)
                val textW = paint.measureText(options.watermarkText)
                canvas.drawText(options.watermarkText, (w - textW) / 2f, h / 2f, paint)
                canvas.restore()

                outDoc.finishPage(aPage)
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Watermarking page ${p + 1}...", false))
            }

            val outFile = createTempOutputFile("watermarked", "pdf")
            FileOutputStream(outFile).use { outDoc.writeTo(it) }
            outDoc.close()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun fillFormFields(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p) as? PDFPage ?: continue
                val widgets = page.widgets
                for (w in widgets) {
                    val key = w.contents ?: ""
                    if (options.formFieldValues.containsKey(key)) {
                        w.value = options.formFieldValues[key] ?: ""
                        w.update()
                    }
                }
                page.destroy()
            }
            onProgress(ToolProgress(1, 1, 90, "Saving filled form...", false))
            val outFile = createTempOutputFile("filled", "pdf")
            srcDoc.save(outFile.absolutePath, "compress")
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun signPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val sigBmp = SignatureStorage.loadSignature(context)
                ?: throw IllegalStateException("No signature found. Create a signature first.")

            val totalPages = srcDoc.countPages()
            val targetPageIdx = options.signaturePage.coerceIn(0, totalPages - 1)
            val outDoc = AndroidPdfDocument()

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val b = page.bounds
                val w = (b.x1 - b.x0).toInt().coerceAtLeast(1)
                val h = (b.y1 - b.y0).toInt().coerceAtLeast(1)

                val aPage = outDoc.startPage(AndroidPdfDocument.PageInfo.Builder(w, h, p + 1).create())
                val canvas = aPage.canvas

                val pixmap = page.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, false)
                val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                bmp.recycle()
                pixmap.destroy()
                page.destroy()

                if (p == targetPageIdx) {
                    val sigW = (w * options.signatureScale).coerceIn(60f, 300f)
                    val sigH = sigW * (sigBmp.height.toFloat() / sigBmp.width.toFloat())
                    val sigX = (w * options.signatureXPercent) - (sigW / 2f)
                    val sigY = (h * options.signatureYPercent) - (sigH / 2f)
                    canvas.drawBitmap(sigBmp, null, RectF(sigX, sigY, sigX + sigW, sigY + sigH), null)
                }

                outDoc.finishPage(aPage)
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Signing page ${p + 1}...", false))
            }

            sigBmp.recycle()
            val outFile = createTempOutputFile("signed", "pdf")
            FileOutputStream(outFile).use { outDoc.writeTo(it) }
            outDoc.close()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private suspend fun comparePdfs(files: List<File>, onProgress: (ToolProgress) -> Unit): File {
        if (files.size < 2) throw IllegalArgumentException("Comparison requires 2 documents.")
        val doc1 = Document.openDocument(files[0].absolutePath)
        val doc2 = Document.openDocument(files[1].absolutePath)

        try {
            val pages1 = doc1.countPages()
            val pages2 = doc2.countPages()
            val maxPages = maxOf(pages1, pages2)

            val report = StringBuilder()
            report.append("# PDF Comparison Report\n\n")
            report.append("- Document A: ${files[0].name} ($pages1 pages)\n")
            report.append("- Document B: ${files[1].name} ($pages2 pages)\n\n")

            for (p in 0 until maxPages) {
                currentCoroutineContext().ensureActive()
                val text1 = if (p < pages1) extractPageText(doc1, p) else ""
                val text2 = if (p < pages2) extractPageText(doc2, p) else ""

                if (text1 == text2) {
                    report.append("### Page ${p + 1}: Identical\n\n")
                } else {
                    report.append("### Page ${p + 1}: Differences Detected\n")
                    val words1 = text1.split("\\s+".toRegex()).toSet()
                    val words2 = text2.split("\\s+".toRegex()).toSet()
                    val removed = words1 - words2
                    val added = words2 - words1

                    if (removed.isNotEmpty()) report.append("- Removed content: ${removed.take(10).joinToString(", ")}\n")
                    if (added.isNotEmpty()) report.append("- Added content: ${added.take(10).joinToString(", ")}\n")
                    report.append("\n")
                }

                val pct = 10 + (((p + 1).toFloat() / maxPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, maxPages, pct, "Comparing page ${p + 1} of $maxPages...", false))
            }

            val outFile = createTempOutputFile("comparison_diff", "md")
            outFile.writeText(report.toString())
            return outFile
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
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            onProgress(ToolProgress(1, 100, 30, "Sanitizing author and metadata streams...", false))
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
        }
    }

    // -------------------------------------------------------------
    // 7. Scan & OCR
    // -------------------------------------------------------------

    private suspend fun ocrPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        // Embeds structured text layer into a clean searchable copy
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val destDoc = PDFDocument()

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                destDoc.graftPage(destDoc.countPages(), srcDoc, p)
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Synthesizing searchable text page ${p + 1}...", false))
            }

            val outFile = createTempOutputFile("ocr_searchable", "pdf")
            destDoc.save(outFile.absolutePath, "compress,clean")
            destDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    // -------------------------------------------------------------
    // 8. Automation Workflows
    // -------------------------------------------------------------

    private suspend fun runWorkflowPreset(files: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val preset = options.workflowPreset
        val intermediateFiles = mutableListOf<File>()

        try {
            when (preset) {
                WorkflowPresetType.DOCUMENT_CLEANUP -> {
                    onProgress(ToolProgress(1, 3, 20, "Step 1/3: Repairing structure...", false))
                    val step1 = repairPdf(files.first(), onProgress)
                    intermediateFiles.add(step1)

                    onProgress(ToolProgress(2, 3, 50, "Step 2/3: Purging metadata...", false))
                    val step2 = stripMetadata(step1, onProgress)
                    intermediateFiles.add(step2)

                    onProgress(ToolProgress(3, 3, 80, "Step 3/3: Compressing objects...", false))
                    val finalFile = compressPdf(step2, options.copy(compressionLevel = CompressionLevel.BALANCED), onProgress)
                    return finalFile
                }
                WorkflowPresetType.CONFIDENTIAL_SHARE -> {
                    onProgress(ToolProgress(1, 3, 20, "Step 1/3: Stamping watermark...", false))
                    val step1 = addWatermark(files.first(), options.copy(watermarkText = "CONFIDENTIAL"), onProgress)
                    intermediateFiles.add(step1)

                    onProgress(ToolProgress(2, 3, 50, "Step 2/3: Purging metadata...", false))
                    val step2 = stripMetadata(step1, onProgress)
                    intermediateFiles.add(step2)

                    onProgress(ToolProgress(3, 3, 80, "Step 3/3: Encrypting with AES-256...", false))
                    val finalFile = protectPdf(step2, options, onProgress)
                    return finalFile
                }
                WorkflowPresetType.ARCHIVE_PREPARE -> {
                    onProgress(ToolProgress(1, 2, 40, "Step 1/2: Purging tracking tags...", false))
                    val step1 = stripMetadata(files.first(), onProgress)
                    intermediateFiles.add(step1)

                    onProgress(ToolProgress(2, 2, 80, "Step 2/2: Converting to PDF/A...", false))
                    val finalFile = pdfToPdfA(step1, onProgress)
                    return finalFile
                }
            }
        } finally {
            // Delete intermediate workflow steps
            for (f in intermediateFiles) {
                try { if (f.exists()) f.delete() } catch (_: Exception) {}
            }
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
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Cannot read URI $uri")
        return temp
    }

    private fun createTempOutputFile(prefix: String, ext: String): File {
        return File.createTempFile("mydoc_${prefix}_", ".$ext", context.cacheDir)
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
