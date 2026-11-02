package com.abhisheksati.mydoc.ui.files

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abhisheksati.mydoc.data.local.PinnedFoldersManager
import com.abhisheksati.mydoc.data.local.StoragePermissionHelper
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.filemanager.FileManagerClipboardBar
import com.abhisheksati.mydoc.ui.filemanager.FileManagerOverlays
import com.abhisheksati.mydoc.ui.filemanager.StoragePermissionBanner
import com.abhisheksati.mydoc.ui.filemanager.shareFiles
import com.abhisheksati.mydoc.ui.storage.StorageViewModel
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import java.io.File

private enum class FilesPane { RECENTS, BROWSE }

/**
 * Files-first home: Recents and Browse share one header, one storage readout
 * and one list/grid choice. Folder browsing keeps the existing FileManager
 * engine; this screen is the Material 3 surface on top of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FileManagerViewModel,
    homeViewModel: HomeViewModel,
    storageViewModel: StorageViewModel,
    onOpenFile: (File) -> Unit,
    onLibraryItemClick: (LibraryItem) -> Unit,
    onLibraryItemDetails: (LibraryItem) -> Unit,
    onSettingsClick: () -> Unit,
    onSpotlightSearch: () -> Unit,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val storageUiState by storageViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // The file manager view model outlives this screen (opening a document
    // replaces the whole tree), so resume on the pane the user was in.
    var paneIndex by rememberSaveable {
        mutableIntStateOf(
            if (uiState.isBrowsingStorage || uiState.activeCategory != null) {
                FilesPane.BROWSE.ordinal
            } else {
                FilesPane.RECENTS.ordinal
            }
        )
    }
    val pane = FilesPane.entries[paneIndex]

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<StorageItem?>(null) }
    var showCleaner by remember { mutableStateOf(false) }
    var showAppManager by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }

    val pinnedFoldersManager = remember { PinnedFoldersManager(context) }
    val pinnedFolders by pinnedFoldersManager.pinnedFolders.collectAsState()

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
                            viewModel.loadDirectory(
                                viewModel.uiState.value.currentDirectory,
                                addToHistory = false
                            )
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val insideBrowse = pane == FilesPane.BROWSE &&
            (uiState.isBrowsingStorage || uiState.activeCategory != null)
    val isBrowsingFolder = uiState.isBrowsingStorage
    val atStorageRoot = uiState.storageVolumes.any { it.path == uiState.currentDirectory.absolutePath } ||
            uiState.currentDirectory.absolutePath == "/storage/emulated/0"

    val title = when {
        !insideBrowse -> "Files"
        uiState.activeCategory != null -> uiState.activeCategory!!.displayName
        atStorageRoot -> uiState.storageVolumes.firstOrNull {
            it.path == uiState.currentDirectory.absolutePath
        }?.title ?: "Internal storage"
        else -> uiState.currentDirectory.name
    }
    val subtitle = when {
        insideBrowse -> if (uiState.filteredItems.size == 1) "1 item" else "${uiState.filteredItems.size} items"
        else -> "${homeUiState.recentLibraryItems.size} recent · ${storageUiState.formattedFree} free"
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(uiState.lastTrashedItems) {
        val trashed = uiState.lastTrashedItems
        if (trashed.isNotEmpty()) {
            val count = trashed.size
            viewModel.clearLastTrashedItems()
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = if (count == 1) "Moved 1 item to Recycle Bin" else "Moved $count items to Recycle Bin",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoLastTrash()
        }
    }

    LaunchedEffect(insideBrowse) {
        if (!insideBrowse) {
            isSearchActive = false
            if (uiState.searchQuery.isNotEmpty()) viewModel.setSearchQuery("")
        }
    }

    BackHandler(
        enabled = isActive && (
                uiState.isTrashOpen ||
                        uiState.isSelectionMode ||
                        isSearchActive ||
                        insideBrowse
                )
    ) {
        when {
            uiState.isTrashOpen -> viewModel.closeTrash()
            uiState.isSelectionMode -> viewModel.clearSelection()
            isSearchActive -> {
                isSearchActive = false
                viewModel.setSearchQuery("")
            }
            else -> viewModel.navigateUp()
        }
    }

    val breadcrumbs: List<Pair<String, () -> Unit>> = when {
        !insideBrowse -> emptyList()
        uiState.activeCategory != null -> listOf(
            "Files" to { viewModel.selectCategory(null) },
            uiState.activeCategory!!.displayName to {}
        )
        else -> uiState.breadcrumbTrail.mapIndexed { index, crumb ->
            val volume = uiState.storageVolumes.firstOrNull { it.path == crumb.absolutePath }
            val label = when {
                volume != null -> volume.title
                crumb.absolutePath == "/storage/emulated/0" -> "Internal storage"
                index == 0 -> "Storage"
                else -> crumb.name
            }
            label to { viewModel.navigateToCrumb(crumb) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        floatingActionButton = {
            if (isBrowsingFolder && !uiState.isSelectionMode && uiState.clipboardState == null) {
                FloatingActionButton(
                    onClick = { showNewFolderDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "New folder")
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                if (uiState.isSelectionMode) {
                    FilesSelectionBar(
                        selectedCount = uiState.selectedFiles.size,
                        totalCount = uiState.filteredItems.size,
                        onClose = { viewModel.clearSelection() },
                        onSelectAll = { viewModel.selectAll() },
                        onShare = { shareFiles(context, uiState.selectedFiles.toList()) },
                        onCopy = { viewModel.copySelected() },
                        onCut = { viewModel.cutSelected() },
                        onRename = if (uiState.selectedFiles.size == 1) {
                            { renameTarget = uiState.selectedFiles.first() }
                        } else null,
                        onCompress = { showCompressDialog = true },
                        onDelete = { showDeleteConfirm = true }
                    )
                } else {
                    FilesHeader(
                        title = title,
                        subtitle = subtitle,
                        isInsideFolder = insideBrowse,
                        pane = pane,
                        uiState = uiState,
                        isSearchActive = isSearchActive,
                        onBack = { viewModel.navigateUp() },
                        onSearch = {
                            if (insideBrowse) {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) viewModel.setSearchQuery("")
                            } else {
                                onSpotlightSearch()
                            }
                        },
                        onSettingsClick = onSettingsClick,
                        onToggleView = { viewModel.toggleViewMode() },
                        onToggleHidden = { viewModel.toggleHiddenFiles() },
                        onNewFolder = { showNewFolderDialog = true },
                        onNewFile = { showNewFileDialog = true },
                        onOpenTrash = { viewModel.openTrash() },
                        onRefresh = {
                            viewModel.loadDirectory(uiState.currentDirectory, addToHistory = false)
                            viewModel.refreshVolumes()
                            storageViewModel.refresh()
                        }
                    )

                    if (!insideBrowse) {
                        PrimaryTabRow(
                            selectedTabIndex = paneIndex,
                            containerColor = MaterialTheme.colorScheme.background
                        ) {
                            Tab(
                                selected = pane == FilesPane.RECENTS,
                                onClick = {
                                    paneIndex = FilesPane.RECENTS.ordinal
                                    isSearchActive = false
                                    if (uiState.searchQuery.isNotEmpty()) viewModel.setSearchQuery("")
                                },
                                text = { Text("Recents") }
                            )
                            Tab(
                                selected = pane == FilesPane.BROWSE,
                                onClick = { paneIndex = FilesPane.BROWSE.ordinal },
                                text = { Text("Browse") }
                            )
                        }
                    }

                    if (isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search this folder") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (breadcrumbs.size > 1) {
                        BreadcrumbRow(crumbs = breadcrumbs)
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
            Column(modifier = Modifier.fillMaxSize()) {
                if (!hasPermission) {
                    StoragePermissionBanner(
                        onRequestPermission = {
                            StoragePermissionHelper.launchManageStorageSettings(context)
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = pane,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "filesPane",
                    modifier = Modifier.fillMaxSize()
                ) { activePane ->
                    when (activePane) {
                        FilesPane.RECENTS -> RecentsPane(
                            recentItems = homeUiState.recentLibraryItems,
                            storageUiState = storageUiState,
                            pinnedFolders = pinnedFolders,
                            trashCount = uiState.trashItems.size,
                            onOpenItem = onLibraryItemClick,
                            onItemDetails = onLibraryItemDetails,
                            onBrowse = {
                                viewModel.startBrowsingStorage()
                                paneIndex = FilesPane.BROWSE.ordinal
                            },
                            onCategory = { category ->
                                viewModel.selectCategory(category)
                                paneIndex = FilesPane.BROWSE.ordinal
                            },
                            onFolder = { folder ->
                                viewModel.startBrowsingStorage(folder)
                                paneIndex = FilesPane.BROWSE.ordinal
                            },
                            onOpenTrash = { viewModel.openTrash() },
                            onInsights = { showInsights = true },
                            onClean = { showCleaner = true },
                            modifier = Modifier.fillMaxSize()
                        )

                        FilesPane.BROWSE -> {
                            if (insideBrowse) {
                                DirectoryPane(
                                    uiState = uiState,
                                    onOpenItem = { item ->
                                        when {
                                            item.isDirectory -> viewModel.loadDirectory(item.file)
                                            item.fileType == StorageFileType.ARCHIVE ||
                                                    item.name.endsWith(".zip", ignoreCase = true) -> {
                                                actionTarget = item
                                            }
                                            else -> onOpenFile(item.file)
                                        }
                                    },
                                    onMoreItem = { actionTarget = it },
                                    onToggleSelection = { viewModel.toggleSelection(it.file) },
                                    onToggleView = { viewModel.toggleViewMode() },
                                    onSort = { field: FileSortField -> viewModel.toggleSort(field) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                BrowseHomePane(
                                    storageUiState = storageUiState,
                                    volumes = storageUiState.volumes.ifEmpty { uiState.storageVolumes },
                                    pinnedFolders = pinnedFolders,
                                    trashCount = uiState.trashItems.size,
                                    onBrowseInternal = {
                                        viewModel.startBrowsingStorage()
                                    },
                                    onBrowseVolume = { volume: StorageVolume ->
                                        viewModel.startBrowsingStorage(volume.file)
                                    },
                                    onCategory = { category -> viewModel.selectCategory(category) },
                                    onFolder = { folder -> viewModel.startBrowsingStorage(folder) },
                                    onOpenTrash = { viewModel.openTrash() },
                                    onApps = { showAppManager = true },
                                    onInsights = { showInsights = true },
                                    onClean = { showCleaner = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                }
            }

            // Category views aggregate files from many folders, so there is no
            // single destination to paste into; only offer it while browsing.
            if (isBrowsingFolder) {
                FileManagerClipboardBar(
                    clipboardState = uiState.clipboardState,
                    onCancel = { viewModel.cancelClipboard() },
                    onPaste = { viewModel.pasteClipboard() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }

    FileManagerOverlays(
        viewModel = viewModel,
        storageViewModel = storageViewModel,
        uiState = uiState,
        pinnedFoldersManager = pinnedFoldersManager,
        showNewFolderDialog = showNewFolderDialog,
        onDismissNewFolder = { showNewFolderDialog = false },
        showNewFileDialog = showNewFileDialog,
        onDismissNewFile = { showNewFileDialog = false },
        showRenameDialogForFile = renameTarget,
        onDismissRename = { renameTarget = null },
        showDeleteConfirmDialog = showDeleteConfirm,
        onDismissDeleteConfirm = { showDeleteConfirm = false },
        showCompressDialog = showCompressDialog,
        onDismissCompress = { showCompressDialog = false },
        singleItemCompressTarget = null,
        onDismissSingleCompress = {},
        showExtractConfirmDialogForFile = null,
        onDismissExtractConfirm = {},
        showStorageCleanerSheet = showCleaner,
        onDismissStorageCleaner = {
            showCleaner = false
            storageViewModel.refresh()
        },
        showAppManagerSheet = showAppManager,
        onDismissAppManager = { showAppManager = false },
        singleItemActionTarget = actionTarget,
        onDismissSingleItemAction = { actionTarget = null },
        onRequestRename = { renameTarget = it },
        onOpenFile = onOpenFile
    )

    if (showInsights) {
        StorageInsightsSheet(
            storageUiState = storageUiState,
            onDismiss = { showInsights = false },
            onClean = {
                showInsights = false
                showCleaner = true
            }
        )
    }
}

@Composable
private fun FilesHeader(
    title: String,
    subtitle: String,
    isInsideFolder: Boolean,
    pane: FilesPane,
    uiState: FileManagerUiState,
    isSearchActive: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleView: () -> Unit,
    onToggleHidden: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onOpenTrash: () -> Unit,
    onRefresh: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isInsideFolder) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = scheme.onSurface
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (isInsideFolder) 0.dp else 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = if (isSearchActive) scheme.primary else scheme.onSurface
            )
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = scheme.onSurface)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (pane == FilesPane.BROWSE) {
                    DropdownMenuItem(
                        text = { Text(if (uiState.isGridView) "List view" else "Grid view") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleView()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (uiState.showHiddenFiles) "Hide hidden files" else "Show hidden files") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.showHiddenFiles) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleHidden()
                        }
                    )
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text("New folder") },
                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onNewFolder()
                    }
                )
                DropdownMenuItem(
                    text = { Text("New file") },
                    leadingIcon = { Icon(Icons.Outlined.NoteAdd, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onNewFile()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            if (uiState.trashItems.isNotEmpty()) "Recycle Bin (${uiState.trashItems.size})" else "Recycle Bin"
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenTrash()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Refresh") },
                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onRefresh()
                    }
                )
            }
        }

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = scheme.onSurface)
        }
    }
}
