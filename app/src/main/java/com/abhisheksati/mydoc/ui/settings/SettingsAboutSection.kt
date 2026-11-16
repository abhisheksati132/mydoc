package com.abhisheksati.mydoc.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.abhisheksati.mydoc.BuildConfig
import com.abhisheksati.mydoc.CrashLog
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.update.UpdateUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsAboutSection(
    updateManager: UpdateManager,
    updateState: UpdateUiState,
    checkUpdatesOnLaunch: Boolean,
    onCheckUpdatesOnLaunchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onDesignSystemPreviewClick: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWordLicenses by rememberSaveable { mutableStateOf(false) }
    if (showWordLicenses) WordEngineLicensesDialog { showWordLicenses = false }
    var showOpenSourceLicenses by rememberSaveable { mutableStateOf(false) }
    if (showOpenSourceLicenses) OpenSourceLicensesDialog { showOpenSourceLicenses = false }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    if (showPrivacy) {
        PrivacyTrustDialog(
            onDismiss = { showPrivacy = false },
            onViewSource = {
                showPrivacy = false
                context.openExternalLink(
                    MYDOC_URL.toUri(),
                    R.string.settings_link_failed
                )
            }
        )
    }
    var crashReport by remember { mutableStateOf(CrashLog.read(context)) }
    var showCrashReport by rememberSaveable { mutableStateOf(false) }
    val crashReportTitle = stringResource(R.string.settings_crash_report_title)
    val report = crashReport
    if (showCrashReport && report != null) {
        CrashReportDialog(
            report = report,
            onCopy = {
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText(crashReportTitle, report))
                Toast.makeText(
                    context,
                    R.string.settings_crash_report_copied,
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDelete = {
                CrashLog.clear(context)
                crashReport = null
                showCrashReport = false
            },
            onDismiss = { showCrashReport = false }
        )
    }

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

        TextButton(onClick = { showPrivacy = true }) {
            Text(stringResource(R.string.settings_privacy_trust))
        }
        TextButton(
            onClick = {
                context.openExternalLink(
                    "$MYDOC_URL/blob/main/PRIVACY.md".toUri(),
                    R.string.settings_link_failed
                )
            }
        ) {
            Text(stringResource(R.string.settings_privacy_policy))
        }
        TextButton(onClick = { context.openIssueTracker() }) {
            Text(stringResource(R.string.settings_report_issue))
        }
        TextButton(onClick = { showWordLicenses = true }) {
            Text(stringResource(R.string.word_engine_licenses))
        }
        TextButton(onClick = { showOpenSourceLicenses = true }) {
            Text(stringResource(R.string.open_source_licenses))
        }
        if (crashReport != null) {
            TextButton(onClick = { showCrashReport = true }) {
                Text(stringResource(R.string.settings_crash_report))
            }
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
                    detail = stringResource(R.string.settings_version_installed, state.currentVersion),
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
                        val locale = LocalConfiguration.current.locales[0]
                        val sizeText = if (release.apkSizeBytes > 0) {
                            val mb = release.apkSizeBytes / (1024.0 * 1024.0)
                            String.format(locale, " (%.1f MB)", mb)
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
private fun CrashReportDialog(
    report: String,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_crash_report_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_crash_report_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = scheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text(stringResource(R.string.settings_crash_report_copy))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.settings_crash_report_delete),
                        color = scheme.error
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_crash_report_close))
                }
            }
        }
    )
}

@Composable
private fun PrivacyTrustDialog(
    onDismiss: () -> Unit,
    onViewSource: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_privacy_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TrustPoint(
                    icon = Icons.Outlined.Shield,
                    title = R.string.settings_privacy_offline_title,
                    body = R.string.settings_privacy_offline_body
                )
                TrustPoint(
                    icon = Icons.Outlined.FolderOpen,
                    title = R.string.settings_privacy_storage_title,
                    body = R.string.settings_privacy_storage_body
                )
                TrustPoint(
                    icon = Icons.Outlined.SystemUpdate,
                    title = R.string.settings_privacy_updates_title,
                    body = R.string.settings_privacy_updates_body
                )
                TrustPoint(
                    icon = Icons.Outlined.BugReport,
                    title = R.string.settings_privacy_crash_title,
                    body = R.string.settings_privacy_crash_body
                )
                Text(
                    text = stringResource(R.string.settings_privacy_source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        dismissButton = {
            TextButton(onClick = onViewSource) {
                Text(stringResource(R.string.settings_view_source))
            }
        }
    )
}

@Composable
private fun TrustPoint(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes body: Int
) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/** GitHub is the only distribution channel, so it is also the only issue tracker. */
private const val MYDOC_URL = "https://github.com/abhisheksati132/mydoc"

private fun Context.openIssueTracker() {
    val body = getString(
        R.string.settings_issue_body,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE,
        "${Build.MANUFACTURER} ${Build.MODEL}",
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT
    )
    val uri = "$MYDOC_URL/issues/new".toUri().buildUpon()
        .appendQueryParameter("title", getString(R.string.settings_issue_title))
        .appendQueryParameter("body", body)
        .build()
    openExternalLink(uri, R.string.settings_report_issue_failed)
}

private fun Context.openExternalLink(uri: Uri, @StringRes failureMessage: Int) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show()
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
