package com.abhisheksati.mydoc.domain.model

import java.io.File
import java.util.Locale

data class StorageVolume(
    val title: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val isPrimary: Boolean
) {
    val usedBytes: Long = (totalBytes - freeBytes).coerceAtLeast(0L)

    val usedRatio: Float = if (totalBytes > 0) {
        (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val formattedUsed: String = formatBytes(usedBytes)
    val formattedTotal: String = formatBytes(totalBytes)
    val formattedFree: String = formatBytes(freeBytes)

    val file: File
        get() = File(path)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
