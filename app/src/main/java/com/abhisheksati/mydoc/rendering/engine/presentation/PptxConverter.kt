package com.abhisheksati.mydoc.rendering.engine.presentation

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.rendering.engine.ConversionKey
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
        val targetPdf = File(cacheDir, "pptx_${ConversionKey.of(bytes)}_${bytes.size}.pdf")

        if (targetPdf.isFile && targetPdf.length() > 0L) {
            return targetPdf
        }

        // Apply orphan placeholder recovery to resolve cut-off titles and missing master styles
        // The temp copy is deleted in finally: if PptxPlaceholderFix throws after the write,
        // the previous code left the file in the cache directory forever.
        val processedBytes = run {
            val tempPptx = File(cacheDir, "temp_${System.currentTimeMillis()}.pptx")
            try {
                tempPptx.writeBytes(bytes)
                PptxPlaceholderFix.apply(tempPptx)
                tempPptx.readBytes()
            } catch (t: Throwable) {
                android.util.Log.w("PptxConvert", "placeholder recovery skipped: ${t.message}")
                bytes
            } finally {
                tempPptx.delete()
            }
        }

        val renderResult = PptxRenderer.renderWithOutlines(processedBytes) ?: run {
            android.util.Log.w("PptxConvert", "no slides laid out for $displayName")
            return null
        }

        // Staged then moved into place: a half-written cache file passes the size check above and
        // would then be reused for every later open.
        val staging = File(cacheDir, "${targetPdf.name}.writing")
        val pdfDoc = renderResult.doc
        return try {
            FileOutputStream(staging).use { out ->
                pdfDoc.writeTo(out)
                out.flush()
            }
            if (!staging.renameTo(targetPdf)) {
                staging.copyTo(targetPdf, overwrite = true)
                staging.delete()
            }
            slideOutlinesMap[targetPdf.absolutePath] = renderResult.outlines
            OutlineCache.saveOutlines(targetPdf, renderResult.outlines)
            targetPdf
        } catch (t: Throwable) {
            android.util.Log.w("PptxConvert", "could not write PDF for $displayName", t)
            staging.delete()
            targetPdf.delete()
            null
        } finally {
            pdfDoc.close()
        }
    }
}
