package com.abhisheksati.mydoc.data.local

import android.content.Context
import android.content.SharedPreferences
import com.abhisheksati.mydoc.domain.model.ReadingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class AppThemeMode {
    SYSTEM,
    AMOLED_DARK,
    LIGHT
}

enum class DocumentSortOrder(val displayName: String) {
    LAST_OPENED("Last Opened"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)")
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("mydoc_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _defaultReadingMode = MutableStateFlow(loadDefaultReadingMode())
    val defaultReadingMode: StateFlow<ReadingMode> = _defaultReadingMode.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, true))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _sortOrder = MutableStateFlow(loadSortOrder())
    val sortOrder: StateFlow<DocumentSortOrder> = _sortOrder.asStateFlow()

    private val _grantedFolderUri = MutableStateFlow(prefs.getString(KEY_GRANTED_FOLDER_URI, null))
    val grantedFolderUri: StateFlow<String?> = _grantedFolderUri.asStateFlow()

    private val _grantedFolderName = MutableStateFlow(prefs.getString(KEY_GRANTED_FOLDER_NAME, null))
    val grantedFolderName: StateFlow<String?> = _grantedFolderName.asStateFlow()

    private val _isBiometricLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_LOCK, false))
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled.asStateFlow()

    private val _volumeKeyPageTurning = MutableStateFlow(prefs.getBoolean(KEY_VOLUME_KEY_TURNING, true))
    val volumeKeyPageTurning: StateFlow<Boolean> = _volumeKeyPageTurning.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDefaultReadingMode(mode: ReadingMode) {
        prefs.edit().putString(KEY_DEFAULT_READING_MODE, mode.name).apply()
        _defaultReadingMode.value = mode
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _keepScreenOn.value = enabled
    }

    fun setSortOrder(order: DocumentSortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
        _sortOrder.value = order
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
        _isBiometricLockEnabled.value = enabled
    }

    fun setVolumeKeyPageTurning(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOLUME_KEY_TURNING, enabled).apply()
        _volumeKeyPageTurning.value = enabled
    }

    fun getBookmarks(uri: String): Set<Int> {
        val raw = prefs.getString("doc_bookmarks_$uri", "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun toggleBookmark(uri: String, pageIndex: Int): Boolean {
        val current = getBookmarks(uri).toMutableSet()
        val isNowBookmarked = if (current.contains(pageIndex)) {
            current.remove(pageIndex)
            false
        } else {
            current.add(pageIndex)
            true
        }
        prefs.edit().putString("doc_bookmarks_$uri", current.sorted().joinToString(",")).apply()
        return isNowBookmarked
    }

    fun setGrantedFolder(uri: String?, name: String?) {
        prefs.edit()
            .putString(KEY_GRANTED_FOLDER_URI, uri)
            .putString(KEY_GRANTED_FOLDER_NAME, name)
            .apply()
        _grantedFolderUri.value = uri
        _grantedFolderName.value = name
    }

    fun getCacheSizeBytes(context: Context): Long {
        return getFolderSize(context.cacheDir)
    }

    fun clearCache(context: Context): Long {
        val sizeBefore = getCacheSizeBytes(context)
        context.cacheDir.listFiles()?.forEach { file ->
            deleteRecursive(file)
        }
        return sizeBefore
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += getFolderSize(child)
        }
        return size
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }

    private fun loadThemeMode(): AppThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return runCatching { AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name) }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

    private fun loadDefaultReadingMode(): ReadingMode {
        val saved = prefs.getString(KEY_DEFAULT_READING_MODE, ReadingMode.NORMAL.name)
        return runCatching { ReadingMode.valueOf(saved ?: ReadingMode.NORMAL.name) }
            .getOrDefault(ReadingMode.NORMAL)
    }

    private fun loadSortOrder(): DocumentSortOrder {
        val saved = prefs.getString(KEY_SORT_ORDER, DocumentSortOrder.LAST_OPENED.name)
        return runCatching { DocumentSortOrder.valueOf(saved ?: DocumentSortOrder.LAST_OPENED.name) }
            .getOrDefault(DocumentSortOrder.LAST_OPENED)
    }

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_DEFAULT_READING_MODE = "default_reading_mode"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SORT_ORDER = "document_sort_order"
        private const val KEY_GRANTED_FOLDER_URI = "granted_folder_uri"
        private const val KEY_GRANTED_FOLDER_NAME = "granted_folder_name"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
        private const val KEY_VOLUME_KEY_TURNING = "volume_key_turning"
    }
}
