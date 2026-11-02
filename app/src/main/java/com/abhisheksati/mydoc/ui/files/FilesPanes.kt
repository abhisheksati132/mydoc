package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageAnalysisUiState
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import java.io.File

private val shortcutCategories = listOf(
    StorageCategoryType.DOCUMENTS,
    StorageCategoryType.DOWNLOADS,
    StorageCategoryType.IMAGES,
    StorageCategoryType.VIDEOS,
    StorageCategoryType.AUDIO,
    StorageCategoryType.APKS
)

@Composable
fun RecentsPane(
    recentItems: List<LibraryItem>,
    onOpenItem: (LibraryItem) -> Unit,
    onItemDetails: (LibraryItem) -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
    ) {
        item(key = "recent_header") {
            FilesSectionHeader(
                title = "Recent",
                meta = if (recentItems.isNotEmpty()) "${recentItems.size}" else null
            )
        }

        if (recentItems.isEmpty()) {
            item(key = "recent_empty") {
                FilesEmptyState(
                    icon = Icons.Outlined.History,
                    title = "No recent files",
                    message = "Documents, photos and media you open will appear here.",
                    actionLabel = "Browse storage",
                    onAction = onBrowse
                )
            }
        } else {
            items(
                items = recentItems,
                key = { it.uri }
            ) { item ->
                RecentFileRow(
                    item = item,
                    onClick = { onOpenItem(item) },
                    onMoreClick = { onItemDetails(item) }
                )
            }
        }
    }
}

