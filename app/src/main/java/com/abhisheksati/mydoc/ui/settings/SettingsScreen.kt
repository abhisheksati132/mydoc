package com.abhisheksati.mydoc.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.data.local.FolderScanner
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.update.UpdateUiState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    updateManager: UpdateManager,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit,
    onDesignSystemPreviewClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val updateState by updateManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val isOled = colors.background == DarkAppColors.background

    // General & Appearance
    val currentTheme by settingsRepository.themeMode.collectAsState()
    val dynamicColor by settingsRepository.dynamicColor.collectAsState()
    val amoledBlack by settingsRepository.amoledBlack.collectAsState()
    val currentStartupTab by settingsRepository.defaultStartupTab.collectAsState()
    val hapticFeedbackEnabled by settingsRepository.isHapticFeedbackEnabled.collectAsState()

    // Files & Storage
    val showHiddenFiles by settingsRepository.showHiddenFiles.collectAsState()
    val scanFullDeviceStorage by settingsRepository.scanFullDeviceStorage.collectAsState()
    val confirmBeforeDelete by settingsRepository.confirmBeforeDelete.collectAsState()
    val grantedFolderUri by settingsRepository.grantedFolderUri.collectAsState()
    val grantedFolderName by settingsRepository.grantedFolderName.collectAsState()
    var cacheSizeBytes by remember { mutableStateOf(settingsRepository.getCacheSizeBytes(context)) }
    var isScanningFolder by remember { mutableStateOf(false) }

    // Documents & Reader
    val currentReadingMode by settingsRepository.defaultReadingMode.collectAsState()
    val highQualityPdf by settingsRepository.highQualityPdf.collectAsState()
    val pdfFitMode by settingsRepository.pdfFitMode.collectAsState()
    val doubleTapZoom by settingsRepository.doubleTapZoom.collectAsState()
    val keepScreenOn by settingsRepository.keepScreenOn.collectAsState()
    val volumeKeyTurning by settingsRepository.volumeKeyPageTurning.collectAsState()

    // Media & Gallery
    val photoEditorQuality by settingsRepository.photoEditorExportQuality.collectAsState()
    val videoHwAccel by settingsRepository.videoHardwareAcceleration.collectAsState()
    val videoAutoLoop by settingsRepository.videoAutoLoop.collectAsState()
    val autoResumeVideo by settingsRepository.autoResumeVideo.collectAsState()

    // Privacy & Security
    val biometricLockEnabled by settingsRepository.isBiometricLockEnabled.collectAsState()

    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null && app != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val segment = uri.lastPathSegment ?: "Folder"
                val cleanName = segment.substringAfterLast(':').ifEmpty { segment }
                settingsRepository.setGrantedFolder(uri.toString(), cleanName)

                isScanningFolder = true
                scope.launch {
                    val count = FolderScanner.scanTree(context, uri, app.documentRepository)
                    isScanningFolder = false
                    Toast.makeText(context, "Added $count documents from $cleanName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to access folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isLegacyDebugInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.abhisheksati.mydoc.debug", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    }) {
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
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. APPEARANCE & GENERAL
            SettingsCard(title = "APPEARANCE & STARTUP") {
                // Theme Mode
                Text(
                    text = "App Theme",
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
                    subtitle = "Use pitch black background in dark mode to save OLED battery",
                    icon = Icons.Outlined.Contrast,
                    checked = amoledBlack,
                    onCheckedChange = {
                        settingsRepository.setAmoledBlack(it)
                    }
                )

                SettingsDivider()

                // Default Launch Tab
                Text(
                    text = "Default Launch Tab",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        Triple(HomeTab.NOW, Icons.Outlined.Folder, "Now"),
                        Triple(HomeTab.LIBRARY, Icons.AutoMirrored.Outlined.Article, "Library"),
                        Triple(HomeTab.TOOLS, Icons.Outlined.Build, "Tools")
                    )
                    for ((tab, icon, label) in tabs) {
                        val isSelected = currentStartupTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                settingsRepository.setDefaultStartupTab(tab)
                            },
                            label = { Text(label, color = if (isSelected) colors.accent else colors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) colors.accent else colors.textSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.16f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) colors.accent else colors.divider,
                                selectedBorderColor = colors.accent,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                SettingsDivider()

                // Tactile Haptics
                SettingToggleRow(
                    title = "Tactile Vibration Haptics",
                    subtitle = "Vibration response for navigation and actions",
                    icon = Icons.Outlined.Vibration,
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = {
                        if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        settingsRepository.setHapticFeedbackEnabled(it)
                    }
                )
            }

            // 2. FILES & STORAGE
            SettingsCard(title = "FILES & STORAGE") {
                // Scan Complete Device Storage
                SettingToggleRow(
                    title = "Scan Entire Phone for Library",
                    subtitle = "Automatically show all documents, books, and media found on device storage",
                    icon = Icons.Outlined.Folder,
                    checked = scanFullDeviceStorage,
                    onCheckedChange = { settingsRepository.setScanFullDeviceStorage(it) }
                )

                SettingsDivider()

                // Show Hidden Files
                SettingToggleRow(
                    title = "Show Hidden Files & Folders",
                    subtitle = "Display dotfiles and hidden folders (.nomedia, .hidden)",
                    icon = Icons.Outlined.Visibility,
                    checked = showHiddenFiles,
                    onCheckedChange = { settingsRepository.setShowHiddenFiles(it) }
                )

                SettingsDivider()

                // Confirm Before Delete
                SettingToggleRow(
                    title = "Confirm Before Delete",
                    subtitle = "Prompt before permanently deleting files",
                    icon = Icons.Outlined.Delete,
                    checked = confirmBeforeDelete,
                    onCheckedChange = { settingsRepository.setConfirmBeforeDelete(it) }
                )

                SettingsDivider()

                // Cache Cleaner
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
                            val toastText = if (freedMb >= 0.1) String.format(Locale.US, "Freed %.1f MB", freedMb) else "Cache cleared"
                            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Clear", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                    }
                }

                SettingsDivider()

                // Document Folder Access
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
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Document Folder",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = grantedFolderName ?: "None selected",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { openFolderLauncher.launch(null) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.divider),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                        ) {
                            Text(if (grantedFolderName != null) "Change" else "Select", fontSize = 12.sp, maxLines = 1)
                        }
                        if (grantedFolderUri != null && app != null) {
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse(grantedFolderUri)
                                    isScanningFolder = true
                                    scope.launch {
                                        val count = FolderScanner.scanTree(context, uri, app.documentRepository)
                                        isScanningFolder = false
                                        Toast.makeText(context, "Scanned $count documents", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isScanningFolder,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, colors.divider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                            ) {
                                Text(if (isScanningFolder) "…" else "Rescan", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 3. DOCUMENTS & READER
            SettingsCard(title = "DOCUMENTS & READER") {
                // Default Reading Mode
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

                // PDF Initial Page Fit Mode
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

                // High-Quality Vector PDF
                SettingToggleRow(
                    title = "High-Quality Vector PDF",
                    subtitle = "Supersample text and vectors for crisp reading",
                    icon = Icons.Outlined.PictureAsPdf,
                    checked = highQualityPdf,
                    onCheckedChange = { settingsRepository.setHighQualityPdf(it) }
                )

                SettingsDivider()

                // Double-Tap Quick Zoom
                SettingToggleRow(
                    title = "Double-Tap Quick Zoom",
                    subtitle = "Double tap document page to jump to 2.5x zoom",
                    icon = Icons.Outlined.Speed,
                    checked = doubleTapZoom,
                    onCheckedChange = { settingsRepository.setDoubleTapZoom(it) }
                )

                SettingsDivider()

                // Keep Screen Awake
                SettingToggleRow(
                    title = "Keep Screen Awake While Reading",
                    subtitle = "Prevents display timeout while reading documents",
                    icon = Icons.Outlined.LightMode,
                    checked = keepScreenOn,
                    onCheckedChange = { settingsRepository.setKeepScreenOn(it) }
                )

                SettingsDivider()

                // Volume Button Page Turning
                SettingToggleRow(
                    title = "Volume Button Page Turning",
                    subtitle = "Use volume keys to flip pages in book view",
                    icon = Icons.Outlined.Vibration,
                    checked = volumeKeyTurning,
                    onCheckedChange = { settingsRepository.setVolumeKeyPageTurning(it) }
                )
            }

            // 4. MEDIA & GALLERY
            SettingsCard(title = "MEDIA & GALLERY") {
                // Photo Editor Export Quality
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
                        label = "High (90%)",
                        icon = null,
                        selected = photoEditorQuality == 90,
                        onClick = { settingsRepository.setPhotoEditorExportQuality(90) }
                    )
                    ThemeChip(
                        label = "Ultra (95%)",
                        icon = null,
                        selected = photoEditorQuality == 95,
                        onClick = { settingsRepository.setPhotoEditorExportQuality(95) }
                    )
                    ThemeChip(
                        label = "100%",
                        icon = null,
                        selected = photoEditorQuality == 100,
                        onClick = { settingsRepository.setPhotoEditorExportQuality(100) }
                    )
                }

                SettingsDivider()

                // Hardware Video Acceleration
                SettingToggleRow(
                    title = "Hardware Video Acceleration",
                    subtitle = "Direct GPU MediaCodec via VLC engine",
                    icon = Icons.Outlined.Memory,
                    checked = videoHwAccel,
                    onCheckedChange = { settingsRepository.setVideoHardwareAcceleration(it) }
                )

                SettingsDivider()

                // Auto-Loop Video
                SettingToggleRow(
                    title = "Auto-Loop Video Playback",
                    subtitle = "Automatically replay video when it ends",
                    icon = Icons.Outlined.Refresh,
                    checked = videoAutoLoop,
                    onCheckedChange = { settingsRepository.setVideoAutoLoop(it) }
                )

                SettingsDivider()

                // Auto-Resume Video
                SettingToggleRow(
                    title = "Auto-Resume Video Playback",
                    subtitle = "Remember position and offer 1-tap resume",
                    icon = Icons.Outlined.Videocam,
                    checked = autoResumeVideo,
                    onCheckedChange = { settingsRepository.setAutoResumeVideo(it) }
                )
            }

            // 5. PRIVACY & SECURITY
            SettingsCard(title = "PRIVACY & SECURITY") {
                // Biometric Lock
                SettingToggleRow(
                    title = "Biometric App Lock",
                    subtitle = "Require fingerprint or device PIN to open mydoc",
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
            SettingsCard(title = "ABOUT & UPDATES") {
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
                                text = "Checking GitHub Releases…",
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
                                    text = "mydoc is up to date (v${state.currentVersion})",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                    color = colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.8.dp, colors.divider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                            ) {
                                Text("Check again", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    is UpdateUiState.UpdateAvailable -> {
                        val release = state.release
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = colors.accent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Version ${release.versionName} available",
                                    style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                    color = colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
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
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black)
                            ) {
                                Text("Install Update", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                            }
                        }
                    }

                    is UpdateUiState.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.message,
                                style = typography.body.copy(fontSize = 12.sp),
                                color = colors.error,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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

                // App Info Shortcut
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
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "mydoc",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Version ${updateManager.currentVersionName}",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, colors.divider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Launch, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("App Info", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }

                if (isLegacyDebugInstalled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isOled) Color(0xFF141418) else colors.surfaceVariant)
                            .border(
                                BorderStroke(
                                    0.75.dp,
                                    if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.6f)
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Old Preview App Installed",
                                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "An old preview build is installed. Tap below to remove it.",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                                        data = Uri.parse("package:com.abhisheksati.mydoc.debug")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(uninstallIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("Uninstall Duplicate", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-Contrast Accent Glow Toggle Switch.
 * - Solid vibrant accent track with pure white thumb when ON.
 * - Crisp slate track with high-contrast thumb and defined border when OFF.
 */
@Composable
private fun HighContrastSettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalAppColors.current
    val isOled = colors.background == DarkAppColors.background
    val haptic = LocalHapticFeedback.current

    Switch(
        checked = checked,
        onCheckedChange = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(it)
        },
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = colors.accent,
            checkedBorderColor = colors.accent,
            uncheckedThumbColor = if (isOled) Color(0xFFD0D4DC) else Color(0xFF6B7280),
            uncheckedTrackColor = if (isOled) Color(0xFF1E222D) else Color(0xFFE5E7EB),
            uncheckedBorderColor = if (isOled) Color(0xFF3B4252) else Color(0xFFCBD5E1)
        )
    )
}

/**
 * Clean, consistent toggle row with zero visual noise.
 */
@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = colors.background == DarkAppColors.background

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (checked) colors.accent.copy(alpha = 0.16f) else (if (isOled) Color(0xFF161922) else colors.surfaceVariant)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = if (checked) colors.accent else colors.textSecondary
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
                style = typography.caption.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        HighContrastSettingSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * Clean, minimal card section without gimmicky pill badges.
 */
@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = colors.background == DarkAppColors.background

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = typography.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isOled) Color(0xFF0F1015) else colors.surface,
            border = BorderStroke(
                1.dp,
                if (isOled) Color.White.copy(alpha = 0.07f) else colors.divider.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
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
        color = colors.divider.copy(alpha = 0.4f),
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun ThemeChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, color = if (selected) colors.accent else colors.textPrimary, fontSize = 12.sp, maxLines = 1) },
        leadingIcon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (selected) colors.accent else colors.textSecondary
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.accent.copy(alpha = 0.16f),
            containerColor = Color.Transparent
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (selected) colors.accent else colors.divider,
            selectedBorderColor = colors.accent,
            enabled = true,
            selected = selected
        ),
        shape = RoundedCornerShape(10.dp)
    )
}
