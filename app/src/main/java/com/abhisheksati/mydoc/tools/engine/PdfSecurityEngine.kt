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
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
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
            val ownerPwd = options.ownerPassword.ifBlank { userPwd }
            require(ownerPwd.none { it == ',' || it.isISOControl() }) {
                "Owner password cannot contain commas or control characters."
            }
            val permissions = if (options.allowPrinting) "print" else "no-print"

            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "protected", "pdf")
            srcDoc.save(outFile.absolutePath, "encryption=aes-256,user-password=$userPwd,owner-password=$ownerPwd,permissions=$permissions")
            onProgress(ToolProgress(100, 100, 95, "Password encryption complete.", false))
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun unlockPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
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

            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "unlocked", "pdf")
            destDoc.save(outFile.absolutePath, "encryption=none,compress")
            destDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun redactPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val query = options.redactQuery.ifBlank { "CONFIDENTIAL" }

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
                        }
                    }
                    // Burn redactions permanently
                    page.applyRedactions(true, PDFPage.REDACT_IMAGE_PIXELS, PDFPage.REDACT_LINE_ART_REMOVE_IF_COVERED, PDFPage.REDACT_TEXT_REMOVE)
                }
                page.destroy()
                val pct = 10 + (((p + 1).toFloat() / totalPages) * 80).toInt()
                onProgress(ToolProgress(p + 1, totalPages, pct, "Redacting page ${p + 1}...", false))
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

        try {
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    doc.documentCatalog?.metadata = null
                    doc.documentInformation = PDDocumentInformation()
                    FileOutputStream(intermediate).use { fos -> doc.save(fos) }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.w("PdfSecurityEngine", "PDFBox metadata clear failed: ${e.message}")
            file.copyTo(intermediate, overwrite = true)
        }

        // Secondary pass via MuPDF xref cleaner to destroy unreachable objects and clear /Info
        val srcDoc = Document.openDocument(intermediate.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
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
