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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.medium)
        ) {
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // APPEARANCE
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                FilterChip(
                    selected = currentTheme == AppThemeMode.AMOLED_DARK,
                    onClick = { settingsRepository.setThemeMode(AppThemeMode.AMOLED_DARK) },
                    label = { Text("Dark") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DarkMode,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                FilterChip(
                    selected = currentTheme == AppThemeMode.LIGHT,
                    onClick = { settingsRepository.setThemeMode(AppThemeMode.LIGHT) },
                    label = { Text("Light") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                FilterChip(
                    selected = currentTheme == AppThemeMode.SYSTEM,
                    onClick = { settingsRepository.setThemeMode(AppThemeMode.SYSTEM) },
                    label = { Text("System") }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // READER
            Text(
                text = "READER",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                FilterChip(
                    selected = currentReadingMode == ReadingMode.NORMAL,
                    onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.NORMAL) },
                    label = { Text("Normal") }
                )
                FilterChip(
                    selected = currentReadingMode == ReadingMode.INVERTED_DARK,
                    onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.INVERTED_DARK) },
                    label = { Text("Dark") }
                )
                FilterChip(
                    selected = currentReadingMode == ReadingMode.SEPIA,
                    onClick = { settingsRepository.setDefaultReadingMode(ReadingMode.SEPIA) },
                    label = { Text("Sepia") }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Keep screen awake",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { settingsRepository.setKeepScreenOn(it) }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Volume button page turn",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = volumeKeyTurning,
                    onCheckedChange = { settingsRepository.setVolumeKeyPageTurning(it) }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // SECURITY
            Text(
                text = "SECURITY",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biometric app lock",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Require fingerprint or PIN to open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // STORAGE
            Text(
                text = "STORAGE",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Document folder",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = grantedFolderName ?: "None selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    OutlinedButton(
                        onClick = { openFolderLauncher.launch(null) },
                        shape = RoundedCornerShape(6.dp)
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
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (isScanningFolder) "…" else "Rescan")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Render Cache
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Render cache",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val formattedCache = if (cacheSizeBytes < 1024) {
                        "$cacheSizeBytes B"
                    } else if (cacheSizeBytes < 1024 * 1024) {
                        String.format(Locale.US, "%.1f KB", cacheSizeBytes / 1024.0)
                    } else {
                        String.format(Locale.US, "%.1f MB", cacheSizeBytes / (1024.0 * 1024.0))
                    }
                    Text(
                        text = formattedCache,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = {
                        val freed = settingsRepository.clearCache(context)
                        cacheSizeBytes = settingsRepository.getCacheSizeBytes(context)
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Clear")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // ABOUT
            Text(
                text = "ABOUT",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "mydoc",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Installed version: ${updateManager.currentVersionName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            if (isLegacyDebugInstalled) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(Spacing.medium)
                ) {
                    Column {
                        Text(
                            text = "Old Duplicate App Detected",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))
                        Text(
                            text = "An old preview build is installed. Tap below to remove it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Button(
                            onClick = {
                                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.parse("package:com.abhisheksati.mydoc.debug")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(uninstallIntent)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Uninstall Duplicate")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // UPDATES
            Text(
                text = "UPDATES",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            when (val state = updateState) {
                is UpdateUiState.Idle -> {
                    OutlinedButton(
                        onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text("Check for updates")
                    }
                }

                is UpdateUiState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = "Checking GitHub Releases…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is UpdateUiState.NoUpdate -> {
                    Column {
                        Text(
                            text = "mydoc is up to date (v${state.currentVersion}).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Row {
                            OutlinedButton(
                                onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Check again")
                            }
                            if (state.latestRelease != null) {
                                Spacer(modifier = Modifier.width(Spacing.small))
                                OutlinedButton(
                                    onClick = { scope.launch { updateManager.downloadAndInstall(state.latestRelease) } },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Reinstall APK")
                                }
                            }
                        }
                    }
                }

                is UpdateUiState.UpdateAvailable -> {
                    val release = state.release
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(Spacing.medium)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = "Version ${release.versionName} available",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            if (release.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Text(
                                    text = release.releaseNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.medium))

                            Button(
                                onClick = { scope.launch { updateManager.downloadAndInstall(release) } },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                val sizeText = if (release.apkSizeBytes > 0) {
                                    val mb = release.apkSizeBytes / (1024.0 * 1024.0)
                                    String.format(Locale.US, " (%.1f MB)", mb)
                                } else ""
                                Text("Download & Install$sizeText")
                            }
                        }
                    }
                }

                is UpdateUiState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Downloading update: ${state.progressPercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))
                        LinearProgressIndicator(
                            progress = { if (state.totalBytes > 0) state.downloadedBytes.toFloat() / state.totalBytes else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                is UpdateUiState.ReadyToInstall -> {
                    Column {
                        Text(
                            text = "Download complete for v${state.release.versionName}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))

                        if (!updateManager.installerHelper.canInstallUnknownApps()) {
                            Text(
                                text = "Permission required: Android requires you to allow mydoc to install update APKs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))
                            Button(
                                onClick = {
                                    val intent = updateManager.installerHelper.createManageUnknownAppSourcesIntent()
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Grant Permission in Settings")
                            }
                        } else {
                            Button(
                                onClick = { updateManager.installerHelper.installApk(state.apkFile) },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Install Update")
                            }
                        }
                    }
                }

                is UpdateUiState.Error -> {
                    Column {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))
                        OutlinedButton(
                            onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}
