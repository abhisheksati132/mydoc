package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import com.abhisheksati.mydoc.ocr.OcrEngine
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetParser
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import com.abhisheksati.mydoc.tools.model.PageMargin
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.ToolProgress
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.PDFDocument
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDMetadata
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Specialized engine handling document format conversions to and from PDF,
 * including rasterization, OCR fallbacks, office formats, and asset extraction.
 */
class PdfConversionEngine(private val context: Context) {

    private fun createTempOutputFile(prefix: String, ext: String): File =
        PdfToolUtils.createTempOutputFile(context.cacheDir, prefix, ext)

    suspend fun imagesToPdf(files: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
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

    suspend fun officeToPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
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

    suspend fun pdfToTextOrMarkdown(file: File, onProgress: (ToolProgress) -> Unit): File {
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

    private fun sanitizeXml(text: String): String = PdfToolUtils.sanitizeXml(text)

    suspend fun pdfToOfficeDocument(file: File, onProgress: (ToolProgress) -> Unit): File {
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

    suspend fun pdfToPdfA(file: File, onProgress: (ToolProgress) -> Unit): File {
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

                    doc.documentInformation?.producer = "MyDoc Archival Engine (PDF/A-1b Profile)"

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
                srcDoc.setMetaData(Document.META_INFO_PRODUCER, "MyDoc Archival Engine (PDF/A-1b Profile)")
                val outFile = createTempOutputFile("pdfa_archival", "pdf")
                srcDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
                return outFile
            } finally {
                srcDoc.destroy()
            }
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
}
