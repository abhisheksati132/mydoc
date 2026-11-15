package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.outlined.Search
import com.abhisheksati.mydoc.designsystem.components.LocalFloatingChromeInset
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.abhisheksati.mydoc.designsystem.components.SkeletonBlock
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.GlassMotionState
import com.abhisheksati.mydoc.designsystem.components.TrackGlassMotion
import com.abhisheksati.mydoc.designsystem.components.tonalSheen
import com.abhisheksati.mydoc.designsystem.animation.StaggeredReveal
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
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
        FileOverflowButton(
            contentDescription = stringResource(R.string.cd_details),
            onClick = onMoreClick
        )
    }
}

@Composable
fun BrowseHomePane(
    storageUiState: StorageAnalysisUiState,
    volumes: List<StorageVolume>,
    pinnedFolders: List<File>,
    onBrowseInternal: () -> Unit,
    onBrowseVolume: (StorageVolume) -> Unit,
    onCategory: (FileCategory) -> Unit,
    onFolder: (File) -> Unit,
    onOpenTrash: () -> Unit,
    onApps: () -> Unit,
    onInsights: () -> Unit,
    onClean: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    glassMotion: GlassMotionState? = null
) {
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    if (glassMotion != null) {
        TrackGlassMotion(listState, glassMotion)
    }
    if (storageUiState.isLoading && (storageUiState.analyzedVolumePath == null ||
        storageUiState.analyzedVolumePath != storageUiState.activeVolume?.path)) {
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
        item(key = "storage_refresh_progress") {
            Box(Modifier.fillMaxWidth().height(4.dp)) {
                if (storageUiState.isLoading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
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
                formattedUsed = storageUiState.formattedUsed,
                formattedTotal = storageUiState.formattedTotal,
                formattedFree = storageUiState.formattedFree,
                usedBytes = storageUiState.usedBytes,
                totalBytes = storageUiState.totalBytes,
                categories = storageUiState.categories,
                onOpenTrash = onOpenTrash,
                onBrowse = onBrowseInternal,
                onInsights = onInsights,
                onClean = onClean,
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
                onCategory = { if (it == FileCategory.APKS) onApps() else onCategory(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

    }
}

@Composable
private fun CategoryShortcutGrid(
    storageUiState: StorageAnalysisUiState,
    onCategory: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 1 else 2
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        shortcutCategories.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEachIndexed { columnIndex, type ->
                    val statType = if (type == StorageCategoryType.APKS) StorageCategoryType.APPS else type
                    val stat = storageUiState.categories.firstOrNull { it.type == statType }
                    StaggeredReveal(
                        index = rowIndex * columns + columnIndex,
                        modifier = Modifier.weight(1f)
                    ) {
                        CategoryTile(
                            type = type,
                            stat = stat,
                            onClick = { type.fileCategory?.let(onCategory) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StorageBreakdown(storageUiState: StorageAnalysisUiState) {
    val colors = AppTheme.colors
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(colors.accent, colors.accentAmber, colors.accentCyan, colors.accentGreen, colors.accentPink)
    // Empty categories are rows of "0%" noise: the list is a breakdown of what *is*
    // holding the disk, so only categories with bytes are read out.
    val entries = storageUiState.categories
        .filter { it.type != StorageCategoryType.DOWNLOADS && it.type != StorageCategoryType.LARGE_FILES && it.bytes > 0L }
        .sortedByDescending { it.bytes }
    val total = storageUiState.totalBytes
    val free = storageUiState.freeBytes
    val freeColor = colors.divider
    val categoryBytes = entries.sumOf { it.bytes }
    val used = (total - free).coerceIn(0, total.coerceAtLeast(0))
    val unclassified = (used - categoryBytes).coerceAtLeast(0)
    val categoriesFit = categoryBytes <= used
    val sweepProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (total > 0) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "storageSweep"
    )
    val usedPercent = if (total > 0) (used * 100 / total).toInt().coerceIn(0, 100) else 0
    val shape = AppRadius.xLargeShape
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (total > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surface, shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.accent.copy(alpha = 0.15f), colors.accent.copy(alpha = 0.02f))
                        ),
                        shape
                    )
                    .tonalSheen()
                    .border(1.dp, colors.accent.copy(alpha = 0.18f), shape)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(Modifier.size(142.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(Modifier.size(142.dp)) {
                            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 16.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            val inset = stroke.width / 2
                            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                            val bounds = androidx.compose.ui.geometry.Size(size.width - stroke.width, size.height - stroke.width)
                            drawArc(freeColor.copy(alpha = 0.45f), 0f, 360f, false, topLeft, bounds, style = stroke)
                            var start = -90f
                            if (!categoriesFit || entries.isEmpty()) {
                                drawArc(colors.accent, start, (used.toDouble() / total * 360).toFloat() * sweepProgress, false, topLeft, bounds, style = stroke)
                            } else {
                                val gap = if (entries.size > 1) 2.5f else 0f
                                entries.forEachIndexed { index, stat ->
                                    val full = (stat.bytes.toDouble() / total * 360).toFloat()
                                    val sweep = full * sweepProgress
                                    if (sweep > gap) drawArc(palette[index % palette.size], start, sweep - gap, false, topLeft, bounds, style = stroke)
                                    start += full
                                }
                                if (unclassified > 0) {
                                    val sweep = (unclassified.toDouble() / total * 360).toFloat() * sweepProgress
                                    if (sweep > gap) drawArc(colors.inkMuted, start, sweep - gap, false, topLeft, bounds, style = stroke)
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$usedPercent%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface
                            )
                            Text(stringResource(R.string.storage_chart_used), style = AppTheme.typography.caption, color = scheme.onSurfaceVariant)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.storage_chart_used).uppercase(),
                            style = AppTheme.typography.overline,
                            color = scheme.onSurfaceVariant
                        )
                        Text(
                            text = storageUiState.formattedUsed,
                            style = AppTheme.typography.title.copy(
                                fontFeatureSettings = "tnum",
                                letterSpacing = (-0.6).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.storage_of_total, storageUiState.formattedTotal),
                            style = AppTheme.typography.caption,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(freeColor))
                            Text(
                                text = stringResource(R.string.storage_free_line, storageUiState.formattedFree),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (!categoriesFit) {
                    Text(stringResource(R.string.storage_chart_overlap), style = AppTheme.typography.caption, color = colors.inkMuted)
                }
            }
        }
        androidx.compose.material3.Surface(shape = AppRadius.xLargeShape, color = scheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                entries.forEachIndexed { index, stat ->
                    val pct = if (total > 0) (stat.bytes.toDouble() / total * 100).toInt() else 0
                    StaggeredReveal(index = index, modifier = Modifier.fillMaxWidth()) {
                        StorageLegendRow(
                            label = stringResource(stat.type.titleRes),
                            value = stat.formattedSize,
                            percent = pct,
                            color = if (categoriesFit) palette[index % palette.size] else colors.inkMuted,
                            lowShare = pct == 0 && stat.bytes > 0L,
                            icon = stat.type.icon,
                            // System & Other is an estimate with a placeholder count, so it gets
                            // no file line; app entries count packages, not files.
                            meta = when {
                                stat.type == StorageCategoryType.SYSTEM_OTHER || stat.fileCount <= 0 -> null
                                stat.type == StorageCategoryType.APPS ->
                                    pluralStringResource(R.plurals.apps_count, stat.fileCount, stat.fileCount)
                                else -> pluralStringResource(R.plurals.files_count, stat.fileCount, stat.fileCount)
                            }
                        )
                    }
                }
                if (total > 0 && categoriesFit && unclassified > 0) {
                    val pct = (unclassified.toDouble() / total * 100).toInt()
                    StaggeredReveal(index = entries.size, modifier = Modifier.fillMaxWidth()) {
                        StorageLegendRow(stringResource(R.string.storage_chart_unclassified), StorageVolume.formatBytes(unclassified),
                            pct, colors.inkMuted, lowShare = pct == 0)
                    }
                }
                if (total > 0) {
                    StaggeredReveal(
                        index = entries.size + if (categoriesFit && unclassified > 0) 1 else 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StorageLegendRow(stringResource(R.string.storage_chart_free), storageUiState.formattedFree,
                            (free.toDouble() / total * 100).toInt(), freeColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageLegendRow(
    label: String,
    value: String,
    percent: Int,
    color: androidx.compose.ui.graphics.Color,
    lowShare: Boolean = false,
    icon: ImageVector? = null,
    meta: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val animatedShare by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "legendShare"
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.14f), AppRadius.insetShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
                }
            } else {
                // Same 36dp slot as the icon wells so every label starts on one axis.
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    label,
                    style = AppTheme.typography.body,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta != null) {
                    Text(
                        meta,
                        style = AppTheme.typography.caption,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(value, style = AppTheme.typography.bodyStrong, color = scheme.onSurface)
                Text(
                    text = if (lowShare) "<1%" else "$percent%",
                    style = AppTheme.typography.caption,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(AppRadius.roundShape)
                .background(scheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedShare)
                    .height(4.dp)
                    .clip(AppRadius.roundShape)
                    .background(color.copy(alpha = 0.85f))
            )
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
    glassMotion: GlassMotionState? = null,
    onClearFilters: (() -> Unit)? = null,
    onImageFolder: (String?) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    var sortMenuOpen by remember { mutableStateOf(false) }
    // Held by the view model, not here: this pane leaves composition whenever a document opens,
    // and a filter kept in remember came back as ALL after pressing back.
    val selectedDocFilter = uiState.docTypeFilter
    // Input changes create fresh scroll state; recreation restores the current source's position.
    val listState = rememberSaveable(
        uiState.currentDirectory.absolutePath, uiState.activeCategory, uiState.advancedSearch, uiState.imageFolder,
        saver = LazyListState.Saver
    ) { LazyListState() }
    val gridState = rememberSaveable(
        uiState.currentDirectory.absolutePath, uiState.activeCategory, uiState.advancedSearch, uiState.imageFolder,
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
    val isImagesCategory = uiState.activeCategory == FileCategory.IMAGES
    val imageFolder = uiState.imageFolder
    val imageFolders = remember(isImagesCategory, uiState.items) {
        if (isImagesCategory) uiState.items.mapNotNull { it.file.parent }.distinct().sorted()
        else emptyList()
    }
    val displayedItems = uiState.filteredItems
    val docCounts = remember(uiState.items, uiState.searchQuery) {
        documentTypeCounts(uiState.items, uiState.searchQuery)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sortDescription = stringResource(R.string.cd_sort)
            val directionDescription = stringResource(if (uiState.sortOrder.isAscending) R.string.sort_ascending else R.string.sort_descending)
            Box(Modifier.weight(1f)) {
                TextButton(
                    onClick = { sortMenuOpen = true },
                    modifier = Modifier.semantics {
                        contentDescription = sortDescription
                        stateDescription = directionDescription
                    }
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(sortLabel(uiState.sortOrder.field), modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (uiState.sortOrder.isAscending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        contentDescription = null, modifier = Modifier.size(16.dp)
                    )
                }
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
            IconButton(onClick = onToggleView, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (uiState.isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = stringResource(if (uiState.isGridView) R.string.menu_list_view else R.string.menu_grid_view),
                    tint = scheme.onSurfaceVariant
                )
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

        if (isImagesCategory && imageFolders.size > 1 && !uiState.isSelectionMode) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = imageFolder == null, onClick = { onImageFolder(null) },
                    label = { Text(stringResource(R.string.gallery_all_images)) })
                imageFolders.forEach { folder ->
                    FilterChip(selected = imageFolder == folder, onClick = { onImageFolder(folder) },
                        label = { Text(java.io.File(folder).name) })
                }
            }
        }

        // Reading these two fields outside the item lambdas keeps a row's recomposition
        // scoped to selection changes instead of every uiState change.
        val inSelectionMode = uiState.isSelectionMode
        val selectedFiles = uiState.selectedFiles

        val hasFilters = imageFolder != null || uiState.searchQuery.isNotBlank() || uiState.advancedSearch != null ||
            (isDocumentsCategory && selectedDocFilter != DocumentTypeFilter.ALL)
        if (uiState.isLoading && (!uiState.isRefreshing || displayedItems.isEmpty())) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.files_loading), style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                repeat(4) { SkeletonBlock(Modifier.fillMaxWidth().height(72.dp)) }
            }
        } else if (displayedItems.isEmpty()) {
            FilesEmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = if (uiState.searchQuery.isNotBlank() || uiState.advancedSearch != null) {
                    stringResource(R.string.empty_no_matches)
                } else if (isDocumentsCategory && selectedDocFilter != DocumentTypeFilter.ALL) {
                    stringResource(R.string.empty_no_documents, selectedDocFilter.label)
                } else {
                    stringResource(R.string.empty_folder)
                },
                message = if (uiState.searchQuery.isNotBlank() || uiState.advancedSearch != null) {
                    stringResource(R.string.empty_try_search)
                } else if (isDocumentsCategory && selectedDocFilter != DocumentTypeFilter.ALL) {
                    stringResource(R.string.empty_no_files_in_documents, selectedDocFilter.label)
                } else {
                    stringResource(R.string.empty_create_hint)
                },
                actionLabel = if (hasFilters && onClearFilters != null) stringResource(R.string.search_clear_filters) else null,
                onAction = if (hasFilters) onClearFilters else null
            )
        } else if (uiState.isGridView) {
            // The photo gallery is a wall of images: three fixed columns, no gaps, no
            // metadata, so each picture gets the whole cell instead of a padded card.
            val gallery = isImagesCategory
            LazyVerticalGrid(
                state = gridState,
                columns = if (gallery) {
                    GridCells.Fixed(3)
                } else {
                    GridCells.Adaptive(minSize = 108.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
                },
                contentPadding = if (gallery) {
                    PaddingValues(bottom = LocalFloatingChromeInset.current + 24.dp)
                } else {
                    PaddingValues(start = 10.dp, top = 4.dp, end = 10.dp, bottom = LocalFloatingChromeInset.current + 24.dp)
                },
                horizontalArrangement = Arrangement.spacedBy(if (gallery) 0.dp else 4.dp),
                verticalArrangement = Arrangement.spacedBy(if (gallery) 0.dp else 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(hazeModifier)
            ) {
                items(displayedItems, key = { it.path }) { item ->
                    FileGridCell(
                        item = item,
                        isSelected = selectedFiles.contains(item.file),
                        isSelectionMode = inSelectionMode,
                        onClick = {
                            if (inSelectionMode) onToggleSelection(item) else onOpenItem(item)
                        },
                        onLongClick = { onToggleSelection(item) },
                        onMoreClick = { onMoreItem(item) },
                        galleryTile = gallery,
                        showMoreButton = !isDocumentsCategory,
                        generateOfficePreview = isDocumentsCategory
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
                        onMoreClick = { onMoreItem(item) },
                        showFileSize = !isImagesCategory || inSelectionMode
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
fun StorageInsightsScreen(
    storageUiState: StorageAnalysisUiState,
    onDismiss: () -> Unit,
    onClean: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val colors = AppTheme.colors
    androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            com.abhisheksati.mydoc.designsystem.components.AppTopBar(
                title = stringResource(R.string.storage_insights),
                subtitle = stringResource(R.string.storage_insights_subtitle),
                onBack = onDismiss,
                contentInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                isGlass = false,
                containerColor = scheme.background
            )
            Spacer(Modifier.height(8.dp))
            if (storageUiState.totalBytes > 0) {
                StorageBreakdown(storageUiState = storageUiState)
            } else {
                Text(stringResource(R.string.storage_calculating), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.storage_measurement_note),
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            val reclaimable = storageUiState.cleanSummary.totalReclaimableBytes
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onClean,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = AppRadius.cardShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                )
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (reclaimable > 0) stringResource(R.string.storage_free_up, StorageVolume.formatBytes(reclaimable))
                    else stringResource(R.string.settings_cleaner_title),
                    style = AppTheme.typography.bodyStrong
                )
            }
            if (reclaimable > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(colors.accentGreen))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.storage_insights_clean_hint),
                        style = AppTheme.typography.caption,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
