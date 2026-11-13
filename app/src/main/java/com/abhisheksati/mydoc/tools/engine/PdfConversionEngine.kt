package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.exifinterface.media.ExifInterface
import com.abhisheksati.mydoc.tools.scan.cropper.BitmapRotation
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import com.abhisheksati.mydoc.ocr.OcrEngine
import com.abhisheksati.mydoc.tools.model.PageMargin
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.ToolProgress
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.abhisheksati.mydoc.util.SecureLog
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max

/**
 * Specialized engine handling document format conversions to and from PDF,
 * including rasterization, OCR fallbacks, office formats, and asset extraction.
 */
class PdfConversionEngine(private val context: Context) {

    private companion object {
        /** Pixels per page point when decoding into a fixed page size. */
        const val PAGE_SUPERSAMPLE = 2f
        const val MAX_SAMPLE_SIZE = 32

        /** Floor for the auto-reduced long-image scale, below which the output is unreadable. */
        const val MIN_STITCH_SCALE = 0.5f

        /** Ceiling for the stitched long image in pixels (~96 MB as ARGB_8888). */
        const val MAX_STITCHED_PIXELS = 24_000_000L
    }

    private fun createTempOutputFile(prefix: String, ext: String): File =
        PdfToolUtils.createTempOutputFile(context.cacheDir, prefix, ext)

    suspend fun imagesToPdf(
        files: List<File>,
        options: ToolOptions,
        onProgress: (ToolProgress) -> Unit,
        onSkipped: (List<String>) -> Unit = {}
    ): File {
        val outDoc = AndroidPdfDocument()
        try {
            val total = files.size
            var added = 0
            val skipped = mutableListOf<String>()

            files.forEachIndexed { index, imgFile ->
                currentCoroutineContext().ensureActive()
                // FIT_IMAGE sizes the page from the image, bounded to prevent OutOfMemory on huge images;
                // a fixed page only needs enough pixels to fill it, so the rest is sampled away while decoding.
                val budgetPx = when (options.pageSizeFormat) {
                    PageSizeFormat.FIT_IMAGE -> 4096
                    PageSizeFormat.A4 -> (max(
                        PageSizeFormat.A4.widthPt, PageSizeFormat.A4.heightPt) * PAGE_SUPERSAMPLE).toInt()
                    PageSizeFormat.LETTER -> (max(
                        PageSizeFormat.LETTER.widthPt, PageSizeFormat.LETTER.heightPt) * PAGE_SUPERSAMPLE).toInt()
                }
                val bitmap = decodeSampled(imgFile, budgetPx)
                if (bitmap == null) {
                    // An unreadable image is left out, but the caller is told: a file that silently
                    // loses pages must not be presented as a complete conversion.
                    skipped.add(imgFile.name)
                    return@forEachIndexed
                }

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
                try {
                    val canvas = page.canvas
                    val scale = minOf(availableW / bitmap.width.toFloat(), availableH / bitmap.height.toFloat())
                    val drawW = bitmap.width * scale
                    val drawH = bitmap.height * scale
                    val left = margin + (availableW - drawW) / 2f
                    val top = margin + (availableH - drawH) / 2f

                    canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), null)
                    added++
                } finally {
                    outDoc.finishPage(page)
                    bitmap.recycle()
                }

                val pct = 10 + (((index + 1).toFloat() / total) * 80).toInt()
                onProgress(ToolProgress(index + 1, total, pct, "Adding image ${index + 1} of $total...", false))
            }

            if (added == 0) {
                throw IllegalArgumentException("None of the selected files could be decoded as an image.")
            }
            if (skipped.isNotEmpty()) onSkipped(skipped.toList())

