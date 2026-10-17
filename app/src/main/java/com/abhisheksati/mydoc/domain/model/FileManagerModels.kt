package com.abhisheksati.mydoc.domain.model

import java.io.File

enum class ClipboardOperation {
    COPY,
    CUT
}

data class ClipboardState(
    val operation: ClipboardOperation,
    val items: List<File>
)

enum class FileSortField {
    NAME,
    DATE,
    SIZE,
    TYPE
}

data class FileSortOrder(
    val field: FileSortField = FileSortField.NAME,
    val isAscending: Boolean = true
)

data class FolderProperties(
    val file: File,
    val totalSizeBytes: Long,
    val totalFilesCount: Int,
    val totalFoldersCount: Int,
    val isReadable: Boolean,
    val isWritable: Boolean
)

enum class FileCategory(val displayName: String) {
    DOWNLOADS("Downloads"),
    DOCUMENTS("Documents"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    APKS("APKs"),
    ARCHIVES("Archives"),
    LARGE_FILES("Large Files")
}

