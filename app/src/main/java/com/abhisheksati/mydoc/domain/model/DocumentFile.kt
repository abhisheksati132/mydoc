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
        "xps", "oxps" -> "XPS"
        "cbz" -> "CBZ"
        "docx", "doc" -> "WORD"
        "xlsx", "xls" -> "EXCEL"
        "pptx", "ppt", "ppsx", "pps", "potx", "pot" -> "PPT"
        "txt" -> "TXT"
        "csv" -> "CSV"
        else -> extension.uppercase(Locale.ROOT).take(5).ifEmpty { "DOC" }
    }

    val category: DocumentCategory = when (extension) {
        "pdf" -> DocumentCategory.PDF
        "epub", "xps", "oxps", "cbz", "fb2" -> DocumentCategory.EPUB
        "docx", "doc" -> DocumentCategory.WORD
        "xlsx", "xls", "csv" -> DocumentCategory.EXCEL
        "pptx", "ppt", "ppsx", "pps", "potx", "pot" -> DocumentCategory.PPT
        "txt", "log", "md", "json", "xml", "yaml", "yml", "ini", "conf", "sh",
        "sql", "kt", "java", "py", "html", "css", "js", "ts", "c", "cpp", "h",
        "gradle", "properties", "bat" -> DocumentCategory.TEXT
        else -> DocumentCategory.ALL
    }

    val formattedSize: String = formatBytesPrecise(sizeBytes)

    val formattedLastOpened: String = formatRelativeTimestamp(lastOpenedTimestamp)
}
