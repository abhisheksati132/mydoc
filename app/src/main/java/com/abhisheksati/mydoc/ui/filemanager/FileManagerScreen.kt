package com.abhisheksati.mydoc.ui.filemanager

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abhisheksati.mydoc.data.local.PinnedFoldersManager
import com.abhisheksati.mydoc.data.local.StoragePermissionHelper
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentFilter
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
    var singleItemActionTarget by remember { mutableStateOf<StorageItem?>(null) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showExtractConfirmDialogForFile by remember { mutableStateOf<File?>(null) }
    var singleItemCompressTarget by remember { mutableStateOf<File?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showStorageCleanerSheet by remember { mutableStateOf(false) }
    var showAppManagerSheet by remember { mutableStateOf(false) }
    val pinnedFoldersManager = remember { PinnedFoldersManager(context) }
    val pinnedFolders by pinnedFoldersManager.pinnedFolders.collectAsState()

    var documentFilter by remember { mutableStateOf(DocumentFilter.ALL) }

    val displayedItems = remember(uiState.filteredItems, uiState.activeCategory, documentFilter) {
        if (uiState.activeCategory == FileCategory.DOCUMENTS) {
            uiState.filteredItems.filter { documentFilter.matches(it.extension) }
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
                    shape = AppRadius.cardShape
                )
            }
        },
        topBar = {
            FileManagerTopBar(
                uiState = uiState,
                viewModel = viewModel,
                displayedItemsCount = displayedItems.size,
                documentFilter = documentFilter,
                isHubMode = isHubMode,
                isSearchActive = isSearchActive,
                showSortMenu = showSortMenu,
                onToggleSearch = { isSearchActive = !isSearchActive },
                onToggleSortMenu = { showSortMenu = it },
                onDocumentFilterChange = { documentFilter = it },
                onShowNewFolderDialog = { showNewFolderDialog = true },
                onShowNewFileDialog = { showNewFileDialog = true },
                onShowDeleteConfirmDialog = { showDeleteConfirmDialog = true },
                onShowCompressDialog = { showCompressDialog = true },
                onRenameSingleSelected = { showRenameDialogForFile = it },
                onToggleTheme = onToggleTheme,
                onSettingsClick = onSettingsClick
            )
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
                    if (!hasPermission) {
                        StoragePermissionBanner(
                            onRequestPermission = {
                                StoragePermissionHelper.launchManageStorageSettings(context)
                            }
                        )
                    }

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            LinearProgressIndicator(
                                color = colors.ink,
                                trackColor = colors.track,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(AppSpacing.rule)
                            )
                        }
                    } else if (displayedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            val category = uiState.activeCategory
                            val emptyTitle: String
                            val emptyMessage: String
                            when {
                                uiState.searchQuery.isNotEmpty() -> {
                                    emptyTitle = "No matches found"
                                    emptyMessage = "Try a different search term"
                                }
                                category == FileCategory.DOCUMENTS && documentFilter != DocumentFilter.ALL -> {
                                    emptyTitle = "No ${documentFilter.label} documents"
                                    emptyMessage = "No ${documentFilter.label} documents on this device"
                                }
                                category != null -> {
                                    emptyTitle = "No ${category.noun} here"
                                    emptyMessage = if (category == FileCategory.DOCUMENTS) {
                                        "Documents you open or scan will appear here"
                                    } else {
                                        "Files you add to ${category.displayName} will appear here"
                                    }
                                }
                                else -> {
                                    emptyTitle = "This folder is empty"
                                    emptyMessage = "Use the menu above to create a folder or file"
                                }
                            }
                            AppEmptyState(title = emptyTitle, message = emptyMessage)
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
            FileManagerClipboardBar(
                clipboardState = uiState.clipboardState,
                onCancel = { viewModel.cancelClipboard() },
                onPaste = { viewModel.pasteClipboard() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
            )
        }
    }

    // Overlays, dialogs, sheets
    FileManagerOverlays(
        viewModel = viewModel,
        storageViewModel = storageViewModel,
        uiState = uiState,
        pinnedFoldersManager = pinnedFoldersManager,
        showNewFolderDialog = showNewFolderDialog,
        onDismissNewFolder = { showNewFolderDialog = false },
        showNewFileDialog = showNewFileDialog,
        onDismissNewFile = { showNewFileDialog = false },
        showRenameDialogForFile = showRenameDialogForFile,
        onDismissRename = { showRenameDialogForFile = null },
        showDeleteConfirmDialog = showDeleteConfirmDialog,
        onDismissDeleteConfirm = { showDeleteConfirmDialog = false },
        showCompressDialog = showCompressDialog,
        onDismissCompress = { showCompressDialog = false },
        singleItemCompressTarget = singleItemCompressTarget,
        onDismissSingleCompress = { singleItemCompressTarget = null },
        showExtractConfirmDialogForFile = showExtractConfirmDialogForFile,
        onDismissExtractConfirm = { showExtractConfirmDialogForFile = null },
        showStorageCleanerSheet = showStorageCleanerSheet,
        onDismissStorageCleaner = { showStorageCleanerSheet = false },
        showAppManagerSheet = showAppManagerSheet,
        onDismissAppManager = { showAppManagerSheet = false },
        singleItemActionTarget = singleItemActionTarget,
        onDismissSingleItemAction = { singleItemActionTarget = null },
        onRequestRename = { file -> showRenameDialogForFile = file },
        onOpenFile = onOpenFile
    )
}
