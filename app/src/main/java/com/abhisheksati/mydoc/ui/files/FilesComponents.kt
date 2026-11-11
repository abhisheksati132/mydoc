package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.abhisheksati.mydoc.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageCategoryStat
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType

@Composable
fun FilesSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StorageOverviewCard(
    usedRatio: Float,
    formattedTotal: String,
    formattedFree: String,
    onOpenTrash: () -> Unit,
    onBrowse: () -> Unit,
    onInsights: () -> Unit,
    onClean: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = modifier.fillMaxWidth(), shape = AppRadius.largeShape,
        colors = CardDefaults.cardColors(containerColor = scheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onInsights) { Text(stringResource(R.string.files_insights)) }
                TextButton(onClick = onOpenTrash) { Text(stringResource(R.string.menu_recycle_bin)) }
                TextButton(onClick = onClean) { Text(stringResource(R.string.settings_clean)) }
            }
            Row(Modifier.fillMaxWidth().clip(AppRadius.cardShape).clickable(onClick = onBrowse).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(24.dp), tint = scheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.files_title_internal), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                    Text(stringResource(R.string.storage_capacity_summary, formattedFree.ifEmpty { "—" }, formattedTotal.ifEmpty { "—" }),
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { usedRatio.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp).clip(CircleShape),
                color = scheme.primary, trackColor = scheme.surfaceVariant, gapSize = 0.dp, drawStopIndicator = {})

        }
    }
}

