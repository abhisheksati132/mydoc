package com.abhisheksati.mydoc.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.stringResource
import com.abhisheksati.mydoc.R
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.update.UpdateUiState
import java.util.Locale
import kotlinx.coroutines.launch

import androidx.compose.material.icons.outlined.DeleteSweep

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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
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
    val photoQuality by settingsRepository.photoEditorExportQuality.collectAsState()

    // LIBRARY
    val startupTab by settingsRepository.defaultStartupTab.collectAsState()
    var cacheSizeBytes by remember { mutableStateOf(settingsRepository.getCacheSizeBytes(context)) }

    // ABOUT & SECURITY
    val biometricLockEnabled by settingsRepository.isBiometricLockEnabled.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background
                )
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
            SettingsCard(title = "LOOK") {
                Text(
                    text = "Theme",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChip(
                        label = "Light",
                        icon = Icons.Outlined.LightMode,
                        selected = currentTheme == AppThemeMode.LIGHT,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settingsRepository.setThemeMode(AppThemeMode.LIGHT)
                        }
                    )
                    ThemeChip(
                        label = "Dark",
                        icon = Icons.Outlined.DarkMode,
                        selected = currentTheme == AppThemeMode.DARK,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settingsRepository.setThemeMode(AppThemeMode.DARK)
                        }
                    )
                    ThemeChip(
                        label = "Black",
                        icon = Icons.Outlined.Contrast,
                        selected = currentTheme == AppThemeMode.BLACK,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settingsRepository.setThemeMode(AppThemeMode.BLACK)
                        }
                    )
                    ThemeChip(
                        label = "System",
                        icon = null,
                        selected = currentTheme == AppThemeMode.SYSTEM,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settingsRepository.setThemeMode(AppThemeMode.SYSTEM)
                        }
                    )
                }

                SettingsDivider()

                SettingToggleRow(
                    title = "Pure AMOLED Black",
                    subtitle = "Use pitch black background in dark mode to save battery",
                    icon = Icons.Outlined.Contrast,
                    checked = amoledBlack,
                    onCheckedChange = { settingsRepository.setAmoledBlack(it) }
                )

                SettingsDivider()

                SettingToggleRow(
                    title = "Tactile Vibration Haptics",
                    subtitle = "Vibration response for navigation, gestures, and actions",
                    icon = Icons.Outlined.Vibration,
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = {
                        if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        settingsRepository.setHapticFeedbackEnabled(it)
                    }
                )
            }

            // 2. READING
            SettingsCard(title = "READING") {
                Text(
                    text = "Default Reading Mode",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChip(
                        label = "Normal",
                        icon = null,
                        selected = currentReadingMode == ReadingMode.NORMAL,
                        onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.NORMAL) }
                    )
                    ThemeChip(
                        label = "Dark",
                        icon = Icons.Outlined.DarkMode,
                        selected = currentReadingMode == ReadingMode.INVERTED_DARK,
                        onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.INVERTED_DARK) }
                    )
                    ThemeChip(
                        label = "Sepia",
                        icon = null,
                        selected = currentReadingMode == ReadingMode.SEPIA,
                        onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.SEPIA) }
                    )
                }

                SettingsDivider()

                Text(
                    text = "PDF Page Scaling",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChip(
                        label = "Fit Width",
                        icon = null,
                        selected = pdfFitMode == "FIT_WIDTH",
                        onClick = { settingsRepository.setPdfFitMode("FIT_WIDTH") }
                    )
                    ThemeChip(
                        label = "Fit Whole Page",
                        icon = null,
                        selected = pdfFitMode == "FIT_PAGE",
                        onClick = { settingsRepository.setPdfFitMode("FIT_PAGE") }
                    )
                }

                SettingsDivider()

                SettingToggleRow(
                    title = "High-Quality Vector PDF",
                    subtitle = "Supersample text and vectors for ultra-crisp reading",
                    icon = Icons.Outlined.PictureAsPdf,
                    checked = highQualityPdf,
                    onCheckedChange = { settingsRepository.setHighQualityPdf(it) }
                )

                SettingsDivider()

                SettingToggleRow(
                    title = "Double-Tap Quick Zoom",
                    subtitle = "Double tap document page to jump to 2.5x zoom",
                    icon = Icons.Outlined.Speed,
                    checked = doubleTapZoom,
                    onCheckedChange = { settingsRepository.setDoubleTapZoom(it) }
                )

                SettingsDivider()

                SettingToggleRow(
                    title = "Keep Screen Awake While Reading",
                    subtitle = "Prevents display timeout while reading documents",
                    icon = Icons.Outlined.LightMode,
                    checked = keepScreenOn,
                    onCheckedChange = { settingsRepository.setKeepScreenOn(it) }
                )

                SettingsDivider()

                SettingToggleRow(
                    title = "Volume Button Page Turning",
                    subtitle = "Use volume keys to flip pages in book view",
                    icon = Icons.Outlined.Vibration,
                    checked = volumeKeyTurning,
                    onCheckedChange = { settingsRepository.setVolumeKeyPageTurning(it) }
                )
            }

            // 3. MEDIA
            SettingsCard(title = "MEDIA") {
                SettingToggleRow(
                    title = "Hardware Video Acceleration",
                    subtitle = "Use GPU hardware decoding for ultra-smooth 4K video playback",
                    icon = Icons.Outlined.Videocam,
                    checked = videoHwAccel,
                    onCheckedChange = { settingsRepository.setVideoHardwareAcceleration(it) }
                )

                SettingsDivider()

                SettingToggleRow(
                    title = "Auto-Resume Video",
                    subtitle = "Remember last playback position and resume automatically",
                    icon = Icons.Outlined.Speed,
                    checked = autoResumeVideo,
                    onCheckedChange = { settingsRepository.setAutoResumeVideo(it) }
                )

                SettingsDivider()

                SettingToggleRow(
                    title = "Video Auto-Loop",
                    subtitle = "Automatically replay videos when they reach the end",
                    icon = Icons.Outlined.Repeat,
                    checked = videoAutoLoop,
                    onCheckedChange = { settingsRepository.setVideoAutoLoop(it) }
                )

                SettingsDivider()

                Text(
                    text = "Photo Editor Export Quality",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChip(
                        label = "95% (Ultra)",
                        icon = null,
                        selected = photoQuality == 95,
                        onClick = { settingsRepository.setPhotoEditorExportQuality(95) }
                    )
                    ThemeChip(
                        label = "90% (High)",
                        icon = null,
                        selected = photoQuality == 90,
                        onClick = { settingsRepository.setPhotoEditorExportQuality(90) }
                    )
                    ThemeChip(
                        label = "85% (Compact)",
                        icon = null,
                        selected = photoQuality == 85,
                        onClick = { settingsRepository.setPhotoEditorExportQuality(85) }
                    )
                }
            }

            // 4. LIBRARY
            SettingsCard(title = "LIBRARY") {
                Text(
                    text = "Default Startup Destination",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeTab.entries.forEach { tab ->
                        ThemeChip(
                            label = tab.label,
                            icon = null,
                            selected = startupTab == tab,
                            onClick = { settingsRepository.setDefaultStartupTab(tab) }
                        )
                    }
                }

                SettingsDivider()

                val formattedCache = if (cacheSizeBytes < 1024) {
                    "$cacheSizeBytes B"
                } else if (cacheSizeBytes < 1024 * 1024) {
                    String.format(Locale.US, "%.1f KB", cacheSizeBytes / 1024.0)
                } else {
                    String.format(Locale.US, "%.1f MB", cacheSizeBytes / (1024.0 * 1024.0))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Storage, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Thumbnail & Render Cache",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Currently used: $formattedCache",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val freed = settingsRepository.clearCache(context)
                            cacheSizeBytes = settingsRepository.getCacheSizeBytes(context)
                            val freedMb = freed / (1024.0 * 1024.0)
                            val toastText = if (freedMb >= 0.1) String.format(Locale.US, "Freed %.1f MB", freedMb) else context.getString(R.string.cache_cleared)
                            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        )
                    ) {
                        Text(stringResource(R.string.clear_cache), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                    }
                }

                if (onClearRecent != null) {
                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recent History & Progress",
                                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Clear recent documents and continue reading/watching progress",
                                style = typography.caption.copy(fontSize = 12.sp),
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClearRecent()
                                Toast.makeText(context, context.getString(R.string.recent_history_cleared), Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            )
                        ) {
                            Text(stringResource(R.string.clear_cache), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                        }
                    }
                }
            }

            // 5. ABOUT
            SettingsCard(title = "ABOUT") {
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

                SettingsDivider()

                when (val state = updateState) {
                    is UpdateUiState.Idle -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Software Updates",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Current version: v${updateManager.currentVersionName}",
                                    style = typography.caption.copy(fontSize = 12.sp),
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                            }
                        }
                    }

                    is UpdateUiState.Checking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.accent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Checking for updates…",
                                style = typography.body.copy(fontSize = 13.5.sp),
                                color = colors.textSecondary
                            )
                        }
                    }

                    is UpdateUiState.NoUpdate -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "App is up to date (v${state.currentVersion})",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                    color = colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Latest release installed.",
                                style = typography.caption.copy(fontSize = 12.sp),
                                color = colors.textSecondary
                            )
                        }
                    }

                    is UpdateUiState.UpdateAvailable -> {
                        val release = state.release
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "New Version Available: v${release.versionName}",
                                style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                                color = colors.accent
                            )
                            if (release.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = release.releaseNotes.trim(),
                                    style = typography.caption.copy(fontSize = 12.sp),
                                    color = colors.textSecondary,
                                    maxLines = 4
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { scope.launch { updateManager.downloadAndInstall(release) } },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    val sizeText = if (release.apkSizeBytes > 0) {
                                        val mb = release.apkSizeBytes / (1024.0 * 1024.0)
                                        String.format(Locale.US, " (%.1f MB)", mb)
                                    } else ""
                                    Text("Download & Install$sizeText", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = { updateManager.dismissUpdate(release.tagName) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.8.dp, colors.divider),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary)
                                ) {
                                    Text("Dismiss", fontSize = 12.5.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    is UpdateUiState.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Downloading: ${state.progressPercent}%",
                                style = typography.body.copy(fontWeight = FontWeight.Medium, fontSize = 13.5.sp),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (state.totalBytes > 0) state.downloadedBytes.toFloat() / state.totalBytes else 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = colors.accent,
                                trackColor = colors.divider
                            )
                        }
                    }

                    is UpdateUiState.ReadyToInstall -> {
                        Column {
                            Text(
                                text = "Download complete for v${state.release.versionName}.",
                                style = typography.body.copy(fontSize = 13.5.sp),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (!updateManager.installerHelper.canInstallUnknownApps()) {
                                        val intent = updateManager.installerHelper.createManageUnknownAppSourcesIntent()
                                        context.startActivity(intent)
                                    } else {
                                        updateManager.installerHelper.installApk(state.apkFile)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Install Update Now", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }

                    is UpdateUiState.Error -> {
                        Column {
                            Text(
                                text = "Update check failed: ${state.message}",
                                style = typography.caption.copy(color = colors.error, fontSize = 12.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.8.dp, colors.divider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                            ) {
                                Text("Retry", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }

                SettingsDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDesignSystemPreviewClick)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Design System Tokens",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Preview color tokens, radii, typography, and elevations",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = typography.caption.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                letterSpacing = 1.1.sp
            ),
            color = colors.accent,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    val colors = LocalAppColors.current
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 0.6.dp,
        color = colors.divider.copy(alpha = 0.5f)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surface
            )
        )
    }
}

@Composable
private fun ThemeChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.accent.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) colors.accent else colors.divider
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = typography.body.copy(
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) colors.accent else colors.textPrimary
            )
        }
    }
}
