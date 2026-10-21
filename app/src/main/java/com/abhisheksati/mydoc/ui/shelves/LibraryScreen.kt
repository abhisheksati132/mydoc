package com.abhisheksati.mydoc.ui.shelves

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.designsystem.components.AppFileRow
import com.abhisheksati.mydoc.designsystem.components.QuickLookModal
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.Shelf
import java.util.Calendar

enum class LibraryTypeFilter(val label: String) {
    EVERYTHING("Everything"),
    DOCS("Docs"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    AUDIO("Audio")
}

private enum class DateGroup(val title: String) {
    TODAY("TODAY"),
    YESTERDAY("YESTERDAY"),
    THIS_MONTH("THIS MONTH"),
    EARLIER("EARLIER")
}

private fun groupDocumentsByDate(documents: List<DocumentFile>): Map<DateGroup, List<DocumentFile>> {
    val now = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
    val thisMonthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return documents.groupBy { doc ->
        val ts = doc.lastOpenedTimestamp
        when {
            ts >= todayStart -> DateGroup.TODAY
            ts >= yesterdayStart -> DateGroup.YESTERDAY
            ts >= thisMonthStart -> DateGroup.THIS_MONTH
            else -> DateGroup.EARLIER
        }
    }
}

/**
 * "LIBRARY" Destination Screen ("Shelves" Design System).
 *
 * Rules:
 * - One unified file list replacing Files, Docs and Media.
 * - Title "Library" + overflow menu (Settings, Storage Analyzer, Recycle Bin, Scan folder, Sort).
 * - Pill search field ("Search N files").
 * - Shelf chips row: All (ink), each shelf in its colour, Unfiled (outlined).
 * - Type filter row with underline on the active item: Everything, Docs, Photos, Videos, Audio.
 *   Photos/Videos switch to a 3-column square grid.
 * - Grouped by Today / Yesterday / This month / Earlier.
 * - Row = 44dp tinted extension square, name (1 line), "Shelf · size · time".
 */
@Composable
fun LibraryScreen(
    documents: List<DocumentFile>,
    shelves: List<Shelf>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedShelfId: Long?,            // null = All, -1 = Unfiled, or shelf.id
    onSelectShelfId: (Long?) -> Unit,
    selectedTypeFilter: LibraryTypeFilter,
    onSelectTypeFilter: (LibraryTypeFilter) -> Unit,
    sortOrder: DocumentSortOrder,
    onSetSortOrder: (DocumentSortOrder) -> Unit,
    onDocumentClick: (DocumentFile) -> Unit,
    onToggleFavorite: (DocumentFile) -> Unit,
    onDeleteDocument: (DocumentFile) -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    onSettingsClick: () -> Unit,
    onStorageAnalyzerClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onScanFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    var isOverflowOpen by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var quickLookDoc by remember { mutableStateOf<DocumentFile?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. Top Bar: Title "Library" + Overflow Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.screenPadding,
                    end = 8.dp,
                    top = 16.dp,
                    bottom = 8.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = typography.display,
                color = colors.ink
            )

            Box {
                IconButton(
                    onClick = {
                        haptics.select()
                        isOverflowOpen = true
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options menu",
                        tint = colors.ink
                    )
                }

                DropdownMenu(
                    expanded = isOverflowOpen,
                    onDismissRequest = { isOverflowOpen = false },
                    shape = RoundedCornerShape(AppSpacing.cardRadius),
                    containerColor = colors.surface,
                    border = BorderStroke(AppSpacing.hairline, colors.line)
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort documents...", style = typography.body) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, tint = colors.accent) },
                        onClick = {
                            isOverflowOpen = false
                            isSortMenuOpen = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Scan folder for notes...", style = typography.body) },
                        leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = colors.ink) },
                        onClick = {
                            isOverflowOpen = false
                            onScanFolderClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Storage Analyzer", style = typography.body) },
                        leadingIcon = { Icon(Icons.Outlined.Storage, contentDescription = null, tint = colors.ink) },
                        onClick = {
                            isOverflowOpen = false
                            onStorageAnalyzerClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Recycle Bin", style = typography.body) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.ink) },
                        onClick = {
                            isOverflowOpen = false
                            onRecycleBinClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", style = typography.body) },
                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = colors.ink) },
                        onClick = {
                            isOverflowOpen = false
                            onSettingsClick()
                        }
                    )
                }

                // Sort Order Submenu
                DropdownMenu(
                    expanded = isSortMenuOpen,
                    onDismissRequest = { isSortMenuOpen = false },
                    shape = RoundedCornerShape(AppSpacing.cardRadius),
                    containerColor = colors.surface,
                    border = BorderStroke(AppSpacing.hairline, colors.line)
                ) {
                    DocumentSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.displayName, style = typography.body) },
                            trailingIcon = {
                                if (sortOrder == order) {
                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                                }
                            },
                            onClick = {
                                haptics.select()
                                onSetSortOrder(order)
                                isSortMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // 2. Pill Search Field ("Search N files")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding, vertical = 6.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(AppSpacing.inputRadius))
                .background(colors.surfaceMuted)
                .border(AppSpacing.hairline, colors.line, RoundedCornerShape(AppSpacing.inputRadius))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.inkMuted,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search ${documents.size} files...",
                            style = typography.body.withTabularFigures(),
                            color = colors.inkMuted
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = typography.body.copy(color = colors.ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear search",
                            tint = colors.inkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Shelf Chips Row (Horizontally scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screenPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "All" chip
            val isAll = selectedShelfId == null
            Surface(
                shape = RoundedCornerShape(AppSpacing.chipRadius),
                color = if (isAll) colors.ink else colors.surfaceMuted,
                border = if (!isAll) BorderStroke(AppSpacing.hairline, colors.line) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(AppSpacing.chipRadius))
                    .clickable {
                        haptics.select()
                        onSelectShelfId(null)
                    }
            ) {
                Text(
                    text = "All",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isAll) colors.background else colors.ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }

            // Each shelf in its flat colour
            for (shelf in shelves) {
                val isSelected = selectedShelfId == shelf.id
                val shelfColor = colors.getShelfColor(shelf.colorIndex)
                Surface(
                    shape = RoundedCornerShape(AppSpacing.chipRadius),
                    color = if (isSelected) shelfColor.color else shelfColor.color.copy(alpha = 0.20f),
                    border = if (isSelected) null else BorderStroke(AppSpacing.hairline, shelfColor.color.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppSpacing.chipRadius))
                        .clickable {
                            haptics.select()
                            onSelectShelfId(if (isSelected) null else shelf.id)
                        }
                ) {
                    Text(
                        text = shelf.name,
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) shelfColor.onColor else colors.ink,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            // "Unfiled" chip
            val isUnfiled = selectedShelfId == -1L
            Surface(
                shape = RoundedCornerShape(AppSpacing.chipRadius),
                color = if (isUnfiled) colors.ink else colors.surfaceMuted,
                border = BorderStroke(AppSpacing.hairline, colors.line),
                modifier = Modifier
                    .clip(RoundedCornerShape(AppSpacing.chipRadius))
                    .clickable {
                        haptics.select()
                        onSelectShelfId(if (isUnfiled) null else -1L)
                    }
            ) {
                Text(
                    text = "Unfiled",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isUnfiled) colors.background else colors.inkMuted,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // 4. Type Filter Row with underline on active item
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (filter in LibraryTypeFilter.entries) {
                val isActive = selectedTypeFilter == filter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptics.select()
                            onSelectTypeFilter(filter)
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter.label,
                        style = typography.body.copy(
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isActive) colors.ink else colors.inkMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(28.dp)
                            .background(if (isActive) colors.accent else Color.Transparent)
                    )
                }
            }
        }

        // Hairline separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.hairline)
                .background(colors.line)
        )

