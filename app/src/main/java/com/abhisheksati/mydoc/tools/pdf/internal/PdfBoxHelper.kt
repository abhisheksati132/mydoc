package com.abhisheksati.mydoc.tools.pdf.internal

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

object PdfBoxHelper {
    private var isInitialized = false

    fun ensureInitialized(context: Context) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    isInitialized = true
                }
            }
        }
    }
}
