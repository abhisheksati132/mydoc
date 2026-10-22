package com.abhisheksati.mydoc.ui.documents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFileRow
import com.abhisheksati.mydoc.designsystem.components.DocumentCoverImage
import com.abhisheksati.mydoc.designsystem.components.DocumentFormatBadge
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import java.util.Calendar
import kotlinx.coroutines.launch

private val CATEGORIES = listOf(
    DocumentCategory.ALL,
    DocumentCategory.PDF,
    DocumentCategory.WORD,
    DocumentCategory.EXCEL,
    DocumentCategory.PPT,
    DocumentCategory.TEXT
)

private enum class DateGroup(val title: String) {
    TODAY("TODAY"),
    YESTERDAY("YESTERDAY"),
    THIS_MONTH("THIS MONTH"),
    EARLIER("EARLIER")
}

private fun groupDocumentsByDate(documents: List<DocumentFile>): Map<DateGroup, List<DocumentFile>> {
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
 * Premier Document Experience Dashboard.
 *
 * Features:
 * - Real generated cover previews for documents
 * - "Jump Back In" Continue Reading Hero Card
 * - Grid / List View Toggle
 * - Swipeable Format Category Pager (All, PDF, Word, Excel, PPT, Text)
 * - Instant Camera Scan Floating Action Button
 */
@Composable
fun DocumentsScreen(
    documents: List<DocumentFile>,
    recentDocs: List<DocumentFile> = emptyList(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: DocumentSortOrder,
    onSetSortOrder: (DocumentSortOrder) -> Unit,
    onDocumentClick: (DocumentFile) -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    onShowDetails: (DocumentFile) -> Unit,
    onDeleteDocument: (DocumentFile) -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()

    var showSearchField by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var isGridView by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { CATEGORIES.size }
    )

    // Calculate document counts per category
    val categoryCounts = remember(documents) {
        CATEGORIES.associateWith { cat ->
            when (cat) {
                DocumentCategory.ALL -> documents.size
                DocumentCategory.PDF -> documents.count { it.category == DocumentCategory.PDF }
                DocumentCategory.WORD -> documents.count { it.category == DocumentCategory.WORD }
                DocumentCategory.EXCEL -> documents.count { it.category == DocumentCategory.EXCEL }
                DocumentCategory.PPT -> documents.count { it.category == DocumentCategory.PPT }
                DocumentCategory.TEXT -> documents.count { it.category == DocumentCategory.TEXT }
            }
        }
    }

    val continueDoc = recentDocs.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.medium, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = !showSearchField, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Article,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "mydoc",
                            style = typography.title.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = colors.textPrimary
                        )
                    }
                }

                AnimatedVisibility(visible = showSearchField, modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceContainer)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search documents…",
                                        style = typography.body.copy(fontSize = 14.sp),
                                        color = colors.textSecondary
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear search",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Search toggle button
                IconButton(
                    onClick = {
                        haptics.select()
                        showSearchField = !showSearchField
                        if (!showSearchField) onSearchQueryChange("")
                    }
                ) {
                    Icon(
                        imageVector = if (showSearchField) Icons.Outlined.Clear else Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = colors.textPrimary
                    )
                }

                // Grid / List Toggle
                IconButton(
                    onClick = {
                        haptics.select()
                        isGridView = !isGridView
                    }
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                        contentDescription = if (isGridView) "List view" else "Grid view",
                        tint = colors.textPrimary
                    )
                }

                // Sort Order menu
                Box {
                    IconButton(
                        onClick = {
                            haptics.select()
                            sortMenuExpanded = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = "Sort order",
                            tint = colors.textPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        containerColor = colors.surfaceContainer,
                        border = BorderStroke(1.dp, colors.cardBorder)
                    ) {
                        val sortOptions = listOf(
                            DocumentSortOrder.LAST_OPENED to "Recently Opened",
                            DocumentSortOrder.NAME_ASC to "Name (A to Z)",
                            DocumentSortOrder.NAME_DESC to "Name (Z to A)",
                            DocumentSortOrder.SIZE_DESC to "Size (Largest)",
                            DocumentSortOrder.SIZE_ASC to "Size (Smallest)"
                        )
                        sortOptions.forEach { (order, label) ->
                            DropdownMenuItem(
                                text = { Text(label, style = typography.body) },
                                leadingIcon = if (sortOrder == order) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent) }
                                } else null,
                                onClick = {
                                    haptics.select()
                                    sortMenuExpanded = false
                                    onSetSortOrder(order)
                                }
                            )
                        }
                    }
                }

                // Settings button
                IconButton(
                    onClick = {
                        haptics.select()
                        onSettingsClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = colors.textPrimary
                    )
                }
            }

            // 2. Scrollable Format Category Tabs
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = AppSpacing.medium, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEachIndexed { index, cat ->
                    val isSelected = pagerState.currentPage == index
                    val count = categoryCounts[cat] ?: 0

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.select()
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.ink else colors.surfaceContainer,
                        border = if (isSelected) null else BorderStroke(1.dp, colors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.label,
                                style = typography.body.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                ),
                                color = if (isSelected) colors.background else colors.textPrimary
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) colors.background.copy(alpha = 0.25f)
                                            else colors.accent.copy(alpha = 0.16f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = typography.caption.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isSelected) colors.background else colors.accent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Pager for Document Categories
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val category = CATEGORIES[pageIndex]
                val pageDocs = remember(documents, category) {
                    when (category) {
                        DocumentCategory.ALL -> documents
                        DocumentCategory.PDF -> documents.filter { it.category == DocumentCategory.PDF }
                        DocumentCategory.WORD -> documents.filter { it.category == DocumentCategory.WORD }
                        DocumentCategory.EXCEL -> documents.filter { it.category == DocumentCategory.EXCEL }
                        DocumentCategory.PPT -> documents.filter { it.category == DocumentCategory.PPT }
                        DocumentCategory.TEXT -> documents.filter { it.category == DocumentCategory.TEXT }
                    }
                }

                if (pageDocs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AppEmptyState(
                            icon = Icons.Outlined.Description,
                            title = if (searchQuery.isNotEmpty()) "No matching documents" else "No ${catName(category)} documents",
                            message = if (searchQuery.isNotEmpty()) "Try searching for a different keyword" else "Documents in this format will appear here automatically",
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }
                } else if (isGridView) {
                    // Grid View: Bookshelf presentation with large covers
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pageDocs, key = { it.uri }) { doc ->
                            DocumentGridCard(
                                document = doc,
                                onClick = { onDocumentClick(doc) },
                                onShare = { onShareDocument(doc) },
                                onDetails = { onShowDetails(doc) },
                                onDelete = { onDeleteDocument(doc) }
                            )
                        }
                    }
                } else {
                    // List View: Rich document rows with covers and hero card
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        // "Continue Reading" Hero Card on "All" tab when user has a recent file
                        if (category == DocumentCategory.ALL && continueDoc != null && searchQuery.isEmpty()) {
                            item(key = "hero_continue_reading") {
                                HeroContinueReadingCard(
                                    document = continueDoc,
                                    onResumeClick = { onDocumentClick(continueDoc) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }

                        // Recent carousel strip
                        if (category == DocumentCategory.ALL && recentDocs.size > 1 && searchQuery.isEmpty()) {
                            item(key = "recents_carousel_section") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 12.dp)
                                ) {
                                    Text(
                                        text = "RECENT DOCUMENTS",
                                        style = typography.caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = 6.dp)
                                    )

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = AppSpacing.medium),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = recentDocs.drop(1).take(8),
                                            key = { "recent_${it.uri}" }
                                        ) { doc ->
                                            RecentCard(
                                                document = doc,
                                                onClick = { onDocumentClick(doc) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(AppSpacing.hairline)
                                            .background(colors.line)
                                    )
                                }
                            }
                        }

                        // Date Grouped Document Rows
                        val grouped = groupDocumentsByDate(pageDocs)
                        DateGroup.entries.forEach { group ->
                            val docsInGroup = grouped[group] ?: emptyList()
                            if (docsInGroup.isNotEmpty()) {
                                item(key = "group_header_${group.name}") {
                                    Text(
                                        text = group.title,
                                        style = typography.caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(
                                            start = AppSpacing.medium,
                                            top = 16.dp,
                                            bottom = 6.dp
                                        )
                                    )
                                }

                                items(
                                    items = docsInGroup,
                                    key = { it.uri }
                                ) { doc ->
                                    AppFileRow(
                                        document = doc,
                                        showContinueReading = true,
                                        showDivider = true,
                                        onClick = { onDocumentClick(doc) },
                                        onShare = { onShareDocument(doc) },
                                        onShowDetails = { onShowDetails(doc) },
                                        onRemoveFromRecents = { onDeleteDocument(doc) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Camera Document Scanner FAB
        FloatingActionButton(
            onClick = {
                haptics.confirm()
                onScanClick()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = colors.accent,
            contentColor = Color.Black,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = "Scan Document",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan",
                    style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )
            }
        }
    }
}

private fun catName(category: DocumentCategory): String = when (category) {
    DocumentCategory.ALL -> "all"
    DocumentCategory.PDF -> "PDF"
    DocumentCategory.WORD -> "Word"
    DocumentCategory.EXCEL -> "Excel"
    DocumentCategory.PPT -> "PowerPoint"
    DocumentCategory.TEXT -> "Text"
}

/**
 * Hero Card highlighting the user's active document with cover thumbnail, page progress, and Resume button.
 */
@Composable
private fun HeroContinueReadingCard(
    document: DocumentFile,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                haptics.confirm()
                onResumeClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real Cover thumbnail with depth
            DocumentCoverImage(
                document = document,
                modifier = Modifier.width(72.dp),
                cornerRadius = 10.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "CONTINUE READING",
                        style = typography.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = colors.accent
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = document.displayName,
                    style = typography.title.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val pageProgressText = if (document.pageCount > 0 && document.lastPage > 0) {
                    val percent = ((document.lastPage + 1) * 100 / document.pageCount)
                    "Page ${document.lastPage + 1} of ${document.pageCount} (${percent}%)"
                } else {
                    document.formattedSize
                }

                Text(
                    text = pageProgressText,
                    style = typography.caption.withTabularFigures().copy(fontSize = 12.sp),
                    color = colors.textSecondary
                )

                if (document.pageCount > 0 && document.lastPage > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (document.lastPage.toFloat() / document.pageCount).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = colors.accent,
                        trackColor = colors.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            haptics.confirm()
                            onResumeClick()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Resume",
                            style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card for Grid View (Bookshelf style).
 */
@Composable
private fun DocumentGridCard(
    document: DocumentFile,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Document Cover
            DocumentCoverImage(
                document = document,
                modifier = Modifier.fillMaxWidth(),
                aspectRatio = 0.75f,
                cornerRadius = 8.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = document.displayName,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = document.formattedSize,
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = {
                            haptics.select()
                            menuExpanded = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = colors.surfaceContainer,
                        border = BorderStroke(1.dp, colors.cardBorder)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share", style = typography.body) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Details", style = typography.body) },
                            onClick = {
                                menuExpanded = false
                                onDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", style = typography.body.copy(color = colors.error)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCard(
    document: DocumentFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Surface(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            DocumentCoverImage(
                document = document,
                modifier = Modifier.fillMaxWidth(),
                aspectRatio = 1.0f,
                cornerRadius = 8.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = document.displayName,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = document.formattedSize,
                style = typography.caption.copy(fontSize = 10.5.sp),
                color = colors.textSecondary
            )

            if (document.pageCount > 0 && document.lastPage > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (document.lastPage.toFloat() / document.pageCount).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = DocumentFormatColors.getTint(document.typeBadge),
                    trackColor = colors.surfaceVariant
                )
            }
        }
    }
}