        // 5. Content: 3-column square grid (Photos/Videos) OR Date-grouped list
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = AppSpacing.navBarHeight + 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No files match your filter.",
                    style = typography.body,
                    color = colors.inkMuted
                )
            }
        } else if (selectedTypeFilter == LibraryTypeFilter.PHOTOS || selectedTypeFilter == LibraryTypeFilter.VIDEOS) {
            // Consistent 3-column square grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 4.dp,
                    end = 4.dp,
                    top = 8.dp,
                    bottom = AppSpacing.navBarHeight + 36.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = documents,
                    key = { "grid_${it.uri}" }
                ) { doc ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceMuted)
                            .clickable {
                                haptics.select()
                                onDocumentClick(doc)
                            }
                    ) {
                        AsyncImage(
                            model = doc.uri,
                            contentDescription = doc.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } else {
            // Grouped by date: Today / Yesterday / This month / Earlier
            val grouped = remember(documents) { groupDocumentsByDate(documents) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = AppSpacing.navBarHeight + 36.dp
                )
            ) {
                for (group in DateGroup.entries) {
                    val groupDocs = grouped[group]
                    if (!groupDocs.isNullOrEmpty()) {
                        item(key = "header_${group.name}") {
                            Text(
                                text = group.title,
                                style = typography.caption.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = colors.inkMuted,
                                modifier = Modifier.padding(
                                    start = AppSpacing.screenPadding,
                                    top = 16.dp,
                                    bottom = 8.dp
                                )
                            )
                        }

                        items(
                            items = groupDocs,
                            key = { "doc_${it.uri}" }
                        ) { doc ->
                            AppFileRow(
                                document = doc,
                                onClick = { onDocumentClick(doc) },
                                onToggleFavorite = { onToggleFavorite(doc) },
                                onShare = { onShareDocument(doc) },
                                onShowDetails = { quickLookDoc = doc },
                                onRemoveFromRecents = { onDeleteDocument(doc) },
                                onLongClick = { quickLookDoc = doc },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Look Modal for Peek & Pop
    QuickLookModal(
        document = quickLookDoc,
        onDismiss = { quickLookDoc = null },
        onOpen = {
            quickLookDoc = null
            onDocumentClick(it)
        },
        onShare = {
            quickLookDoc = null
            onShareDocument(it)
        },
        onToggleFavorite = {
            onToggleFavorite(it)
        },
        onDelete = {
            quickLookDoc = null
            onDeleteDocument(it)
        }
    )
}
