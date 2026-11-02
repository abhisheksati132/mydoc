package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.EditorialAction
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
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

    SettingsCard(
        title = "About & updates",
        icon = Icons.Outlined.Info,
        modifier = modifier
    ) {
        SettingToggleRow(
            title = "Check for updates on launch",
            subtitle = "Check GitHub for a new version when the app starts",
            icon = Icons.Outlined.Refresh,
            checked = checkUpdatesOnLaunch,
            onCheckedChange = onCheckUpdatesOnLaunchChange
        )

        SettingsDivider()

        when (val state = updateState) {
            is UpdateUiState.Idle -> {
                UpdateLine(
                    title = "Software updates",
                    detail = "Current version v${updateManager.currentVersionName}",
                    actionLabel = "Check",
                    onAction = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } }
                )
            }

            is UpdateUiState.Checking -> {
                UpdateLine(
                    title = "Checking for updates…",
                    detail = "Asking GitHub for the latest release",
                    actionLabel = null,
                    onAction = null
                )
            }

            is UpdateUiState.NoUpdate -> {
                UpdateLine(
                    title = "App is up to date",
                    detail = "v${state.currentVersion} installed",
                    actionLabel = "Check again",
                    onAction = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                    leadingCheck = true
                )
            }

            is UpdateUiState.UpdateAvailable -> {
                val release = state.release
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.small)
                ) {
                    Text(
                        text = "New version v${release.versionName}",
                        style = typography.bodyLargeStrong,
                        color = colors.ink
                    )
                    if (release.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(AppSpacing.xSmall))
                        Text(
                            text = release.releaseNotes.trim(),
                            style = typography.caption,
                            color = colors.inkMuted,
                            maxLines = 4
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.large)
                    ) {
                        val sizeText = if (release.apkSizeBytes > 0) {
                            val mb = release.apkSizeBytes / (1024.0 * 1024.0)
                            String.format(Locale.US, " (%.1f MB)", mb)
                        } else ""
                        EditorialAction(
                            label = "Download & install$sizeText",
                            onClick = { scope.launch { updateManager.downloadAndInstall(release) } }
                        )
                        Text(
                            text = "Dismiss",
                            style = typography.captionStrong,
                            color = colors.inkMuted,
                            modifier = Modifier
                                .padding(vertical = AppSpacing.xSmall)
                                .pressScaleEffect(onClick = { updateManager.dismissUpdate(release.tagName) })
                        )
                    }
                }
            }

            is UpdateUiState.Downloading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.small)
                ) {
                    Text(
                        text = "Downloading ${state.progressPercent}%",
                        style = typography.caption.withTabularFigures(),
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(AppRadius.pillShape)
                            .background(colors.track)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(
                                    if (state.totalBytes > 0) {
                                        (state.downloadedBytes.toFloat() / state.totalBytes).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                )
                                .background(colors.accent)
                        )
                    }
                }
            }

            is UpdateUiState.ReadyToInstall -> {
                UpdateLine(
                    title = "Download complete",
                    detail = "v${state.release.versionName} is ready to install",
                    actionLabel = "Install now",
                    onAction = {
                        if (!updateManager.installerHelper.canInstallUnknownApps()) {
                            val intent = updateManager.installerHelper.createManageUnknownAppSourcesIntent()
                            context.startActivity(intent)
                        } else {
                            updateManager.installerHelper.installApk(state.apkFile)
                        }
                    }
                )
            }

            is UpdateUiState.Error -> {
                UpdateLine(
                    title = "Update check failed",
                    detail = state.message,
                    actionLabel = "Retry",
                    onAction = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } }
                )
            }
        }
    }
}

@Composable
private fun UpdateLine(
    title: String,
    detail: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    leadingCheck: Boolean = false
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingCheck) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier
                    .padding(end = AppSpacing.small)
                    .size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyLargeStrong,
                color = colors.ink
            )
            Text(
                text = detail,
                style = typography.caption,
                color = colors.inkMuted
            )
        }
        if (actionLabel != null && onAction != null) {
            EditorialAction(
                label = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(start = AppSpacing.medium)
            )
        }
    }
}
