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
    // A document indexed by MediaStore appears as file://… in a directory scan but keeps
    // the content://… URI it was opened with in the recents table, so distinctBy(uri)
    // cannot see that the two rows are one file. Name+size+kind survives that split.
    val identityKey: String = "$name|$sizeBytes|$kind"

    val extension: String = run {
        val dot = name.lastIndexOf('.')
        if (dot != -1 && dot < name.length - 1) name.substring(dot + 1).lowercase(Locale.ROOT) else ""
    }

    val typeBadge: String = when (kind) {
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

    val formattedSize: String get() = formatBytesPrecise(sizeBytes)

    val formattedProgress: String
        get() = when (kind) {
            MediaKind.DOCUMENT -> if (pageCount > 0) "Page ${lastPage + 1} of $pageCount" else ""
            MediaKind.VIDEO, MediaKind.AUDIO -> {
                val posSec = (lastPositionMs / 1000).coerceAtLeast(0)
                val durSec = (durationMs / 1000).coerceAtLeast(0)
                val posStr = "${(posSec / 60).toString().padStart(2, '0')}:${(posSec % 60).toString().padStart(2, '0')}"
                if (durSec > 0) {
                    val durStr = "${(durSec / 60).toString().padStart(2, '0')}:${(durSec % 60).toString().padStart(2, '0')}"
                    "$posStr / $durStr"
                } else {
                    posStr
                }
            }
            MediaKind.PHOTO -> mediumDateFormatter.get().format(Date(lastModified))
        }

    val formattedLastModified: String get() = formatRelativeTimestamp(lastModified)
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


// DateFormat is not thread safe and these strings are now built off the main thread.
private val mediumDateFormatter: ThreadLocal<DateFormat> = object : ThreadLocal<DateFormat>() {
    override fun initialValue(): DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
}

fun formatBytesPrecise(sizeBytes: Long): String {
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

fun formatRelativeTimestamp(lastModified: Long): String {
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
        else -> mediumDateFormatter.get().format(Date(lastModified))
    }
}