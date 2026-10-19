package com.abhisheksati.mydoc.rendering.engine.word

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import java.io.File

object DocxConverter {

    /**
     * Converts a .docx file into a high-fidelity vector PDF file and caches it.
     * Returns the cached PDF File, or null if conversion fails.
     */
    fun convertDocxToPdf(context: Context, uri: Uri, displayName: String): File? {
        val bytes = try {
            DocumentStreamResolver.openStream(context, uri, displayName)?.use {
                it.readBytes()
            }
        } catch (_: Throwable) {
            null
        } ?: return null

        val cacheDir = File(context.cacheDir, "converted_docs").apply { mkdirs() }
        val targetPdf = File(cacheDir, "docx_${displayName.hashCode()}_${bytes.size}.pdf")

        if (targetPdf.exists() && targetPdf.length() > 0L) {
            return targetPdf
        }

        val success = try {
            DocxWebRenderer.render(context, bytes, targetPdf)
        } catch (_: Throwable) {
            false
        }

        return if (success && targetPdf.exists() && targetPdf.length() > 0L) {
            targetPdf
        } else {
            targetPdf.delete()
            null
        }
    }
}
