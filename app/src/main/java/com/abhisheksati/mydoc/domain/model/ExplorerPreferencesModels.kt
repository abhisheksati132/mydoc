package com.abhisheksati.mydoc.domain.model

data class FolderViewPreferences(val sortOrder: FileSortOrder = FileSortOrder(), val grid: Boolean = false)

data class SavedExplorerSearch(
    val id: String,
    val label: String,
    val directory: String,
    val filters: ExplorerSearchOptions,
    val modifiedWithinDays: Int? = null
) {
    fun optionsAt(now: Long = System.currentTimeMillis()): ExplorerSearchOptions =
        filters.copy(modifiedAfter = modifiedWithinDays?.let { now - it * 86_400_000L }, modifiedWithinDays = modifiedWithinDays)
}
