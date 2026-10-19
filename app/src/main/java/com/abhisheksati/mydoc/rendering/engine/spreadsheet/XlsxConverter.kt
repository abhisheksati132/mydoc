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
        sheetOutlinesMap[pdfPath] ?: emptyList()

    suspend fun convertXlsxToPdf(
        context: Context,
        uri: Uri,
        displayName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetPdf = File(context.cacheDir, "xlsx_converted_${safeName.hashCode()}.pdf")

            // Re-use cached conversion if available and valid (> 1KB)
            if (targetPdf.exists() && targetPdf.length() > 1024) {
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

            val renderResult = XlsxPdfRenderer.renderWithOutlines(workbook) ?: return@withContext null
            sheetOutlinesMap[targetPdf.absolutePath] = renderResult.outlines

            FileOutputStream(targetPdf).use { out ->
                renderResult.doc.writeTo(out)
                out.flush()
            }
            renderResult.doc.close()

            if (targetPdf.exists() && targetPdf.length() > 0) {
                targetPdf
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
