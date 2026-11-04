package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.rendering.engine.word.DocxConverter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * H-7 on device: the size budget has to stop a huge document before its bytes are buffered, and the
 * failure has to say so instead of blaming unimplemented features.
 */
@RunWith(AndroidJUnit4::class)
class OfficeConversionBudgetOnDeviceTest {

    private lateinit var context: Context
    private lateinit var dir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "office-budget").apply {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }
    }

    @Test
    fun aDocumentOverTheBudgetIsRefusedBeforeItsBytesExist() {
        // Sparse file: the length is real, the 250 MB of content is never written. A reader that
        // buffered first would take seconds and die; this one must refuse on the length alone.
        val huge = File(dir, "huge.docx")
        RandomAccessFile(huge, "rw").use { it.setLength(250L * 1024 * 1024) }

        val started = System.nanoTime()
        val source = OfficeSourceReader.read(context, Uri.fromFile(huge), huge.name)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        val failed = source as? OfficeSource.Failed
        assertEquals(OfficeSourceFailure.TOO_LARGE, failed?.failure)
        assertTrue("refusal took ${elapsedMs}ms — bytes were buffered anyway", elapsedMs < 1_000)

        val failure = failed!!.asConversionFailure()
        assertEquals(LoadFailure.EXCEEDS_LIMITS, failure.failure)
        assertTrue(failure.message.contains("larger than MyDoc can convert"))
        assertFalse("must not blame features", failure.message.contains("features"))
        huge.delete()
    }

    @Test
    fun aRealDocxStillConvertsAndTheSecondOpenReusesTheCache() {
        val docx = File(dir, "report.docx")
        writeDocx(docx, "Budget check paragraph")

        runBlocking {
            val first = DocxConverter.convertDocxToPdf(context, Uri.fromFile(docx), docx.name)
            assertTrue("expected Success, got $first", first is OfficeConversion.Success)
            val pdf = (first as OfficeConversion.Success).pdf
            assertTrue("no PDF written", pdf.isFile && pdf.length() > 0L)
            assertTrue(
                "not a PDF",
                pdf.inputStream().use { it.readNBytes(6).decodeToString() }.startsWith("%PDF")
            )

            val second = DocxConverter.convertDocxToPdf(context, Uri.fromFile(docx), docx.name)
            assertEquals("the cached conversion was not reused", pdf, (second as OfficeConversion.Success).pdf)
        }

        docx.delete()
    }

    @Test
    fun anEmptyDocumentIsRefusedRatherThanAnnouncedAsValid() {
        val empty = File(dir, "empty.docx").apply { writeText("") }

        runBlocking {
            val result = DocxConverter.convertDocxToPdf(context, Uri.fromFile(empty), empty.name)
            assertTrue("expected Failure, got $result", result is OfficeConversion.Failure)
            assertFalse(
                "an empty document must not be announced as a features problem",
                (result as OfficeConversion.Failure).message.contains("features our converter")
            )
        }

        empty.delete()
    }

    private fun writeDocx(file: File, text: String) {
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
                   <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                     <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                     <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                   </Types>""".toByteArray()
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
                   <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                     <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                   </Relationships>""".toByteArray()
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
                   <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                     <w:body>
                       <w:p><w:r><w:t>$text</w:t></w:r></w:p>
                     </w:body>
                   </w:document>""".toByteArray()
            )
            zip.closeEntry()
        }
    }
}
