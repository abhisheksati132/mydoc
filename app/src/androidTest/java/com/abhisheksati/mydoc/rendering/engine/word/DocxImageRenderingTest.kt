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
    @Test fun simultaneousThumbnailAndViewerConversionsAllProduceUsablePages() = kotlinx.coroutines.runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File.createTempFile("word-race-", ".docx", context.cacheDir)
        java.util.zip.ZipOutputStream(source.outputStream()).use { zip ->
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
