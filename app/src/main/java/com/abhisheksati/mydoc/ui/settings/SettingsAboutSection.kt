package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.update.UpdateUiState
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsAboutSection(
    updateManager: UpdateManager,
    updateState: UpdateUiState,
    checkUpdatesOnLaunch: Boolean,
    onCheckUpdatesOnLaunchChange: (Boolean) -> Unit,
    onDesignSystemPreviewClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWordLicenses by rememberSaveable { mutableStateOf(false) }
    if (showWordLicenses) WordEngineLicensesDialog { showWordLicenses = false }

    SettingsCard(
        showHeader = false,
        title = stringResource(R.string.settings_about_title),
        icon = Icons.Outlined.Info,
        modifier = modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = onDesignSystemPreviewClick
        )
    ) {
        SettingToggleRow(
            title = stringResource(R.string.settings_check_updates_launch),
            subtitle = stringResource(R.string.settings_check_updates_subtitle),
            icon = Icons.Outlined.Refresh,
            checked = checkUpdatesOnLaunch,
            onCheckedChange = onCheckUpdatesOnLaunchChange
        )

        SettingsDivider()

        TextButton(onClick = { showWordLicenses = true }) {
            Text(stringResource(R.string.word_engine_licenses))
        }

        SettingsDivider()

        when (val state = updateState) {
            is UpdateUiState.Idle -> {
                UpdateLine(
                    title = stringResource(R.string.settings_updates_title),
                    detail = stringResource(R.string.settings_current_version, updateManager.currentVersionName),
                    actionLabel = stringResource(R.string.settings_check),
                    onAction = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } }
                )
            }

            is UpdateUiState.Checking -> {
                UpdateLine(
                    title = stringResource(R.string.settings_checking_title),
                    detail = stringResource(R.string.settings_checking_detail),
                    actionLabel = null,
                    onAction = null
                )
            }

            is UpdateUiState.NoUpdate -> {
                UpdateLine(
                    title = stringResource(R.string.settings_up_to_date),
                    detail = "v${state.currentVersion} installed",
                    actionLabel = stringResource(R.string.settings_check_again),
                    onAction = { scope.launch { updateManager.checkForUpdates(isLaunchCheck = false) } },
                    leadingCheck = true
                )
            }

            is UpdateUiState.UpdateAvailable -> {
                val release = state.release
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_new_version, release.versionName),
                        style = typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    if (release.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = release.releaseNotes.trim(),
                            style = typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 4
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sizeText = if (release.apkSizeBytes > 0) {
                            val mb = release.apkSizeBytes / (1024.0 * 1024.0)
                            String.format(Locale.US, " (%.1f MB)", mb)
                        } else ""
                        TextButton(
                            onClick = { scope.launch { updateManager.downloadAndInstall(release) } },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(stringResource(R.string.settings_download_install, sizeText))
                        }
                        TextButton(
                            onClick = { updateManager.dismissUpdate(release.tagName) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(stringResource(R.string.settings_dismiss), color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }

            is UpdateUiState.Downloading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_downloading, state.progressPercent),
                        style = typography.bodyMedium.withTabularFigures(),
                        color = scheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (state.totalBytes > 0) {
                                (state.downloadedBytes.toFloat() / state.totalBytes).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            is UpdateUiState.ReadyToInstall -> {
                UpdateLine(
                    title = stringResource(R.string.settings_download_complete),
                    detail = stringResource(R.string.settings_ready_install_detail, state.release.versionName),
                    actionLabel = stringResource(R.string.settings_install_now),
                    onAction = {
                        if (!updateManager.installerHelper.canInstallUnknownApps()) {
                            val intent = updateManager.installerHelper.createManageUnknownAppSourcesIntent()
                            context.startActivity(intent)
                        } else {
                            updateManager.install(state.apkFile)
                        }
                    }
                )
            }

            is UpdateUiState.Error -> {
                UpdateLine(
                    title = stringResource(R.string.settings_update_failed),
                    detail = state.message,
                    actionLabel = stringResource(R.string.settings_retry),
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
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingCheck) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyLarge,
                color = scheme.onSurface
            )
            Text(
                text = detail,
                style = typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}
