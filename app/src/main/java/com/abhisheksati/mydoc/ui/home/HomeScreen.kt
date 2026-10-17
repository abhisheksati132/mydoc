package com.abhisheksati.mydoc.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.data.local.FolderScanner
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFileRow
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppToolbar
import com.abhisheksati.mydoc.designsystem.components.ContinueReadingCard
import com.abhisheksati.mydoc.designsystem.components.DocumentGridCard
import com.abhisheksati.mydoc.designsystem.components.FloatingGlassDock
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DocumentTypeGradients
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.ui.ToolRunnerSheet
import com.abhisheksati.mydoc.tools.ui.ToolsScreen
import com.abhisheksati.mydoc.tools.ui.components.CameraScanSheet
import com.abhisheksati.mydoc.tools.ui.components.SignPdfSheet
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

val SUPPORTED_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/x-pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-powerpoint",
    "text/plain",
    "text/csv",
    "text/comma-separated-values",
    "application/octet-stream",
    "*/*"
)

enum class HomeTab {
    DOCUMENTS,
    MEDIA,
    TOOLS
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    toolsViewModel: ToolsViewModel,
    mediaViewModel: com.abhisheksati.mydoc.viewmodel.MediaViewModel? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onDocumentClick: (DocumentFile) -> Unit,
    onOpenMedia: ((com.abhisheksati.mydoc.domain.model.MediaItem, List<com.abhisheksati.mydoc.domain.model.MediaItem>) -> Unit)? = null,
    onSettingsClick: () -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp

    var currentTab by remember { mutableStateOf(HomeTab.DOCUMENTS) }
    var isGridView by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var showClearRecentsDialog by remember { mutableStateOf(false) }
    var selectedDocForDetails by remember { mutableStateOf<DocumentFile?>(null) }

