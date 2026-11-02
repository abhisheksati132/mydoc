package com.abhisheksati.mydoc.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.animation.documentHero
import com.abhisheksati.mydoc.designsystem.animation.documentHeroKey
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.PaperSurface
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.PaperShape
import com.abhisheksati.mydoc.designsystem.tokens.SheetPalette
import com.abhisheksati.mydoc.designsystem.tokens.rememberSheetPalette
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.tools.model.PdfTool
import kotlinx.coroutines.launch

/**
 * Sheet prototype: the home page is a desk. Documents are printed sheets with
 * grain, a folded corner and a folio; actions are printed rules, not gradient
 * cards. One signal accent, two radii, no glows.
 */
@Composable
fun HomeTabContent(
    continueItems: List<LibraryItem>,
    recentItems: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onToolClick: (PdfTool) -> Unit = {},
    onSettingsClick: () -> Unit,
    onToggleTheme: () -> Unit = {},
    onClearRecent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val paper = rememberSheetPalette()
    val haptics = rememberAppHaptics()

    var showClearRecentDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(DocumentFilter.ALL) }

    // Lazy items do not invalidate on palette changes alone; folding the palette
    // into their keys forces a rebuild when the theme flips.
    val paletteKey = if (AppTheme.isDark) if (AppTheme.isOled) "b" else "d" else "l"

    val continueItem = continueItems.firstOrNull()
    val filteredItems = remember(recentItems, continueItem, selectedFilter) {
        (listOfNotNull(continueItem) + recentItems)
            .distinctBy { it.uri }
            .filter { selectedFilter.matches(it.extension) }
    }
    val deckItems = filteredItems.take(3)
    val heroUri = deckItems.firstOrNull()?.uri

    if (showClearRecentDialog) {
        AppDialog(
            isOpen = true,
            title = stringResource(R.string.clear_recent_history),
            message = stringResource(R.string.clear_recent_history_desc),
            confirmLabel = stringResource(R.string.clear_cache),
            dismissLabel = "Cancel",
            onConfirm = {
                showClearRecentDialog = false
                onClearRecent()
            },
            onDismiss = { showClearRecentDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        DeskHeader(
            paper = paper,
            onSearchClick = onSearchClick,
            onToggleTheme = onToggleTheme,
            onSettingsClick = onSettingsClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppSpacing.large)
        ) {
            item(key = "deck_$paletteKey") {
                DeskHero(
                    paper = paper,
                    items = deckItems,
                    onOpen = onItemClick,
                    onScan = onScanClick
                )
            }

            item(key = "rail_$paletteKey") {
                ToolRail(
                    paper = paper,
                    onScan = onScanClick,
                    onToolClick = onToolClick,
                    onTick = haptics::tick
                )
            }

            item(key = "index_header_$paletteKey") {
                IndexHeader(
                    paper = paper,
                    count = filteredItems.size,
                    canClear = recentItems.isNotEmpty(),
                    onClear = { showClearRecentDialog = true }
                )
            }

            item(key = "index_tabs_$paletteKey") {
                FilterTabs(
                    paper = paper,
                    selected = selectedFilter,
                    onSelected = {
                        haptics.tick()
                        selectedFilter = it
                    }
                )
            }

            if (filteredItems.isEmpty()) {
                item(key = "index_empty_$paletteKey") {
                    if (selectedFilter == DocumentFilter.ALL) {
                        EmptyIndex(
                            paper = paper,
                            actionLabel = "Open camera",
                            onAction = onScanClick
                        )
                    } else {
                        EmptyIndex(
                            paper = paper,
                            title = "No ${selectedFilter.label.lowercase()} sheets",
                            message = "Nothing in this format yet. The rest of the index is on All.",
                            actionLabel = "Show all sheets",
                            onAction = { selectedFilter = DocumentFilter.ALL }
                        )
                    }
                }
            } else {
                items(
                    count = filteredItems.size,
                    key = { index -> "${paletteKey}_${filteredItems[index].uri}" }
                ) { index ->
                    val item = filteredItems[index]
                    IndexRow(
                        paper = paper,
                        folio = index + 1,
                        item = item,
                        onClick = { onItemClick(item) },
                        modifier = if (item.uri != heroUri) {
                            Modifier.documentHero(documentHeroKey(item.uri))
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeskHeader(
    paper: SheetPalette,
    onSearchClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val typography = AppTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AppSpacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "MyDoc", style = typography.wordmark, color = paper.ink)
            Text(text = ".", style = typography.wordmark, color = paper.accent)
            Spacer(modifier = Modifier.weight(1f))
            HeaderAction(
                icon = Icons.Outlined.Search,
                label = "Search",
                tint = paper.ink,
                onClick = onSearchClick
            )
            HeaderAction(
                icon = if (AppTheme.isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                label = "Toggle theme",
                tint = paper.ink,
                onClick = onToggleTheme
            )
            HeaderAction(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                tint = paper.ink,
                onClick = onSettingsClick
            )
        }
        Hairline(color = paper.rule)
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DeskHero(
    paper: SheetPalette,
    items: List<LibraryItem>,
    onOpen: (LibraryItem) -> Unit,
    onScan: () -> Unit
) {
    val typography = AppTheme.typography
    val top = items.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.medium,
                bottom = AppSpacing.medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SheetDeck(
            paper = paper,
            items = items,
            onOpen = onOpen,
            onScan = onScan
        )

        Spacer(modifier = Modifier.width(AppSpacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (top != null) "ON THE DESK" else "BLANK DESK",
                style = typography.overline,
                color = paper.inkMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = top?.name?.let { formatMiddleTruncated(it, 44) } ?: "Scan a page",
                style = typography.titleSmall,
                color = paper.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = top?.let { sheetMeta(it) } ?: "Paper goes in, PDF comes out.",
                style = typography.caption.withTabularFigures(),
                color = paper.inkMuted,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(14.dp))
            InkButton(
                paper = paper,
                label = if (top != null) "Open sheet" else "Scan a page",
                onClick = { if (top != null) onOpen(top) else onScan() }
            )
            if (top != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Scan another page",
                    style = typography.captionStrong,
                    color = paper.accent,
                    modifier = Modifier
                        .clickable(onClick = onScan)
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Three sheets on a desk. Drag horizontally to fan them, release to settle;
 * the top sheet opens the document.
 */
@Composable
private fun SheetDeck(
    paper: SheetPalette,
    items: List<LibraryItem>,
    onOpen: (LibraryItem) -> Unit,
    onScan: () -> Unit
) {
    val fan = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(width = 132.dp, height = 182.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            fan.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 380f))
                        }
                    },
                    onDragCancel = {
                        scope.launch { fan.animateTo(0f) }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val next = (fan.value + dragAmount / 360f).coerceIn(-1f, 1f)
                    scope.launch { fan.snapTo(next) }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (items.isEmpty()) {
            PaperSurface(
                palette = paper,
                modifier = Modifier
                    .size(width = 124.dp, height = 174.dp)
                    .clickable(onClick = onScan),
                elevation = 8.dp,
                foldSize = 16.dp
            ) {
                CropMarks(color = paper.rule)
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = paper.inkMuted,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan",
                        style = AppTheme.typography.captionStrong,
                        color = paper.ink
                    )
                }
            }
        } else {
            val drag = fan.value
            for (depth in items.lastIndex downTo 0) {
                val item = items[depth]
                val lift = depth.toFloat()
                val isTop = depth == 0
                PaperSurface(
                    palette = paper,
                    modifier = Modifier
                        .size(width = 124.dp, height = 174.dp)
                        .offset(
                            x = (lift * 6f + drag * 9f * lift).dp,
                            y = (-lift * 5f + drag * -3f * lift).dp
                        )
                        .graphicsLayer {
                            rotationZ = if (isTop) drag * -7f else lift * (2.6f + drag * 4f)
                        }
                        .then(if (isTop) Modifier.clickable { onOpen(item) } else Modifier),
                    elevation = (10 - depth * 3).dp,
                    foldSize = if (isTop) 16.dp else 0.dp
                ) {
                    if (isTop) {
                        DeckSheetContent(paper = paper, item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckSheetContent(
    paper: SheetPalette,
    item: LibraryItem
) {
    val typography = AppTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.typeBadge,
                style = typography.micro,
                color = paper.inkMuted
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Hairline(color = paper.rule)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = item.name,
            style = typography.bodyStrong,
            color = paper.ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sheetMeta(item),
            style = typography.micro.withTabularFigures(),
            color = paper.inkMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (item.progressFraction > 0f) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(paper.track)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progressFraction.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(paper.accent)
                )
            }
        }
    }
}

@Composable
private fun CropMarks(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = 10.dp.toPx()
        val arm = 8.dp.toPx()
        val stroke = 1.dp.toPx()
        val corners = listOf(
            Offset(inset, inset) to Offset(1f, 1f),
            Offset(size.width - inset, inset) to Offset(-1f, 1f),
            Offset(inset, size.height - inset) to Offset(1f, -1f),
            Offset(size.width - inset, size.height - inset) to Offset(-1f, -1f)
        )
        corners.forEach { (origin, dir) ->
            drawLine(color, origin, Offset(origin.x + arm * dir.x, origin.y), stroke)
            drawLine(color, origin, Offset(origin.x, origin.y + arm * dir.y), stroke)
        }
    }
}

@Composable
private fun ToolRail(
    paper: SheetPalette,
    onScan: () -> Unit,
    onToolClick: (PdfTool) -> Unit,
    onTick: () -> Unit
) {
    val typography = AppTheme.typography

    Column(modifier = Modifier.fillMaxWidth()) {
        Hairline(color = paper.rule)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = AppSpacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RailItem(
                icon = Icons.Outlined.CameraAlt,
                label = "Scan",
                paper = paper,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTick()
                    onScan()
                }
            )
            RailDivider(color = paper.rule)
            RailItem(
                icon = Icons.AutoMirrored.Outlined.MergeType,
                label = "Merge",
                paper = paper,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTick()
                    onToolClick(PdfTool.MERGE)
                }
            )
            RailDivider(color = paper.rule)
            RailItem(
                icon = Icons.Outlined.Compress,
                label = "Compress",
                paper = paper,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTick()
                    onToolClick(PdfTool.COMPRESS)
                }
            )
            RailDivider(color = paper.rule)
            RailItem(
                icon = Icons.Outlined.Draw,
                label = "Sign",
                paper = paper,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTick()
                    onToolClick(PdfTool.SIGN_PDF)
                }
            )
        }
        Hairline(color = paper.rule)
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    paper: SheetPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = paper.ink,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            style = AppTheme.typography.label,
            color = paper.ink
        )
    }
}

@Composable
private fun RailDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(color)
    )
}

