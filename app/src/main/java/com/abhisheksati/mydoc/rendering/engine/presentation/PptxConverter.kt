package com.abhisheksati.mydoc.rendering.engine.presentation

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object PptxConverter {

    /**
     * Converts a .pptx file into a high-fidelity vector PDF file and caches it.
     * Returns the cached File, or null if parsing fails.
     */
    fun convertPptxToPdf(context: Context, uri: Uri, displayName: String): File? {
        val bytes = try {
            com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, displayName)?.use {
                it.readBytes()
            }
        } catch (_: Throwable) {
            null
        } ?: return null

        val cacheDir = File(context.cacheDir, "converted_presentations").apply { mkdirs() }
        val targetPdf = File(cacheDir, "pptx_${displayName.hashCode()}_${bytes.size}.pdf")

        if (targetPdf.exists() && targetPdf.length() > 0L) {
            return targetPdf
        }

        val pdfDoc = PptxRenderer.render(bytes) ?: return null
        return try {
            FileOutputStream(targetPdf).use { out ->
                pdfDoc.writeTo(out)
            }
            targetPdf
        } catch (_: Throwable) {
            targetPdf.delete()
            null
        } finally {
            pdfDoc.close()
        }
    }
}
