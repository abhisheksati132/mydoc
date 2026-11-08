package com.abhisheksati.mydoc.rendering.engine.word

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.async
import java.io.ByteArrayOutputStream
import java.io.File

class DocxImageRenderingTest {
    private fun withPdf(blocks: List<DocxBlock>, check: (PdfRenderer) -> Unit) {
        val rendered = requireNotNull(DocxPdfRenderer.render(blocks, byteArrayOf()))
        val file = File.createTempFile("word-layout-", ".pdf", InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)
        try {
            try { file.outputStream().use { rendered.doc.writeTo(it) } }
            finally { rendered.doc.close() }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use(check)
            }
        } finally { file.delete() }
    }

    private fun inkBands(blocks: List<DocxBlock>): List<Int> {
        var bands = emptyList<Int>()
        withPdf(blocks) { pdf ->
            pdf.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val rows = (60..200).filter { y -> (70..120).any { x ->
                        val pixel = bitmap.getPixel(x, y)
                        Color.red(pixel) < 80 && Color.green(pixel) < 80 && Color.blue(pixel) < 80
                    } }
                    bands = rows.filterIndexed { index, row -> index == 0 || row > rows[index - 1] + 1 }
                } finally { bitmap.recycle() }
            }
        }
        return bands
    }

    @Test fun blankParagraphProducesVisibleVerticalSpace() {
        val bands = inkBands(listOf(DocxBlock.Paragraph(listOf(DocxRun("MMMM"))),
            DocxBlock.Paragraph(emptyList()), DocxBlock.Paragraph(listOf(DocxRun("MMMM")))))
        assertEquals(2, bands.size)
        assertTrue("Blank paragraph must occupy a line", bands[1] - bands[0] in 40..46)
    }

    @Test fun adjacentParagraphSpacingUsesMaximumRatherThanSum() {
        val bands = inkBands(listOf(
            DocxBlock.Paragraph(listOf(DocxRun("MMMM")), DocxParagraphLayout(after = 20f)),
            DocxBlock.Paragraph(listOf(DocxRun("MMMM")), DocxParagraphLayout(before = 30f))))
        assertEquals(2, bands.size)
        assertTrue("Spacing should be 30 points plus line height", bands[1] - bands[0] in 42..46)
    }

    @Test fun centeredPictureUsesParagraphAlignment() {
        withPdf(listOf(DocxBlock.Image(pictureBytes(), 72f, 36f, "Red rectangle",
            layout = DocxParagraphLayout(alignment = DocxAlignment.CENTER)))) { pdf ->
            pdf.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val center = bitmap.getPixel(280, 85)
                    assertTrue(Color.red(center) > 220 && Color.green(center) < 40)
                    assertEquals(Color.WHITE, bitmap.getPixel(85, 85))
                } finally { bitmap.recycle() }
            }
        }
    }

    @Test fun explicitPageBreakCreatesSeparatePages() {
        withPdf(listOf(DocxBlock.Paragraph(listOf(DocxRun("Before"))), DocxBlock.PageBreak,
            DocxBlock.Paragraph(listOf(DocxRun("After"))))) { assertEquals(2, it.pageCount) }
    }

    @Test fun continuedHeadingDoesNotCreateDuplicateOutlineEntry() {
        val rendered = requireNotNull(DocxPdfRenderer.render(listOf(DocxBlock.Heading("Before", 2),
            DocxBlock.PageBreak, DocxBlock.Heading("After", 2, isContinuation = true)), byteArrayOf()))
        try {
            assertEquals(1, rendered.outlines.size)
            assertEquals("Before", rendered.outlines.single().title)
        } finally { rendered.doc.close() }
    }

    @Test fun pageBreakBeforeFirstParagraphDoesNotInventBlankPage() {
        withPdf(listOf(DocxBlock.Paragraph(listOf(DocxRun("First")),
            DocxParagraphLayout(pageBreakBefore = true)))) { assertEquals(1, it.pageCount) }
    }

    @Test fun pageBreakBeforeLaterParagraphStartsNewPage() {
        withPdf(listOf(DocxBlock.Paragraph(listOf(DocxRun("First"))),
            DocxBlock.Paragraph(listOf(DocxRun("Next")), DocxParagraphLayout(pageBreakBefore = true)))) {
            assertEquals(2, it.pageCount)
        }
    }

    @Test fun centeredParagraphMovesActualTextToPageCenter() {
        withPdf(listOf(DocxBlock.Paragraph(listOf(DocxRun("MMMM")),
            DocxParagraphLayout(alignment = DocxAlignment.CENTER)))) { pdf ->
            pdf.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    var firstInk = page.width
                    for (y in 60..110) for (x in 0 until page.width) {
                        val pixel = bitmap.getPixel(x, y)
                        if (Color.red(pixel) < 80 && Color.green(pixel) < 80 && Color.blue(pixel) < 80)
                            firstInk = minOf(firstInk, x)
                    }
                    assertTrue("Centered text should start near the page center, was $firstInk", firstInk in 260..300)
                } finally { bitmap.recycle() }
            }
        }
    }

    @Test fun manualLineBreakProducesTwoSeparateTextLines() {
        withPdf(listOf(DocxBlock.Paragraph(listOf(DocxRun("MMMM\nMMMM"))))) { pdf ->
            pdf.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val rows = (60..120).filter { y -> (70..120).any { x ->
                        val pixel = bitmap.getPixel(x, y)
                        Color.red(pixel) < 80 && Color.green(pixel) < 80 && Color.blue(pixel) < 80
                    } }
                    val bands = rows.filterIndexed { index, row -> index == 0 || row > rows[index - 1] + 1 }.size
                    assertEquals(2, bands)
                } finally { bitmap.recycle() }
            }
        }
    }
    @Test fun simultaneousThumbnailAndViewerConversionsAllProduceUsablePages() = kotlinx.coroutines.runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File.createTempFile("word-race-", ".docx", context.cacheDir)
        java.util.zip.ZipOutputStream(source.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("[Content_Types].xml"))
            zip.write("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("_rels/.rels"))
            zip.write("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="office" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
            zip.write(("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>" + java.util.UUID.randomUUID() + "</w:t></w:r></w:p></w:body></w:document>").toByteArray())
            zip.closeEntry()
        }
        val files = mutableSetOf<File>()
        try {
            val start = kotlinx.coroutines.CompletableDeferred<Unit>()
            val tasks = (1..8).map {
                async(kotlinx.coroutines.Dispatchers.IO) {
                    start.await()
                    DocxConverter.convertDocxToPdf(context, android.net.Uri.fromFile(source), source.name)
                }
            }
            start.complete(Unit)
            val results = tasks.map { it.await() }
            results.mapNotNull { it.pdfFile }.forEach { files.add(it) }
            assertTrue("All simultaneous opens must succeed: $results", results.all { it is com.abhisheksati.mydoc.rendering.engine.OfficeConversion.Success })
            for (file in files) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { assertEquals(1, it.pageCount) }
                }
            }
        } finally {
            source.delete()
            files.forEach { file -> file.delete(); File(file.path + ".note").delete(); File(file.path + ".outline").delete() }
        }
    }
    private fun pictureBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(120, 60, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    @Test fun embeddedPictureProducesVisiblePixelsOnConvertedPage() {
        val rendered = requireNotNull(DocxPdfRenderer.render(listOf(
            DocxBlock.Image(pictureBytes(), 72f, 36f, "Red rectangle")
        ), byteArrayOf()))
        val file = File.createTempFile("word-picture-", ".pdf", InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)
        try {
            file.outputStream().use { rendered.doc.writeTo(it) }
            rendered.doc.close()
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { pdf ->
                    assertEquals(1, pdf.pageCount)
                    pdf.openPage(0).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        try {
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            var red = 0
                            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                                val pixel = bitmap.getPixel(x,y)
                                if (Color.red(pixel) > 220 && Color.green(pixel) < 40 && Color.blue(pixel) < 40) red++
                            }
                            assertTrue("The picture must occupy approximately 72 x 36 points", red in 2400..2800)
                        } finally { bitmap.recycle() }
                    }
                }
            }
        } finally { file.delete() }
    }

    @Test fun invalidImageShowsPlaceholderWithoutBreakingConversion() {
        val rendered = requireNotNull(DocxPdfRenderer.render(listOf(
            DocxBlock.Image(byteArrayOf(1,2,3), 72f,36f,"Missing picture"),
            DocxBlock.Paragraph(listOf(DocxRun("Text after picture")))
        ), byteArrayOf()))
        rendered.doc.close()
        assertNull(DocxImageDecoder.decode(byteArrayOf(1,2,3)))
    }

    @Test fun largeRasterIsSampledBeforePixelAllocation() {
        val bitmap = Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes)
        bitmap.recycle()
        val decoded = requireNotNull(DocxImageDecoder.decode(bytes.toByteArray()))
        try { assertTrue(decoded.width.toLong() * decoded.height <= 4_000_000) }
        finally { decoded.recycle() }
    }
}
