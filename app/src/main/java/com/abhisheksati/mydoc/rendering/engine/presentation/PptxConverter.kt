package com.abhisheksati.mydoc.rendering.engine.presentation

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.rendering.engine.OfficeConversion
import com.abhisheksati.mydoc.rendering.engine.OfficeSource
import com.abhisheksati.mydoc.rendering.engine.OfficeSourceReader
import com.abhisheksati.mydoc.rendering.engine.asConversionFailure
import com.abhisheksati.mydoc.util.DocumentIdentity
import com.abhisheksati.mydoc.rendering.engine.OutlineCache
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.util.SecureLog
import kotlinx.coroutines.CancellationException
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
    fun convertPptxToPdf(context: Context, uri: Uri, displayName: String): OfficeConversion {
        val source = OfficeSourceReader.read(context, uri, displayName)
        val bytes = when (source) {
            is OfficeSource.Ready -> source.bytes
            is OfficeSource.Failed -> return source.asConversionFailure()
        }

        val cacheDir = File(context.cacheDir, "converted_presentations").apply { mkdirs() }
        val targetPdf = File(cacheDir, "pptx_${DocumentIdentity.ofBytes(bytes)}_${bytes.size}.pdf")

        if (targetPdf.isFile && targetPdf.length() > 0L) {
            // The disclosure travels with the cached PDF: a deck converted once as partial must
            // still say so the next time it is opened.
            val noteFile = File(cacheDir, "${targetPdf.name}.note")
            val cachedNote = runCatching { if (noteFile.isFile) noteFile.readText() else null }
                .getOrNull()?.takeIf { it.isNotBlank() }
            return OfficeConversion.Success(targetPdf, conversionNote = cachedNote)
        }

        // Apply orphan placeholder recovery to resolve cut-off titles and missing master styles
        // The temp copy is deleted in finally: if PptxPlaceholderFix throws after the write,
        // the previous code left the file in the cache directory forever.
        val processedBytes = run {
            val tempPptx = File(cacheDir, "temp_${System.currentTimeMillis()}.pptx")
            val recovered: ByteArray? = try {
                tempPptx.writeBytes(bytes)
                PptxPlaceholderFix.apply(tempPptx)
                (OfficeSourceReader.readFile(tempPptx) as? OfficeSource.Ready)?.bytes
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                android.util.Log.w("PptxConvert", "placeholder recovery skipped", t)
                null
            } finally {
                tempPptx.delete()
            }
            recovered ?: bytes
        }

        val renderResult = PptxRenderer.renderWithOutlines(processedBytes) ?: run {
            SecureLog.warn("PptxConvert", "no slides laid out for presentation") { displayName }
            return layoutFailure()
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
            val truncationNote = if (renderResult.droppedSlides > 0) {
                "Only the first ${renderResult.outlines.size} slides were converted. " +
                    "${renderResult.droppedSlides} more are not shown."
            } else {
                null
            }
            val noteFile = File(cacheDir, "${targetPdf.name}.note")
            if (truncationNote != null) {
                runCatching { noteFile.writeText(truncationNote) }
            } else {
                runCatching { noteFile.delete() }
            }
            OfficeConversion.Success(targetPdf, conversionNote = truncationNote)
        } catch (t: Throwable) {
            SecureLog.warn("PptxConvert", "could not write PDF for presentation", t) { displayName }
            staging.delete()
            targetPdf.delete()
            layoutFailure()
        } finally {
            pdfDoc.close()
        }
    }

    /** The file was read but produced no slides; only the converter knows it lays out slides. */
    private fun layoutFailure() = OfficeConversion.Failure(
        LoadFailure.CONVERSION_FAILED,
        "MyDoc read the file but could not lay out its slides. " +
            "It likely uses features our converter does not implement."
    )
}
