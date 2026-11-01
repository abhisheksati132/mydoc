package com.abhisheksati.mydoc.ui.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.foundation.layout.statusBarsPadding
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.data.local.StoragePermissionHelper
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.ui.storage.StorageViewModel
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    storageViewModel: StorageViewModel,
    onOpenFile: (File) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val storageUiState by storageViewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(StoragePermissionHelper.hasStoragePermission(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = StoragePermissionHelper.hasStoragePermission(context)
                if (granted != hasPermission) {
                    hasPermission = granted
                    if (granted) {
                        viewModel.refreshVolumes()
                        storageViewModel.refresh()
                        if (viewModel.uiState.value.isBrowsingStorage) {
                            viewModel.loadDirectory(viewModel.uiState.value.currentDirectory, addToHistory = false)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    // Dialog & menu state
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialogForFile by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var singleItemActionTarget by remember { mutableStateOf<StorageItem?>(null) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showExtractConfirmDialogForFile by remember { mutableStateOf<File?>(null) }
    var singleItemCompressTarget by remember { mutableStateOf<File?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showStorageCleanerSheet by remember { mutableStateOf(false) }
    var showAppManagerSheet by remember { mutableStateOf(false) }
    var isSelectionMenuOpen by remember { mutableStateOf(false) }
    val pinnedFoldersManager = remember { com.abhisheksati.mydoc.data.local.PinnedFoldersManager(context) }
    val pinnedFolders by pinnedFoldersManager.pinnedFolders.collectAsState()

    var docSubCategoryFilter by remember { mutableStateOf("All") }

    val displayedItems = remember(uiState.filteredItems, uiState.activeCategory, docSubCategoryFilter) {
        if (uiState.activeCategory == FileCategory.DOCUMENTS) {
            when (docSubCategoryFilter) {
                "PDF" -> uiState.filteredItems.filter { it.extension == "pdf" }
                "Word" -> uiState.filteredItems.filter { it.extension in setOf("doc", "docx", "dot", "dotx", "odt", "rtf") }
                "Excel" -> uiState.filteredItems.filter { it.extension in setOf("xls", "xlsx", "xlsm", "csv", "ods") }
                "PPT" -> uiState.filteredItems.filter { it.extension in setOf("ppt", "pptx", "pps", "odp") }
                "Text" -> uiState.filteredItems.filter { it.extension in setOf("txt", "log", "md", "json", "xml") }
                else -> uiState.filteredItems
            }
        } else {
            uiState.filteredItems
        }
    }

    val isHubMode = !uiState.isBrowsingStorage && uiState.activeCategory == null && !isSearchActive

    // User message toasts
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Undo Trash snackbar
    LaunchedEffect(uiState.lastTrashedItems) {
        val trashedItems = uiState.lastTrashedItems
        if (trashedItems.isNotEmpty()) {
            val count = trashedItems.size
            viewModel.clearLastTrashedItems()
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = if (count == 1) "Moved 1 item to Recycle Bin" else "Moved $count items to Recycle Bin",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastTrash()
            }
        }
    }

    // Intercept back presses only when Files tab is active
    BackHandler(enabled = isActive && (uiState.isTrashOpen || uiState.isSelectionMode || isSearchActive || uiState.isBrowsingStorage || uiState.activeCategory != null)) {
        if (uiState.isTrashOpen) {
            viewModel.closeTrash()
        } else if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            viewModel.setSearchQuery("")
        } else if (uiState.activeCategory != null) {
            viewModel.selectCategory(null)
        } else if (uiState.isBrowsingStorage) {
            viewModel.navigateUp()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = colors.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
            ) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = colors.surfaceVariant,
                    contentColor = colors.textPrimary,
                    actionColor = colors.accent,
                    shape = RoundedCornerShape(AppSpacing.cornerRadius)
                )
            }
        },
        topBar = {
            AnimatedContent(
                targetState = uiState.isSelectionMode,
                transitionSpec = {
                    (slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow))).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ) + fadeOut()
                    )
                },
                label = "FileManagerTopBar"
            ) { isSelection ->
                if (isSelection) {
                    // Multi-selection Top Bar
                Surface(
                    color = colors.background,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.textPrimary)
                            }
                            Text(
                                text = "${uiState.selectedFiles.size} selected",
                                style = typography.title.copy(fontSize = 16.sp),
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 1. Share
                            IconButton(onClick = {
                                shareFiles(context, uiState.selectedFiles.toList())
                            }) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = colors.textPrimary)
                            }

                            // 2. Move (Cut)
                            IconButton(onClick = { viewModel.cutSelected() }) {
                                Icon(Icons.Outlined.ContentCut, contentDescription = "Move", tint = colors.textPrimary)
                            }

                            // 3. Delete
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = colors.error)
                            }

                            // 4. Overflow Menu (Safe 48dp touch targets)
                            Box {
                                IconButton(onClick = { isSelectionMenuOpen = true }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "More actions", tint = colors.textPrimary)
                                }

                                DropdownMenu(
                                    expanded = isSelectionMenuOpen,
                                    onDismissRequest = { isSelectionMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Select All", style = typography.body) },
                                        leadingIcon = { Icon(Icons.Outlined.SelectAll, contentDescription = null, tint = colors.accent) },
                                        onClick = {
                                            isSelectionMenuOpen = false
                                            viewModel.selectAll()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Copy", style = typography.body) },
                                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = colors.accent) },
                                        onClick = {
                                            isSelectionMenuOpen = false
                                            viewModel.copySelected()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Compress to ZIP", style = typography.body) },
                                        leadingIcon = { Icon(Icons.Outlined.FolderZip, contentDescription = null, tint = colors.accent) },
                                        onClick = {
                                            isSelectionMenuOpen = false
                                            showCompressDialog = true
                                        }
                                    )
                                    if (uiState.selectedFiles.size == 1) {
                                        DropdownMenuItem(
                                            text = { Text("Rename", style = typography.body) },
                                            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null, tint = colors.accent) },
                                            onClick = {
                                                isSelectionMenuOpen = false
                                                showRenameDialogForFile = uiState.selectedFiles.first()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                Box {
                    AppTabHeader(
                        title = when {
                            uiState.activeCategory != null -> uiState.activeCategory!!.displayName
                            uiState.isBrowsingStorage -> {
                                val isAtRoot = uiState.storageVolumes.any { it.path == uiState.currentDirectory.absolutePath } ||
                                        uiState.currentDirectory.absolutePath == "/storage/emulated/0"
                                if (isAtRoot) "Internal Storage" else uiState.currentDirectory.name
                            }
                            else -> "Files"
                        },
                        navigationIcon = if (uiState.activeCategory != null || uiState.isBrowsingStorage) {
                            {
                                IconButton(onClick = {
                                    if (uiState.activeCategory != null) {
                                        viewModel.selectCategory(null)
                                    } else {
                                        viewModel.navigateUp()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back",
                                        tint = colors.textPrimary
                                    )
                                }
                            }
                        } else null,
                        onSearchClick = { isSearchActive = !isSearchActive },
                        isSearchActive = isSearchActive,
                        searchContent = {
                            AppSearchField(
                                query = uiState.searchQuery,
                                onQueryChanged = { viewModel.setSearchQuery(it) },
                                placeholder = "Search files..."
                            )
                        },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        onSettingsClick = onSettingsClick,
                        moreMenuContent = { onDismiss ->
                            DropdownMenuItem(
                                text = { Text(if (uiState.isGridView) "Switch to List View" else "Switch to Grid View", style = typography.body, color = colors.textPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (uiState.isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                        contentDescription = null,
                                        tint = colors.accent
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    viewModel.toggleViewMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort files...", style = typography.body, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, tint = colors.accent) },
                                onClick = {
                                    onDismiss()
                                    showSortMenu = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.showHiddenFiles) "Hide hidden files" else "Show hidden files",
                                        style = typography.body,
                                        color = colors.textPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (uiState.showHiddenFiles) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = colors.accent
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    viewModel.toggleHiddenFiles()
                                }
                            )

                            HorizontalDivider(color = colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("New Folder", style = typography.body, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = colors.accent) },
                                onClick = {
                                    onDismiss()
                                    showNewFolderDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New File", style = typography.body, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null, tint = colors.accent) },
                                onClick = {
                                    onDismiss()
                                    showNewFileDialog = true
                                }
                            )

                            HorizontalDivider(color = colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text(if (uiState.trashItems.isNotEmpty()) "Recycle Bin (${uiState.trashItems.size})" else "Recycle Bin", style = typography.body, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.accent) },
                                onClick = {
                                    onDismiss()
                                    viewModel.openTrash()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Refresh", style = typography.body, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null, tint = colors.textSecondary) },
                                onClick = {
                                    onDismiss()
                                    viewModel.loadDirectory(uiState.currentDirectory, addToHistory = false)
                                    viewModel.refreshVolumes()
                                }
                            )
                        },
                        bottomContent = if (!isHubMode) {
                            {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Fast Categories Chips
                                    FileManagerCategoryChips(
                                        activeCategory = uiState.activeCategory,
                                        onCategorySelected = { category ->
                                            docSubCategoryFilter = "All"
                                            if (uiState.activeCategory == category) {
                                                viewModel.selectCategory(null)
                                            } else {
                                                viewModel.selectCategory(category)
                                            }
                                        }
                                    )

                                    // Breadcrumb Trail or Category Banner
                                    if (uiState.activeCategory != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Category: ${uiState.activeCategory?.displayName}",
                                                    style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                    color = colors.accent
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${displayedItems.size} items)",
                                                    style = typography.caption.copy(fontSize = 12.sp),
                                                    color = colors.textSecondary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    docSubCategoryFilter = "All"
                                                    viewModel.selectCategory(null)
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Exit Category",
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Sub-category filters for documents (PDF, Word, Excel, PPT, Text)
                                        if (uiState.activeCategory == FileCategory.DOCUMENTS) {
                                            val docFilters = listOf("All", "PDF", "Word", "Excel", "PPT", "Text")
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                docFilters.forEach { label ->
                                                    val isSelected = docSubCategoryFilter == label
                                                    val chipBg by animateColorAsState(
                                                        targetValue = if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surfaceContainer,
                                                        animationSpec = tween(durationMillis = 200),
                                                        label = "fileFilterBg"
                                                    )
                                                    val chipText by animateColorAsState(
                                                        targetValue = if (isSelected) colors.accent else colors.textSecondary,
                                                        animationSpec = tween(durationMillis = 200),
                                                        label = "fileFilterText"
                                                    )

                                                    Surface(
                                                        shape = AppRadius.pillShape,
                                                        color = chipBg,
                                                        border = BorderStroke(0.5.dp, if (isSelected) colors.accent.copy(alpha = 0.35f) else colors.line),
                                                        modifier = Modifier
                                                            .clip(AppRadius.pillShape)
                                                            .pressScaleEffect(onClick = { docSubCategoryFilter = label })
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = typography.caption.copy(
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = 12.sp
                                                            ),
                                                            color = chipText,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            uiState.breadcrumbTrail.forEachIndexed { index, crumb ->
                                                val isLast = index == uiState.breadcrumbTrail.size - 1
                                                val volumeForCrumb = uiState.storageVolumes.firstOrNull { it.path == crumb.absolutePath }
                                                val crumbName = when {
                                                    volumeForCrumb != null -> volumeForCrumb.title
                                                    crumb.absolutePath == "/storage/emulated/0" -> "Internal Storage"
                                                    index == 0 -> "Storage"
                                                    else -> crumb.name
                                                }

                                                Text(
                                                    text = crumbName,
                                                    style = typography.body.copy(
                                                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = if (isLast) colors.accent else colors.textSecondary,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { viewModel.navigateToCrumb(crumb) }
                                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                                )

                                                if (!isLast) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.ChevronRight,
                                                        contentDescription = null,
                                                        tint = colors.divider,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else null
                    )

                    SortOrderDropdownMenu(
                        expanded = showSortMenu,
                        currentSort = uiState.sortOrder,
                        onDismiss = { showSortMenu = false },
                        onSortSelected = { viewModel.toggleSort(it) }
                    )
                }
            }
        }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isHubMode) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!hasPermission) {
                        StoragePermissionBanner(
                            onRequestPermission = {
                                StoragePermissionHelper.launchManageStorageSettings(context)
                            }
                        )
                    }

                    GoogleFilesHubContent(
                        storageUiState = storageUiState,
                        onBrowseInternalStorage = {
                            viewModel.startBrowsingStorage()
                        },
                        onBrowseStorageVolume = { volume ->
                            viewModel.startBrowsingStorage(volume.file)
                        },
                        onCategoryClick = { category ->
                            viewModel.selectCategory(category)
                        },
                        onAppsClick = {
                            showAppManagerSheet = true
                        },
                        onCleanClick = {
                            showStorageCleanerSheet = true
                        },
                        onOpenTrash = {
                            viewModel.openTrash()
                        },
                        pinnedFolders = pinnedFolders,
                        onFolderClick = { folder ->
                            viewModel.startBrowsingStorage(folder)
                        },
                        trashCount = uiState.trashItems.size,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Storage Permission Missing Warning
                    if (!hasPermission) {
                        StoragePermissionBanner(
                            onRequestPermission = {
                                StoragePermissionHelper.launchManageStorageSettings(context)
                            }
                        )
                    }

                    // File List / Grid Content
                if (uiState.isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                } else if (displayedItems.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AppEmptyState(
                            title = if (uiState.searchQuery.isNotEmpty()) "No matches found"
                                    else if (uiState.activeCategory != null) {
                                        if (uiState.activeCategory == FileCategory.DOCUMENTS && docSubCategoryFilter != "All") {
                                            "No $docSubCategoryFilter documents"
                                        } else {
                                            "No files found"
                                        }
                                    } else "This folder is empty",
                            message = if (uiState.searchQuery.isNotEmpty()) "Try a different search term"
                                      else if (uiState.activeCategory != null) {
                                        if (uiState.activeCategory == FileCategory.DOCUMENTS && docSubCategoryFilter != "All") {
                                            "No $docSubCategoryFilter files found on this device"
                                        } else {
                                            "No ${uiState.activeCategory?.displayName?.lowercase()} files on this device"
                                        }
                                      } else "Tap '+' to create a new folder or file"
                        )
                    }
                } else {
                    if (uiState.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 104.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(
                                items = displayedItems,
                                key = { it.path },
                                contentType = { if (it.isDirectory) "dir_grid" else "file_grid" }
                            ) { item ->
                                val isSelected = uiState.selectedFiles.contains(item.file)
                                StorageItemGrid(
                                    item = item,
                                    modifier = Modifier.animateItem(),
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelection(item.file)
                                        } else {
                                            if (item.isDirectory) {
                                                viewModel.loadDirectory(item.file)
                                            } else if (item.fileType == StorageFileType.ARCHIVE || item.name.endsWith(".zip", ignoreCase = true)) {
                                                singleItemActionTarget = item
                                            } else {
                                                onOpenFile(item.file)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(item.file)
                                    },
                                    onMoreClick = {
                                        singleItemActionTarget = item
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 6.dp, bottom = 104.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(
                                items = displayedItems,
                                key = { it.path },
                                contentType = { if (it.isDirectory) "dir_row" else "file_row" }
                            ) { item ->
                                val isSelected = uiState.selectedFiles.contains(item.file)
                                StorageItemRow(
                                    item = item,
                                    modifier = Modifier.animateItem(),
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelection(item.file)
                                        } else {
                                            if (item.isDirectory) {
                                                viewModel.loadDirectory(item.file)
                                            } else if (item.fileType == StorageFileType.ARCHIVE || item.name.endsWith(".zip", ignoreCase = true)) {
                                                singleItemActionTarget = item
                                            } else {
                                                onOpenFile(item.file)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(item.file)
                                    },
                                    onMoreClick = {
                                        singleItemActionTarget = item
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

            // Floating Clipboard Action Bar (Paste / Cancel)
            AnimatedVisibility(
                visible = uiState.clipboardState != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
            ) {
                uiState.clipboardState?.let { clip ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (clip.operation == ClipboardOperation.COPY) Icons.Outlined.ContentCopy else Icons.Outlined.ContentCut,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${clip.items.size} items ready to ${if (clip.operation == ClipboardOperation.COPY) "copy" else "move"}",
                                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Navigate to destination folder",
                                        style = typography.caption.copy(fontSize = 11.sp),
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { viewModel.cancelClipboard() }) {
                                    Text("Cancel", color = colors.textSecondary)
                                }
                                Button(
                                    onClick = { viewModel.pasteClipboard() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                                ) {
                                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paste")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                showNewFolderDialog = false
                viewModel.createNewFolder(name)
            }
        )
    }

    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = { showNewFileDialog = false },
            onConfirm = { name ->
                showNewFileDialog = false
                viewModel.createNewFile(name)
            }
        )
    }

    showRenameDialogForFile?.let { file ->
        RenameDialog(
            file = file,
            onDismiss = { showRenameDialogForFile = null },
            onConfirm = { newName ->
                showRenameDialogForFile = null
                viewModel.renameItem(file, newName)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            count = uiState.selectedFiles.size,
            fileNamePreview = uiState.selectedFiles.firstOrNull()?.name,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.moveToTrashSelected()
            }
        )
    }

    uiState.selectedProperties?.let { props ->
        FilePropertiesDialog(
            properties = props,
            checksum = uiState.calculatedChecksum,
            onDismiss = { viewModel.dismissPropertiesDialog() }
        )
    }

    if (showCompressDialog) {
        val defaultName = if (uiState.selectedFiles.size == 1) {
            "${uiState.selectedFiles.first().nameWithoutExtension}.zip"
        } else "Archive.zip"
        CompressDialog(
            defaultName = defaultName,
            onDismiss = { showCompressDialog = false },
            onConfirm = { name ->
                showCompressDialog = false
                viewModel.compressSelected(name)
            }
        )
    }

    singleItemCompressTarget?.let { file ->
        CompressDialog(
            defaultName = "${file.nameWithoutExtension}.zip",
            onDismiss = { singleItemCompressTarget = null },
            onConfirm = { name ->
                singleItemCompressTarget = null
                viewModel.compressSingle(file, name)
            }
        )
    }

    showExtractConfirmDialogForFile?.let { file ->
        ExtractConfirmDialog(
            archiveName = file.name,
            onDismiss = { showExtractConfirmDialogForFile = null },
            onConfirm = {
                showExtractConfirmDialogForFile = null
                viewModel.extractArchive(file)
            }
        )
    }

    if (showStorageCleanerSheet) {
        com.abhisheksati.mydoc.ui.cleaner.StorageCleanerSheet(
            rootDir = android.os.Environment.getExternalStorageDirectory(),
            onDismiss = {
                showStorageCleanerSheet = false
                viewModel.refreshVolumes()
                storageViewModel.refresh()
                if (uiState.isBrowsingStorage) {
                    viewModel.loadDirectory(uiState.currentDirectory, addToHistory = false)
                }
            },
            onFileClick = { file ->
                showStorageCleanerSheet = false
                onOpenFile(file)
            }
        )
    }

    if (showAppManagerSheet) {
        com.abhisheksati.mydoc.ui.appmanager.AppManagerSheet(
            onDismiss = {
                showAppManagerSheet = false
                viewModel.refreshVolumes()
                storageViewModel.refresh()
            }
        )
    }

    if (uiState.isTrashOpen) {
        com.abhisheksati.mydoc.ui.filemanager.TrashBinSheet(
            trashItems = uiState.trashItems,
            onRestore = { items ->
                viewModel.restoreTrashItems(items)
            },
            onDeletePermanently = { items ->
                viewModel.permanentlyDeleteTrashItems(items)
            },
            onEmptyTrash = {
                viewModel.emptyTrash()
            },
            onClose = {
                viewModel.closeTrash()
            }
        )
    }

    // Single item context action bottom sheet
    singleItemActionTarget?.let { item ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { singleItemActionTarget = null },
            sheetState = sheetState,
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = getFileTypeIcon(item.fileType)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tint.copy(alpha = 0.15f))
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = typography.title.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.formattedSize,
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colors.divider)
                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                ItemActionOption(
                    title = "Open",
                    icon = Icons.Outlined.FolderOpen,
                    onClick = {
                        singleItemActionTarget = null
                        if (item.isDirectory) {
                            viewModel.loadDirectory(item.file)
                        } else {
                            onOpenFile(item.file)
                        }
                    }
                )

                ItemActionOption(
                    title = "Copy",
                    icon = Icons.Outlined.ContentCopy,
                    onClick = {
                        singleItemActionTarget = null
                        viewModel.toggleSelection(item.file)
                        viewModel.copySelected()
                    }
                )

                ItemActionOption(
                    title = "Move (Cut)",
                    icon = Icons.Outlined.ContentCut,
                    onClick = {
                        singleItemActionTarget = null
                        viewModel.toggleSelection(item.file)
                        viewModel.cutSelected()
                    }
                )

                ItemActionOption(
                    title = "Rename",
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    onClick = {
                        singleItemActionTarget = null
                        showRenameDialogForFile = item.file
                    }
                )

                ItemActionOption(
                    title = "Move to Recycle Bin",
                    icon = Icons.Outlined.Delete,
                    tint = colors.error,
                    onClick = {
                        singleItemActionTarget = null
                        viewModel.moveToTrashSingle(item.file)
                    }
                )

                if (item.isDirectory) {
                    val isPinned = pinnedFoldersManager.isPinned(item.file)
                    ItemActionOption(
                        title = if (isPinned) "Unpin from Quick Access" else "Pin to Quick Access",
                        icon = if (isPinned) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                        tint = colors.accent,
                        onClick = {
                            singleItemActionTarget = null
                            pinnedFoldersManager.togglePin(item.file)
                        }
                    )
                }

                val isZip = item.name.endsWith(".zip", ignoreCase = true) || item.name.endsWith(".jar", ignoreCase = true)
                if (isZip) {
                    ItemActionOption(
                        title = "Extract ZIP Archive",
                        icon = Icons.Outlined.FolderZip,
                        tint = colors.accent,
                        onClick = {
                            singleItemActionTarget = null
                            showExtractConfirmDialogForFile = item.file
                        }
                    )
                }

                ItemActionOption(
                    title = "Compress to ZIP",
                    icon = Icons.Outlined.Archive,
                    onClick = {
                        singleItemActionTarget = null
                        singleItemCompressTarget = item.file
                    }
                )

                ItemActionOption(
                    title = "Details & Checksum",
                    icon = Icons.Outlined.Info,
                    onClick = {
                        singleItemActionTarget = null
                        viewModel.inspectItem(item.file)
                    }
                )

                if (!item.isDirectory) {
                    ItemActionOption(
                        title = "Share",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            singleItemActionTarget = null
                            shareFiles(context, listOf(item.file))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ItemActionOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = LocalAppColors.current.textPrimary,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = typography.body.copy(fontSize = 14.sp),
            color = tint
        )
    }
}

