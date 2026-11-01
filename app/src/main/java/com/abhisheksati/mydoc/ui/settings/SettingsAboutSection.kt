package com.abhisheksati.mydoc.ui.settings

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.update.UpdateUiState
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SettingsAboutSection(
    updateManager: UpdateManager,
    updateState: UpdateUiState,
    checkUpdatesOnLaunch: Boolean,
    onCheckUpdatesOnLaunchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SettingsCard(title = "ABOUT & UPDATES", modifier = modifier) {
        SettingToggleRow(
            title = "Check for updates on launch",
            subtitle = "Automatically check for new versions on GitHub when the app starts",
            icon = Icons.Outlined.Refresh,
            checked = checkUpdatesOnLaunch,
            onCheckedChange = onCheckUpdatesOnLaunchChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = colors.divider.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )

        when (val state = updateState) {
            is UpdateUiState.Idle -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Software Updates",
                            style = typography.bodyStrong,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Current version: v${updateManager.currentVersionName}",
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                        shape = AppRadius.pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check", style = AppTheme.typography.captionStrong, maxLines = 1)
                    }
                }
            }

            is UpdateUiState.Checking -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Checking for updates...",
                        style = typography.bodyMedium,
                        color = colors.textPrimary
                    )
                }
            }

            is UpdateUiState.NoUpdate -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "App is up to date (v${state.currentVersion})",
                                style = typography.bodyStrong,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latest release installed.",
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                        shape = AppRadius.pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check", style = AppTheme.typography.captionStrong, maxLines = 1)
                    }
                }
            }

            is UpdateUiState.UpdateAvailable -> {
                val release = state.release
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "New Version Available: v${release.versionName}",
                        style = typography.bodyStrong,
                        color = colors.accent
                    )
                    if (release.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = release.releaseNotes.trim(),
                            style = typography.caption,
                            color = colors.textSecondary,
                            maxLines = 4
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { scope.launch { updateManager.downloadAndInstall(release) } },
                            shape = AppRadius.pillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = Color.Black
                            )
                        ) {
                            val sizeText = if (release.apkSizeBytes > 0) {
                                val mb = release.apkSizeBytes / (1024.0 * 1024.0)
                                String.format(Locale.US, " (%.1f MB)", mb)
                            } else ""
                            Text("Download & Install$sizeText", style = AppTheme.typography.captionStrong, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { updateManager.dismissUpdate(release.tagName) },
                            shape = AppRadius.pillShape,
                            border = BorderStroke(0.8.dp, colors.divider),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary)
                        ) {
                            Text("Dismiss", style = AppTheme.typography.caption, maxLines = 1)
                        }
                    }
                }
            }

            is UpdateUiState.Downloading -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Downloading: ${state.progressPercent}%",
                        style = typography.label,
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
                        style = typography.label,
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
                        shape = AppRadius.pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Install Update Now", style = AppTheme.typography.captionStrong)
                    }
                }
            }

            is UpdateUiState.Error -> {
                Column {
                    Text(
                        text = "Update check failed: ${state.message}",
                        style = typography.caption.copy(color = colors.error)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                        shape = AppRadius.pillShape,
                        border = BorderStroke(0.8.dp, colors.divider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                    ) {
                        Text("Retry", style = AppTheme.typography.caption, maxLines = 1)
                    }
                }
            }
        }
    }
}
