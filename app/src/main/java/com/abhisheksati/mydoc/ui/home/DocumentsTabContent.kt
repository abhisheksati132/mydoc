package com.abhisheksati.mydoc.ui.home

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFileRow
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.components.DocumentFormatBadge
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.viewmodel.HomeUiState
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

private data class DocumentNavCategory(
    val category: DocumentCategory,
    val icon: ImageVector,
    val color: Color,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsTabContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSettingsClick: () -> Unit,
    onDocumentClick: (DocumentFile) -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    openFolderLauncher: ActivityResultLauncher<Uri?>,
    openDocumentLauncher: ActivityResultLauncher<Array<String>>,
    onShowClearRecentsDialog: () -> Unit,
    onSelectDocForDetails: (DocumentFile) -> Unit,
    onOpenTrash: (() -> Unit)? = null,
    trashCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val isOled = colors.background == DarkAppColors.background
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var isSortMenuOpen by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(uiState.searchQuery.isNotEmpty()) }

    // Tactile pull-to-search gesture listener
    val pullToSearchConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 35f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 && !isSearchActive) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isSearchActive = true
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box {
                AppTabHeader(
                    title = "Documents",
                    onSearchClick = { isSearchActive = !isSearchActive },
                    isSearchActive = isSearchActive || uiState.searchQuery.isNotEmpty(),
                    searchContent = {
                        AppSearchField(
                            query = uiState.searchQuery,
                            onQueryChanged = viewModel::onSearchQueryChanged,
                            placeholder = "Search documents..."
                        )
                    },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onSettingsClick = onSettingsClick,
                    moreMenuContent = { onDismiss ->
                        DropdownMenuItem(
                            text = { Text("Sort documents...", style = typography.body) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                onDismiss()
                                isSortMenuOpen = true
                            }
                        )
                        if (onOpenTrash != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Recycle Bin", style = typography.body)
                                        if (trashCount > 0) {
                                            Badge { Text(trashCount.toString()) }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.accent)
                                },
                                onClick = {
                                    onDismiss()
                                    onOpenTrash()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Scan folder", style = typography.body) },
                            leadingIcon = {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = colors.textSecondary)
                            },
                            onClick = {
                                onDismiss()
                                openFolderLauncher.launch(null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear recents", style = typography.body) },
                            leadingIcon = {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.error)
                            },
                            onClick = {
                                onDismiss()
                                onShowClearRecentsDialog()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings", style = typography.body) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Settings, contentDescription = null, tint = colors.textSecondary)
                            },
                            onClick = {
                                onDismiss()
                                onSettingsClick()
                            }
                        )
                    },
                    bottomContent = {
                        Column {
                            // Full-Width Open Document Action Card
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    openDocumentLauncher.launch(SUPPORTED_MIME_TYPES)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceContainer,
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FileOpen,
                                        contentDescription = "Open Document",
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Open Document",
                                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                        color = colors.textPrimary
                                    )
                                }
                            }

                            // Symmetrical rounded rect category pills matching Neo-Precision identity
                            val docCategories = listOf(
                                DocumentNavCategory(DocumentCategory.ALL, Icons.Outlined.AllInclusive, colors.accent, "All"),
                                DocumentNavCategory(DocumentCategory.PDF, Icons.Outlined.PictureAsPdf, DocumentFormatColors.pdfTint, "PDF"),
                                DocumentNavCategory(DocumentCategory.EPUB, Icons.Outlined.Book, DocumentFormatColors.zipTint, "E-Books"),
                                DocumentNavCategory(DocumentCategory.WORD, Icons.Outlined.Description, DocumentFormatColors.wordTint, "Word"),
                                DocumentNavCategory(DocumentCategory.EXCEL, Icons.Outlined.TableChart, DocumentFormatColors.excelTint, "Excel"),
                                DocumentNavCategory(DocumentCategory.PPT, Icons.Outlined.Slideshow, DocumentFormatColors.pptTint, "PPT"),
                                DocumentNavCategory(DocumentCategory.TEXT, Icons.AutoMirrored.Outlined.Article, DocumentFormatColors.textTint, "Text")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (item in docCategories) {
                                    val isSelected = uiState.selectedCategory == item.category
                                    val chipBg = if (isSelected) item.color.copy(alpha = 0.16f) else colors.surfaceContainer
                                    val chipBorderColor = if (isSelected) item.color else colors.cardBorder

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = chipBg,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, chipBorderColor),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.selectCategory(item.category)
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label,
                                                tint = if (isSelected) item.color else item.color.copy(alpha = 0.85f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.label,
                                                style = typography.caption.copy(
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) colors.textPrimary else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

                // Sort Menu
                DropdownMenu(
                    expanded = isSortMenuOpen,
                    onDismissRequest = { isSortMenuOpen = false },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = colors.surface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp
                ) {
                    DropdownMenuItem(
                        text = { Text("Last opened", style = typography.body) },
                        trailingIcon = {
                            if (uiState.sortOrder == DocumentSortOrder.LAST_OPENED) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                            }
                        },
                        onClick = {
                            viewModel.setSortOrder(DocumentSortOrder.LAST_OPENED)
                            isSortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Name (A to Z)", style = typography.body) },
                        trailingIcon = {
                            if (uiState.sortOrder == DocumentSortOrder.NAME_ASC) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                            }
                        },
                        onClick = {
                            viewModel.setSortOrder(DocumentSortOrder.NAME_ASC)
                            isSortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Name (Z to A)", style = typography.body) },
                        trailingIcon = {
                            if (uiState.sortOrder == DocumentSortOrder.NAME_DESC) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                            }
                        },
                        onClick = {
                            viewModel.setSortOrder(DocumentSortOrder.NAME_DESC)
                            isSortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Size (largest first)", style = typography.body) },
                        trailingIcon = {
                            if (uiState.sortOrder == DocumentSortOrder.SIZE_DESC) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                            }
                        },
                        onClick = {
                            viewModel.setSortOrder(DocumentSortOrder.SIZE_DESC)
                            isSortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Size (smallest first)", style = typography.body) },
                        trailingIcon = {
                            if (uiState.sortOrder == DocumentSortOrder.SIZE_ASC) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                            }
                        },
                        onClick = {
                            viewModel.setSortOrder(DocumentSortOrder.SIZE_ASC)
                            isSortMenuOpen = false
                        }
                    )
                }
            }

            // Document list or empty state
            if (uiState.documents.isEmpty()) {
                if (uiState.isSearching) {
                    AppEmptyState(
                        title = "No documents matched your search.",
                        message = "Try another search term or clear the filter."
                    )
                } else {
                    AppEmptyState(
                        title = "No documents found.",
                        message = "Open a file or scan a folder to begin.",
                        actionLabel = "Open document",
                        onAction = { openDocumentLauncher.launch(SUPPORTED_MIME_TYPES) }
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pullToSearchConnection),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp)
                ) {
                    if (uiState.isSearching) {
                        item {
                            AppSectionHeader(title = "SEARCH RESULTS")
                        }
                        items(
                            items = uiState.documents,
                            key = { "search_${it.uri}" },
                            contentType = { "doc_row" }
                        ) { document ->
                            AppFileRow(
                                document = document,
                                onClick = { onDocumentClick(document) },
                                onToggleFavorite = { viewModel.toggleFavorite(document) },
                                onRemoveFromRecents = {
                                    viewModel.removeFromRecents(document)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Removed \"${document.displayName}\"",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                onShare = { onShareDocument(document) },
                                onShowDetails = { onSelectDocForDetails(document) }
                            )
                        }
                    } else {
                        // Continue reading row if applicable
                        val continueDoc = uiState.continueReadingDoc
                        if (continueDoc != null && uiState.selectedCategory == DocumentCategory.ALL) {
                            item(key = "continue_header") {
                                AppSectionHeader(title = "CONTINUE READING")
                            }
                            item(key = "continue_${continueDoc.uri}") {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colors.surface,
                                    border = BorderStroke(1.dp, colors.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    AppFileRow(
                                        document = continueDoc,
                                        showContinueReading = true,
                                        showDivider = false,
                                        onClick = { onDocumentClick(continueDoc) },
                                        onToggleFavorite = { viewModel.toggleFavorite(continueDoc) },
                                        onRemoveFromRecents = { viewModel.removeFromRecents(continueDoc) },
                                        onShare = { onShareDocument(continueDoc) },
                                        onShowDetails = { onSelectDocForDetails(continueDoc) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        // Main Section Header
                        val headerTitle = when (uiState.selectedCategory) {
                            DocumentCategory.ALL -> "ALL DOCUMENTS"
                            DocumentCategory.FAVORITES -> "FAVORITES"
                            DocumentCategory.PDF -> "PDF DOCUMENTS"
                            DocumentCategory.EPUB -> "E-BOOKS & EPUB"
                            DocumentCategory.WORD -> "WORD DOCUMENTS"
                            DocumentCategory.EXCEL -> "EXCEL SPREADSHEETS"
                            DocumentCategory.PPT -> "POWERPOINT PRESENTATIONS"
                            DocumentCategory.TEXT -> "TEXT DOCUMENTS"
                        }
                        item(key = "section_header_main") {
                            AppSectionHeader(title = headerTitle)
                        }

                        // Document Items without gesture interference
                        items(
                            items = uiState.documents,
                            key = { "doc_${it.uri}" },
                            contentType = { "doc_row" }
                        ) { document ->
                            AppFileRow(
                                document = document,
                                onClick = { onDocumentClick(document) },
                                onToggleFavorite = { viewModel.toggleFavorite(document) },
                                onRemoveFromRecents = {
                                    viewModel.removeFromRecents(document)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Removed \"${document.displayName}\"",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                onShare = { onShareDocument(document) },
                                onShowDetails = { onSelectDocForDetails(document) }
                            )
                        }

                        // Favorites Section (if on ALL and has favorites)
                        if (uiState.selectedCategory == DocumentCategory.ALL && uiState.favorites.isNotEmpty()) {
                            item(key = "section_header_fav") {
                                AppSectionHeader(title = "FAVORITES")
                            }
                            items(
                                items = uiState.favorites,
                                key = { "fav_${it.uri}" },
                                contentType = { "doc_row" }
                            ) { document ->
                                AppFileRow(
                                    document = document,
                                    onClick = { onDocumentClick(document) },
                                    onToggleFavorite = { viewModel.toggleFavorite(document) },
                                    onRemoveFromRecents = {
                                        viewModel.removeFromRecents(document)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Removed \"${document.displayName}\"",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onShare = { onShareDocument(document) },
                                    onShowDetails = { onSelectDocForDetails(document) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Snackbar Host for Undo actions
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        )
    }
}

/**
 * Horizontal Carousel for quickly accessing recently opened or pinned documents.
 */
@Composable
private fun DocumentsRecentsCarousel(
    documents: List<DocumentFile>,
    onDocumentClick: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val isOled = colors.background == DarkAppColors.background

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = documents.take(8),
            key = { "carousel_${it.uri}" }
        ) { doc ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isOled) colors.surface else colors.surfaceContainer,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier
                    .width(136.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDocumentClick(doc)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DocumentFormatBadge(
                            extension = doc.typeBadge,
                            size = 32.dp
                        )
                        if (doc.isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorited",
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = doc.displayName,
                        style = typography.caption.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.height(34.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${doc.formattedSize}  •  ${doc.typeBadge.uppercase()}",
                        style = typography.caption.withTabularFigures().copy(fontSize = 10.sp),
                        color = colors.textSecondary,
                        maxLines = 1
                    )

                    if (doc.pageCount > 0 && doc.lastPage > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = doc.lastPage.toFloat() / doc.pageCount
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = DocumentFormatColors.getTint(doc.typeBadge),
                            trackColor = colors.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

