package com.abhisheksati.mydoc.rendering.engine.word

import android.content.Context
import android.net.Uri
import android.util.Log
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.util.DocumentIdentity
import com.abhisheksati.mydoc.rendering.engine.OutlineCache
import java.io.File
import java.io.FileOutputStream

object DocxConverter {

    private const val Tag = "DocxConvert"

    /**
     * Converts a .docx into a paginated vector PDF and caches it under a content-keyed name, so an
     * edited or replaced document never reuses the previous conversion.
     */
    suspend fun convertDocxToPdf(context: Context, uri: Uri, displayName: String): File? {
        val bytes = try {
            DocumentStreamResolver.openStream(context, uri)?.use { it.readBytes() }
        } catch (e: OutOfMemoryError) {
            Log.e(Tag, "OOM reading $displayName", e)
            return null
        } catch (t: Throwable) {
            Log.w(Tag, "could not read $displayName", t)
            null
        } ?: return null
        if (bytes.isEmpty()) return null
        if (bytes.size > 200 * 1024 * 1024) {
            Log.w(Tag, "$displayName exceeds 200 MB — refusing to convert")
            return null
        }

        val cacheDir = File(context.cacheDir, "converted_docs").apply { mkdirs() }
        val targetPdf = File(cacheDir, "docx_${DocumentIdentity.ofBytes(bytes)}_${bytes.size}.pdf")
        if (targetPdf.isFile && targetPdf.length() > 0L) return targetPdf

        val blocks = try {
            DocxParser().parse(bytes.inputStream()).blocks
        } catch (t: Throwable) {
            Log.w(Tag, "could not parse $displayName", t)
            return null
        }

        val rendered = try {
            DocxPdfRenderer.render(blocks, bytes)
        } catch (t: Throwable) {
            Log.w(Tag, "could not lay out $displayName", t)
            null
        } ?: run {
            Log.w(Tag, "no pages produced for $displayName")
            return null
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
            targetPdf
        } catch (t: Throwable) {
            Log.w(Tag, "could not write PDF for $displayName", t)
            staging.delete()
            targetPdf.delete()
            null
        } finally {
            rendered.doc.close()
        }
    }
}
