package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.ui.files.LibraryThumbnail

/** File identity comes first; every existing recent-file action remains available. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RecentFileRow(
    item: LibraryItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDetails: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = AppTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(AppRadius.cardShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else colors.surface)
            .then(if (selectionMode) Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })
                else Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LibraryThumbnail(item = item, modifier = Modifier.size(width = 40.dp, height = 48.dp).clip(AppRadius.insetShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.name, style = AppTheme.typography.bodyLarge, color = colors.ink,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.recent_file_metadata, item.typeBadge, item.formattedSize, item.formattedLastModified),
                style = AppTheme.typography.caption, color = colors.inkSecondary)
            item.folder?.takeIf { it.isNotBlank() }?.let { folder ->
                Text(folder, style = AppTheme.typography.caption, color = colors.inkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (selectionMode) {
            Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null, tint = if (selected) colors.accent else colors.inkMuted)
        } else Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_options_for_file, item.name), tint = colors.inkSecondary)
            }
            AppDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_details)) }, onClick = { menuOpen = false; onDetails() })
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_share)) }, onClick = { menuOpen = false; onShare() })
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_remove_recent)) }, onClick = { menuOpen = false; onRemove() })
            }
        }
    }
}
