package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * N-5 and N-6: image work has to stay inside a memory budget, land on the page the way the camera
 * intended, and actually produce pixels.
 */
@RunWith(AndroidJUnit4::class)
class ImageConversionMemoryOnDeviceTest {

    private lateinit var context: Context
    private lateinit var dir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dir = File(context.getExternalFilesDir(null), "image-memory").apply {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }
    }

    @Test
    fun aRotatedCameraPhotoLandsOnThePageUpright() {
        val landscape = Bitmap.createBitmap(160, 100, Bitmap.Config.ARGB_8888)
        val jpg = File(dir, "camera.jpg")
        FileOutputStream(jpg).use { landscape.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        landscape.recycle()
        ExifInterface(jpg.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val pdf = runBlocking {
            PdfConversionEngine(context).imagesToPdf(
                listOf(jpg),
                ToolOptions(pageSizeFormat = PageSizeFormat.FIT_IMAGE),
                onProgress = {}
            )
        }

        val doc = Document.openDocument(pdf.absolutePath)
        val bounds = doc.loadPage(0).bounds
        doc.destroy()

        assertTrue(
            "a 90 degree photo should become portrait, got ${bounds.x1 - bounds.x0} x ${bounds.y1 - bounds.y0}",
            bounds.y1 - bounds.y0 > bounds.x1 - bounds.x0
        )
    }

    @Test
    fun aLongImageExportStaysInsideThePixelBudget() {
        val source = writePages("twenty_pages.pdf", pages = 20, width = 595, height = 842)

        // 300 dpi asks for scale 3.0: twenty A4 pages would be 1785 x 50520 = 90 million pixels.
        val out = runBlocking {
            PdfConversionEngine(context).pdfToImagesZip(
                source,
                ToolOptions(exportAsLongImage = true, exportDpi = 300)
            ) {}
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(out.absolutePath, bounds)
        assertTrue("not a decodable image", bounds.outWidth > 0 && bounds.outHeight > 0)
        val pixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
        assertTrue("${bounds.outWidth} x ${bounds.outHeight} = $pixels pixels over budget", pixels <= 24_000_000L)
        assertTrue("the scale was never reduced: ${bounds.outWidth} px wide", bounds.outWidth < 595 * 3)
    }

    @Test
    fun exportingPagesAsImagesProducesOneDecodableImagePerPage() {
        val pages = 5
        val source = writePages("five_pages.pdf", pages = pages, width = 400, height = 300)

        val zip = runBlocking {
            PdfConversionEngine(context).pdfToImagesZip(
                source,
                ToolOptions(exportDpi = 150, exportFormatJpg = false)
            ) {}
        }

        val entries = ZipInputStream(FileInputStream(zip)).use { zis ->
            generateSequence { zis.nextEntry }.map { it.name }.toList()
        }
        assertEquals((1..pages).map { "page_%03d.png".format(it) }, entries)

        ZipInputStream(FileInputStream(zip)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val bytes = zis.readBytes()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                assertTrue("${entry.name} is not a decodable image", bounds.outWidth > 0 && bounds.outHeight > 0)
                assertTrue("${entry.name} carries no pixels", bytes.size > 100)
            }
        }
    }

    /**
     * Why the export paths had to change: MuPDF refuses to hand back the pixels of a pixmap rendered
     * without an alpha channel, so the shipped `toPixmap(matrix, DeviceRGB, false)` + `.pixels`
     * combination throws. Asserting the rejection keeps the requirement from being reverted.
     */
    @Test
    fun aPixmapWithoutAnAlphaChannelCannotBeReadAsPixels() {
        val source = writePages("one_page.pdf", pages = 1, width = 300, height = 200)
        val doc = Document.openDocument(source.absolutePath)
        val page = doc.loadPage(0)

        val alphaless = runCatching { page.toPixmap(Matrix(1f, 1f), ColorSpace.DeviceRGB, false).pixels }
        val withAlpha = runCatching { page.toPixmap(Matrix(1f, 1f), ColorSpace.DeviceRGB, true).pixels }

        page.destroy()
        doc.destroy()

        assertTrue("alpha-less pixels unexpectedly worked", alphaless.isFailure)
        assertTrue("alpha pixels failed for another reason: ${withAlpha.exceptionOrNull()}", withAlpha.isSuccess)
    }

    @Test
    fun anUndecodableImageYieldsNoPageAndAnHonestError() {
        val bogus = File(dir, "not_an_image.pdf").apply { writeText("plain text, not a picture") }

        val error = runCatching {
            runBlocking { PdfConversionEngine(context).imagesToPdf(listOf(bogus), ToolOptions(), onProgress = {}) }
        }.exceptionOrNull()

        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("could be decoded"))
    }

    @Test
    fun aPartialBatchStillProducesTheImagesThatDecoded() {
        val good = File(dir, "good.png")
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        FileOutputStream(good).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val bad = File(dir, "bad.png").apply { writeText("not a png") }

        val pdf = runBlocking {
            PdfConversionEngine(context).imagesToPdf(listOf(good, bad), ToolOptions(), onProgress = {})
        }
        assertTrue("no PDF produced", pdf.length() > 0L)

        val doc = Document.openDocument(pdf.absolutePath)
        assertEquals("the decodable image should still have been placed", 1, doc.countPages())
        doc.destroy()
    }

    private fun writePages(name: String, pages: Int, width: Int, height: Int): File {
        val file = File(dir, name)
        val doc = PdfDocument()
        repeat(pages) { index ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
            page.canvas.drawText(
                "Page ${index + 1}",
                60f,
                120f,
                Paint().apply { color = Color.BLACK; textSize = 28f }
            )
            doc.finishPage(page)
        }
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