    // SAF Document Picker launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.onDocumentOpened(uri) { result ->
                result.onSuccess { doc ->
                    onDocumentClick(doc)
                }.onFailure {
                    Toast.makeText(context, "Unable to access the selected document", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF Folder Picker launcher
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null && app != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val segment = uri.lastPathSegment ?: "Folder"
                val cleanName = segment.substringAfterLast(':').ifEmpty { segment }
                app.settingsRepository.setGrantedFolder(uri.toString(), cleanName)
                scope.launch {
                    val count = FolderScanner.scanTree(context, uri, app.documentRepository)
                    Toast.makeText(context, "Added $count documents from $cleanName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to access folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .statusBarsPadding()
            ) {
                AppToolbar(
                    title = stringResource(R.string.app_name),
                    actions = {
                        // Day / Night mode toggle directly on main page (Sun & Moon logo)
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleTheme()
                            },
                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode",
                                tint = colors.textPrimary
                            )
                        }

                        // Grid vs List view toggle (visible on Documents tab)
                        if (currentTab == HomeTab.DOCUMENTS) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isGridView = !isGridView
                                },
                                modifier = Modifier.size(AppSpacing.minTouchTarget)
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                    contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                                    tint = colors.textSecondary
                                )
                            }
                        }

                        // Sort order dropdown
                    Box {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isSortMenuOpen = true
                            },
                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = "Sort documents",
                                tint = colors.textSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false }
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

                    // Folder scan button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            openFolderLauncher.launch(null)
                        },
                        modifier = Modifier.size(AppSpacing.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Scan folder",
                            tint = colors.textSecondary
                        )
                    }

                    // Settings button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSettingsClick()
                        },
                        modifier = Modifier.size(AppSpacing.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colors.textSecondary
                        )
                    }

                    // Clear recents menu
                    Box {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isMoreMenuOpen = true
                            },
                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More options",
                                tint = colors.textSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = isMoreMenuOpen,
                            onDismissRequest = { isMoreMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear recents", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.error)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    showClearRecentsDialog = true
                                }
                            )
                        }
                    }
                }
            )
            AppDivider()
        }
    },
    bottomBar = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            if (currentTab == HomeTab.DOCUMENTS) {
                // One-hand reach search & filter surface with frosted glass background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.paddingLarge, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppSearchField(
                            query = uiState.searchQuery,
                            onQueryChanged = viewModel::onSearchQueryChanged,
                            placeholder = "Search documents...",
                            modifier = Modifier.weight(1f)
                        )

                        // Apple-style Frosted Glass "+ Open" button
                        Box(
                            modifier = Modifier
                                .height(AppSpacing.minTouchTarget)
                                .clip(RoundedCornerShape(AppSpacing.pillCornerRadius))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(colors.accent, colors.accent.copy(alpha = 0.85f))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(AppSpacing.pillCornerRadius)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    openDocumentLauncher.launch(SUPPORTED_MIME_TYPES)
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.FileOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                    ) {
                        AppFilterChip(
                            label = "All",
                            isSelected = uiState.selectedCategory == DocumentCategory.ALL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.ALL)
                            }
                        )
                        AppFilterChip(
                            label = "PDF",
                            isSelected = uiState.selectedCategory == DocumentCategory.PDF,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.PDF)
                            }
                        )
                        AppFilterChip(
                            label = "Word",
                            isSelected = uiState.selectedCategory == DocumentCategory.WORD,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.WORD)
                            }
                        )
                        AppFilterChip(
                            label = "Excel",
                            isSelected = uiState.selectedCategory == DocumentCategory.EXCEL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.EXCEL)
                            }
                        )
                        AppFilterChip(
                            label = "PPT",
                            isSelected = uiState.selectedCategory == DocumentCategory.PPT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.PPT)
                            }
                        )
                        AppFilterChip(
                            label = "Text",
                            isSelected = uiState.selectedCategory == DocumentCategory.TEXT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.TEXT)
                            }
                        )
                    }
                }
            }

            // Floating Glass Dock (Primary Navigation)
            FloatingGlassDock(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                }
            )
        }
    },
    containerColor = colors.background
) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (currentTab == HomeTab.TOOLS) {
            ToolsScreen(
                viewModel = toolsViewModel,
                onToolClick = { tool ->
                    if (tool == PdfTool.SCAN_TO_PDF) {
                        showCameraScanner = true
                    } else {
                        toolsViewModel.selectTool(tool)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (currentTab == HomeTab.MEDIA) {
            if (mediaViewModel != null) {
                com.abhisheksati.mydoc.ui.media.MediaTabContent(
                    viewModel = mediaViewModel,
                    onOpenMedia = { item, list ->
                        onOpenMedia?.invoke(item, list)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (uiState.documents.isEmpty()) {
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
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.isSearching) {
                        item(span = { GridItemSpan(2) }) {
                            AppSectionHeader(title = "SEARCH RESULTS")
                        }
                        items(
                            items = uiState.documents,
                            key = { "grid_search_${it.uri}" }
                        ) { document ->
                            DocumentGridCard(
                                document = document,
                                isFavorite = document.isFavorite || uiState.favorites.any { fav -> fav.uri == document.uri },
                                onToggleFavorite = { viewModel.toggleFavorite(document) },
                                onClick = { onDocumentClick(document) },
                                onLongClick = { selectedDocForDetails = document }
                            )
                        }
                    } else {
                        // 1. Hero Continue Reading Card (spans 2 columns)
                        val continueDoc = uiState.continueReadingDoc
                        if (continueDoc != null && uiState.selectedCategory == DocumentCategory.ALL) {
                            item(span = { GridItemSpan(2) }) {
                                AppSectionHeader(title = "CONTINUE READING")
                            }
                            item(span = { GridItemSpan(2) }, key = "grid_continue_${continueDoc.uri}") {
                                ContinueReadingCard(
                                    document = continueDoc,
                                    onClick = { onDocumentClick(continueDoc) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 2. Section Header & Documents Grid
                        val headerTitle = when (uiState.selectedCategory) {
                            DocumentCategory.ALL -> "RECENTS"
                            DocumentCategory.FAVORITES -> "FAVORITES"
                            DocumentCategory.PDF -> "PDF DOCUMENTS"
                            DocumentCategory.WORD -> "WORD DOCUMENTS"
                            DocumentCategory.EXCEL -> "EXCEL SPREADSHEETS"
                            DocumentCategory.PPT -> "POWERPOINT PRESENTATIONS"
                            DocumentCategory.TEXT -> "TEXT DOCUMENTS"
                        }
                        item(span = { GridItemSpan(2) }) {
                            AppSectionHeader(title = headerTitle)
                        }
                        items(
                            items = uiState.documents,
                            key = { "grid_doc_${it.uri}" }
                        ) { document ->
                            DocumentGridCard(
                                document = document,
                                isFavorite = document.isFavorite || uiState.favorites.any { fav -> fav.uri == document.uri },
                                onToggleFavorite = { viewModel.toggleFavorite(document) },
                                onClick = { onDocumentClick(document) },
                                onLongClick = { selectedDocForDetails = document }
                            )
                        }

                        // 3. Favorites Section (if viewing All and has favorites)
                        if (uiState.selectedCategory == DocumentCategory.ALL && uiState.favorites.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                AppSectionHeader(title = "FAVORITES")
                            }
                            items(
                                items = uiState.favorites,
                                key = { "grid_fav_${it.uri}" }
                            ) { document ->
                                DocumentGridCard(
                                    document = document,
                                    isFavorite = true,
                                    onToggleFavorite = { viewModel.toggleFavorite(document) },
                                    onClick = { onDocumentClick(document) },
                                    onLongClick = { selectedDocForDetails = document }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    if (uiState.isSearching) {
                        item {
                            AppSectionHeader(title = "SEARCH RESULTS")
                        }
                        items(
                            items = uiState.documents,
                            key = { "search_${it.uri}" }
                        ) { document ->
                            AppFileRow(
                                document = document,
                                onClick = { onDocumentClick(document) },
                                onToggleFavorite = { viewModel.toggleFavorite(document) },
                                onRemoveFromRecents = { viewModel.removeFromRecents(document) },
                                onShare = { onShareDocument(document) },
                                onShowDetails = { selectedDocForDetails = document }
                            )
                        }
                    } else {
                        // 1. Hero Continue Reading Card
                        val continueDoc = uiState.continueReadingDoc
                        if (continueDoc != null && uiState.selectedCategory == DocumentCategory.ALL) {
                            item {
                                AppSectionHeader(title = "CONTINUE READING")
                            }
                            item(key = "continue_${continueDoc.uri}") {
                                ContinueReadingCard(
                                    document = continueDoc,
                                    onClick = { onDocumentClick(continueDoc) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppSpacing.paddingLarge, vertical = 4.dp)
                                )
                            }
                        }

                        // 2. Section Header & Documents List
                        val headerTitle = when (uiState.selectedCategory) {
                            DocumentCategory.ALL -> "RECENTS"
                            DocumentCategory.FAVORITES -> "FAVORITES"
                            DocumentCategory.PDF -> "PDF DOCUMENTS"
                            DocumentCategory.WORD -> "WORD DOCUMENTS"
                            DocumentCategory.EXCEL -> "EXCEL SPREADSHEETS"
                            DocumentCategory.PPT -> "POWERPOINT PRESENTATIONS"
                            DocumentCategory.TEXT -> "TEXT DOCUMENTS"
                        }
                        item {
                            AppSectionHeader(title = headerTitle)
                        }
                        items(
                            items = uiState.documents,
                            key = { "doc_${it.uri}" }
                        ) { document ->
                            AppFileRow(
                                document = document,
                                onClick = { onDocumentClick(document) },
                                onToggleFavorite = { viewModel.toggleFavorite(document) },
                                onRemoveFromRecents = { viewModel.removeFromRecents(document) },
                                onShare = { onShareDocument(document) },
                                onShowDetails = { selectedDocForDetails = document }
                            )
                        }

                        // 3. Favorites Section (if viewing All and has favorites)
                        if (uiState.selectedCategory == DocumentCategory.ALL && uiState.favorites.isNotEmpty()) {
                            item {
                                AppSectionHeader(title = "FAVORITES")
                            }
                            items(
                                items = uiState.favorites,
                                key = { "fav_${it.uri}" }
                            ) { document ->
                                AppFileRow(
                                    document = document,
                                    onClick = { onDocumentClick(document) },
                                    onToggleFavorite = { viewModel.toggleFavorite(document) },
                                    onRemoveFromRecents = { viewModel.removeFromRecents(document) },
                                    onShare = { onShareDocument(document) },
                                    onShowDetails = { selectedDocForDetails = document }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Document Details Dialog
    if (selectedDocForDetails != null) {
        DocumentDetailsDialog(
            document = selectedDocForDetails,
            onDismiss = { selectedDocForDetails = null },
            onOpen = { doc ->
                selectedDocForDetails = null
                onDocumentClick(doc)
            },
            onShare = { doc ->
                onShareDocument(doc)
            },
            onRemove = { doc ->
                selectedDocForDetails = null
                viewModel.removeFromRecents(doc)
            }
        )
    }

    // Clear Recents Confirmation Dialog (Destructive action confirms, no undo snackbar)
    AppDialog(
        isOpen = showClearRecentsDialog,
        title = "Clear recent history",
        message = "Recent document history will be cleared. Files on storage will remain intact.",
        confirmLabel = "Clear all",
        dismissLabel = "Cancel",
        onConfirm = {
            showClearRecentsDialog = false
            viewModel.clearAllRecents()
            Toast.makeText(context, "Recent history cleared", Toast.LENGTH_SHORT).show()
        },
        onDismiss = { showClearRecentsDialog = false }
    )

    // PDF Tool Runner Sheet
    val selectedTool by toolsViewModel.selectedTool.collectAsState()
    if (selectedTool != null) {
        ToolRunnerSheet(
            viewModel = toolsViewModel,
            onOpenResultFile = { file, _ ->
                val uri = Uri.fromFile(file)
                scope.launch {
                    val appInst = context.applicationContext as? MyDocApp
                    if (appInst != null) {
                        val res = appInst.documentRepository.registerOpenedDocument(uri)
                        res.onSuccess { doc ->
                            toolsViewModel.closeTool()
                            onDocumentClick(doc)
                        }
                    }
                }
            }
        )
    }

    // Camera Document Scanner
    if (showCameraScanner) {
        CameraScanSheet(
            onDismiss = { showCameraScanner = false },
            onScannedUris = { uris ->
                showCameraScanner = false
                toolsViewModel.setInputUris(uris)
                toolsViewModel.selectTool(PdfTool.SCAN_TO_PDF)
            }
        )
    }

    // Signature Pad
    if (showSignaturePad) {
        SignPdfSheet(
            onDismiss = { showSignaturePad = false },
            onSignatureSaved = {
                showSignaturePad = false
                Toast.makeText(context, "Signature encrypted and saved.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
