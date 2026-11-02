package com.abhisheksati.mydoc.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import com.abhisheksati.mydoc.update.UpdateManager
import java.util.Locale
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
    onClearRecent: (() -> Unit)? = null
) {
    val updateState by updateManager.state.collectAsState()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // LOOK
    val currentTheme by settingsRepository.themeMode.collectAsState()
    val amoledBlack by settingsRepository.amoledBlack.collectAsState()
    val hapticFeedbackEnabled by settingsRepository.isHapticFeedbackEnabled.collectAsState()

    // READING
    val currentReadingMode by settingsRepository.defaultReadingMode.collectAsState()
    val highQualityPdf by settingsRepository.highQualityPdf.collectAsState()
    val pdfFitMode by settingsRepository.pdfFitMode.collectAsState()
    val doubleTapZoom by settingsRepository.doubleTapZoom.collectAsState()
    val keepScreenOn by settingsRepository.keepScreenOn.collectAsState()
    val volumeKeyTurning by settingsRepository.volumeKeyPageTurning.collectAsState()

    // MEDIA
    val videoHwAccel by settingsRepository.videoHardwareAcceleration.collectAsState()
    val autoResumeVideo by settingsRepository.autoResumeVideo.collectAsState()
    val videoAutoLoop by settingsRepository.videoAutoLoop.collectAsState()

    // LIBRARY
    val startupTab by settingsRepository.defaultStartupTab.collectAsState()
    var cacheSizeBytes by remember { mutableLongStateOf(settingsRepository.getCacheSizeBytes(context)) }
    // Read through resources, not LocalContext, so a configuration change re-composes
    // with the right values instead of leaving a stale string in the callback.
    val cacheClearedMessage = stringResource(R.string.cache_cleared)
    val recentHistoryClearedMessage = stringResource(R.string.recent_history_cleared)

    // ABOUT & SECURITY
    val biometricLockEnabled by settingsRepository.isBiometricLockEnabled.collectAsState()
    val checkUpdatesOnLaunch by settingsRepository.checkUpdatesOnLaunch.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "Settings",
                subtitle = "MyDoc ${updateManager.currentVersionName}",
                onBack = onBackClick
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. LOOK
                SettingsAppearanceSection(
                    currentTheme = currentTheme,
                    amoledBlack = amoledBlack,
                    hapticFeedbackEnabled = hapticFeedbackEnabled,
                    onThemeChange = { settingsRepository.setThemeMode(it) },
                    onAmoledBlackChange = { settingsRepository.setAmoledBlack(it) },
                    onHapticFeedbackChange = { settingsRepository.setHapticFeedbackEnabled(it) }
                )

                // 2. READING
                SettingsReadingSection(
                    currentReadingMode = currentReadingMode,
                    pdfFitMode = pdfFitMode,
                    highQualityPdf = highQualityPdf,
                    doubleTapZoom = doubleTapZoom,
                    keepScreenOn = keepScreenOn,
                    volumeKeyTurning = volumeKeyTurning,
                    onReadingModeChange = { settingsRepository.setDefaultReadingMode(it) },
                    onPdfFitModeChange = { settingsRepository.setPdfFitMode(it) },
                    onHighQualityPdfChange = { settingsRepository.setHighQualityPdf(it) },
                    onDoubleTapZoomChange = { settingsRepository.setDoubleTapZoom(it) },
                    onKeepScreenOnChange = { settingsRepository.setKeepScreenOn(it) },
                    onVolumeKeyTurningChange = { settingsRepository.setVolumeKeyPageTurning(it) }
                )

                // 3. MEDIA
                SettingsMediaSection(
                    videoHwAccel = videoHwAccel,
                    autoResumeVideo = autoResumeVideo,
                    videoAutoLoop = videoAutoLoop,
                    onVideoHwAccelChange = { settingsRepository.setVideoHardwareAcceleration(it) },
                    onAutoResumeVideoChange = { settingsRepository.setAutoResumeVideo(it) },
                    onVideoAutoLoopChange = { settingsRepository.setVideoAutoLoop(it) }
                )

                // 4. LIBRARY & STORAGE
                SettingsStorageSection(
                    startupTab = startupTab,
                    cacheSizeBytes = cacheSizeBytes,
                    onStartupTabChange = { settingsRepository.setDefaultStartupTab(it) },
                    onClearCacheClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val freed = settingsRepository.clearCache(context)
                        cacheSizeBytes = settingsRepository.getCacheSizeBytes(context)
                        val freedMb = freed / (1024.0 * 1024.0)
                        val toastText = if (freedMb >= 0.1) {
                            String.format(Locale.US, "Freed %.1f MB", freedMb)
                        } else {
                            cacheClearedMessage
                        }
                        context.showMessage(toastText)
                    },
                    onClearRecentClick = onClearRecent?.let { clearAction ->
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            clearAction()
                            context.showMessage(recentHistoryClearedMessage)
                        }
                    }
                )

                // 5. PRIVACY & SECURITY
                SettingsCard(
                    title = "Privacy & security",
                    icon = Icons.Outlined.Security
                ) {
                    SettingToggleRow(
                        title = "Biometric App Lock",
                        subtitle = "Require fingerprint or device PIN to open app",
                        icon = Icons.Outlined.Security,
                        checked = biometricLockEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                                if (km == null || !km.isDeviceSecure) {
                                    context.showMessage("App lock requires a screen lock (PIN, pattern, or fingerprint)")
                                } else {
                                    settingsRepository.setBiometricLockEnabled(true)
                                }
                            } else {
                                settingsRepository.setBiometricLockEnabled(false)
                            }
                        }
                    )
                }

                // 6. ABOUT & UPDATES
                SettingsAboutSection(
                    updateManager = updateManager,
                    updateState = updateState,
                    checkUpdatesOnLaunch = checkUpdatesOnLaunch,
                    onCheckUpdatesOnLaunchChange = { settingsRepository.setCheckUpdatesOnLaunch(it) }
                )
            }
        }
    }
}
