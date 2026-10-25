package com.abhisheksati.mydoc.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.ReadingPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class AppSystemDiagnostics(
    val deviceName: String,
    val androidVersion: String,
    val apiLevel: Int,
    val abiArchitecture: String,
    val jvmHeapUsedMb: Long,
    val jvmHeapMaxMb: Long,
    val availableStorageMb: Long
)

enum class AppThemeMode {
    LIGHT,
    DARK,
    BLACK,
    SYSTEM;

    companion object {
        val AMOLED_DARK = BLACK
    }
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

    private val _defaultStartupTab = MutableStateFlow(loadDefaultStartupTab())
    val defaultStartupTab: StateFlow<HomeTab> = _defaultStartupTab.asStateFlow()

    private val _isHapticFeedbackEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true))
    val isHapticFeedbackEnabled: StateFlow<Boolean> = _isHapticFeedbackEnabled.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(prefs.getBoolean(KEY_SHOW_HIDDEN_FILES, false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _autoResumeVideo = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RESUME_VIDEO, true))
    val autoResumeVideo: StateFlow<Boolean> = _autoResumeVideo.asStateFlow()

    private val _scanFullDeviceStorage = MutableStateFlow(prefs.getBoolean(KEY_SCAN_FULL_DEVICE, true))
    val scanFullDeviceStorage: StateFlow<Boolean> = _scanFullDeviceStorage.asStateFlow()

    private val _highQualityPdf = MutableStateFlow(prefs.getBoolean(KEY_HIGH_QUALITY_PDF, true))
    val highQualityPdf: StateFlow<Boolean> = _highQualityPdf.asStateFlow()

    private val _defaultReadingMode = MutableStateFlow(loadDefaultReadingMode())
    val defaultReadingMode: StateFlow<ReadingMode> = _defaultReadingMode.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, true))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _readingPrefs = MutableStateFlow(loadReadingPrefs())
    val readingPrefs: StateFlow<ReadingPrefs> = _readingPrefs.asStateFlow()

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

    private val _confirmBeforeDelete = MutableStateFlow(prefs.getBoolean(KEY_CONFIRM_BEFORE_DELETE, true))
    val confirmBeforeDelete: StateFlow<Boolean> = _confirmBeforeDelete.asStateFlow()

    private val _videoHardwareAcceleration = MutableStateFlow(prefs.getBoolean(KEY_VIDEO_HW_ACCEL, true))
    val videoHardwareAcceleration: StateFlow<Boolean> = _videoHardwareAcceleration.asStateFlow()

    private val _videoAutoLoop = MutableStateFlow(prefs.getBoolean(KEY_VIDEO_AUTO_LOOP, true))
    val videoAutoLoop: StateFlow<Boolean> = _videoAutoLoop.asStateFlow()

    private val _pdfFitMode = MutableStateFlow(prefs.getString(KEY_PDF_FIT_MODE, "FIT_WIDTH") ?: "FIT_WIDTH")
    val pdfFitMode: StateFlow<String> = _pdfFitMode.asStateFlow()

    private val _doubleTapZoom = MutableStateFlow(prefs.getBoolean(KEY_DOUBLE_TAP_ZOOM, true))
    val doubleTapZoom: StateFlow<Boolean> = _doubleTapZoom.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _amoledBlack = MutableStateFlow(prefs.getBoolean(KEY_AMOLED_BLACK, true))
    val amoledBlack: StateFlow<Boolean> = _amoledBlack.asStateFlow()

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setAmoledBlack(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMOLED_BLACK, enabled).apply()
        _amoledBlack.value = enabled
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDefaultReadingMode(mode: ReadingMode) {
        prefs.edit().putString(KEY_DEFAULT_READING_MODE, mode.name).apply()
        _defaultReadingMode.value = mode
        _readingPrefs.value = _readingPrefs.value.copy(readingMode = mode)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _keepScreenOn.value = enabled
        _readingPrefs.value = _readingPrefs.value.copy(keepScreenOn = enabled)
    }

    fun updateReadingPrefs(update: (ReadingPrefs) -> ReadingPrefs) {
        val next = update(_readingPrefs.value)
        _readingPrefs.value = next
        prefs.edit()
            .putString(KEY_DEFAULT_READING_MODE, next.readingMode.name)
            .putFloat(KEY_READING_BRIGHTNESS, next.brightness)
            .putFloat(KEY_READING_FONT_SCALE, next.fontScale)
            .putBoolean(KEY_KEEP_SCREEN_ON, next.keepScreenOn)
            .putBoolean(KEY_READING_CONTINUOUS_SCROLL, next.isContinuousScroll)
            .putBoolean(KEY_READING_DUAL_PAGE, next.isDualPageView)
            .putBoolean(KEY_READING_FIT_PAGE, next.isFitPage)
            .apply()
        _defaultReadingMode.value = next.readingMode
        _keepScreenOn.value = next.keepScreenOn
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

    fun setDefaultStartupTab(tab: HomeTab) {
        prefs.edit().putString(KEY_DEFAULT_STARTUP_TAB, tab.name).apply()
        _defaultStartupTab.value = tab
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
        _isHapticFeedbackEnabled.value = enabled
    }

    fun setShowHiddenFiles(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN_FILES, enabled).apply()
        _showHiddenFiles.value = enabled
    }

    fun setAutoResumeVideo(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESUME_VIDEO, enabled).apply()
        _autoResumeVideo.value = enabled
    }

    fun setScanFullDeviceStorage(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCAN_FULL_DEVICE, enabled).apply()
        _scanFullDeviceStorage.value = enabled
    }

    fun setHighQualityPdf(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_QUALITY_PDF, enabled).apply()
        _highQualityPdf.value = enabled
    }

    fun setConfirmBeforeDelete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_BEFORE_DELETE, enabled).apply()
        _confirmBeforeDelete.value = enabled
    }

    fun setVideoHardwareAcceleration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_HW_ACCEL, enabled).apply()
        _videoHardwareAcceleration.value = enabled
    }

    fun setVideoAutoLoop(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_AUTO_LOOP, enabled).apply()
        _videoAutoLoop.value = enabled
    }

    fun setPdfFitMode(mode: String) {
        prefs.edit().putString(KEY_PDF_FIT_MODE, mode).apply()
        _pdfFitMode.value = mode
    }

    fun setDoubleTapZoom(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DOUBLE_TAP_ZOOM, enabled).apply()
        _doubleTapZoom.value = enabled
    }

    fun getSystemDiagnostics(): AppSystemDiagnostics {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        val maxBytes = runtime.maxMemory()
        val dataDir = Environment.getDataDirectory()
        val freeStorage = dataDir.freeSpace
        return AppSystemDiagnostics(
            deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            abiArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "Universal",
            jvmHeapUsedMb = (usedBytes / (1024 * 1024)).coerceAtLeast(1),
            jvmHeapMaxMb = (maxBytes / (1024 * 1024)).coerceAtLeast(1),
            availableStorageMb = freeStorage / (1024 * 1024)
        )
    }

    fun getCacheSizeBytes(context: Context): Long {
        return getFolderSize(context.cacheDir) + getFolderSize(context.externalCacheDir)
    }

    fun clearCache(context: Context): Long {
        val sizeBefore = getCacheSizeBytes(context)
        context.cacheDir.listFiles()?.forEach { file ->
            deleteRecursive(file)
        }
        context.externalCacheDir?.listFiles()?.forEach { file ->
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
        if (saved == "AMOLED_DARK") return AppThemeMode.BLACK
        return runCatching { AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name) }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

    private fun loadDefaultStartupTab(): HomeTab {
        val saved = prefs.getString(KEY_DEFAULT_STARTUP_TAB, HomeTab.HOME.name)
        if (saved == "NOW" || saved == "DOCUMENTS") return HomeTab.HOME
        return runCatching { HomeTab.valueOf(saved ?: HomeTab.HOME.name) }
            .getOrDefault(HomeTab.HOME)
    }

    private fun loadReadingPrefs(): ReadingPrefs {
        val mode = loadDefaultReadingMode()
        val brightness = prefs.getFloat(KEY_READING_BRIGHTNESS, -1f)
        val fontScale = prefs.getFloat(KEY_READING_FONT_SCALE, 1.0f)
        val keepOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        val continuous = prefs.getBoolean(KEY_READING_CONTINUOUS_SCROLL, true)
        val dual = prefs.getBoolean(KEY_READING_DUAL_PAGE, false)
        val fitPage = prefs.getBoolean(KEY_READING_FIT_PAGE, false)
        return ReadingPrefs(
            readingMode = mode,
            brightness = brightness,
            fontScale = fontScale,
            keepScreenOn = keepOn,
            isContinuousScroll = continuous,
            isDualPageView = dual,
            isFitPage = fitPage
        )
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
        private const val KEY_DEFAULT_STARTUP_TAB = "default_startup_tab"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"
        private const val KEY_SHOW_HIDDEN_FILES = "show_hidden_files"
        private const val KEY_AUTO_RESUME_VIDEO = "auto_resume_video"
        private const val KEY_HIGH_QUALITY_PDF = "high_quality_pdf"
        private const val KEY_DEFAULT_READING_MODE = "default_reading_mode"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SORT_ORDER = "document_sort_order"
        private const val KEY_GRANTED_FOLDER_URI = "granted_folder_uri"
        private const val KEY_GRANTED_FOLDER_NAME = "granted_folder_name"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
        private const val KEY_VOLUME_KEY_TURNING = "volume_key_turning"
        private const val KEY_CONFIRM_BEFORE_DELETE = "confirm_before_delete"
        private const val KEY_PHOTO_EDITOR_QUALITY = "photo_editor_export_quality"
        private const val KEY_VIDEO_HW_ACCEL = "video_hw_acceleration"
        private const val KEY_VIDEO_AUTO_LOOP = "video_auto_loop"
        private const val KEY_PDF_FIT_MODE = "pdf_fit_mode"
        private const val KEY_DOUBLE_TAP_ZOOM = "double_tap_zoom"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        private const val KEY_AMOLED_BLACK = "amoled_black_enabled"
        private const val KEY_SCAN_FULL_DEVICE = "scan_full_device_storage"
        private const val KEY_READING_BRIGHTNESS = "reading_brightness"
        private const val KEY_READING_FONT_SCALE = "reading_font_scale"
        private const val KEY_READING_CONTINUOUS_SCROLL = "reading_continuous_scroll"
        private const val KEY_READING_DUAL_PAGE = "reading_dual_page"
        private const val KEY_READING_FIT_PAGE = "reading_fit_page"
    }
}
