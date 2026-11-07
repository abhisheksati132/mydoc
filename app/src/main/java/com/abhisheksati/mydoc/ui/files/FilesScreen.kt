package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.outlined.Tab
import com.abhisheksati.mydoc.designsystem.components.showMessage
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.LocalFloatingChromeInset
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import com.abhisheksati.mydoc.data.local.PinnedFoldersManager
import com.abhisheksati.mydoc.data.local.StoragePermissionHelper
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageViewModel
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import java.io.File

/**
 * Files-first home: Recents and Browse share one header, one storage readout
 * and one list/grid choice. Folder browsing keeps the existing FileManager
 * engine; this screen is the Material 3 surface on top of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesPaneScreen(
    viewModel: FileManagerViewModel,
    homeViewModel: HomeViewModel,
    storageViewModel: StorageViewModel,
    onOpenFile: (File, List<File>?) -> Unit,
    onLibraryItemClick: (LibraryItem) -> Unit,
    onLibraryItemDetails: (LibraryItem) -> Unit,
    onSettingsClick: () -> Unit,
    onSpotlightSearch: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    onOpenInTools: ((File) -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    glassMotion: com.abhisheksati.mydoc.designsystem.components.GlassMotionState? = null,
    workspaceInsets: Boolean = false,
    onShowTransfers: () -> Unit = {},
    workspaceMenu: @Composable androidx.compose.foundation.layout.ColumnScope.(() -> Unit) -> Unit = {}
) {

    val chromeInset = LocalFloatingChromeInset.current
    val density = LocalDensity.current
    var clipboardHeight by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()
    val clipboardInset = if (uiState.isBrowsingStorage && uiState.clipboardState != null)
        with(density) { clipboardHeight.toDp() } else 0.dp
    val storageUiState by storageViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var showAdvancedSearch by remember { mutableStateOf(false) }
    var showSaveSearch by remember { mutableStateOf(false) }
    var batchRenameFiles by remember { mutableStateOf<List<File>?>(null) }
    var showRenameRecovery by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    // Resources, not LocalContext: reading a string through the context is not
    // configuration-aware, so a locale change would leave stale text in the snackbar.
    val resources = LocalResources.current

    var actionTarget by remember { mutableStateOf<StorageItem?>(null) }
    var showCleaner by remember { mutableStateOf(false) }
    var showAppManager by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }
    var showFolderInsights by remember { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<File?>(null) }
    val transfers by viewModel.explorerClipboard.transfers.state.collectAsState()
    val isTransferring = transfers.any { it.running }

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

    val insideBrowse = uiState.isBrowsingStorage || uiState.activeCategory != null
    val isBrowsingFolder = uiState.isBrowsingStorage
    val atStorageRoot = uiState.storageVolumes.any { it.path == uiState.currentDirectory.absolutePath } ||
            uiState.currentDirectory.absolutePath == "/storage/emulated/0"

    val activeCategory = uiState.activeCategory
    val title = when {
        !insideBrowse -> stringResource(R.string.files_title_library)
        activeCategory != null -> activeCategory.displayName
        atStorageRoot -> uiState.storageVolumes.firstOrNull {
            it.path == uiState.currentDirectory.absolutePath
        }?.let { stringResource(it.titleRes) } ?: stringResource(R.string.files_title_internal)
        else -> uiState.currentDirectory.name
    }
    val subtitle = when {
        insideBrowse -> if (uiState.filteredItems.size == 1) {
            stringResource(R.string.files_subtitle_one)
        } else {
            stringResource(R.string.files_subtitle_many, uiState.filteredItems.size)
        }
        else -> stringResource(R.string.files_browse_subtitle)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val messageScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            val message = it.resolve(context)
            // SnackbarHostState queues calls. This scope survives consuming the message key,
            // while disposal of this pane cancels messages belonging to that pane.
            if (isActive) messageScope.launch { snackbarHostState.showSnackbar(message, withDismissAction = true) }
            else context.showMessage(message)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(uiState.lastTrashedItems) {
        val trashed = uiState.lastTrashedItems
        if (trashed.isNotEmpty()) {
            val count = trashed.size
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = if (count == 1) resources.getString(R.string.files_moved_one) else resources.getString(R.string.files_moved_many, count),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Short
            )
            // Cleared only after the prompt is answered. Clearing it first changed this effect's
            // own key, which cancelled the effect - and with it the Undo that had just appeared.
            viewModel.clearLastTrashedItems()
            if (result == SnackbarResult.ActionPerformed) viewModel.undoTrash(trashed)
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
        activeCategory != null -> listOf(
            stringResource(R.string.files_crumb_files) to { viewModel.selectCategory(null) },
            activeCategory.displayName to {}
        )
        else -> uiState.breadcrumbTrail.mapIndexed { index, crumb ->
            val volume = uiState.storageVolumes.firstOrNull { it.path == crumb.absolutePath }
            val label = when {
                volume != null -> stringResource(volume.titleRes)
                crumb.absolutePath == "/storage/emulated/0" -> stringResource(R.string.files_title_internal)
                index == 0 -> stringResource(R.string.files_crumb_storage)
                else -> crumb.name
            }
            label to { viewModel.navigateToCrumb(crumb) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.isSelectionMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .then(if (workspaceInsets) Modifier else Modifier.statusBarsPadding())
                    ) {
                        FilesSelectionBar(
                            selectedCount = uiState.selectedFiles.size,
                            totalCount = uiState.filteredItems.size,
                            onClose = { viewModel.clearSelection() },
                            onSelectAll = { viewModel.selectAll() },
                            onShare = { shareFiles(context, uiState.selectedFiles.toList()) },
                            onCopy = { viewModel.copySelected() },
                            onCut = { viewModel.cutSelected() },
                            onRename = if (uiState.selectedFiles.isNotEmpty()) {
                                {
                                    if (uiState.selectedFiles.size == 1) renameTarget = uiState.selectedFiles.first()
                                    else batchRenameFiles = uiState.selectedFiles.toList()
                                }
                            } else null,
                            onCompress = { showCompressDialog = true },
                            onDelete = { showDeleteConfirm = true }
                        )
                    }
                } else {
                    FilesHeader(
                            title = title,
                            subtitle = subtitle,
                            isInsideFolder = insideBrowse,
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
                            workspaceInsets = workspaceInsets,
                            workspaceMenu = workspaceMenu,
                            onAdvancedSearch = { showAdvancedSearch = true },
                            onFolderInsights = { showFolderInsights = true },
                            onResetFolderView = viewModel::resetFolderView,
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

                        if (isSearchActive) {
                            com.abhisheksati.mydoc.ui.search.FileSearchField(
                                query = uiState.searchQuery, onQueryChange = viewModel::setSearchQuery,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            Text(stringResource(when {
                                uiState.advancedSearch?.recursive == true -> R.string.search_folder_recursive_scope
                                isBrowsingFolder -> R.string.search_folder_scope
                                else -> R.string.search_category_scope
                            }, title),
                                modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.labelMedium)
                            androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                androidx.compose.material3.TextButton(onClick = { showAdvancedSearch = true }) { Text(stringResource(R.string.explorer_advanced_search)) }
                                androidx.compose.material3.TextButton(onClick = onSpotlightSearch) { Text(stringResource(R.string.search_indexed_files)) }
                            }
                        }

                        if (uiState.advancedSearch != null) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.explorer_search_scope, uiState.currentDirectory.name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                androidx.compose.material3.TextButton(onClick = { showSaveSearch = true }) { Text(stringResource(R.string.saved_search_save)) }
                                androidx.compose.material3.TextButton(onClick = { viewModel.clearAdvancedSearch() }) { Text(stringResource(R.string.explorer_clear_search)) }
                            }
                            if (uiState.searchIncomplete) Text(stringResource(R.string.explorer_search_incomplete), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        if (uiState.isLoading) androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        if (uiState.renameRecovery.isNotEmpty()) androidx.compose.material3.TextButton(onClick = { showRenameRecovery = true }) {
                            Text(stringResource(R.string.explorer_recovery_title), color = MaterialTheme.colorScheme.error)
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
            val listInset = chromeInset + clipboardInset
            CompositionLocalProvider(LocalFloatingChromeInset provides listInset) {
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
                            targetState = insideBrowse,
                            transitionSpec = {
                                val forward = targetState
                                AppMotion.peerEnter(forward) togetherWith AppMotion.peerExit(forward)
                            },
                            label = "libraryPane",
                            modifier = Modifier.fillMaxSize()
                        ) { isInside ->
                            if (isInside) {
                                DirectoryPane(
                                    uiState = uiState,
                                    onOpenItem = { item ->
                                        when {
                                            item.isDirectory -> viewModel.loadDirectory(item.file)
                                            item.name.endsWith(".zip", ignoreCase = true) -> archiveTarget = item.file
                                            item.fileType == StorageFileType.ARCHIVE -> actionTarget = item
                                            item.fileType == StorageFileType.IMAGE -> {
                                                val siblingImages = uiState.filteredItems
                                                    .filter { it.fileType == StorageFileType.IMAGE }
                                                    .map { it.file }
                                                onOpenFile(item.file, siblingImages)
                                            }
                                            else -> onOpenFile(item.file, null)
                                        }
                                    },
                                    onMoreItem = { actionTarget = it },
                                    onToggleSelection = { viewModel.toggleSelection(it.file) },
                                    onToggleView = { viewModel.toggleViewMode() },
                                    onSort = { field: FileSortField -> viewModel.toggleSort(field) },
                                    onDocFilter = { viewModel.setDocTypeFilter(it) },
                                    modifier = Modifier.fillMaxSize(),
                                    hazeState = hazeState,
                                    glassMotion = glassMotion
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
                                    onSearch = onSpotlightSearch,
                                    modifier = Modifier.fillMaxSize(),
                                    hazeState = hazeState,
                                    glassMotion = glassMotion
                                )
                            }
                        }
                    }
                }

            }
            // Notices and clipboard actions share a layout so their entrance animations cannot overlap.
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = chromeInset + 12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (isBrowsingFolder && !uiState.isSelectionMode && uiState.clipboardState == null) {
                    GlassSurface(shape = RoundedCornerShape(28.dp), hazeState = hazeState, motion = glassMotion) {
                        IconButton(onClick = { showNewFolderDialog = true }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Outlined.Add, tint = MaterialTheme.colorScheme.onSurface,
                                contentDescription = stringResource(R.string.action_new_folder))
                        }
                    }
                }
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        dismissActionContentColor = MaterialTheme.colorScheme.onSurface,
                        actionColor = MaterialTheme.colorScheme.primary)
                }
                // Category views do not provide a single paste destination.
                if (isBrowsingFolder) {
                    FileManagerClipboardBar(
                        clipboardState = uiState.clipboardState,
                        destinationName = title,
                        onHeightChanged = { clipboardHeight = it },
                        onCancel = { viewModel.cancelClipboard() },
                        onPaste = { viewModel.pasteClipboard(); onShowTransfers() },
                        isTransferring = isTransferring,
                        onShowTransfers = onShowTransfers
                    )
                }
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
        onOpenFile = { file -> if (file.extension.equals("zip", true)) archiveTarget = file else onOpenFile(file, null) },
        onOpenInTools = onOpenInTools
    )

    archiveTarget?.let { archive ->
        DisposableEffect(archive) { onDispose { viewModel.cancelArchivePreview() } }
        ExplorerArchiveSheet(archive,
        onDismiss = { viewModel.cancelArchivePreview(); archiveTarget = null },
        onExtract = { archiveTarget = null; viewModel.extractArchive(archive) },
        canExtract = uiState.activeOperation == null && !isTransferring,
        onOpenMember = { member, stamp -> viewModel.openArchiveMember(archive, member, File(context.cacheDir, "shared/archive-previews"), stamp) { prepared ->
            archiveTarget = null
            if (prepared.extension.equals("zip", true)) archiveTarget = prepared else onOpenFile(prepared, null)
        } },
        onExtractSelected = { selected, stamp -> archiveTarget = null; viewModel.extractArchiveSelection(archive, selected, stamp) }) }
    if (showFolderInsights) ExplorerFolderInsightsSheet(uiState.currentDirectory,
        onDismiss = { showFolderInsights = false },
        onOpenFolder = { folder -> showFolderInsights = false; viewModel.loadDirectory(folder) })

    if (showSaveSearch) SaveExplorerSearchDialog(onDismiss = { showSaveSearch = false }, onSave = viewModel::saveCurrentSearch)
    if (showAdvancedSearch) ExplorerSearchDialog(
        directory = uiState.currentDirectory,
        initial = uiState.advancedSearch,
        onDismiss = { showAdvancedSearch = false },
        onSearch = { showAdvancedSearch = false; viewModel.searchAdvanced(it) }
    )
    batchRenameFiles?.let { files -> ExplorerRenameDialog(files,
        onDismiss = { batchRenameFiles = null },
        onApply = { plan -> batchRenameFiles = null; viewModel.applyBatchRename(plan) }) }
    if (showRenameRecovery) androidx.compose.material3.AlertDialog(
        onDismissRequest = { showRenameRecovery = false },
        title = { Text(stringResource(R.string.explorer_recovery_title)) },
        text = {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(stringResource(R.string.explorer_recovery_help) + "\n\n" + uiState.renameRecovery.joinToString("\n\n"))
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { showRenameRecovery = false }) { Text(stringResource(R.string.action_cancel)) } }
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
    onRefresh: () -> Unit,
    workspaceInsets: Boolean = false,
    onAdvancedSearch: () -> Unit = {},
    onFolderInsights: () -> Unit = {},
    onResetFolderView: () -> Unit = {},
    workspaceMenu: @Composable androidx.compose.foundation.layout.ColumnScope.(() -> Unit) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    AppTopBar(
        title = title,
        subtitle = subtitle,
        onBack = if (isInsideFolder) onBack else null,
        isGlass = false,
        contentInsets = if (workspaceInsets) WindowInsets(0, 0, 0, 0) else androidx.compose.material3.TopAppBarDefaults.windowInsets,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        actions = {
            if (isInsideFolder) IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = if (isSearchActive) scheme.primary else scheme.onSurface
                )
            }

            Box {
                var workspaceOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { workspaceOpen = true }) {
                    Icon(Icons.Outlined.Tab, contentDescription = stringResource(R.string.workspace_tools), tint = scheme.onSurface)
                }
                AppDropdownMenu(expanded = workspaceOpen, onDismissRequest = { workspaceOpen = false }) {
                    workspaceMenu { workspaceOpen = false }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(stringResource(R.string.cd_settings)) }, onClick = { workspaceOpen = false; onSettingsClick() })
                }
            }
            if (isInsideFolder) Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cd_more_options), tint = scheme.onSurface)
                }
                AppDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (isInsideFolder) {
                    HorizontalDivider()
                    if (uiState.isBrowsingStorage && uiState.activeCategory == null) DropdownMenuItem(
                        text = { Text(stringResource(R.string.folder_insights_title)) },
                        onClick = { menuOpen = false; onFolderInsights() })
                    if (uiState.isBrowsingStorage && uiState.activeCategory == null) DropdownMenuItem(
                        text = { Text(stringResource(R.string.folder_view_reset)) }, onClick = { menuOpen = false; onResetFolderView() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.explorer_advanced_search)) }, onClick = { menuOpen = false; onAdvancedSearch() })
                    DropdownMenuItem(
                        text = { Text(stringResource(if (uiState.isGridView) R.string.menu_list_view else R.string.menu_grid_view)) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleView()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(if (uiState.showHiddenFiles) R.string.menu_hide_hidden else R.string.menu_show_hidden)) },
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
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_new_folder)) },
                        leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onNewFolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_new_file)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onNewFile()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (uiState.trashItems.isNotEmpty()) stringResource(R.string.menu_recycle_bin_count, uiState.trashItems.size) else stringResource(R.string.menu_recycle_bin)
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpenTrash()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_refresh)) },
                        leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRefresh()
                        }
                    )
                    }
                }
            }

            if (!isInsideFolder) IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.cd_settings), tint = scheme.onSurface)
            }
        }
    )
}
