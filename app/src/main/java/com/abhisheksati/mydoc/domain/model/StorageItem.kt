package com.abhisheksati.mydoc.domain.model

import androidx.compose.runtime.Immutable
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class StorageFileType {
    DIRECTORY,
    PDF,
    WORD,
    EXCEL,
    PPT,
    IMAGE,
    VIDEO,
    AUDIO,
    ARCHIVE,
    APK,
    CODE_TEXT,
    GENERIC
}

@Immutable
data class StorageItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val sizeBytes: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val itemCount: Int = 0,
    val isHidden: Boolean = file.name.startsWith(".")
) {
    val extension: String
        get() {
            if (isDirectory) return ""
            val dot = name.lastIndexOf('.')
            return if (dot != -1 && dot < name.length - 1) {
                name.substring(dot + 1).lowercase(Locale.ROOT)
            } else ""
        }

    val fileType: StorageFileType
        get() {
            if (isDirectory) return StorageFileType.DIRECTORY
            return when (extension) {
                "pdf" -> StorageFileType.PDF
                "doc", "docx" -> StorageFileType.WORD
                "xls", "xlsx", "csv", "tsv" -> StorageFileType.EXCEL
                "ppt", "pptx", "pps", "ppsx" -> StorageFileType.PPT
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg" -> StorageFileType.IMAGE
                "mp4", "mkv", "webm", "avi", "mov", "3gp", "ts" -> StorageFileType.VIDEO
                "mp3", "wav", "aac", "flac", "ogg", "m4a", "wma" -> StorageFileType.AUDIO
                "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> StorageFileType.ARCHIVE
                "apk", "xapk", "apks" -> StorageFileType.APK
                "txt", "md", "json", "xml", "html", "css", "js", "ts", "kt", "java", "py", "c", "cpp", "h", "log", "yaml", "yml", "ini", "conf", "sh", "sql" -> StorageFileType.CODE_TEXT
                else -> StorageFileType.GENERIC
            }
        }

    val formattedSize: String
        get() {
            if (isDirectory) {
                return if (itemCount == 1) "1 item" else "$itemCount items"
            }
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

    val formattedLastModified: String
        get() {
            if (lastModified <= 0) return ""
            val df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            val tf = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
            val date = Date(lastModified)
            return "${df.format(date)} ${tf.format(date)}"
        }
}
