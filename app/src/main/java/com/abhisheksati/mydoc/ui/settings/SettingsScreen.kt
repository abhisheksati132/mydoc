package com.abhisheksati.mydoc.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Memory
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.data.local.FolderScanner
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.engine.libreoffice.EngineStatus
import com.abhisheksati.mydoc.rendering.engine.libreoffice.LibreOfficeEngineManager
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

    val currentTheme by settingsRepository.themeMode.collectAsState()
    val currentStartupTab by settingsRepository.defaultStartupTab.collectAsState()
    val hapticFeedbackEnabled by settingsRepository.isHapticFeedbackEnabled.collectAsState()
    val showHiddenFiles by settingsRepository.showHiddenFiles.collectAsState()
    val autoResumeVideo by settingsRepository.autoResumeVideo.collectAsState()
    val highQualityPdf by settingsRepository.highQualityPdf.collectAsState()
    val currentReadingMode by settingsRepository.defaultReadingMode.collectAsState()
    val keepScreenOn by settingsRepository.keepScreenOn.collectAsState()
    val volumeKeyTurning by settingsRepository.volumeKeyPageTurning.collectAsState()
    val biometricLockEnabled by settingsRepository.isBiometricLockEnabled.collectAsState()
    val grantedFolderUri by settingsRepository.grantedFolderUri.collectAsState()
    val grantedFolderName by settingsRepository.grantedFolderName.collectAsState()

    val diagnostics = remember { settingsRepository.getSystemDiagnostics() }
    var cacheSizeBytes by remember { mutableStateOf(settingsRepository.getCacheSizeBytes(context)) }
    var isScanningFolder by remember { mutableStateOf(false) }

    val loEngineManager = remember { app?.libreOfficeEngineManager ?: LibreOfficeEngineManager.getInstance(context) }
    val loEngineStatus by loEngineManager.status.collectAsState()
    val isLibreOfficeEnabled by settingsRepository.isLibreOfficeEngineEnabled.collectAsState()
    var showDeleteEngineDialog by remember { mutableStateOf(false) }

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
        },
        containerColor = colors.background
    ) { innerPadding ->
        val isLegacyDebugInstalled = remember {
            try {
                context.packageManager.getPackageInfo("com.abhisheksati.mydoc.debug", 0)
                true
            } catch (_: Exception) {
                false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // HERO / TELEMETRY BANNER (Nothing OS Hardware Aesthetic)
            SettingsDoubleBezelCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accent.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "mydoc CORE",
                                style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "v${updateManager.currentVersionName}  •  Supercharged",
                                style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = colors.accent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live hardware telemetry pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagnosticChip(
                        label = "CPU ARCH",
                        value = diagnostics.abiArchitecture,
                        modifier = Modifier.weight(1f)
                    )
                    DiagnosticChip(
                        label = "JVM HEAP",
                        value = "${diagnostics.jvmHeapUsedMb} MB",
                        modifier = Modifier.weight(1f)
                    )
                    DiagnosticChip(
                        label = "STORAGE",
                        value = "${diagnostics.availableStorageMb / 1024} GB",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // APPEARANCE & STARTUP
            AppSectionHeader(title = "APPEARANCE & STARTUP")
            SettingsDoubleBezelCard {
                    Text(
                        text = "App Theme",
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentTheme == AppThemeMode.AMOLED_DARK,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                settingsRepository.setThemeMode(AppThemeMode.AMOLED_DARK)
                            },
                            label = { Text("Dark", color = if (currentTheme == AppThemeMode.AMOLED_DARK) colors.accent else colors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (currentTheme == AppThemeMode.AMOLED_DARK) colors.accent else colors.textSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.16f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (currentTheme == AppThemeMode.AMOLED_DARK) colors.accent else colors.divider,
                                selectedBorderColor = colors.accent,
                                enabled = true,
                                selected = currentTheme == AppThemeMode.AMOLED_DARK
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = currentTheme == AppThemeMode.LIGHT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                settingsRepository.setThemeMode(AppThemeMode.LIGHT)
                            },
                            label = { Text("Light", color = if (currentTheme == AppThemeMode.LIGHT) colors.accent else colors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (currentTheme == AppThemeMode.LIGHT) colors.accent else colors.textSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.16f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (currentTheme == AppThemeMode.LIGHT) colors.accent else colors.divider,
                                selectedBorderColor = colors.accent,
                                enabled = true,
                                selected = currentTheme == AppThemeMode.LIGHT
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = currentTheme == AppThemeMode.SYSTEM,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                settingsRepository.setThemeMode(AppThemeMode.SYSTEM)
                            },
                            label = { Text("System", color = if (currentTheme == AppThemeMode.SYSTEM) colors.accent else colors.textPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.16f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (currentTheme == AppThemeMode.SYSTEM) colors.accent else colors.divider,
                                selectedBorderColor = colors.accent,
                                enabled = true,
                                selected = currentTheme == AppThemeMode.SYSTEM
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "Default Launch Tab",
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf(
                            Triple(HomeTab.FILES, Icons.Outlined.Folder, "Files"),
                            Triple(HomeTab.DOCUMENTS, Icons.AutoMirrored.Outlined.Article, "Documents"),
                            Triple(HomeTab.MEDIA, Icons.Outlined.PermMedia, "Media")
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TOUCH & HAPTICS
            AppSectionHeader(title = "TOUCH & HAPTICS")
            SettingsDoubleBezelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tactile Vibration Haptics",
                            style = typography.body,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Subtle vibration response for dock, hold-selection, and list actions",
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = {
                            if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            settingsRepository.setHapticFeedbackEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.accent,
                            uncheckedBorderColor = colors.divider
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // READER & PLAYBACK ENGINE
            AppSectionHeader(title = "READER & PLAYBACK ENGINE")
            SettingsDoubleBezelCard {
                    Text(
                        text = "Default reading mode",
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentReadingMode == ReadingMode.NORMAL,
                            onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.NORMAL) },
                            label = { Text("Normal", color = if (currentReadingMode == ReadingMode.NORMAL) colors.accent else colors.textPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.16f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (currentReadingMode == ReadingMode.NORMAL) colors.accent else colors.divider,
                                selectedBorderColor = colors.accent,
                                enabled = true,
                                selected = currentReadingMode == ReadingMode.NORMAL
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = currentReadingMode == ReadingMode.INVERTED_DARK,
                            onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.INVERTED_DARK) },
                            label = { Text("Dark", color = if (currentReadingMode == ReadingMode.INVERTED_DARK) colors.accent else colors.textPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.16f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (currentReadingMode == ReadingMode.INVERTED_DARK) colors.accent else colors.divider,
                                selectedBorderColor = colors.accent,
                                enabled = true,
                                selected = currentReadingMode == ReadingMode.INVERTED_DARK
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = currentReadingMode == ReadingMode.SEPIA,
                            onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.SEPIA) },
                            label = { Text("Sepia", color = if (currentReadingMode == ReadingMode.SEPIA) colors.accent else colors.textPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.16f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (currentReadingMode == ReadingMode.SEPIA) colors.accent else colors.divider,
                                selectedBorderColor = colors.accent,
                                enabled = true,
                                selected = currentReadingMode == ReadingMode.SEPIA
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "High-Quality Vector PDF",
                                style = typography.body,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Supersample text and vector geometry for ultra-crisp reading",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
                            checked = highQualityPdf,
                            onCheckedChange = { settingsRepository.setHighQualityPdf(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedBorderColor = colors.divider
                            )
                        )
                    }

                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Resume Video Playback",
                                style = typography.body,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Remember and automatically offer to resume where you left off",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
                            checked = autoResumeVideo,
                            onCheckedChange = { settingsRepository.setAutoResumeVideo(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedBorderColor = colors.divider
                            )
                        )
                    }

                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Keep screen awake while reading",
                            style = typography.body,
                            color = colors.textPrimary
                        )
                        Switch(
                            checked = keepScreenOn,
                            onCheckedChange = { settingsRepository.setKeepScreenOn(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedBorderColor = colors.divider
                            )
                        )
                    }

                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Volume button page turn",
                            style = typography.body,
                            color = colors.textPrimary
                        )
                        Switch(
                            checked = volumeKeyTurning,
                            onCheckedChange = { settingsRepository.setVolumeKeyPageTurning(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedBorderColor = colors.divider
                            )
                        )
                    }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ADVANCED OFFICE ENGINE (LIBREOFFICE CORE)
            AppSectionHeader(title = "OFFICE ENGINE (LIBREOFFICE)")
            SettingsDoubleBezelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accent.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Article,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LibreOffice Engine",
                                    style = typography.body.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val isReady = loEngineStatus is EngineStatus.Ready
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isReady) Color(0x2210B981) else Color(0x223B82F6))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isReady) Color(0xFF10B981) else Color(0xFF3B82F6))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isReady) "READY" else "OPTIONAL",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = if (isReady) Color(0xFF10B981) else Color(0xFF3B82F6)
                                    )
                                }
                            }
                            Text(
                                text = when (loEngineStatus) {
                                    is EngineStatus.Ready -> "Installed • Desktop fidelity office viewer"
                                    is EngineStatus.Downloading -> "Downloading engine assets..."
                                    is EngineStatus.Extracting -> "Installing native components..."
                                    else -> "Optional desktop rendering engine for PPT/DOC/XLS"
                                },
                                style = typography.caption.copy(fontSize = 11.sp),
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Clean simple CTA button
                    when (val st = loEngineStatus) {
                        is EngineStatus.Ready -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFEF5350).copy(alpha = 0.12f))
                                    .clickable { showDeleteEngineDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Delete",
                                    color = Color(0xFFEF5350),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is EngineStatus.Downloading -> {
                            Text(
                                text = "${st.progressPercent}%",
                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                color = colors.accent
                            )
                        }
                        is EngineStatus.Extracting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                        }
                        else -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.accent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        loEngineManager.downloadAndInstall()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    tint = if (isOled) Color(0xFF000000) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Download",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOled) Color(0xFF000000) else Color.White
                                )
                            }
                        }
                    }
                }

                if (loEngineStatus is EngineStatus.Downloading) {
                    val dl = loEngineStatus as EngineStatus.Downloading
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { dl.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = colors.accent,
                        trackColor = colors.accent.copy(alpha = 0.2f)
                    )
                }
            }

            if (showDeleteEngineDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteEngineDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF5350)
                        )
                    },
                    title = {
                        Text(
                            text = "Delete LibreOffice Engine?",
                            style = typography.title.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "This will delete the downloaded engine files and free up storage. You can download it again anytime.",
                            style = typography.caption.copy(lineHeight = 18.sp),
                            color = colors.textSecondary
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteEngineDialog = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                loEngineManager.uninstall {
                                    settingsRepository.setLibreOfficeEngineEnabled(false)
                                }
                                Toast.makeText(context, "LibreOffice Engine deleted", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "Delete",
                                color = Color(0xFFEF5350),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteEngineDialog = false }) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                    },
                    containerColor = colors.surface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PRIVACY & SECURITY
            AppSectionHeader(title = "PRIVACY & SECURITY")
            SettingsDoubleBezelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric app lock",
                                style = typography.body,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Require fingerprint or device PIN to access mydoc",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
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
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedBorderColor = colors.divider
                            )
                        )
                    }

                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show hidden files & folders",
                                style = typography.body,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Display hidden dotfiles (.nomedia, .hidden) in file browser",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
                            checked = showHiddenFiles,
                            onCheckedChange = { settingsRepository.setShowHiddenFiles(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedBorderColor = colors.divider
                            )
                        )
                    }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // STORAGE & OPTIMIZER
            AppSectionHeader(title = "STORAGE & OPTIMIZER")
            SettingsDoubleBezelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Document folder",
                                style = typography.body,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = grantedFolderName ?: "None selected",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { openFolderLauncher.launch(null) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, colors.divider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                            ) {
                                Text(if (grantedFolderName != null) "Change" else "Select")
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
                                    Text(if (isScanningFolder) "…" else "Rescan")
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Render & thumbnail cache",
                                style = typography.body,
                                color = colors.textPrimary
                            )
                            val formattedCache = if (cacheSizeBytes < 1024) {
                                "$cacheSizeBytes B"
                            } else if (cacheSizeBytes < 1024 * 1024) {
                                String.format(Locale.US, "%.1f KB", cacheSizeBytes / 1024.0)
                            } else {
                                String.format(Locale.US, "%.1f MB", cacheSizeBytes / (1024.0 * 1024.0))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formattedCache,
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val freed = settingsRepository.clearCache(context)
                                cacheSizeBytes = settingsRepository.getCacheSizeBytes(context)
                                val freedMb = freed / (1024.0 * 1024.0)
                                val toastText = if (freedMb >= 0.1) String.format(Locale.US, "Optimized: freed %.1f MB", freedMb) else "Cache cleared"
                                Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Deep Clean", fontWeight = FontWeight.Bold)
                        }
                    }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SYSTEM & HARDWARE DIAGNOSTICS ("ENGINE ROOM")
            AppSectionHeader(title = "SYSTEM & DIAGNOSTICS")
            SettingsDoubleBezelCard {
                DiagnosticRow(label = "Device", value = diagnostics.deviceName)
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                DiagnosticRow(label = "Android Version", value = "${diagnostics.androidVersion} (API ${diagnostics.apiLevel})")
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                DiagnosticRow(label = "Processor ABI", value = diagnostics.abiArchitecture)
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                DiagnosticRow(label = "Allocated JVM Heap", value = "${diagnostics.jvmHeapUsedMb} MB / ${diagnostics.jvmHeapMaxMb} MB")
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                DiagnosticRow(label = "Free Internal Storage", value = "${diagnostics.availableStorageMb / 1024} GB free")

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(0.8.dp, colors.divider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("App Info", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onDesignSystemPreviewClick,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(0.8.dp, colors.accent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.DeveloperMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Design Lab", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // UPDATES
            AppSectionHeader(title = "UPDATES & GITHUB")
            SettingsDoubleBezelCard {
                when (val state = updateState) {
                    is UpdateUiState.Idle -> {
                        Button(
                            onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for updates", fontWeight = FontWeight.Bold)
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
                                style = typography.body,
                                color = colors.textSecondary
                            )
                        }
                    }

                    is UpdateUiState.NoUpdate -> {
                        Column {
                            Text(
                                text = "mydoc is up to date (v${state.currentVersion}).",
                                style = typography.body,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row {
                                OutlinedButton(
                                    onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(0.8.dp, colors.divider),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                                ) {
                                    Text("Check again", fontWeight = FontWeight.Medium)
                                }
                                if (state.latestRelease != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { scope.launch { updateManager.downloadAndInstall(state.latestRelease) } },
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(0.8.dp, colors.divider),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                                    ) {
                                        Text("Reinstall APK", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    is UpdateUiState.UpdateAvailable -> {
                        val release = state.release
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.SystemUpdate,
                                    contentDescription = null,
                                    tint = colors.accent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Version ${release.versionName} available",
                                    style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                                    color = colors.textPrimary
                                )
                            }

                            if (release.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = release.releaseNotes,
                                    style = typography.body,
                                    color = colors.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { scope.launch { updateManager.downloadAndInstall(release) } },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = Color.Black
                                )
                            ) {
                                val sizeText = if (release.apkSizeBytes > 0) {
                                    val mb = release.apkSizeBytes / (1024.0 * 1024.0)
                                    String.format(Locale.US, " (%.1f MB)", mb)
                                } else ""
                                Text("Download & Install$sizeText", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateUiState.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Downloading update: ${state.progressPercent}%",
                                style = typography.body,
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
                                style = typography.body,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (!updateManager.installerHelper.canInstallUnknownApps()) {
                                Text(
                                    text = "Permission required: Android requires you to allow mydoc to install update APKs.",
                                    style = typography.caption,
                                    color = colors.accent
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val intent = updateManager.installerHelper.createManageUnknownAppSourcesIntent()
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black)
                                ) {
                                    Text("Grant Permission in Settings", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { updateManager.installerHelper.installApk(state.apkFile) },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black)
                                ) {
                                    Text("Install Update", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is UpdateUiState.Error -> {
                        Column {
                            Text(
                                text = state.message,
                                style = typography.body,
                                color = colors.error
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(0.8.dp, colors.divider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                            ) {
                                Text("Retry", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ABOUT
            AppSectionHeader(title = "ABOUT")
            SettingsDoubleBezelCard {
                Text(
                    text = "mydoc",
                    style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Installed version: ${updateManager.currentVersionName}  •  Apache 2.0  •  100% Offline & Private",
                    style = typography.caption,
                    color = colors.textSecondary
                )

                if (isLegacyDebugInstalled) {
                    val isOled = colors.background == DarkAppColors.background
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
                                text = "Old Duplicate App Detected",
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
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("Uninstall Duplicate", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = colors.background == DarkAppColors.background

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isOled) Color(0xFF141418) else colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                style = typography.caption.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = colors.accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = typography.body.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = typography.caption,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = typography.body.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = colors.textPrimary
        )
    }
}

/**
 * Sleek, single-surface card for all Settings sections with generous breathing space.
 */
@Composable
private fun SettingsDoubleBezelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val isOled = colors.background == DarkAppColors.background

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isOled) Color(0xFF0F1015) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.07f) else colors.divider.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            content()
        }
    }
}
