package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.outlined.Search
import com.abhisheksati.mydoc.designsystem.components.LocalFloatingChromeInset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import com.abhisheksati.mydoc.designsystem.components.BrowseSkeleton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.GlassMotionState
import com.abhisheksati.mydoc.designsystem.components.TrackGlassMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.domain.model.DocumentTypeFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageAnalysisUiState
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import com.abhisheksati.mydoc.viewmodel.documentTypeCounts
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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
                text = stringResource(R.string.files_item_meta, item.formattedSize, item.formattedLastModified),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onMoreClick, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.cd_details),
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
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    glassMotion: GlassMotionState? = null
) {
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    if (glassMotion != null) {
        TrackGlassMotion(listState, glassMotion)
    }
    if (storageUiState.isLoading) {
        Column(
            modifier = modifier.then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            BrowseSkeleton()
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier),
        contentPadding = PaddingValues(bottom = LocalFloatingChromeInset.current + 24.dp)
    ) {
        if (!storageUiState.errorMessage.isNullOrBlank()) {
            item(key = "storage_error_banner") {
                GlassSurface(
                    shape = AppRadius.cardShape,
                    backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = storageUiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item(key = "browse_search") {
            androidx.compose.material3.OutlinedButton(onClick = onSearch,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.search_files_label))
            }
        }
        if (pinnedFolders.isNotEmpty()) {
            item(key = "browse_pinned_header") {
                FilesSectionHeader(title = stringResource(R.string.section_pinned))
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

        item(key = "browse_storage") {
            StorageOverviewCard(
                usedRatio = storageUiState.usedRatio,
                formattedTotal = storageUiState.formattedTotal,
                formattedFree = storageUiState.formattedFree,
                reclaimableBytes = storageUiState.cleanSummary.totalReclaimableBytes,
                onBrowse = onBrowseInternal,
                onInsights = onInsights,
                onClean = if (storageUiState.cleanSummary.totalReclaimableBytes > 0) onClean else null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        val secondaryVolumes = volumes.filterNot { it.isPrimary }
        if (secondaryVolumes.isNotEmpty()) {
            item(key = "browse_secondary_volumes_header") {
                FilesSectionHeader(title = stringResource(R.string.section_external_storage))
            }
            items(
                items = secondaryVolumes,
                key = { "vol_${it.path}" }
            ) { volume ->
                LocationRow(
                    title = stringResource(volume.titleRes),
                    meta = "${volume.formattedFree} free of ${volume.formattedTotal}",
                    icon = Icons.Outlined.SdCard,
                    onClick = { onBrowseVolume(volume) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }



        item(key = "browse_categories_header") {
            FilesSectionHeader(title = stringResource(R.string.section_categories))
        }
        item(key = "browse_categories") {
            CategoryShortcutGrid(
                storageUiState = storageUiState,
                onCategory = onCategory,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "browse_trash_header") {
            FilesSectionHeader(title = stringResource(R.string.section_utilities))
        }
        item(key = "browse_apps") {
            LocationRow(
                title = stringResource(R.string.files_category_apps),
                meta = if (storageUiState.userAppCount > 0) {
                    "${storageUiState.userAppCount} installed · ${StorageVolume.formatBytes(storageUiState.userAppBytes)}"
                } else {
                    stringResource(R.string.apps_subtitle)
                },
                icon = Icons.Outlined.Storage,
                onClick = onApps,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
        item(key = "browse_trash") {
            LocationRow(
                title = stringResource(R.string.menu_recycle_bin),
                meta = if (trashCount > 0) stringResource(R.string.trash_meta_count, trashCount) else stringResource(R.string.trash_meta_empty),
                icon = Icons.Outlined.DeleteSweep,
                onClick = onOpenTrash,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            if (trashCount == 0) {
                Text(
                    text = stringResource(R.string.trash_note),
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
    val columns = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 1 else 3
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shortcutCategories.chunked(columns).forEach { row ->
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
                repeat(columns - row.size) {
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
                text = stringResource(R.string.scanning_storage),
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
                        text = stringResource(stat.type.titleRes),
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
                    color = scheme.tertiary,
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
    onDocFilter: (DocumentTypeFilter) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    glassMotion: GlassMotionState? = null
) {
    val scheme = MaterialTheme.colorScheme
    var sortMenuOpen by remember { mutableStateOf(false) }
    // Held by the view model, not here: this pane leaves composition whenever a document opens,
    // and a filter kept in remember came back as ALL after pressing back.
    val selectedDocFilter = uiState.docTypeFilter
    // Input changes create fresh scroll state; recreation restores the current source's position.
    val listState = rememberSaveable(
        uiState.currentDirectory.absolutePath, uiState.activeCategory, uiState.advancedSearch,
        saver = LazyListState.Saver
    ) { LazyListState() }
    val gridState = rememberSaveable(
        uiState.currentDirectory.absolutePath, uiState.activeCategory, uiState.advancedSearch,
        saver = LazyGridState.Saver
    ) { LazyGridState() }
    if (glassMotion != null) {
        if (uiState.isGridView) TrackGlassMotion(gridState, glassMotion)
        else TrackGlassMotion(listState, glassMotion)
    }
    val hazeModifier = if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier

    val isDocumentsCategory = uiState.activeCategory == FileCategory.DOCUMENTS

    // The ViewModel applies the document-type chip itself now, so the rows drawn here are exactly
    // the rows Select All selects and the count above the list reports.
    val displayedItems = uiState.filteredItems
    val docCounts = remember(uiState.items, uiState.searchQuery) {
        documentTypeCounts(uiState.items, uiState.searchQuery)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 0.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { sortMenuOpen = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = stringResource(R.string.cd_sort), tint = scheme.onSurfaceVariant)
            }
            IconButton(
                onClick = onToggleView,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = stringResource(if (uiState.isGridView) R.string.menu_list_view else R.string.menu_grid_view),
                    tint = scheme.onSurfaceVariant
                )
            }
            Box {
                AppDropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
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

        if (isDocumentsCategory) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocumentTypeFilter.entries.forEach { filter ->
                    val count = docCounts[filter] ?: 0
                    FilterChip(
                        selected = selectedDocFilter == filter,
                        onClick = { onDocFilter(filter) },
                        label = {
                            Text(
                                text = if (count > 0 && filter != DocumentTypeFilter.ALL) "${filter.label} ($count)" else filter.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.primaryContainer,
                            selectedLabelColor = scheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Reading these two fields outside the item lambdas keeps a row's recomposition
        // scoped to selection changes instead of every uiState change.
        val inSelectionMode = uiState.isSelectionMode
        val selectedFiles = uiState.selectedFiles

        if (displayedItems.isEmpty()) {
            FilesEmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = if (uiState.searchQuery.isNotEmpty()) {
                    stringResource(R.string.empty_no_matches)
                } else if (isDocumentsCategory && selectedDocFilter != DocumentTypeFilter.ALL) {
                    stringResource(R.string.empty_no_documents, selectedDocFilter.label)
                } else {
                    stringResource(R.string.empty_folder)
                },
                message = if (uiState.searchQuery.isNotEmpty()) {
                    stringResource(R.string.empty_try_search)
                } else if (isDocumentsCategory && selectedDocFilter != DocumentTypeFilter.ALL) {
                    stringResource(R.string.empty_no_files_in_documents, selectedDocFilter.label)
                } else {
                    stringResource(R.string.empty_create_hint)
                }
            )
        } else if (uiState.isGridView) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(start = 10.dp, top = 4.dp, end = 10.dp, bottom = LocalFloatingChromeInset.current + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(hazeModifier)
            ) {
                items(displayedItems, key = { it.path }) { item ->
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
                state = listState,
                contentPadding = PaddingValues(top = 4.dp, bottom = LocalFloatingChromeInset.current + 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(hazeModifier)
            ) {
                items(displayedItems, key = { it.path }) { item ->
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

@Composable
private fun sortLabel(field: FileSortField): String = when (field) {
    FileSortField.NAME -> stringResource(R.string.sort_name)
    FileSortField.DATE -> stringResource(R.string.sort_date_modified)
    FileSortField.SIZE -> stringResource(R.string.sort_size)
    FileSortField.TYPE -> stringResource(R.string.sort_type)
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
                text = stringResource(R.string.storage_insights),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (storageUiState.formattedTotal.isNotEmpty()) {
                    "${storageUiState.formattedUsed} used of ${storageUiState.formattedTotal} · ${storageUiState.formattedFree} free"
                } else {
                    stringResource(R.string.storage_calculating)
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
                color = scheme.tertiary,
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
                    Text(stringResource(R.string.storage_free_up, StorageVolume.formatBytes(reclaimable)))
                }
            }
        }
    }
}
