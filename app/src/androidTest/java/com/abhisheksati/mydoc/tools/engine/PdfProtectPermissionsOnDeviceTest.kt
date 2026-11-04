package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.artifex.mupdf.fitz.Document
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * The Protect tool is only worth trusting if the file it produces is opened again and asked what
 * it allows. MuPDF owns the /Encrypt dictionary, so this reads the real output with the same
 * native library that wrote it and with a byte-level look at the /P integer it was given.
 *
 * Two things here were not knowable from the source: the writer option is "encrypt=", not
 * "encryption=" (the old string made every save throw "Unused pdf arguments found"), and a user
 * password that also serves as the owner password authenticates as the owner, which is granted
 * every permission whatever /P says.
 */
@RunWith(AndroidJUnit4::class)
class PdfProtectPermissionsOnDeviceTest {

    private lateinit var context: Context
    private lateinit var workDir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        PdfBoxHelper.ensureInitialized(context)
        workDir = File(context.getExternalFilesDir(null), "protect-permissions").apply {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }
    }

    private fun plainSource(): File {
        val file = File(workDir, "source.pdf")
        PDDocument().use { doc ->
            val page = PDPage()
            page.mediaBox = PDRectangle(595f, 842f)
            doc.addPage(page)
            FileOutputStream(file).use { doc.save(it) }
        }
        assertTrue("fixture PDF was not written", file.length() > 0)
        return file
    }

    private fun protect(source: File, allowPrinting: Boolean, allowCopying: Boolean, owner: String = ""): File =
        runBlocking {
            PdfSecurityEngine(context).protectPdf(
                source,
                ToolOptions(
                    userPassword = "user-password",
                    ownerPassword = owner,
                    allowPrinting = allowPrinting,
                    allowCopying = allowCopying
                )
            ) { }
        }

    /** /P is the only part of the encryption dictionary readable without a parser. */
    private fun permissionsField(pdf: File): Int {
        val text = pdf.readText(Charsets.ISO_8859_1)
        val match = Regex("/P\\s+(-?\\d+)").find(text)
        assertTrue("no /P entry in ${pdf.name}", match != null)
        return match!!.groupValues[1].toInt()
    }

    private fun openWithUserPassword(pdf: File): Document {
        val doc = Document.openDocument(pdf.absolutePath)
        assertTrue("${pdf.name} is not password protected", doc.needsPassword())
        assertTrue("${pdf.name} did not open with the user password", doc.authenticatePassword("user-password"))
        return doc
    }

    @Test
    fun everyPermissionCombinationIsWrittenAndEnforced() {
        data class Case(val printing: Boolean, val copying: Boolean, val expectedP: Int)
        val cases = listOf(
            Case(true, true, -4),
            Case(false, true, -4 - PdfPermissions.PRINT - PdfPermissions.PRINT_HIGH_QUALITY),
            Case(true, false, -4 - PdfPermissions.COPY - PdfPermissions.ACCESSIBILITY),
            Case(false, false, -4 - PdfPermissions.PRINT - PdfPermissions.PRINT_HIGH_QUALITY - PdfPermissions.COPY - PdfPermissions.ACCESSIBILITY)
        )
        cases.forEach { case ->
            val out = protect(plainSource(), case.printing, case.copying)
            assertEquals("P value for print=${case.printing} copy=${case.copying}", case.expectedP, permissionsField(out))
            val doc = openWithUserPassword(out)
            try {
                assertEquals(
                    "printing ${if (case.printing) "allowed" else "denied"} for ${out.name}",
                    case.printing, doc.hasPermission(Document.PERMISSION_PRINT)
                )
                assertEquals(
                    "copying ${if (case.copying) "allowed" else "denied"} for ${out.name}",
                    case.copying, doc.hasPermission(Document.PERMISSION_COPY)
                )
                assertEquals(1, doc.countPages())
            } finally {
                doc.destroy()
            }
        }
    }

    @Test
    fun denyingPrintingAlsoDeniesFullResolutionPrinting() {
        val out = protect(plainSource(), allowPrinting = false, allowCopying = true)
        val p = permissionsField(out)
        // The original defect: the -64 base left bit 12 set, so a PDF 1.7 reader could still
        // print at full quality while the switch said it could not.
        assertEquals(0, p and PdfPermissions.PRINT_HIGH_QUALITY)
        assertEquals(0, p and PdfPermissions.PRINT)
    }

    @Test
    fun denyingCopyingAlsoDeniesTheAccessibilityExtractionBit() {
        val out = protect(plainSource(), allowPrinting = true, allowCopying = false)
        val p = permissionsField(out)
        assertEquals(0, p and PdfPermissions.ACCESSIBILITY)
        assertEquals(0, p and PdfPermissions.COPY)
    }

    @Test
    fun theProtectedFileCannotBeOpenWithoutThePassword() {
        val out = protect(plainSource(), allowPrinting = false, allowCopying = false)
        val doc = Document.openDocument(out.absolutePath)
        try {
            assertTrue(doc.needsPassword())
            assertFalse("an empty password opened a password-protected file", doc.authenticatePassword(""))
            assertFalse("a wrong password opened a password-protected file", doc.authenticatePassword("wrong"))
            assertTrue(doc.authenticatePassword("user-password"))
        } finally {
            doc.destroy()
        }
    }

    @Test
    fun pageCountIsKnownBeforeAuthenticationSoZeroPagesMeansDamagedNotEncrypted() {
        // Measured, not assumed: MuPDF reports the real page count of an encrypted file before
        // any password is offered. The viewer can therefore treat "0 pages" as damage without
        // mistaking an encrypted document for a broken one, and must use needsPassword() to
        // decide whether to ask for a password.
        val out = protect(plainSource(), allowPrinting = true, allowCopying = true)
        val doc = Document.openDocument(out.absolutePath)
        try {
            assertTrue(doc.needsPassword())
            assertEquals(1, doc.countPages())
        } finally {
            doc.destroy()
        }
    }

    @Test
    fun theEncryptionDictionaryIsWrittenInPlaintext() {
        val out = protect(plainSource(), allowPrinting = false, allowCopying = false)
        val text = out.readText(Charsets.ISO_8859_1)
        assertTrue("no /Encrypt dictionary", text.contains("/Encrypt"))
        assertTrue(
            "no standard security handler",
            Regex("/Filter\\s*/Standard").containsMatchIn(text)
        )
        assertTrue("no AES-256 crypt filter", text.contains("AESV3"))
    }

    @Test
    fun anOwnerPasswordEqualToTheUserPasswordIsRefusedRatherThanFalselyReported() {
        // With owner == user, authenticating the user password satisfies the owner entry and every
        // permission is granted no matter what /P says. The tool must not call that "protected".
        val failure = runCatching {
            protect(plainSource(), allowPrinting = false, allowCopying = false, owner = "user-password")
        }.exceptionOrNull()
        assertTrue("expected the verification step to refuse the file, got $failure", failure != null)
        assertTrue(
            "unexpected refusal reason: ${failure?.message}",
            failure is IllegalStateException && failure.message?.contains("did not match") == true
        )
    }

    @Test
    fun unlockingAFileProducedByTheToolRemovesProtection() {
        val out = protect(plainSource(), allowPrinting = false, allowCopying = false)
        val unlocked = runBlocking {
            PdfSecurityEngine(context).unlockPdf(
                out,
                ToolOptions(userPassword = "user-password")
            ) { }
        }
        val doc = Document.openDocument(unlocked.absolutePath)
        try {
            assertFalse("unlock left the file password protected", doc.needsPassword())
            assertEquals(1, doc.countPages())
        } finally {
            doc.destroy()
        }
    }
}