            val outFile = createTempOutputFile("converted", "pdf")
            FileOutputStream(outFile).use { outDoc.writeTo(it) }
            return outFile
        } finally {
            // A document abandoned half way through used to leak its native pages.
            outDoc.close()
        }
    }

    /**
     * Decodes at no larger than the resolution the output can show, and rotates to match the
     * camera's orientation tag: `BitmapFactory` ignores EXIF, so a portrait photo used to land on
     * the page sideways at four times the pixels it needed.
     */
    private fun decodeSampled(file: File, maxDimensionPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample.toLong() * 2) >= maxDimensionPx &&
            sample <= MAX_SAMPLE_SIZE
        ) {
            sample *= 2
        }

        var bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null

        val degrees = exifRotation(file)
        if (degrees != 0) {
            val rotated = BitmapRotation.rotate(bitmap, degrees)
            if (rotated !== bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }
        return bitmap
    }

    private fun exifRotation(file: File): Int = runCatching {
        file.inputStream().use { input ->
            when (ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
    }.getOrDefault(0)

    suspend fun textOrCsvToPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        val lines = file.readLines()
        return renderTextToPdf(lines, onProgress)
    }

    suspend fun renderTextToPdf(lines: List<String>, onProgress: (ToolProgress) -> Unit): File {
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

    suspend fun pdfToImagesZip(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
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
                // Measure first. Page bounds come from the document without rendering anything, so
                // the stitched bitmap can be sized - and the scale reduced to fit it inside the
                // memory budget - before a single pixel is produced. The previous version kept every
                // page bitmap alive and then allocated one enormous bitmap on top of them.
                var maxWidthPt = 1f
                var totalHeightPt = 1f
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    try {
                        val bounds = page.bounds
                        maxWidthPt = max(maxWidthPt, bounds.x1 - bounds.x0)
                        totalHeightPt += (bounds.y1 - bounds.y0)
                    } finally {
                        page.destroy()
                    }
                }

                val layout = stitchedImageLayout(maxWidthPt, totalHeightPt, scale,
                    MIN_STITCH_SCALE, MAX_STITCHED_PIXELS)
                val stitchScale = layout.scale
                val outWidth = layout.width
                val outHeight = layout.height
                if (stitchScale < scale) {
                    onProgress(ToolProgress(0, totalPages, 5,
                        "Long image reduced to ${((stitchScale / scale) * 100).toInt()}% of the chosen resolution to fit in memory...", false))
                }
                val stitchedBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(stitchedBitmap)
                    canvas.drawColor(Color.WHITE)
                    var currentY = 0f
                    for (p in 0 until totalPages) {
                        currentCoroutineContext().ensureActive()
                        val page = srcDoc.loadPage(p)
                        val pixmap = page.toPixmap(
                            Matrix(stitchScale, stitchScale), ColorSpace.DeviceRGB, true
                        )
                        try {
                            val bounds = page.bounds
                            val drawnWidth = (bounds.x1 - bounds.x0) * stitchScale
                            val drawnHeight = (bounds.y1 - bounds.y0) * stitchScale
                            val left = (outWidth - drawnWidth) / 2f
                            val pageBitmap = Bitmap.createBitmap(
                                pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888
                            )
                            try {
                                canvas.drawBitmap(pageBitmap, null, android.graphics.RectF(left, currentY, left + drawnWidth, currentY + drawnHeight), null)
                                currentY += drawnHeight
                            } finally {
                                pageBitmap.recycle()
                            }
                        } finally {
                            pixmap.destroy()
                            page.destroy()
                        }

                        val pct = 10 + (((p + 1).toFloat() / totalPages) * 60).toInt()
                        onProgress(
                            ToolProgress(p + 1, totalPages, pct, "Rendering page ${p + 1} of $totalPages...", false)
                        )
                    }

                    val outFile = createTempOutputFile("stitched_long_image", ext)
                    FileOutputStream(outFile).use { fos ->
                        stitchedBitmap.compress(compressFormat, quality, fos)
                    }
                    return outFile
                } finally {
                    if (!stitchedBitmap.isRecycled) {
                        stitchedBitmap.recycle()
                    }
                }
            }

            val zipFile = createTempOutputFile("rendered_images", "zip")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    try {
                        val bounds = page.bounds
                        val effectiveScale = cappedRenderScale(bounds.x1 - bounds.x0, bounds.y1 - bounds.y0, scale)
                        val pixmap = page.toPixmap(Matrix(effectiveScale, effectiveScale), ColorSpace.DeviceRGB, true)
                        try {
                            val bitmap = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                            try {
                                zos.putNextEntry(ZipEntry(String.format(java.util.Locale.ROOT, "page_%03d.$ext", p + 1)))
                                bitmap.compress(compressFormat, quality, zos)
                                zos.closeEntry()
                            } finally {
                                bitmap.recycle()
                            }
                        } finally {
                            pixmap.destroy()
                        }
                    } finally {
                        page.destroy()
                    }

                    val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Rendering page ${p + 1} of $totalPages...", false))
                }
            }
            return zipFile
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun pdfToTextOrMarkdown(file: File, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot open document")
        var ocrEngine: OcrEngine? = null
        try {
            val totalPages = srcDoc.countPages()
            val sb = StringBuilder()
            sb.append("# ${file.nameWithoutExtension}\n\n")

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val st = page.toStructuredText()
                val pageSb = StringBuilder()

                for (block in st.blocks) {
                    for (line in block.lines) {
                        for (charItem in line.chars) {
                            pageSb.appendCodePoint(charItem.c)
                        }
                        pageSb.append("\n")
                    }
                    pageSb.append("\n")
                }
                st.destroy()
                page.destroy()

                var pageStr = pageSb.toString().trim()
                if (pageStr.isBlank()) {
                    // A scanned page among selectable ones used to be dropped entirely: the OCR
                    // fallback ran only when no page anywhere had text.
                    val engine = ocrEngine ?: OcrEngine(context).also { ocrEngine = it }
                    val ocrPage = srcDoc.loadPage(p)
                    val pixmap = try {
                        ocrPage.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, true)
                    } finally {
                        ocrPage.destroy()
                    }
                    val bmp = try {
                        Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                    } finally {
                        pixmap.destroy()
                    }
                    pageStr = try {
                        engine.recognizeText(bmp).trim()
                    } catch (_: Exception) {
                        ""
                    } finally {
                        bmp.recycle()
                    }
                }
                if (pageStr.isNotBlank()) {
                    sb.append("## Page ${p + 1}\n\n").append(pageStr).append("\n\n")
                }

                val pct = 10 + (((p + 1).toFloat() / totalPages) * 70).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Extracting text from page ${p + 1}...", false))
            }

            val textContent = sb.toString().trim()
            if (textContent.isEmpty()) {
                throw IllegalStateException("No readable or extractable text found in this document.")
            }
            val outFile = createTempOutputFile("extracted", "md")
            outFile.writeText(sb.toString())
            return outFile
        } finally {
            ocrEngine?.close()
            srcDoc.destroy()
        }
    }

    private fun sanitizeXml(text: String): String = PdfToolUtils.sanitizeXml(text)

    suspend fun pdfToOfficeDocument(file: File, onProgress: (ToolProgress) -> Unit): File {
        // Generates an offline valid OOXML .docx with structured paragraphs
        val srcDoc = Document.openDocument(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot open document")
        var ocrEngine: OcrEngine? = null
        try {
            val totalPages = srcDoc.countPages()
            val paragraphs = mutableListOf<String>()

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p)
                val st = page.toStructuredText()
                val before = paragraphs.size
                for (b in st.blocks) {
                    val pText = StringBuilder()
                    for (l in b.lines) {
                        for (c in l.chars) pText.appendCodePoint(c.c)
                        pText.append(" ")
                    }
                    if (pText.isNotBlank()) paragraphs.add(pText.toString().trim())
                }
                st.destroy()
                page.destroy()

                if (paragraphs.size == before) {
                    // A scanned page in an otherwise selectable document used to be left out: OCR
                    // ran only when no page anywhere contributed text.
                    val engine = ocrEngine ?: OcrEngine(context).also { ocrEngine = it }
                    val ocrPage = srcDoc.loadPage(p)
                    val pixmap = try {
                        ocrPage.toPixmap(Matrix(1.5f, 1.5f), ColorSpace.DeviceRGB, true)
                    } finally {
                        ocrPage.destroy()
                    }
                    val bmp = try {
                        Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                    } finally {
                        pixmap.destroy()
                    }
                    val ocrText = try {
                        engine.recognizeText(bmp)
                    } catch (_: Exception) {
                        ""
                    } finally {
                        bmp.recycle()
                    }
                    for (l in ocrText.lines()) {
                        val trimmed = l.trim()
                        if (trimmed.isNotBlank()) paragraphs.add(trimmed)
                    }
                }

                val pct = 10 + (((p + 1).toFloat() / totalPages) * 70).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Converting page ${p + 1}...", false))
            }

            if (paragraphs.isEmpty()) {
                throw IllegalStateException("No readable or extractable text found to convert into Office document.")
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
            ocrEngine?.close()
            srcDoc.destroy()
        }
    }

    suspend fun extractAssetsZip(file: File, onProgress: (ToolProgress) -> Unit): File {
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
                                            val entryName = String.format(
                                                java.util.Locale.ROOT,
                                                "images/page_%03d_img_%02d.png",
                                                p + 1,
                                                pageImgIdx++
                                            )
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
                OcrEngine(context).use { ocrEngine ->
                    for (p in 0 until totalPages) {
                        currentCoroutineContext().ensureActive()
                        val page = srcDoc.loadPage(p)
                        try {
                            val st = page.toStructuredText()
                            try {
                                val pageTextSb = StringBuilder()
                                for (b in st.blocks) {
                                    for (l in b.lines) {
                                        for (c in l.chars) pageTextSb.appendCodePoint(c.c)
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
                                    val bounds = page.bounds
                                    val renderScale = cappedRenderScale(bounds.x1 - bounds.x0, bounds.y1 - bounds.y0, 2.0f)
                                    val pixmap = page.toPixmap(Matrix(renderScale, renderScale), ColorSpace.DeviceRGB, true)
                                    try {
                                        val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                                        try {
                                            zos.putNextEntry(ZipEntry(String.format(java.util.Locale.ROOT, "images/page_%03d.png", p + 1)))
                                            bmp.compress(Bitmap.CompressFormat.PNG, 95, zos)
                                            zos.closeEntry()

                                            if (pageText.isBlank()) {
                                                val ocrText = try {
                                                    ocrEngine.recognizeText(bmp)
                                                } catch (e: Exception) {
                                                    if (e is CancellationException) throw e
                                                    // A failed page is a degradation the reader must hear about,
                                                    // not a silently empty extraction.
                                                    SecureLog.warn("PdfConversion", "OCR failed for page ${p + 1}", e) { file.name }
                                                    ""
                                                }
                                                if (ocrText.isNotBlank()) {
                                                    fullText.append(ocrText).append("\n\n")
                                                }
                                            }
                                        } finally {
                                            bmp.recycle()
                                        }
                                    } finally {
                                        pixmap.destroy()
                                    }
                                }
                            } finally {
                                st.destroy()
                            }
                        } finally {
                            page.destroy()
                        }

                        val pct = 20 + (((p + 1).toFloat() / totalPages) * 75).toInt()
                        onProgress(ToolProgress(p + 1, totalPages, pct, "Extracting page ${p + 1} assets...", false))
                    }
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
}

/**
 * Render scale for a page of [widthPt] x [heightPt] points, reduced so neither side exceeds
 * [maxPx] pixels. Every raster path shares this ceiling: an oversized page rendered at the full
 * scale is an OutOfMemoryError, not a better extraction.
 */
internal fun cappedRenderScale(widthPt: Float, heightPt: Float, baseScale: Float, maxPx: Float = 4096f): Float {
    val longest = max(widthPt * baseScale, heightPt * baseScale)
    return if (longest > maxPx) baseScale * (maxPx / longest) else baseScale
}
