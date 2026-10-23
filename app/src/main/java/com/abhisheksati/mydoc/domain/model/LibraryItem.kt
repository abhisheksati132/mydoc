package com.abhisheksati.mydoc.domain.model

import androidx.compose.runtime.Immutable
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class MediaKind {
    DOCUMENT,
    PHOTO,
    VIDEO,
    AUDIO
}

@Immutable
data class LibraryItem(
    val uri: String,
    val name: String,
    val kind: MediaKind,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val folder: String? = null,
    val isFavorite: Boolean = false,
    val lastPage: Int = 0,
    val pageCount: Int = 0,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progressFraction: Float = 0f
) {
    val extension: String
        get() {
            val dot = name.lastIndexOf('.')
            return if (dot != -1 && dot < name.length - 1) name.substring(dot + 1).lowercase(Locale.ROOT) else ""
        }

    val typeBadge: String
        get() = when (kind) {
            MediaKind.DOCUMENT -> when (extension) {
                "pdf" -> "PDF"
                "epub" -> "EPUB"
                "docx", "doc" -> "WORD"
                "xlsx", "xls" -> "EXCEL"
                "pptx", "ppt" -> "PPT"
                "txt" -> "TXT"
                "csv" -> "CSV"
                else -> extension.uppercase(Locale.ROOT).take(5).ifEmpty { "DOC" }
            }
            MediaKind.PHOTO -> extension.uppercase(Locale.ROOT).take(4).ifEmpty { "IMG" }
            MediaKind.VIDEO -> extension.uppercase(Locale.ROOT).take(4).ifEmpty { "VID" }
            MediaKind.AUDIO -> extension.uppercase(Locale.ROOT).take(4).ifEmpty { "AUD" }
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

    val formattedProgress: String
        get() = when (kind) {
            MediaKind.DOCUMENT -> if (pageCount > 0) "Page ${lastPage + 1} of $pageCount" else ""
            MediaKind.VIDEO, MediaKind.AUDIO -> {
                val posSec = (lastPositionMs / 1000).coerceAtLeast(0)
                val durSec = (durationMs / 1000).coerceAtLeast(0)
                val posStr = String.format(Locale.US, "%02d:%02d", posSec / 60, posSec % 60)
                if (durSec > 0) {
                    val durStr = String.format(Locale.US, "%02d:%02d", durSec / 60, durSec % 60)
                    "$posStr / $durStr"
                } else {
                    posStr
                }
            }
            MediaKind.PHOTO -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastModified))
        }

    val formattedLastModified: String
        get() {
            if (lastModified <= 0) return ""
            val now = System.currentTimeMillis()
            val diff = now - lastModified
            val oneMinute = 60 * 1000L
            val oneHour = 60 * oneMinute
            val oneDay = 24 * oneHour

            return when {
                diff < oneMinute -> "Just now"
                diff < oneHour -> "${diff / oneMinute}m ago"
                diff < oneDay -> "${diff / oneHour}h ago"
                diff < 2 * oneDay -> "Yesterday"
                else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastModified))
            }
        }
}

fun LibraryItem.toMediaItem(): MediaItem {
    val parsedUri = android.net.Uri.parse(uri)
    return MediaItem(
        id = uri.hashCode().toLong(),
        uri = parsedUri,
        displayName = name,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
        dateAddedSec = lastModified / 1000L,
        dateModifiedSec = lastModified / 1000L,
        durationMs = durationMs,
        bucketName = folder,
        isVideo = kind == MediaKind.VIDEO,
        isAudio = kind == MediaKind.AUDIO,
        filePath = parsedUri.path
    )
}