@Composable
fun CategoryTile(
    type: StorageCategoryType,
    stat: StorageCategoryStat?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = AppTheme.colors
    val tone = when (type) {
        StorageCategoryType.IMAGES, StorageCategoryType.DOWNLOADS -> colors.accentCyan
        StorageCategoryType.VIDEOS, StorageCategoryType.ARCHIVES -> colors.accentPink
        StorageCategoryType.AUDIO, StorageCategoryType.APKS -> colors.accentGreen
        else -> colors.accent
    }
    GlassSurface(
        shape = AppRadius.cardShape,
        elevation = 0.dp,
        backgroundColor = colors.surface,
        modifier = modifier
            .clip(AppRadius.cardShape)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(AppRadius.insetShape)
                    .background(tone.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(if (type == StorageCategoryType.APKS) R.string.files_category_apps else type.titleRes),
                style = AppTheme.typography.bodyStrong,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    stat == null -> stringResource(R.string.files_scanning_short)
                    stat.fileCount == 0 -> stringResource(R.string.files_empty_short)
                    else -> "${stat.fileCount} · ${stat.formattedSize}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FolderPlate(modifier: Modifier = Modifier, size: Int = 44) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(AppRadius.insetShape)
            .background(scheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = scheme.onSecondaryContainer,
            modifier = Modifier.size((size * 0.48f).dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    item: StorageItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFileSize: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberAppHaptics()
    val pressInteractions = androidx.compose.runtime.remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(AppRadius.cardShape)
            .background(if (isSelected) scheme.primaryContainer else scheme.background)
            .then(
                if (isSelectionMode) {
                    // R7: `toggleable` is what publishes a checked state to the accessibility layer.
                    // Measured on mydoc_api35: a plain `semantics { selected = … }` never reached the
                    // node, and a bare `role = Role.Checkbox` made every row checkable while the
                    // selected one still read unchecked — worse than saying nothing. Selection mode
                    // only, so a row being browsed normally is not announced as a checkbox.
                    Modifier.toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { onClick() }
                    )
                } else {
                    Modifier.pressScale(pressInteractions).combinedClickable(
                        interactionSource = pressInteractions,
                        indication = ripple(),
                        onClick = onClick,
                        onLongClick = {
                            haptics.snap()
                            onLongClick()
                        }
                    )
                }
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (item.isDirectory) {
            FolderPlate()
        } else {
            StorageThumbnail(
                item = item,
                modifier = Modifier
                    .size(width = 44.dp, height = 52.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = AppTheme.typography.bodyLargeStrong,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (item.isDirectory) {
                    item.formattedSize
                } else {
                    listOf(if (showFileSize) item.formattedSize else "", item.formattedLastModified)
                        .filter { it.isNotBlank() }.joinToString(" · ")
                },
                style = AppTheme.typography.caption,
                color = scheme.onSurfaceVariant
            )
        }

        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = stringResource(if (isSelected) R.string.cd_selected else R.string.cd_not_selected),
                tint = if (isSelected) scheme.primary else scheme.outline,
                modifier = Modifier.size(22.dp)
            )
        } else {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.more_options_for_file, item.name),
                    tint = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridCell(
    item: StorageItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    galleryTile: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberAppHaptics()
    val pressInteractions = androidx.compose.runtime.remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppRadius.cardShape)
            .then(
                if (isSelectionMode) {
                    // R7: `toggleable` is what publishes a checked state to the accessibility layer.
                    // Measured on mydoc_api35: a plain `semantics { selected = … }` never reached the
                    // node, and a bare `role = Role.Checkbox` made every row checkable while the
                    // selected one still read unchecked — worse than saying nothing. Selection mode
                    // only, so a row being browsed normally is not announced as a checkbox.
                    Modifier.toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { onClick() }
                    )
                } else {
                    Modifier.pressScale(pressInteractions).combinedClickable(
                        interactionSource = pressInteractions,
                        indication = ripple(),
                        onClick = onClick,
                        onLongClick = {
                            haptics.snap()
                            onLongClick()
                        }
                    )
                }
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (LocalDensity.current.fontScale > 1.3f) 2f else 1f)
                .clip(AppRadius.insetShape)
                .background(if (isSelected) scheme.primaryContainer else scheme.surfaceContainerLow)
        ) {
            if (item.isDirectory) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = if (isSelected) scheme.onPrimaryContainer else scheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            } else {
                StorageThumbnail(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scheme.primary.copy(alpha = 0.16f))
                    )
                }
            }

            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = stringResource(if (isSelected) R.string.cd_selected else R.string.cd_not_selected),
                    tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .background(scheme.surface, CircleShape)
                )
            } else if (!galleryTile) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(48.dp)
                        .background(scheme.surface, CircleShape)
                ) {
                    Icon(Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more_options_for_file, item.name),
                        tint = scheme.onSurfaceVariant)
                }
            }
        }
        if (galleryTile) return@Column
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.formattedSize,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun LocationRow(
    title: String,
    meta: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppRadius.cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(AppRadius.insetShape)
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FilesEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.abhisheksati.mydoc.designsystem.components.PaperCompanion(icon)
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilesSelectionBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: (() -> Unit)?,
    onCompress: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onMergePdfs: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    var isMenuOpen by remember { mutableStateOf(false) }
    val hasSelection = selectedCount > 0

    Column(modifier.fillMaxWidth().background(scheme.surfaceContainer).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cd_close_selection))
            }
            Text(stringResource(R.string.files_selected_count, selectedCount),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (selectedCount < totalCount) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Outlined.SelectAll, contentDescription = stringResource(R.string.cd_select_all))
                }
            }
            Box {
                IconButton(onClick = { isMenuOpen = true }, enabled = hasSelection) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cd_more_actions))
                }
                AppDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                    if (onMergePdfs != null) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.library_merge_pdfs)) },
                            onClick = { isMenuOpen = false; onMergePdfs() })
                    }
                    if (onRename != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                isMenuOpen = false
                                onRename()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dialog_compress_zip)) },
                        leadingIcon = { Icon(Icons.Outlined.FolderZip, contentDescription = null) },
                        onClick = {
                            isMenuOpen = false
                            onCompress()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.photo_move_to_bin_title), color = scheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = scheme.error) },
                        onClick = {
                            isMenuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onCopy, enabled = hasSelection) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_copy))
            }
            TextButton(onClick = onCut, enabled = hasSelection) {
                Icon(Icons.Outlined.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_move))
            }
            TextButton(onClick = onShare, enabled = hasSelection) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_share))
            }
        }
    }
}

@Composable
fun BreadcrumbRow(
    crumbs: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        crumbs.forEachIndexed { index, (label, onClick) ->
            val isLast = index == crumbs.size - 1
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isLast) scheme.onSurface else scheme.onSurfaceVariant,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(AppRadius.microShape)
                    .clickable(onClick = onClick)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = scheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
