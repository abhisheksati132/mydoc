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
    val extension: String
        get() {
            val dotIndex = displayName.lastIndexOf('.')
            return if (dotIndex != -1 && dotIndex < displayName.length - 1) {
                displayName.substring(dotIndex + 1).lowercase(Locale.ROOT)
            } else ""
        }

    val typeBadge: String
        get() = when (extension) {
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

    val category: DocumentCategory
        get() = when (extension) {
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

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "Unknown size"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    val formattedLastOpened: String
        get() {
            if (lastOpenedTimestamp <= 0) return ""
            val now = System.currentTimeMillis()
            val diff = now - lastOpenedTimestamp
            val oneMinute = 60 * 1000L
            val oneHour = 60 * oneMinute
            val oneDay = 24 * oneHour

            return when {
                diff < oneMinute -> "Just now"
                diff < oneHour -> "${diff / oneMinute}m ago"
                diff < oneDay -> "${diff / oneHour}h ago"
                diff < 2 * oneDay -> "Yesterday"
                else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastOpenedTimestamp))
            }
        }
}
