package com.abhisheksati.mydoc.ui.settings

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.app.KeyguardManager
import android.content.Context
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import com.abhisheksati.mydoc.ui.appmanager.AppManagerSheet
import com.abhisheksati.mydoc.ui.cleaner.StorageCleanerSheet
import com.abhisheksati.mydoc.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.abhisheksati.mydoc.designsystem.components.showMessage

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    updateManager: UpdateManager,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDesignSystemPreviewClick: () -> Unit = {},
    onClearRecent: (suspend () -> Unit)? = null
) {
    val updateState by updateManager.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    // Read here: the toggle callback is not composable.
    val keyguardMessage = stringResource(R.string.settings_app_lock_needs_keyguard)
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // LOOK
    val currentTheme by settingsRepository.themeMode.collectAsState()
    val amoledBlack by settingsRepository.amoledBlack.collectAsState()
    val hapticFeedbackEnabled by settingsRepository.isHapticFeedbackEnabled.collectAsState()
    val glassEffectMode by settingsRepository.glassEffectMode.collectAsState()

    var showStorageCleaner by remember { mutableStateOf(false) }
    var showClearHistoryConfirmation by remember { mutableStateOf(false) }
    var showAppManager by remember { mutableStateOf(false) }

    // READING
    val currentReadingMode by settingsRepository.defaultReadingMode.collectAsState()
    val readingPrefs by settingsRepository.readingPrefs.collectAsState()
    val pdfFitMode = if (readingPrefs.isFitPage) "FIT_PAGE" else "FIT_WIDTH"
    val doubleTapZoom by settingsRepository.doubleTapZoom.collectAsState()
    val keepScreenOn by settingsRepository.keepScreenOn.collectAsState()
    val volumeKeyTurning by settingsRepository.volumeKeyPageTurning.collectAsState()

    // MEDIA
    val autoResumeVideo by settingsRepository.autoResumeVideo.collectAsState()
    val videoAutoLoop by settingsRepository.videoAutoLoop.collectAsState()

    // LIBRARY
    val startupTab by settingsRepository.defaultStartupTab.collectAsState()
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        // The size is a recursive walk of both cache trees; doing it in composition stalled the
        // first frame of Settings on a device with a large render cache.
        cacheSizeBytes = withContext(Dispatchers.IO) { settingsRepository.getCacheSizeBytes(context) }
    }
    // Read through resources, not LocalContext, so a configuration change re-composes
    // with the right values instead of leaving a stale string in the callback.
    val cacheClearedMessage = stringResource(R.string.cache_cleared)
    val recentHistoryClearedMessage = stringResource(R.string.recent_history_cleared)

    // ABOUT & SECURITY
    val biometricLockEnabled by settingsRepository.isBiometricLockEnabled.collectAsState()
    val checkUpdatesOnLaunch by settingsRepository.checkUpdatesOnLaunch.collectAsState()

    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = SettingsCategory.entries.firstOrNull { it.name == selectedCategoryName }
    val categoryStates = rememberSaveableStateHolder()
    BackHandler(enabled = selectedCategory != null) { selectedCategoryName = null }
    val navigateBack = { if (selectedCategory != null) selectedCategoryName = null else onBackClick() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = stringResource(selectedCategory?.title ?: R.string.settings_title),
                subtitle = if (selectedCategory == null) stringResource(R.string.settings_subtitle, updateManager.currentVersionName) else null,
                onBack = navigateBack,
                isGlass = false,
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            categoryStates.SaveableStateProvider(selectedCategory?.name ?: "index") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedCategory == null) SettingsCategoryIndex(onSelect = { selectedCategoryName = it.name })

                    if (selectedCategory == SettingsCategory.APPEARANCE) {
                        // 1. LOOK
                        SettingsAppearanceSection(
                            currentTheme = currentTheme,
                            amoledBlack = amoledBlack,
                            hapticFeedbackEnabled = hapticFeedbackEnabled,
                            glassEffectMode = glassEffectMode,
                            onThemeChange = { settingsRepository.setThemeMode(it) },
                            onAmoledBlackChange = { settingsRepository.setAmoledBlack(it) },
                            onHapticFeedbackChange = { settingsRepository.setHapticFeedbackEnabled(it) },
                            onGlassEffectModeChange = { settingsRepository.setGlassEffectMode(it) }
                        )
                    }

                    if (selectedCategory == SettingsCategory.READING) {
                        // 2. READING
                        SettingsReadingSection(
                            currentReadingMode = currentReadingMode,
                            pdfFitMode = pdfFitMode,
                            doubleTapZoom = doubleTapZoom,
                            keepScreenOn = keepScreenOn,
                            volumeKeyTurning = volumeKeyTurning,
                            onReadingModeChange = { settingsRepository.setDefaultReadingMode(it) },
                            onPdfFitModeChange = { mode ->
                                settingsRepository.updateReadingPrefs {
                                    it.copy(isFitPage = mode == "FIT_PAGE")
                                }
                            },
                            onDoubleTapZoomChange = { settingsRepository.setDoubleTapZoom(it) },
                            onKeepScreenOnChange = { settingsRepository.setKeepScreenOn(it) },
                            onVolumeKeyTurningChange = { settingsRepository.setVolumeKeyPageTurning(it) }
                        )
                    }

                    if (selectedCategory == SettingsCategory.MEDIA) {
                        // 3. MEDIA
                        SettingsMediaSection(
                            autoResumeVideo = autoResumeVideo,
                            videoAutoLoop = videoAutoLoop,
                            onAutoResumeVideoChange = { settingsRepository.setAutoResumeVideo(it) },
                            onVideoAutoLoopChange = { settingsRepository.setVideoAutoLoop(it) }
                        )
                    }

                    if (selectedCategory == SettingsCategory.STORAGE) {
                        // 4. LIBRARY & STORAGE
                        SettingsStorageSection(
                            startupTab = startupTab,
                            cacheSizeBytes = cacheSizeBytes,
                            onStartupTabChange = { settingsRepository.setDefaultStartupTab(it) },
                            onClearCacheClick = {
                                if (hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    val freed = withContext(Dispatchers.IO) { settingsRepository.clearCache(context) }
                                    cacheSizeBytes = withContext(Dispatchers.IO) { settingsRepository.getCacheSizeBytes(context) }
                                    val freedMb = freed / (1024.0 * 1024.0)
                                    val toastText = if (freedMb >= 0.1) {
                                        resources.getString(R.string.settings_freed_mb, freedMb)
                                    } else {
                                        cacheClearedMessage
                                    }
                                    context.showMessage(toastText)
                                }
                            },
                            // Clearing history is one tap away from erasing every reading position, so it
                            // asks first and says what goes and what stays.
                            onClearRecentClick = if (onClearRecent == null) null else {
                                {
                                    if (hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showClearHistoryConfirmation = true
                                }
                            },
                            onOpenStorageCleaner = { showStorageCleaner = true },
                            onOpenAppManager = { showAppManager = true }
                        )
                    }

                    if (selectedCategory == SettingsCategory.PRIVACY) {
                        // 5. PRIVACY & SECURITY
                        SettingsCard(
                            title = stringResource(R.string.settings_privacy_security),
                            icon = Icons.Outlined.Security
                        ) {
                            SettingToggleRow(
                                title = stringResource(R.string.settings_app_lock_title),
                                subtitle = stringResource(R.string.settings_app_lock_subtitle),
                                icon = Icons.Outlined.Security,
                                checked = biometricLockEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                                        if (km == null || !km.isDeviceSecure) {
                                            context.showMessage(keyguardMessage)
                                        } else {
                                            settingsRepository.setBiometricLockEnabled(true)
                                        }
                                    } else {
                                        settingsRepository.setBiometricLockEnabled(false)
                                    }
                                }
                            )
                        }
                    }

                    if (selectedCategory == SettingsCategory.ABOUT) {
                        // 6. ABOUT & UPDATES
                        SettingsAboutSection(
                            updateManager = updateManager,
                            updateState = updateState,
                            checkUpdatesOnLaunch = checkUpdatesOnLaunch,
                            onCheckUpdatesOnLaunchChange = { settingsRepository.setCheckUpdatesOnLaunch(it) },
                            onDesignSystemPreviewClick = onDesignSystemPreviewClick
                        )
                    }
                }
            }
        }

        if (showClearHistoryConfirmation) {
            AppDialog(
                isOpen = true,
                title = stringResource(R.string.settings_clear_history_title),
                message = stringResource(R.string.settings_clear_history_message),
                confirmLabel = stringResource(R.string.settings_clear_history_confirm),
                isDestructive = true,
                onDismissRequest = { showClearHistoryConfirmation = false },
                onConfirm = {
                    showClearHistoryConfirmation = false
                    val clear = onClearRecent
                    if (clear == null) {
                        context.showMessage(recentHistoryClearedMessage)
                    } else {
                        // Announce the clear only once the database work has returned, not when
                        // the coroutine that does it is merely launched.
                        scope.launch {
                            clear()
                            context.showMessage(recentHistoryClearedMessage)
                        }
                    }
                }
            )
        }

        if (showStorageCleaner) {
            StorageCleanerSheet(
                rootDir = android.os.Environment.getExternalStorageDirectory(),
                onDismiss = { showStorageCleaner = false },
                onFileClick = {
                    showStorageCleaner = false
                }
            )
        }

        if (showAppManager) {
            AppManagerSheet(
                onDismiss = { showAppManager = false }
            )
        }
    }
}
