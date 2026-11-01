package com.abhisheksati.mydoc.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.update.UpdateManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    updateManager: UpdateManager,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit,
    onDesignSystemPreviewClick: () -> Unit = {},
    onClearRecent: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val updateState by updateManager.state.collectAsState()
    val context = LocalContext.current
    val colors = LocalAppColors.current
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
    var cacheSizeBytes by remember { mutableStateOf(settingsRepository.getCacheSizeBytes(context)) }

    // ABOUT & SECURITY
    val biometricLockEnabled by settingsRepository.isBiometricLockEnabled.collectAsState()
    val checkUpdatesOnLaunch by settingsRepository.checkUpdatesOnLaunch.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            AppTabHeader(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        context.getString(R.string.cache_cleared)
                    }
                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                },
                onClearRecentClick = onClearRecent?.let { clearAction ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clearAction()
                        Toast.makeText(context, context.getString(R.string.recent_history_cleared), Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 5. PRIVACY & SECURITY
            SettingsCard(title = "PRIVACY & SECURITY") {
                SettingToggleRow(
                    title = "Biometric App Lock",
                    subtitle = "Require fingerprint or device PIN to open app",
                    icon = Icons.Outlined.Security,
                    checked = biometricLockEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                            if (km?.isDeviceSecure == false) {
                                Toast.makeText(context, "Set a screen lock in device settings first", Toast.LENGTH_SHORT).show()
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
