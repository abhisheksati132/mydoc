package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
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

    SettingsCard(title = "STORAGE & FILES", modifier = modifier) {
        Text(
            text = "Default Startup Destination",
            style = typography.captionStrong,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeTab.entries.forEach { tab ->
                ThemeChip(
                    label = tab.label,
                    icon = null,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(AppRadius.pillShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thumbnail & Render Cache",
                    style = typography.bodyStrong,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Currently used: $formattedCache",
                    style = typography.caption,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClearCacheClick,
                shape = AppRadius.pillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                )
            ) {
                Text(
                    text = stringResource(R.string.clear_cache),
                    style = AppTheme.typography.captionStrong,
                    maxLines = 1
                )
            }
        }

        if (onClearRecentClick != null) {
            SettingsDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(AppRadius.pillShape)
                        .background(colors.accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recent History & Progress",
                        style = typography.bodyStrong,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Clear recent documents and continue reading/watching progress",
                        style = typography.caption,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onClearRecentClick,
                    shape = AppRadius.pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    )
                ) {
                    Text(
                        text = stringResource(R.string.clear_cache),
                        style = AppTheme.typography.captionStrong,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
