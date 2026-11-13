package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.HomeTab

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsStorageSection(
    startupTab: HomeTab,
    cacheSizeBytes: Long,
    onStartupTabChange: (HomeTab) -> Unit,
    onClearCacheClick: () -> Unit,
    onClearRecentClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        showHeader = false,
        title = stringResource(R.string.settings_storage_title),
        icon = Icons.Outlined.Storage,
        modifier = modifier
    ) {
        SettingsSectionLabel(text = stringResource(R.string.settings_startup_destination))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeTab.entries.forEach { tab ->
                FilterChip(
                    selected = startupTab == tab,
                    onClick = { onStartupTabChange(tab) },
                    label = { Text(tab.label) }
                )
            }
        }

        SettingsDivider(modifier = Modifier.padding(top = 12.dp))

        val locale = LocalConfiguration.current.locales[0]
        val formattedCache = if (cacheSizeBytes < 1024) {
            "$cacheSizeBytes B"
        } else if (cacheSizeBytes < 1024 * 1024) {
            String.format(locale, "%.1f KB", cacheSizeBytes / 1024.0)
        } else {
            String.format(locale, "%.1f MB", cacheSizeBytes / (1024.0 * 1024.0))
        }

        SettingActionRow(
            title = stringResource(R.string.settings_cache_title),
            subtitle = stringResource(R.string.settings_cache_subtitle, formattedCache),
            icon = Icons.Outlined.Storage,
            actionLabel = stringResource(R.string.clear_cache),
            onAction = onClearCacheClick
        )

        if (onClearRecentClick != null) {
            SettingsDivider()
            SettingActionRow(
                title = stringResource(R.string.settings_history_title),
                subtitle = stringResource(R.string.settings_history_subtitle),
                icon = Icons.Outlined.DeleteSweep,
                actionLabel = stringResource(R.string.clear_cache),
                onAction = onClearRecentClick
            )
        }
    }
}

/**
 * A settings row whose action is a text button on the right.
 */
@Composable
internal fun SettingActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                style = typography.bodyLarge,
                color = scheme.onSurface
            )
            Text(
                text = subtitle,
                style = typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onAction) {
            Text(
                text = actionLabel,
                color = scheme.primary
            )
        }
    }
}
