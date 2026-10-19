package com.abhisheksati.mydoc.tools.pdf.form

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfFlattener {

    fun flatten(context: Context, sourceFile: File, destinationFile: File): Int {
        PdfBoxHelper.ensureInitialized(context)
        var fieldCount = 0
        FileInputStream(sourceFile).use { input ->
            PDDocument.load(input).use { doc ->
                val acroForm = doc.documentCatalog?.acroForm
                if (acroForm != null) {
                    fieldCount = acroForm.fields?.size ?: 0
                    if (fieldCount > 0) {
                        runCatching { acroForm.flatten() }
                    }
                }
                FileOutputStream(destinationFile).use { output ->
                    doc.save(output)
                }
            }
        }
        return fieldCount
    }

    fun flatten(context: Context, sourceUri: Uri, destinationUri: Uri): Int {
        PdfBoxHelper.ensureInitialized(context)
        var fieldCount = 0
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            PDDocument.load(input).use { doc ->
                val acroForm = doc.documentCatalog?.acroForm
                if (acroForm != null) {
                    fieldCount = acroForm.fields?.size ?: 0
                    if (fieldCount > 0) {
                        runCatching { acroForm.flatten() }
                    }
                }
                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    doc.save(output)
                } ?: throw IllegalStateException("Unable to write flattened PDF")
            }
        } ?: throw IllegalStateException("Unable to read PDF")
        return fieldCount
    }
}
