package com.abhisheksati.mydoc.rendering.engine.word

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.rendering.engine.OfficeConversion

/** Compatibility entry point shared by Word viewing and thumbnail generation. */
object DocxConverter {
    suspend fun convertDocxToPdf(context: Context, uri: Uri, displayName: String, password: String? = null): OfficeConversion =
        NativeWordConverter.convert(context, uri, displayName, password)
}
