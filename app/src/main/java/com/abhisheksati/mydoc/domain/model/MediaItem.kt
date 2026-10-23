package com.abhisheksati.mydoc.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MediaFilter {
    ALL,
    PHOTOS,
    VIDEOS,
    AUDIO
}

@Immutable
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val dateAddedSec: Long,
    val dateModifiedSec: Long = dateAddedSec,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val orientation: Int = 0,
    val bucketId: String? = null,
    val bucketName: String? = null,
    val isVideo: Boolean = false,
    val isAudio: Boolean = false,
    val filePath: String? = null
) {
    val isGif: Boolean
        get() = mimeType.equals("image/gif", ignoreCase = true) || displayName.endsWith(".gif", ignoreCase = true)

    val isWebp: Boolean
        get() = mimeType.equals("image/webp", ignoreCase = true) || displayName.endsWith(".webp", ignoreCase = true)

    val formattedDuration: String
        get() {
            if (durationMs <= 0L) return ""
            val totalSeconds = durationMs / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
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

    val formattedDate: String
        get() {
            val millis = if (dateModifiedSec > 0) dateModifiedSec * 1000 else dateAddedSec * 1000
            if (millis <= 0) return ""
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(millis))
        }
}

data class ExifMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val dateTimeOriginal: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val locationText: String?
        get() {
            if (latitude == null || longitude == null) return null
            val latRef = if (latitude >= 0) "N" else "S"
            val lonRef = if (longitude >= 0) "E" else "W"
            return String.format(Locale.US, "%.4f° %s, %.4f° %s", Math.abs(latitude), latRef, Math.abs(longitude), lonRef)
        }
}
