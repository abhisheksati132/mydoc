package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object XlsxConverter {

    private val sheetOutlinesMap = java.util.concurrent.ConcurrentHashMap<String, List<com.abhisheksati.mydoc.rendering.engine.OutlineItem>>()

    fun getSheetOutlines(pdfPath: String): List<com.abhisheksati.mydoc.rendering.engine.OutlineItem> =
        sheetOutlinesMap[pdfPath] ?: com.abhisheksati.mydoc.rendering.engine.OutlineCache.loadOutlines(pdfPath)

    suspend fun convertXlsxToPdf(
        context: Context,
        uri: Uri,
        displayName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val safeName = com.abhisheksati.mydoc.util.DocumentIdentity.ofText(uri.toString(), displayName)
            val stamp = DocumentStreamResolver.sourceStamp(context, uri)
            val targetPdf = File(context.cacheDir, "xlsx_converted_${safeName}_${stamp.first}_${stamp.second}.pdf")

            if (targetPdf.isFile && targetPdf.length() > 1024) {
                return@withContext targetPdf
            }

            // Obtain input stream either via SAF resolver or cached stream
            var inputStream = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            var tempResolvedFile: File? = null

            if (inputStream == null) {
                tempResolvedFile = File(context.cacheDir, "temp_xlsx_src_${System.currentTimeMillis()}.xlsx")
                val resolveResult = DocumentStreamResolver.resolveToCacheFile(
                    context = context,
                    uri = uri,
                    displayName = displayName,
                    targetCacheFile = tempResolvedFile
                )
                if (resolveResult.isSuccess && tempResolvedFile.exists()) {
                    inputStream = tempResolvedFile.inputStream()
                }
            }

            val stream = inputStream ?: return@withContext null
            val workbook = stream.use { XlsxReader.read(it) }

            // Clean up temporary source file if created
            tempResolvedFile?.delete()

            val renderResult = XlsxPdfRenderer.renderWithOutlines(workbook) ?: run {
                android.util.Log.w("XlsxConvert", "no printable range found in $displayName")
                return@withContext null
            }

            // Staged then moved into place, so an interrupted write can never be cached as valid.
            val staging = File(context.cacheDir, "xlsx_writing_${System.currentTimeMillis()}.pdf")
            FileOutputStream(staging).use { out ->
                renderResult.doc.writeTo(out)
                out.flush()
            }
            renderResult.doc.close()
            if (!staging.renameTo(targetPdf)) {
                staging.copyTo(targetPdf, overwrite = true)
                staging.delete()
            }
            sheetOutlinesMap[targetPdf.absolutePath] = renderResult.outlines
            com.abhisheksati.mydoc.rendering.engine.OutlineCache.saveOutlines(targetPdf, renderResult.outlines)

            if (targetPdf.isFile && targetPdf.length() > 0) targetPdf else null
        } catch (t: Throwable) {
            android.util.Log.w("XlsxConvert", "conversion failed for $displayName", t)
            null
        }
    }
}
