package com.abhisheksati.mydoc.tools.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.PDFDocument
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Every writer option string the PDF tools send, checked against the native library that is
 * actually packaged.
 *
 * This test exists because the option vocabulary is not enforced at compile time and the failures
 * it produces are indistinguishable from a document problem at the far end of the tool: three of
 * the shipped strings ("encryption=aes-256", "encryption=none", "deflate") were never valid options
 * for this MuPDF and each save threw "Unused pdf arguments found", which the callers reported as a
 * failed operation. Accepted here, rejected there — that is the whole contract.
 */
@RunWith(AndroidJUnit4::class)
class PdfWriterOptionsOnDeviceTest {

    private lateinit var workDir: File
    private lateinit var source: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PdfBoxHelper.ensureInitialized(context)
        workDir = File(context.getExternalFilesDir(null), "writer-options").apply {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }
        source = File(workDir, "source.pdf")
        PDDocument().use { doc ->
            val page = PDPage()
            page.mediaBox = PDRectangle(595f, 842f)
            doc.addPage(page)
            FileOutputStream(source).use { doc.save(it) }
        }
    }

    private fun saveWith(option: String): File {
        val doc = Document.openDocument(source.absolutePath) as PDFDocument
        val out = File(workDir, "out-${System.nanoTime()}.pdf")
        try {
            doc.save(out.absolutePath, option)
            return out
        } finally {
            doc.destroy()
        }
    }

    @Test
    fun everyOptionTheToolsSendIsAccepted() {
        val usedByTheApp = listOf(
            "compress",
            "compress,clean,garbage=4",
            "compress,clean,garbage=3",
            "compress,clean,garbage=2",
            "compress,garbage=3",
            "compress,garbage=2",
            "compress,encrypt=none"
        )
        usedByTheApp.forEach { option ->
            val out = runCatching { saveWith(option) }
                .getOrElse { throw AssertionError("MuPDF rejected the option string '$option': ${it.message}", it) }
            assertTrue("'$option' produced no file", out.length() > 0)
        }
    }

    @Test
    fun optionsThatAreNotWriterOptionsAreRejected() {
        // Pinned so nobody reintroduces them: these are the strings that used to be in the code.
        listOf("encryption=aes-256", "encryption=none", "deflate", "progressive", "linear").forEach { option ->
            val result = runCatching { saveWith(option) }
            assertTrue("'$option' was unexpectedly accepted", result.isFailure)
            assertTrue(
                "unexpected reason for '$option': ${result.exceptionOrNull()?.message}",
                result.exceptionOrNull()?.message?.contains("Unused pdf arguments") == true
            )
        }
    }

    @Test
    fun theEncryptionVocabularyProducesTheFileTheToolClaims() {
        val protected = saveWith(
            "encrypt=aes-256,user-password=pw,owner-password=other-pw,permissions=${PdfPermissions.mask(false, false)}"
        )
        val doc = Document.openDocument(protected.absolutePath)
        try {
            assertTrue("the protected file is not password protected", doc.needsPassword())
            assertEquals(-4 - 2052 - 528, PdfPermissions.mask(allowPrinting = false, allowCopying = false))
        } finally {
            doc.destroy()
        }

        val writer = Document.openDocument(protected.absolutePath) as PDFDocument
        val open = File(workDir, "unlocked.pdf")
        try {
            writer.authenticatePassword("pw")
            writer.save(open.absolutePath, "compress,encrypt=none")
        } finally {
            writer.destroy()
        }
        val check = Document.openDocument(open.absolutePath)
        try {
            assertTrue("encrypt=none left the document protected", !check.needsPassword())
            assertEquals(1, check.countPages())
        } finally {
            check.destroy()
        }
    }
}
