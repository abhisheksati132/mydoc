package com.abhisheksati.mydoc.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.data.local.FolderScanner
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.ui.theme.Spacing
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.update.UpdateUiState
import kotlinx.coroutines.launch
import java.util.Locale

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

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

    val currentTheme by settingsRepository.themeMode.collectAsState()
    val currentReadingMode by settingsRepository.defaultReadingMode.collectAsState()
    val keepScreenOn by settingsRepository.keepScreenOn.collectAsState()
    val volumeKeyTurning by settingsRepository.volumeKeyPageTurning.collectAsState()
    val biometricLockEnabled by settingsRepository.isBiometricLockEnabled.collectAsState()
    val grantedFolderUri by settingsRepository.grantedFolderUri.collectAsState()
    val grantedFolderName by settingsRepository.grantedFolderName.collectAsState()

    var cacheSizeBytes by remember { mutableStateOf(settingsRepository.getCacheSizeBytes(context)) }
    var isScanningFolder by remember { mutableStateOf(false) }

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
                .padding(bottom = 32.dp)
        ) {
            // APPEARANCE
            AppSectionHeader(title = "APPEARANCE")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentTheme == AppThemeMode.AMOLED_DARK,
                        onClick = { settingsRepository.setThemeMode(AppThemeMode.AMOLED_DARK) },
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
                        onClick = { settingsRepository.setThemeMode(AppThemeMode.LIGHT) },
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
                        onClick = { settingsRepository.setThemeMode(AppThemeMode.SYSTEM) },
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // READER
            AppSectionHeader(title = "READER")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Default reading mode",
                        style = typography.caption,
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
                        Text(
                            text = "Keep screen awake",
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SECURITY
            AppSectionHeader(title = "SECURITY")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                            text = "Require fingerprint or PIN to open",
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // STORAGE
            AppSectionHeader(title = "STORAGE")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
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
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
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
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
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
                                text = "Render cache",
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
                        OutlinedButton(
                            onClick = {
                                val freed = settingsRepository.clearCache(context)
                                cacheSizeBytes = settingsRepository.getCacheSizeBytes(context)
                                Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ABOUT
            AppSectionHeader(title = "ABOUT")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "mydoc",
                        style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Installed version: ${updateManager.currentVersionName}",
                        style = typography.caption,
                        color = colors.textSecondary
                    )

                    if (isLegacyDebugInstalled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = colors.divider,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(
                                    color = colors.background,
                                    shape = RoundedCornerShape(10.dp)
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
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                                ) {
                                    Text("Uninstall Duplicate")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // UPDATES
            AppSectionHeader(title = "UPDATES")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = colors.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    when (val state = updateState) {
                        is UpdateUiState.Idle -> {
                            Button(
                                onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                                shape = RoundedCornerShape(10.dp),
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
                                Text("Check for updates", fontWeight = FontWeight.SemiBold)
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
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                                    ) {
                                        Text("Check again")
                                    }
                                    if (state.latestRelease != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { scope.launch { updateManager.downloadAndInstall(state.latestRelease) } },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                                        ) {
                                            Text("Reinstall APK")
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
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black)
                                    ) {
                                        Text("Grant Permission in Settings")
                                    }
                                } else {
                                    Button(
                                        onClick = { updateManager.installerHelper.installApk(state.apkFile) },
                                        shape = RoundedCornerShape(10.dp),
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
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
