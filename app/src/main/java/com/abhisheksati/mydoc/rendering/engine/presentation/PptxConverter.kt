package com.abhisheksati.mydoc.rendering.engine.presentation

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.rendering.engine.NativeOfficeConverter
import com.abhisheksati.mydoc.rendering.engine.OfficeConversion
import com.abhisheksati.mydoc.rendering.engine.OutlineCache
import com.abhisheksati.mydoc.rendering.engine.OutlineItem

/** Impress renders the original deck; MyDoc's parser remains available for notes/slideshow. */
object PptxConverter {
    fun getSlideOutlines(pdfPath: String): List<OutlineItem> = OutlineCache.loadOutlines(pdfPath)

    suspend fun convertPptxToPdf(
        context: Context,
        uri: Uri,
        displayName: String,
        password: String? = null
    ): OfficeConversion = NativeOfficeConverter.convert(context, uri, displayName, password)
}
