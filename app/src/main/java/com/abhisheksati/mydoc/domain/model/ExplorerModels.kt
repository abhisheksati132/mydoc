package com.abhisheksati.mydoc.domain.model

import java.io.File

data class ExplorerSearchOptions(
    val name: String = "",
    val extension: String = "",
    val minBytes: Long? = null,
    val maxBytes: Long? = null,
    val modifiedAfter: Long? = null,
    val recursive: Boolean = false
) {
    val isValid: Boolean get() = (minBytes == null || minBytes >= 0) &&
        (maxBytes == null || maxBytes >= 0) && (minBytes == null || maxBytes == null || minBytes <= maxBytes)

    fun matches(item: StorageItem): Boolean = item.name.contains(name.trim(), ignoreCase = true) &&
        (extension.isBlank() || !item.isDirectory && item.file.extension.equals(extension.trim().removePrefix("."), true)) &&
        (minBytes == null || !item.isDirectory && item.sizeBytes >= minBytes) &&
        (maxBytes == null || !item.isDirectory && item.sizeBytes <= maxBytes) &&
        (modifiedAfter == null || item.lastModified >= modifiedAfter)
}

data class ExplorerSearchResult(val items: List<StorageItem>, val incomplete: Boolean)

data class ExplorerRenameOptions(
    val prefix: String = "",
    val suffix: String = "",
    val find: String = "",
    val replacement: String = "",
    val numbered: Boolean = false,
    val start: Int = 1,
    val preserveExtension: Boolean = true
)

data class ExplorerRenameEntry(
    val source: File,
    val target: File,
    val size: Long,
    val modified: Long,
    val directory: Boolean,
    val problem: String? = null
)

data class ExplorerRenamePlan(val entries: List<ExplorerRenameEntry>) {
    val canApply: Boolean get() = entries.isNotEmpty() && entries.all { it.problem == null } && entries.any { it.source != it.target }
}

data class ExplorerRenameResult(val success: Boolean, val unresolvedPaths: List<String> = emptyList())
