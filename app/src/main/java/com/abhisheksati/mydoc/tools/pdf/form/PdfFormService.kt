package com.abhisheksati.mydoc.tools.pdf.form

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDChoice
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDNonTerminalField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfFormService {

    enum class FieldType {
        TEXT,
        CHECKBOX,
        CHOICE
    }

    data class FormField(
        val name: String,
        val type: FieldType,
        val value: String,
        val options: List<String> = emptyList()
    )

    fun readFields(context: Context, file: File): List<FormField> {
        PdfBoxHelper.ensureInitialized(context)
        val fields = mutableListOf<FormField>()
        FileInputStream(file).use { input ->
            PDDocument.load(input).use { doc ->
                val acro = doc.documentCatalog?.acroForm ?: return emptyList()
                acro.fields?.forEach { collect(it, fields) }
            }
        }
        return fields
    }

    fun readFields(context: Context, uri: Uri): List<FormField> {
        PdfBoxHelper.ensureInitialized(context)
        val fields = mutableListOf<FormField>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc ->
                val acro = doc.documentCatalog?.acroForm ?: return emptyList()
                acro.fields?.forEach { collect(it, fields) }
            }
        }
        return fields
    }

    private fun collect(field: PDField, out: MutableList<FormField>) {
        when (field) {
            is PDNonTerminalField -> field.children?.forEach { collect(it, out) }
            is PDTextField -> out.add(
                FormField(
                    name = field.fullyQualifiedName ?: field.partialName ?: "",
                    type = FieldType.TEXT,
                    value = field.valueAsString ?: ""
                )
            )
            is PDCheckBox -> out.add(
                FormField(
                    name = field.fullyQualifiedName ?: field.partialName ?: "",
                    type = FieldType.CHECKBOX,
                    value = if (field.isChecked) "true" else "false"
                )
            )
            is PDChoice -> out.add(
                FormField(
                    name = field.fullyQualifiedName ?: field.partialName ?: "",
                    type = FieldType.CHOICE,
                    value = field.valueAsString ?: "",
                    options = runCatching { field.options ?: emptyList() }.getOrDefault(emptyList())
                )
            )
            else -> { /* Other non-editable buttons / signatures skipped */ }
        }
    }

    fun fill(
        context: Context,
        sourceFile: File,
        destinationFile: File,
        values: Map<String, String>,
        flatten: Boolean
    ): Int {
        PdfBoxHelper.ensureInitialized(context)
        var written = 0
        FileInputStream(sourceFile).use { input ->
            PDDocument.load(input).use { doc ->
                val acro = doc.documentCatalog?.acroForm
                    ?: throw IllegalStateException("This PDF has no fillable form fields")
                acro.fields?.forEach { written += applyValue(it, values) }
                if (flatten && written > 0) {
                    runCatching { acro.flatten() }
                }
                FileOutputStream(destinationFile).use { output ->
                    doc.save(output)
                }
            }
        }
        return written
    }

    fun fill(
        context: Context,
        sourceUri: Uri,
        destinationUri: Uri,
        values: Map<String, String>,
        flatten: Boolean
    ): Int {
        PdfBoxHelper.ensureInitialized(context)
        var written = 0
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            PDDocument.load(input).use { doc ->
                val acro = doc.documentCatalog?.acroForm
                    ?: throw IllegalStateException("This PDF has no fillable form fields")
                acro.fields?.forEach { written += applyValue(it, values) }
                if (flatten && written > 0) {
                    runCatching { acro.flatten() }
                }
                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    doc.save(output)
                } ?: throw IllegalStateException("Unable to write PDF output")
            }
        } ?: throw IllegalStateException("Unable to read source PDF")
        return written
    }

    private fun applyValue(field: PDField, values: Map<String, String>): Int {
        var count = 0
        val key = field.fullyQualifiedName ?: field.partialName ?: ""
        when (field) {
            is PDNonTerminalField -> field.children?.forEach { count += applyValue(it, values) }
            is PDTextField -> values[key]?.let {
                runCatching { field.setValue(it) }.onSuccess { count++ }
            }
            is PDCheckBox -> values[key]?.let {
                runCatching {
                    if (it.equals("true", ignoreCase = true) || it == "1" || it.equals("yes", ignoreCase = true)) {
                        field.check()
                    } else {
                        field.unCheck()
                    }
                }.onSuccess { count++ }
            }
            is PDChoice -> values[key]?.let {
                runCatching { field.setValue(it) }.onSuccess { count++ }
            }
            else -> {}
        }
        return count
    }
}
