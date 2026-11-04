package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.ToolProgress
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.PDFAnnotation
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFPage
import com.artifex.mupdf.fitz.Rect
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Specialized engine handling PDF security, encryption, decryption, redaction, and metadata sanitization.
 */
class PdfSecurityEngine(private val context: Context) {

    suspend fun protectPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = try {
            Document.openDocument(file.absolutePath) as? PDFDocument
        } catch (_: Throwable) {
            null
        } ?: throw IllegalArgumentException("Cannot open document or native engine unavailable")
        try {
            onProgress(ToolProgress(1, 100, 30, "Encrypting with AES-256...", false))
            val userPwd = options.userPassword
            require(userPwd.isNotBlank()) {
                "Enter a password before protecting the document."
            }
            // MuPDF takes these as a comma-separated option string, so a comma would
            // let a password rewrite the rest of the options (for example permissions).
            require(userPwd.none { it == ',' || it.isISOControl() }) {
                "Password cannot contain commas or control characters."
            }
            val ownerPwd = options.ownerPassword.ifBlank { randomOwnerPassword() }
            require(ownerPwd.none { it == ',' || it.isISOControl() }) {
                "Owner password cannot contain commas or control characters."
            }
            // /P is written as a signed 32-bit integer by MuPDF. See PdfPermissions for the bit
            // table and for why the value is built by clearing denied bits from the unrestricted
            // base rather than setting allowed bits into a restricted one.
            val permissions = PdfPermissions.mask(
                allowPrinting = options.allowPrinting,
                allowCopying = options.allowCopying
            ).toString()

            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "protected", "pdf")
            // The key is "encrypt=". "encryption=" is not a writer option in this MuPDF and the
            // save throws "Unused pdf arguments found", which is what this call used to send.
            srcDoc.save(
                outFile.absolutePath,
                "encrypt=aes-256,user-password=$userPwd,owner-password=$ownerPwd,permissions=$permissions"
            )
            // A user password that doubles as the owner password authenticates as the owner, and
            // the owner is granted every permission regardless of /P — so the two switches on
            // screen would be written to the file and then ignored. Check the file we just made
            // reports what the user asked for before calling it protected.
            verifyDeclaredPermissions(outFile, userPwd, options.allowPrinting, options.allowCopying)
            onProgress(ToolProgress(100, 100, 95, "Password encryption complete.", false))
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    /**
     * The owner password keeps the permissions enforceable; the document owner is the person
     * holding the user password, so nothing here needs to be remembered by them.
     */
    private fun randomOwnerPassword(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyDeclaredPermissions(file: File, userPassword: String, allowPrinting: Boolean, allowCopying: Boolean) {
        val doc = try {
            Document.openDocument(file.absolutePath)
        } catch (_: Throwable) {
            throw IllegalStateException("The protected document could not be reopened to check it.")
        }
        try {
            if (!doc.needsPassword()) {
                throw IllegalStateException("The document was written without password protection.")
            }
            if (!doc.authenticatePassword(userPassword)) {
                throw IllegalStateException("The document did not open with the password it was given.")
            }
            val printing = runCatching { doc.hasPermission(Document.PERMISSION_PRINT) }.getOrNull()
            val copying = runCatching { doc.hasPermission(Document.PERMISSION_COPY) }.getOrNull()
            if (printing != allowPrinting || copying != allowCopying) {
                throw IllegalStateException(
                    "The permissions in the protected document did not match the ones selected " +
                        "(printing ${if (allowPrinting) "on" else "off"}, copying ${if (allowCopying) "on" else "off"})."
                )
            }
        } finally {
            doc.destroy()
        }
    }

    suspend fun unlockPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = try {
            Document.openDocument(file.absolutePath) as? PDFDocument
        } catch (_: Throwable) {
            null
        } ?: throw IllegalArgumentException("Cannot open document or native engine unavailable")
        try {
            if (srcDoc.needsPassword()) {
                val ok = srcDoc.authenticatePassword(options.userPassword)
                if (!ok) throw IllegalArgumentException("Invalid document password provided.")
            }

            onProgress(ToolProgress(1, 100, 40, "Removing encryption...", false))
            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "unlocked", "pdf")
            // "encrypt=none" is the writer option that drops the encryption dictionary. The
            // "encryption=none" this used to send is not an option at all, so the save threw
            // "Unused pdf arguments found" and the tool never produced a file. Writing the
            // opened document also keeps the outline, form fields and annotations that the
            // page-by-page rebuild used to copy.
            srcDoc.save(outFile.absolutePath, "compress,encrypt=none")
            verifyUnlocked(outFile)
            onProgress(ToolProgress(100, 100, 95, "Encryption removed.", false))
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    private fun verifyUnlocked(file: File) {
        val doc = try {
            Document.openDocument(file.absolutePath)
        } catch (_: Throwable) {
            throw IllegalStateException("The unlocked document could not be reopened to check it.")
        }
        try {
            if (doc.needsPassword()) {
                throw IllegalStateException("The document is still password protected.")
            }
            if (doc.countPages() <= 0) {
                throw IllegalStateException("The unlocked document has no pages.")
            }
        } finally {
            doc.destroy()
        }
    }

    suspend fun redactPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val query = options.redactQuery.ifBlank { "CONFIDENTIAL" }
        val srcDoc = try {
            Document.openDocument(file.absolutePath) as? PDFDocument
        } catch (_: Throwable) {
            null
        }

        if (srcDoc == null) {
            val containsQuery = try {
                PdfBoxHelper.ensureInitialized(context)
                FileInputStream(file).use { fis ->
                    PDDocument.load(fis).use { doc ->
                        val stripper = PDFTextStripper()
                        val text = stripper.getText(doc)
                        text.contains(query, ignoreCase = true)
                    }
                }
            } catch (_: Throwable) {
                file.readText(Charsets.ISO_8859_1).contains(query, ignoreCase = true)
            }
            if (!containsQuery) {
                throw IllegalArgumentException("No match for \"$query\" was found. Nothing was redacted.")
            }
            throw IllegalStateException("Cannot securely redact document: native redaction engine failed to process document structure.")
        }
        try {
            val totalPages = srcDoc.countPages()
            val query = options.redactQuery.ifBlank { "CONFIDENTIAL" }
            var redacted = 0

            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                val page = srcDoc.loadPage(p) as? PDFPage ?: continue
                val quads = page.search(query)
                if (quads != null && quads.isNotEmpty()) {
                    for (hit in quads) {
                        for (q in hit) {
                            val annot = page.createAnnotation(PDFAnnotation.TYPE_REDACT)
                            annot.rect = Rect(q.ul_x, q.ul_y, q.lr_x, q.lr_y)
                            annot.color = floatArrayOf(0f, 0f, 0f)
                            annot.update()
                            redacted++
                        }
                    }
                    // Burn redactions permanently
                    page.applyRedactions(true, PDFPage.REDACT_IMAGE_PIXELS, PDFPage.REDACT_LINE_ART_REMOVE_IF_COVERED, PDFPage.REDACT_TEXT_REMOVE)
                }
                page.destroy()
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Redacting page ${p + 1}...", false))
            }

            if (redacted == 0) {
                throw IllegalArgumentException("No match for \"$query\" was found. Nothing was redacted.")
            }

            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "redacted", "pdf")
            srcDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun stripMetadata(file: File, onProgress: (ToolProgress) -> Unit): File {
        onProgress(ToolProgress(1, 100, 25, "Purging XMP streams & author tags...", false))
        PdfBoxHelper.ensureInitialized(context)
        val intermediate = PdfToolUtils.createTempOutputFile(context.cacheDir, "pdfbox_stripped", "pdf")

        var pdfBoxSucceeded = false
        try {
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    doc.documentCatalog?.metadata = null
                    doc.documentInformation = PDDocumentInformation()
                    FileOutputStream(intermediate).use { fos -> doc.save(fos) }
                    pdfBoxSucceeded = true
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.w("PdfSecurityEngine", "PDFBox metadata clear failed: ${e.message}")
            file.copyTo(intermediate, overwrite = true)
        }

        // Secondary pass via MuPDF xref cleaner to destroy unreachable objects and clear /Info
        val srcDoc = try {
            Document.openDocument(intermediate.absolutePath) as? PDFDocument
        } catch (_: Throwable) {
            null
        }

        if (srcDoc == null) {
            if (!pdfBoxSucceeded) {
                try { if (intermediate.exists()) intermediate.delete() } catch (_: Exception) {}
                throw IllegalStateException("Failed to sanitize metadata: document structure could not be processed by sanitization engines.")
            }
            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "sanitized", "pdf")
            intermediate.copyTo(outFile, overwrite = true)
            onProgress(ToolProgress(100, 100, 95, "Metadata purged.", false))
            try { if (intermediate.exists()) intermediate.delete() } catch (_: Exception) {}
            return outFile
        }

        try {
            srcDoc.setMetaData(Document.META_INFO_AUTHOR, "")
            srcDoc.setMetaData(Document.META_INFO_TITLE, "")
            srcDoc.setMetaData(Document.META_INFO_CREATOR, "")
            srcDoc.setMetaData(Document.META_INFO_PRODUCER, "")
            srcDoc.setMetaData(Document.META_INFO_SUBJECT, "")
            srcDoc.setMetaData(Document.META_INFO_KEYWORDS, "")

            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "sanitized", "pdf")
            srcDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
            onProgress(ToolProgress(100, 100, 95, "Metadata purged.", false))
            return outFile
        } finally {
            srcDoc.destroy()
            try { if (intermediate.exists()) intermediate.delete() } catch (_: Exception) {}
        }
    }
}
