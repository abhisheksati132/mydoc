package com.abhisheksati.mydoc.domain.model

import androidx.compose.runtime.Immutable
import java.io.File
import java.text.DateFormat
import java.util.Date

@Immutable
data class TrashItem(
    val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val displayName: String,
    val sizeBytes: Long,
    val deletedTimestamp: Long,
    val isDirectory: Boolean
) {
    val originalFile: File
        get() = File(originalPath)

    val trashFile: File
        get() = File(trashPath)

    val existsInTrash: Boolean
        get() = trashFile.exists()

    val formattedSize: String
        get() = when {
            sizeBytes > 0 -> StorageVolume.formatBytes(sizeBytes)
            isDirectory -> "Folder"
            else -> "0 B"
        }

    val formattedDeletedTime: String
        get() {
            if (deletedTimestamp <= 0) return ""
            val now = System.currentTimeMillis()
            val diff = now - deletedTimestamp
            val oneMinute = 60 * 1000L
            val oneHour = 60 * oneMinute
            val oneDay = 24 * oneHour

            return when {
                diff < oneMinute -> "Just now"
                diff < oneHour -> "${diff / oneMinute}m ago"
                diff < oneDay -> "${diff / oneHour}h ago"
                diff < 2 * oneDay -> "Yesterday"
                else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(deletedTimestamp))
            }
        }
}
