package com.abhisheksati.mydoc.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val updateState by updateManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                .padding(horizontal = Spacing.medium)
        ) {
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // App Version Info
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
                            text = "The initial preview build (com.abhisheksati.mydoc.debug) is still on your device. Tap below to uninstall it.",
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
                            Text("Uninstall Old Duplicate")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // Self-updater Section
            Text(
                text = "UPDATES",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = "Check GitHub releases for updates. The app only accesses the network when checking or downloading an update.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

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
        }
    }
}
