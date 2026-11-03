package com.abhisheksati.mydoc.tools.pdf.internal

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

object PdfBoxHelper {
    private var isInitialized = false

    fun ensureInitialized(context: Context) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    val app = runCatching { context.applicationContext }.getOrNull() ?: context
                    runCatching { PDFBoxResourceLoader.init(app) }
                    isInitialized = true
                }
            }
        }
    }
}
