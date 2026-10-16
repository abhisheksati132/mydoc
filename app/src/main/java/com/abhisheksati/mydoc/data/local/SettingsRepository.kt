package com.abhisheksati.mydoc.data.local

import android.content.Context
import android.content.SharedPreferences
import com.abhisheksati.mydoc.domain.model.ReadingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    AMOLED_DARK,
    LIGHT
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("mydoc_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _defaultReadingMode = MutableStateFlow(loadDefaultReadingMode())
    val defaultReadingMode: StateFlow<ReadingMode> = _defaultReadingMode.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, true))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _grantedFolderUri = MutableStateFlow(prefs.getString(KEY_GRANTED_FOLDER_URI, null))
    val grantedFolderUri: StateFlow<String?> = _grantedFolderUri.asStateFlow()

    private val _grantedFolderName = MutableStateFlow(prefs.getString(KEY_GRANTED_FOLDER_NAME, null))
    val grantedFolderName: StateFlow<String?> = _grantedFolderName.asStateFlow()

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

    fun setGrantedFolder(uri: String?, name: String?) {
        prefs.edit()
            .putString(KEY_GRANTED_FOLDER_URI, uri)
            .putString(KEY_GRANTED_FOLDER_NAME, name)
            .apply()
        _grantedFolderUri.value = uri
        _grantedFolderName.value = name
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

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_DEFAULT_READING_MODE = "default_reading_mode"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_GRANTED_FOLDER_URI = "granted_folder_uri"
        private const val KEY_GRANTED_FOLDER_NAME = "granted_folder_name"
    }
}
