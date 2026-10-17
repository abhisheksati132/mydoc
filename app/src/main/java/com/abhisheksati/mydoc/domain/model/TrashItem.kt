package com.abhisheksati.mydoc.domain.model

import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

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
        get() {
            if (sizeBytes <= 0) return if (isDirectory) "Folder" else "0 B"
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