@Composable
private fun IndexHeader(
    paper: SheetPalette,
    count: Int,
    canClear: Boolean,
    onClear: () -> Unit
) {
    val typography = AppTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.medium,
                bottom = AppSpacing.small
            ),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(text = "Index", style = typography.heading, color = paper.ink)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString().padStart(2, '0'),
            style = typography.caption.withTabularFigures(),
            color = paper.inkMuted,
            modifier = Modifier.padding(bottom = 3.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (canClear) {
            Text(
                text = "Clear",
                style = typography.captionStrong,
                color = paper.accent,
                modifier = Modifier
                    .clickable(onClick = onClear)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun FilterTabs(
    paper: SheetPalette,
    selected: DocumentFilter,
    onSelected: (DocumentFilter) -> Unit
) {
    val typography = AppTheme.typography
    val options = listOf(DocumentFilter.ALL) + DocumentFilter.formats

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            options.forEach { filter ->
                val isSelected = filter == selected
                Column(
                    modifier = Modifier
                        .clickable { onSelected(filter) }
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = filter.label,
                        style = typography.captionStrong,
                        color = if (isSelected) paper.ink else paper.inkMuted
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 18.dp else 0.dp)
                            .height(2.dp)
                            .background(if (isSelected) paper.accent else Color.Transparent)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Hairline(color = paper.rule)
    }
}

@Composable
private fun IndexRow(
    paper: SheetPalette,
    folio: Int,
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = AppTheme.typography

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = AppSpacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = folio.toString().padStart(2, '0'),
                style = typography.caption.withTabularFigures(),
                color = paper.inkMuted,
                modifier = Modifier.width(28.dp)
            )
            MiniSheet(paper = paper, item = item)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = typography.bodyStrong,
                    color = paper.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sheetMeta(item),
                    style = typography.caption.withTabularFigures(),
                    color = paper.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Hairline(color = paper.rule)
    }
}

@Composable
private fun MiniSheet(
    paper: SheetPalette,
    item: LibraryItem
) {
    PaperSurface(
        palette = paper,
        modifier = Modifier.size(width = 26.dp, height = 34.dp),
        elevation = 1.dp,
        foldSize = 7.dp
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
        ) {
            Text(
                text = item.typeBadge.take(3),
                style = AppTheme.typography.micro,
                color = paper.inkMuted
            )
        }
    }
}

@Composable
private fun EmptyIndex(
    paper: SheetPalette,
    title: String = "No sheets on the desk",
    message: String = "Scan a page or open a file. Every document lands here, numbered and ready.",
    actionLabel: String = "Scan a page",
    onAction: () -> Unit = {}
) {
    val typography = AppTheme.typography

    PaperSurface(
        palette = paper,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.medium,
                bottom = AppSpacing.small
            ),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = typography.titleSmall, color = paper.ink)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = message, style = typography.caption, color = paper.inkMuted)
            Spacer(modifier = Modifier.height(14.dp))
            InkButton(paper = paper, label = actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun InkButton(
    paper: SheetPalette,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .background(paper.ink, PaperShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTheme.typography.captionStrong,
            color = paper.sheet
        )
    }
}

@Composable
private fun Hairline(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

private fun sheetMeta(item: LibraryItem): String {
    val pages = if (item.pageCount > 0 && item.extension == "pdf") "${item.pageCount} pp · " else ""
    return "${item.typeBadge} · $pages${item.formattedSize}"
}
