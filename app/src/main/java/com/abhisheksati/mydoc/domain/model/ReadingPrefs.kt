package com.abhisheksati.mydoc.domain.model

/**
 * Shared reading preferences across all document viewer formats.
 * Persisted in SettingsRepository and applied uniformly to PDF, Word, Text, and Spreadsheets.
 */
data class ReadingPrefs(
    val readingMode: ReadingMode = ReadingMode.NORMAL,
    val brightness: Float = -1f,
    val fontScale: Float = 1.0f,
    val keepScreenOn: Boolean = true,
    val isContinuousScroll: Boolean = true,
    val isDualPageView: Boolean = false,
    val isFitPage: Boolean = false
)
