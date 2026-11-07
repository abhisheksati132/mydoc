package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecentFilesHeader(
    count: Int,
    showPreviews: Boolean,
    onBrowse: () -> Unit,
    onTogglePreviews: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xSmall)
    ) {
        Text(
            stringResource(R.string.home_recent_files_count, count),
            style = AppTheme.typography.heading,
            color = AppTheme.colors.ink,
            modifier = Modifier.align(Alignment.CenterVertically).semantics { heading() }
        )
        Row(
            modifier = Modifier.widthIn(max = 240.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xSmall)
        ) {
            TextButton(onClick = onBrowse, modifier = Modifier.weight(1f, fill = false)) {
                Text(stringResource(R.string.home_browse_files), style = AppTheme.typography.captionStrong)
            }
            IconButton(onClick = onTogglePreviews) {
                Icon(
                    if (showPreviews) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = stringResource(if (showPreviews) R.string.home_show_file_list else R.string.home_show_previews),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
