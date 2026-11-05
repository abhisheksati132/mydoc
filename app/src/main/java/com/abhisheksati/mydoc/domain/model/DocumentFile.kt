package com.abhisheksati.mydoc.domain.model

import androidx.compose.runtime.Immutable
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class DocumentFile(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val lastOpenedTimestamp: Long,
    val pageCount: Int = 0,
    val lastPage: Int = 0,
    val isFavorite: Boolean = false
) {
    val extension: String = run {
        val dotIndex = displayName.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < displayName.length - 1) {
            displayName.substring(dotIndex + 1).lowercase(Locale.ROOT)
        } else ""
    }

    val typeBadge: String = when (extension) {
        "pdf" -> "PDF"
        "epub" -> "EPUB"
        "fb2" -> "FB2"
        "xps", "oxps" -> "XPS"
        "cbz" -> "CBZ"
        "docx", "doc" -> "WORD"
        "xlsx", "xls" -> "EXCEL"
        "pptx", "ppt", "ppsx", "pps", "potx", "pot", "pptm", "potm" -> "PPT"
        "txt" -> "TXT"
        "csv" -> "CSV"
        else -> extension.uppercase(Locale.ROOT).take(5).ifEmpty { "DOC" }
    }

    val category: DocumentCategory = DocumentFormats.categoryOf(extension)

    val formattedSize: String = formatBytesPrecise(sizeBytes)

    val formattedLastOpened: String = formatRelativeTimestamp(lastOpenedTimestamp)
}
