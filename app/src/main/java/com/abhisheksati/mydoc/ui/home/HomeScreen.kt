package com.abhisheksati.mydoc.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.FolderScanner
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.ShelvesBottomBar
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.ui.ToolRunnerSheet
import com.abhisheksati.mydoc.tools.ui.components.CameraScanSheet
import com.abhisheksati.mydoc.tools.ui.components.SignPdfSheet
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import com.abhisheksati.mydoc.ui.shelves.AutoShelvesDialog
import com.abhisheksati.mydoc.ui.shelves.DetectedFolder
import com.abhisheksati.mydoc.ui.shelves.LibraryScreen
import com.abhisheksati.mydoc.ui.shelves.NewShelfSheet
import com.abhisheksati.mydoc.ui.shelves.NowScreen
import com.abhisheksati.mydoc.ui.shelves.ShelvesToolsScreen
import com.abhisheksati.mydoc.ui.storage.StorageScreen
import com.abhisheksati.mydoc.ui.storage.StorageViewModel
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import com.abhisheksati.mydoc.viewmodel.MediaViewModel
import java.io.File
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    toolsViewModel: ToolsViewModel,
    mediaViewModel: MediaViewModel? = null,
    fileManagerViewModel: FileManagerViewModel? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onDocumentClick: (DocumentFile) -> Unit,
    onOpenFile: ((File) -> Unit)? = null,
    onOpenMedia: ((com.abhisheksati.mydoc.domain.model.MediaItem, List<com.abhisheksati.mydoc.domain.model.MediaItem>) -> Unit)? = null,
    onSettingsClick: () -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val fileManagerUiState by fileManagerViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp

    val storageViewModel: StorageViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = StorageViewModel.Factory(
            context = context.applicationContext,
            storageRepository = app?.storageRepository ?: (context.applicationContext as MyDocApp).storageRepository
        )
    )

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(
        initialPage = currentTab.ordinal,
        pageCount = { HomeTab.entries.size }
    )

    // Synchronize pager swipe to viewModel
    LaunchedEffect(pagerState.currentPage) {
        val tab = HomeTab.entries.getOrNull(pagerState.currentPage) ?: HomeTab.NOW
        if (tab != currentTab) {
            viewModel.selectTab(tab)
        }
    }

    // Synchronize viewModel tab changes to pager
    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab.ordinal) {
            pagerState.animateScrollToPage(currentTab.ordinal)
        }
    }

    var showCameraScanner by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var showClearRecentsDialog by remember { mutableStateOf(false) }
    var selectedDocForDetails by remember { mutableStateOf<DocumentFile?>(null) }
    var showStorageAnalyzer by remember { mutableStateOf(false) }
    var showNewShelfSheet by remember { mutableStateOf(false) }
    var showAutoShelvesDialog by remember { mutableStateOf(false) }
    var detectedFolders by remember { mutableStateOf<List<DetectedFolder>>(emptyList()) }
    var chosenShelfFolderUri by remember { mutableStateOf<String?>(null) }
    var chosenShelfFolderName by remember { mutableStateOf<String?>(null) }

    val documentScanner = com.abhisheksati.mydoc.tools.scan.rememberDocumentScanner(
        onPages = { uris ->
            showCameraScanner = false
            toolsViewModel.setInputUris(uris)
            toolsViewModel.selectTool(PdfTool.SCAN_TO_PDF)
        },
        onError = {
            // Fallback to built-in camera sheet if GMS scanner is unavailable
            showCameraScanner = true
        }
    )

    // Trigger instant scanning when launched from Quick Settings tile
    LaunchedEffect(Unit) {
        MainActivity.quickScanEvents.collect {
            documentScanner()
        }
    }

    // Navigate back to NOW tab when pressing Back on secondary tabs
    BackHandler(
        enabled = currentTab != HomeTab.NOW &&
            !showStorageAnalyzer &&
            !showNewShelfSheet &&
            !showAutoShelvesDialog &&
            !showCameraScanner &&
            !showSignaturePad &&
            selectedDocForDetails == null &&
            !showClearRecentsDialog &&
            !(fileManagerUiState?.isTrashOpen == true)
    ) {
        viewModel.selectTab(HomeTab.NOW)
        scope.launch { pagerState.animateScrollToPage(HomeTab.NOW.ordinal) }
    }

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

    // SAF Folder Picker launcher for Shelf creation
    val openFolderLauncherForShelf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val segment = uri.lastPathSegment ?: "Folder"
                val cleanName = segment.substringAfterLast(':').ifEmpty { segment }
                chosenShelfFolderUri = uri.toString()
                chosenShelfFolderName = cleanName
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to access folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // SAF Folder Picker launcher for general scanning
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
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ShelvesBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    viewModel.selectTab(tab)
                    scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                }
            )
        }
    ) { innerPadding ->
        // 1. Edge-to-Edge Three Destination Pager: Now | Library | Tools
        HorizontalPager(
            state = pagerState,
            key = { HomeTab.entries[it].name },
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) { page ->
            when (HomeTab.entries[page]) {
                HomeTab.NOW -> {
                    NowScreen(
                        shelves = uiState.shelves,
                        continueDoc = uiState.continueReadingDoc,
                        recentDocs = uiState.recents,
                        unfiledCount = uiState.unfiledCount,
                        onDocumentClick = onDocumentClick,
                        onShelfClick = { shelf ->
                            viewModel.selectShelf(shelf.id)
                            viewModel.selectTab(HomeTab.LIBRARY)
                            scope.launch { pagerState.animateScrollToPage(HomeTab.LIBRARY.ordinal) }
                        },
                        onUnfiledClick = {
                            viewModel.selectShelf(-1L)
                            viewModel.selectTab(HomeTab.LIBRARY)
                            scope.launch { pagerState.animateScrollToPage(HomeTab.LIBRARY.ordinal) }
                        },
                        onNewShelfClick = {
                            showNewShelfSheet = true
                        },
                        onSearchClick = {
                            viewModel.selectTab(HomeTab.LIBRARY)
                            scope.launch { pagerState.animateScrollToPage(HomeTab.LIBRARY.ordinal) }
                        },
                        onChooseFolder = {
                            openFolderLauncher.launch(null)
                        },
                        onOpenSingleFile = {
                            openDocumentLauncher.launch(SUPPORTED_MIME_TYPES)
                        },
                        onUpdateShelf = { shelf ->
                            viewModel.updateShelf(shelf)
                        },
                        onDeleteShelf = { shelfId ->
                            viewModel.deleteShelf(shelfId)
                        },
                        onAssignDocumentToShelf = { uri, shelfId ->
                            viewModel.assignDocumentToShelf(uri, shelfId)
                        },
                        onUnassignDocument = { uri ->
                            viewModel.unassignDocument(uri)
                        },
                        onRemoveFromRecents = { doc ->
                            viewModel.removeFromRecents(doc)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                HomeTab.LIBRARY -> {
                    LibraryScreen(
                        documents = uiState.documents,
                        shelves = uiState.shelves,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                        selectedShelfId = uiState.selectedShelfId,
                        onSelectShelfId = { viewModel.selectShelf(it) },
                        selectedTypeFilter = uiState.selectedTypeFilter,
                        onSelectTypeFilter = { viewModel.selectTypeFilter(it) },
                        sortOrder = uiState.sortOrder,
                        onSetSortOrder = { viewModel.setSortOrder(it) },
                        onDocumentClick = onDocumentClick,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteDocument = { viewModel.removeFromRecents(it) },
                        onShareDocument = onShareDocument,
                        onAssignDocumentToShelf = { uri, shelfId ->
                            viewModel.assignDocumentToShelf(uri, shelfId)
                        },
                        onUnassignDocument = { uri ->
                            viewModel.unassignDocument(uri)
                        },
                        onUpdateShelf = { shelf ->
                            viewModel.updateShelf(shelf)
                        },
                        onDeleteShelf = { shelfId ->
                            viewModel.deleteShelf(shelfId)
                        },
                        onSettingsClick = onSettingsClick,
                        onStorageAnalyzerClick = { showStorageAnalyzer = true },
                        onRecycleBinClick = { fileManagerViewModel?.openTrash() },
                        onScanFolderClick = { openFolderLauncher.launch(null) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                HomeTab.TOOLS -> {
                    ShelvesToolsScreen(
                        recentDoc = uiState.continueReadingDoc ?: uiState.recents.firstOrNull(),
                        onToolClick = { tool ->
                            if (tool == PdfTool.SCAN_TO_PDF) {
                                documentScanner()
                            } else {
                                toolsViewModel.selectTool(tool)
                            }
                        },
                        onScanToPdf = {
                            documentScanner()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Storage Analyzer Screen (as overlay when requested from Library overflow menu)
    if (showStorageAnalyzer) {
        BackHandler { showStorageAnalyzer = false }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            StorageScreen(
                viewModel = storageViewModel,
                onNavigateToCategory = {
                    showStorageAnalyzer = false
                },
                onSettingsClick = onSettingsClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // New Shelf Creation Sheet
    if (showNewShelfSheet) {
        NewShelfSheet(
            onDismiss = {
                showNewShelfSheet = false
                chosenShelfFolderUri = null
                chosenShelfFolderName = null
            },
            onCreateShelf = { name, colorIndex, folderUri ->
                viewModel.createShelf(name, colorIndex, folderUri) { createdShelfId ->
                    if (folderUri != null && app != null) {
                        scope.launch {
                            val count = FolderScanner.scanTree(
                                context,
                                Uri.parse(folderUri),
                                app.documentRepository,
                                shelfId = createdShelfId
                            )
                            Toast.makeText(context, "Created shelf '$name' with $count documents", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Shelf '$name' created", Toast.LENGTH_SHORT).show()
                    }
                }
                showNewShelfSheet = false
                chosenShelfFolderUri = null
                chosenShelfFolderName = null
            },
            onPickFolder = {
                openFolderLauncherForShelf.launch(null)
            },
            chosenFolderUri = chosenShelfFolderUri,
            chosenFolderName = chosenShelfFolderName
        )
    }

    // Auto-Shelves Dialog
    if (showAutoShelvesDialog && detectedFolders.isNotEmpty()) {
        AutoShelvesDialog(
            folders = detectedFolders,
            onConfirm = { selected ->
                scope.launch {
                    selected.forEach { folder ->
                        val shelfId = app?.documentRepository?.createShelf(folder.name, folder.colorIndex, folder.uri) ?: 0L
                        if (shelfId > 0 && app != null) {
                            FolderScanner.scanTree(context, Uri.parse(folder.uri), app.documentRepository, shelfId = shelfId)
                        }
                    }
                    Toast.makeText(context, "Created ${selected.size} shelves", Toast.LENGTH_SHORT).show()
                }
                showAutoShelvesDialog = false
            },
            onMakeMyself = {
                showAutoShelvesDialog = false
                showNewShelfSheet = true
            },
            onDismiss = { showAutoShelvesDialog = false }
        )
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

    // Clear Recents Confirmation Dialog
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

    // Recycle Bin Full Screen Sheet
    if (fileManagerViewModel != null && fileManagerUiState?.isTrashOpen == true) {
        com.abhisheksati.mydoc.ui.filemanager.TrashBinSheet(
            trashItems = fileManagerUiState!!.trashItems,
            onRestore = {
                fileManagerViewModel.restoreTrashItems(it) {
                    mediaViewModel?.refresh()
                }
            },
            onDeletePermanently = {
                fileManagerViewModel.permanentlyDeleteTrashItems(it) {
                    mediaViewModel?.refresh()
                }
            },
            onEmptyTrash = {
                fileManagerViewModel.emptyTrash {
                    mediaViewModel?.refresh()
                }
            },
            onClose = {
                fileManagerViewModel.closeTrash()
                mediaViewModel?.refresh()
            }
        )
    }
}