@Composable
fun RecentFileRow(
    item: LibraryItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(AppRadius.cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LibraryThumbnail(
            item = item,
            modifier = Modifier.size(width = 44.dp, height = 52.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${item.formattedSize} · ${item.formattedLastModified}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Details",
                tint = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BrowseHomePane(
    storageUiState: StorageAnalysisUiState,
    volumes: List<StorageVolume>,
    pinnedFolders: List<File>,
    trashCount: Int,
    onBrowseInternal: () -> Unit,
    onBrowseVolume: (StorageVolume) -> Unit,
    onCategory: (FileCategory) -> Unit,
    onFolder: (File) -> Unit,
    onOpenTrash: () -> Unit,
    onApps: () -> Unit,
    onInsights: () -> Unit,
    onClean: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "browse_storage") {
            StorageOverviewCard(
                usedRatio = storageUiState.usedRatio,
                formattedUsed = storageUiState.formattedUsed,
                formattedTotal = storageUiState.formattedTotal,
                formattedFree = storageUiState.formattedFree,
                usedPercentage = storageUiState.usedPercentage,
                reclaimableBytes = storageUiState.cleanSummary.totalReclaimableBytes,
                onBrowse = onBrowseInternal,
                onInsights = onInsights,
                onClean = if (storageUiState.cleanSummary.totalReclaimableBytes > 0) onClean else null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item(key = "browse_categories_header") {
            FilesSectionHeader(title = "Browse")
        }
        item(key = "browse_categories") {
            CategoryShortcutGrid(
                storageUiState = storageUiState,
                onCategory = onCategory,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "browse_insights_header") {
            FilesSectionHeader(title = "Storage by type")
        }
        item(key = "browse_insights") {
            StorageBreakdown(storageUiState = storageUiState)
        }

        item(key = "browse_locations_header") {
            FilesSectionHeader(title = "Locations")
        }
        item(key = "browse_internal") {
            LocationRow(
                title = "Internal storage",
                meta = if (storageUiState.formattedTotal.isNotEmpty()) {
                    "${storageUiState.formattedFree} free of ${storageUiState.formattedTotal}"
                } else {
                    "Open device storage"
                },
                icon = Icons.Outlined.Storage,
                onClick = onBrowseInternal,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
        items(
            items = volumes.filterNot { it.isPrimary },
            key = { "vol_${it.path}" }
        ) { volume ->
            LocationRow(
                title = volume.title,
                meta = "${volume.formattedFree} free of ${volume.formattedTotal}",
                icon = Icons.Outlined.SdCard,
                onClick = { onBrowseVolume(volume) },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }

        if (pinnedFolders.isNotEmpty()) {
            item(key = "browse_pinned_header") {
                FilesSectionHeader(title = "Pinned")
            }
            items(pinnedFolders, key = { "pin_${it.absolutePath}" }) { folder ->
                LocationRow(
                    title = folder.name,
                    meta = folder.absolutePath,
                    icon = Icons.Outlined.Folder,
                    onClick = { onFolder(folder) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        item(key = "browse_trash_header") {
            FilesSectionHeader(title = "Utilities")
        }
        item(key = "browse_apps") {
            LocationRow(
                title = "Apps",
                meta = if (storageUiState.userAppCount > 0) {
                    "${storageUiState.userAppCount} installed · ${StorageVolume.formatBytes(storageUiState.userAppBytes)}"
                } else {
                    "Installed apps and data"
                },
                icon = Icons.Outlined.Storage,
                onClick = onApps,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
        item(key = "browse_trash") {
            LocationRow(
                title = "Recycle Bin",
                meta = if (trashCount > 0) "$trashCount items kept for 30 days" else "Empty",
                icon = Icons.Outlined.DeleteSweep,
                onClick = onOpenTrash,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            if (trashCount == 0) {
                Text(
                    text = "Deleted files stay here for 30 days before they are removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryShortcutGrid(
    storageUiState: StorageAnalysisUiState,
    onCategory: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shortcutCategories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { type ->
                    val stat = storageUiState.categories.firstOrNull { it.type == type }
                    CategoryTile(
                        type = type,
                        stat = stat,
                        onClick = { type.fileCategory?.let(onCategory) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StorageBreakdown(
    storageUiState: StorageAnalysisUiState,
    limit: Int = 4
) {
    val scheme = MaterialTheme.colorScheme
    val entries = storageUiState.categories
        .filter { it.type != StorageCategoryType.APPS && it.type != StorageCategoryType.SYSTEM_OTHER }
        .sortedByDescending { it.bytes }
        .take(limit)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (entries.isEmpty()) {
            Text(
                text = "Scanning storage…",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        entries.forEach { stat ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = stat.type.icon,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stat.type.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stat.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { stat.percentageOfUsed.coerceIn(0f, 1f).coerceAtLeast(0.02f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = scheme.primary,
                    trackColor = scheme.surfaceVariant,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@Composable
fun DirectoryPane(
    uiState: FileManagerUiState,
    onOpenItem: (StorageItem) -> Unit,
    onMoreItem: (StorageItem) -> Unit,
    onToggleSelection: (StorageItem) -> Unit,
    onToggleView: () -> Unit,
    onSort: (FileSortField) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var sortMenuOpen by remember { mutableStateOf(false) }
    val items = uiState.filteredItems

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 0.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { sortMenuOpen = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort", tint = scheme.onSurfaceVariant)
            }
            IconButton(
                onClick = onToggleView,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = if (uiState.isGridView) "List view" else "Grid view",
                    tint = scheme.onSurfaceVariant
                )
            }
            Box {
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    FileSortField.entries.forEach { field ->
                        val active = uiState.sortOrder.field == field
                        DropdownMenuItem(
                            text = { Text(sortLabel(field)) },
                            trailingIcon = if (active) {
                                {
                                    Icon(
                                        imageVector = if (uiState.sortOrder.isAscending) {
                                            Icons.Outlined.ArrowUpward
                                        } else {
                                            Icons.Outlined.ArrowDownward
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                sortMenuOpen = false
                                onSort(field)
                            }
                        )
                    }
                }
            }
        }

        // Reading these two fields outside the item lambdas keeps a row's recomposition
        // scoped to selection changes instead of every uiState change.
        val inSelectionMode = uiState.isSelectionMode
        val selectedFiles = uiState.selectedFiles

        if (items.isEmpty()) {
            FilesEmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = if (uiState.searchQuery.isNotEmpty()) "No matches" else "This folder is empty",
                message = if (uiState.searchQuery.isNotEmpty()) {
                    "Try a different search term."
                } else {
                    "Create a folder or add files using the button below."
                }
            )
        } else if (uiState.isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(start = 10.dp, top = 4.dp, end = 10.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items, key = { it.path }) { item ->
                    val selected = selectedFiles.contains(item.file)
                    FileGridCell(
                        item = item,
                        isSelected = selected,
                        isSelectionMode = inSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) onToggleSelection(item) else onOpenItem(item)
                        },
                        onLongClick = { onToggleSelection(item) },
                        onMoreClick = { onMoreItem(item) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items, key = { it.path }) { item ->
                    val selected = selectedFiles.contains(item.file)
                    FileRow(
                        item = item,
                        isSelected = selected,
                        isSelectionMode = inSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) onToggleSelection(item) else onOpenItem(item)
                        },
                        onLongClick = { onToggleSelection(item) },
                        onMoreClick = { onMoreItem(item) }
                    )
                }
            }
        }
    }
}

private fun sortLabel(field: FileSortField): String = when (field) {
    FileSortField.NAME -> "Name"
    FileSortField.DATE -> "Date modified"
    FileSortField.SIZE -> "Size"
    FileSortField.TYPE -> "Type"
}


/**
 * The full storage readout: every category against the used space, plus the
 * reclaimable total. Opened from the storage card and the Browse hub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageInsightsSheet(
    storageUiState: StorageAnalysisUiState,
    onDismiss: () -> Unit,
    onClean: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
        ) {
            Text(
                text = "Storage insights",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (storageUiState.formattedTotal.isNotEmpty()) {
                    "${storageUiState.formattedUsed} used of ${storageUiState.formattedTotal} · ${storageUiState.formattedFree} free"
                } else {
                    "Calculating storage…"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { storageUiState.usedRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = scheme.primary,
                trackColor = scheme.surfaceVariant,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(24.dp))
            StorageBreakdown(storageUiState = storageUiState, limit = 10)
            val reclaimable = storageUiState.cleanSummary.totalReclaimableBytes
            if (reclaimable > 0) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onClean,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Free up ${StorageVolume.formatBytes(reclaimable)}")
                }
            }
        }
    }
}
