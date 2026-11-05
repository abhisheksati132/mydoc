package com.abhisheksati.mydoc.rendering.engine.word

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.rendering.engine.OfficeConversion
import com.abhisheksati.mydoc.rendering.engine.OfficeSource
import com.abhisheksati.mydoc.rendering.engine.OfficeSourceReader
import com.abhisheksati.mydoc.rendering.engine.OutlineCache
import com.abhisheksati.mydoc.rendering.engine.asConversionFailure
import com.abhisheksati.mydoc.util.DocumentIdentity
import com.abhisheksati.mydoc.util.SecureLog
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.FileOutputStream

object DocxConverter {

    private const val Tag = "DocxConvert"

    /**
     * Converts a .docx into a paginated vector PDF and caches it under a content-keyed name, so an
     * edited or replaced document never reuses the previous conversion.
     */
    suspend fun convertDocxToPdf(context: Context, uri: Uri, displayName: String): OfficeConversion {
        val source = OfficeSourceReader.read(context, uri, displayName)
        val bytes = when (source) {
            is OfficeSource.Ready -> source.bytes
            is OfficeSource.Failed -> return source.asConversionFailure()
        }

        val cacheDir = File(context.cacheDir, "converted_docs").apply { mkdirs() }
        val targetPdf = File(cacheDir, "docx_${DocumentIdentity.ofBytes(bytes)}_${bytes.size}.pdf")
        if (targetPdf.isFile && targetPdf.length() > 0L) return OfficeConversion.Success(targetPdf)

        val blocks = try {
            DocxParser().parse(bytes.inputStream()).blocks
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            SecureLog.warn(Tag, "could not parse word document", t) { displayName }
            return layoutFailure()
        }

        val rendered = try {
            DocxPdfRenderer.render(blocks, bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            SecureLog.warn(Tag, "could not lay out word document", t) { displayName }
            null
        } ?: run {
            SecureLog.warn(Tag, "word document produced no pages") { displayName }
            return layoutFailure()
        }

        // Written aside and moved into place, because a half-written cache file looks valid to the
        // size check above and would be reused forever.
        val staging = File(cacheDir, "${targetPdf.name}.writing")
        return try {
            FileOutputStream(staging).use { out ->
                rendered.doc.writeTo(out)
                out.flush()
            }
            if (!staging.renameTo(targetPdf)) {
                staging.copyTo(targetPdf, overwrite = true)
                staging.delete()
            }
            OutlineCache.saveOutlines(targetPdf, rendered.outlines)
            OfficeConversion.Success(targetPdf)
        } catch (t: Throwable) {
            SecureLog.warn(Tag, "could not write PDF for word document", t) { displayName }
            staging.delete()
            targetPdf.delete()
            layoutFailure()
        } finally {
            rendered.doc.close()
        }
    }

    /** The file was read but produced no pages; only the converter knows it lays out pages. */
    private fun layoutFailure() = OfficeConversion.Failure(
        LoadFailure.CONVERSION_FAILED,
        "MyDoc read the file but could not lay out its pages. You can read it as reflowed text instead."
    )
}
