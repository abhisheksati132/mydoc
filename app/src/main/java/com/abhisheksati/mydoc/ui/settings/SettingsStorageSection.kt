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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppChip
import com.abhisheksati.mydoc.designsystem.components.EditorialAction
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.VividGradients
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.HomeTab
import java.util.Locale

@Composable
fun SettingsStorageSection(
    startupTab: HomeTab,
    cacheSizeBytes: Long,
    onStartupTabChange: (HomeTab) -> Unit,
    onClearCacheClick: () -> Unit,
    onClearRecentClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    SettingsCard(
        title = "Storage & files",
        icon = Icons.Outlined.Storage,
        family = VividGradients.Family.FOREST,
        modifier = modifier
    ) {
        Text(
            text = "Default startup destination",
            style = typography.caption,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = AppSpacing.small, bottom = AppSpacing.small)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            HomeTab.entries.forEach { tab ->
                AppChip(
                    label = tab.label,
                    selected = startupTab == tab,
                    onClick = { onStartupTabChange(tab) }
                )
            }
        }

        SettingsDivider()

        val formattedCache = if (cacheSizeBytes < 1024) {
            "$cacheSizeBytes B"
        } else if (cacheSizeBytes < 1024 * 1024) {
            String.format(Locale.US, "%.1f KB", cacheSizeBytes / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", cacheSizeBytes / (1024.0 * 1024.0))
        }

        SettingActionRow(
            title = "Thumbnail & render cache",
            subtitle = "Currently using $formattedCache",
            icon = Icons.Outlined.Storage,
            actionLabel = stringResource(R.string.clear_cache),
            onAction = onClearCacheClick
        )

        if (onClearRecentClick != null) {
            SettingsDivider()
            SettingActionRow(
                title = "Recent history & progress",
                subtitle = "Clears recent documents and reading progress",
                icon = Icons.Outlined.DeleteSweep,
                actionLabel = stringResource(R.string.clear_cache),
                onAction = onClearRecentClick
            )
        }
    }
}

/**
 * A settings row whose action is a word on the right, not a filled button.
 */
@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier
                .padding(end = AppSpacing.medium)
                .size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyLargeStrong,
                color = colors.ink
            )
            Text(
                text = subtitle,
                style = typography.caption.withTabularFigures(),
                color = colors.inkMuted
            )
        }
        EditorialAction(
            label = actionLabel,
            onClick = onAction,
            modifier = Modifier.padding(start = AppSpacing.medium)
        )
    }
}
