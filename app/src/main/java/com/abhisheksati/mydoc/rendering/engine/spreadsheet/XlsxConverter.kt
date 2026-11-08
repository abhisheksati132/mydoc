package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.rendering.engine.NativeOfficeConverter
import com.abhisheksati.mydoc.rendering.engine.OfficeConversion
import com.abhisheksati.mydoc.rendering.engine.OutlineCache
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import java.io.File

/** Calc provides page layout, charts and drawings; the independent cell grid stays available. */
object XlsxConverter {
    fun getSheetOutlines(pdfPath: String): List<OutlineItem> = OutlineCache.loadOutlines(pdfPath)

    suspend fun convertToPages(
        context: Context,
        uri: Uri,
        displayName: String,
        password: String? = null
    ): OfficeConversion = NativeOfficeConverter.convert(context, uri, displayName, password)

    /** Compatibility helper for thumbnail and existing PDF callers. */
    suspend fun convertXlsxToPdf(context: Context, uri: Uri, displayName: String): File? =
        convertToPages(context, uri, displayName).pdfFile
}
