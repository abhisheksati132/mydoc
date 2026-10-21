package com.abhisheksati.mydoc.rendering.engine.presentation

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.rendering.engine.OutlineCache
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object PptxConverter {

    private val slideOutlinesMap = ConcurrentHashMap<String, List<OutlineItem>>()

    fun getSlideOutlines(pdfPath: String): List<OutlineItem> =
        slideOutlinesMap[pdfPath] ?: OutlineCache.loadOutlines(pdfPath)

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

        // Apply orphan placeholder recovery to resolve cut-off titles and missing master styles
        val processedBytes = runCatching {
            val tempPptx = File(cacheDir, "temp_${System.currentTimeMillis()}.pptx")
            tempPptx.writeBytes(bytes)
            PptxPlaceholderFix.apply(tempPptx)
            val fixed = tempPptx.readBytes()
            tempPptx.delete()
            fixed
        }.getOrDefault(bytes)

        val renderResult = PptxRenderer.renderWithOutlines(processedBytes) ?: return null
        slideOutlinesMap[targetPdf.absolutePath] = renderResult.outlines
        OutlineCache.saveOutlines(targetPdf, renderResult.outlines)

        val pdfDoc = renderResult.doc
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
