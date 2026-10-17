package com.abhisheksati.mydoc.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
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
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
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
                    .background(colors.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Google Drive / Files style Pill Search Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = colors.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            modifier = Modifier.weight(1f),
                            textStyle = typography.body.copy(color = colors.textPrimary),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search documents...",
                                        style = typography.body,
                                        color = colors.textSecondary
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChanged("") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Theme toggle (Sun / Moon)
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleTheme()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = if (isDarkTheme) "Light mode" else "Dark mode",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Sort Menu
                        Box {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isSortMenuOpen = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                                    contentDescription = "Sort documents",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
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

                        // More options menu (Scan folder, Settings, Clear recents)
                        Box {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMoreMenuOpen = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More options",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = isMoreMenuOpen,
                                onDismissRequest = { isMoreMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Scan folder", style = typography.body) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = colors.textSecondary)
                                    },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        openFolderLauncher.launch(null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings", style = typography.body) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Settings, contentDescription = null, tint = colors.textSecondary)
                                    },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        onSettingsClick()
                                    }
                                )
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
                }

                // Category Filter Chips (visible on Documents tab)
                if (currentTab == HomeTab.DOCUMENTS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        },
        bottomBar = {
            NavigationBar(
                containerColor = colors.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == HomeTab.DOCUMENTS,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentTab = HomeTab.DOCUMENTS
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.DOCUMENTS) Icons.AutoMirrored.Filled.Article else Icons.AutoMirrored.Outlined.Article,
                            contentDescription = "Documents"
                        )
                    },
                    label = { Text("Documents", style = typography.caption) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.accent.copy(alpha = 0.16f),
                        selectedIconColor = colors.accent,
                        selectedTextColor = colors.accent,
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == HomeTab.MEDIA,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentTab = HomeTab.MEDIA
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.MEDIA) Icons.Filled.PermMedia else Icons.Outlined.PermMedia,
                            contentDescription = "Media"
                        )
                    },
                    label = { Text("Media", style = typography.caption) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.accent.copy(alpha = 0.16f),
                        selectedIconColor = colors.accent,
                        selectedTextColor = colors.accent,
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == HomeTab.TOOLS,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentTab = HomeTab.TOOLS
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = colors.accent,
                                    contentColor = colors.background
                                ) {
                                    Text("30", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == HomeTab.TOOLS) Icons.Filled.Build else Icons.Outlined.Build,
                                contentDescription = "Tools"
                            )
                        }
                    },
                    label = { Text("Tools", style = typography.caption) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.accent.copy(alpha = 0.16f),
                        selectedIconColor = colors.accent,
                        selectedTextColor = colors.accent,
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentTab == HomeTab.DOCUMENTS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        openDocumentLauncher.launch(SUPPORTED_MIME_TYPES)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text("Open", fontWeight = FontWeight.SemiBold)
                    },
                    containerColor = colors.accent,
                    contentColor = if (colors.background == DarkAppColors.background) Color(0xFF0F0F10) else Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
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
                        // 1. "Continue reading" row at very top showing last opened file and progress
                        val continueDoc = uiState.continueReadingDoc
                        if (continueDoc != null && uiState.selectedCategory == DocumentCategory.ALL) {
                            item {
                                AppSectionHeader(title = "CONTINUE READING")
                            }
                            item(key = "continue_${continueDoc.uri}") {
                                AppFileRow(
                                    document = continueDoc,
                                    showContinueReading = true,
                                    onClick = { onDocumentClick(continueDoc) },
                                    onToggleFavorite = { viewModel.toggleFavorite(continueDoc) },
                                    onRemoveFromRecents = { viewModel.removeFromRecents(continueDoc) },
                                    onShare = { onShareDocument(continueDoc) },
                                    onShowDetails = { selectedDocForDetails = continueDoc }
                                )
                            }
                        }

                        // 2. Main Documents List
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

                        // 3. Favorites Section (if on ALL and has favorites)
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
