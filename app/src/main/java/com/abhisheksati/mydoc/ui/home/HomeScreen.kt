package com.abhisheksati.mydoc.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import com.abhisheksati.mydoc.ui.filemanager.FileManagerScreen
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import java.io.File
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.data.local.FolderScanner
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFileRow
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.abhisheksati.mydoc.domain.model.HomeTab

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
    mediaViewModel: com.abhisheksati.mydoc.viewmodel.MediaViewModel? = null,
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
    val trashCount = fileManagerUiState?.trashItems?.size ?: 0
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp

    val currentTab by viewModel.currentTab.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = viewModel.currentTab.value.ordinal,
        pageCount = { HomeTab.entries.size }
    )

    // Synchronize pager swipe to viewModel
    LaunchedEffect(pagerState.currentPage) {
        val tab = HomeTab.entries.getOrNull(pagerState.currentPage) ?: HomeTab.FILES
        if (tab != viewModel.currentTab.value) {
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {},
        bottomBar = {
            NavigationBar(
                containerColor = colors.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == HomeTab.FILES,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selectTab(HomeTab.FILES)
                        scope.launch { pagerState.animateScrollToPage(HomeTab.FILES.ordinal) }
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.FILES) Icons.Filled.Folder else Icons.Outlined.Folder,
                            contentDescription = "Files"
                        )
                    },
                    label = { Text("Files", style = typography.caption) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.accent.copy(alpha = 0.16f),
                        selectedIconColor = colors.accent,
                        selectedTextColor = colors.accent,
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == HomeTab.DOCUMENTS,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selectTab(HomeTab.DOCUMENTS)
                        scope.launch { pagerState.animateScrollToPage(HomeTab.DOCUMENTS.ordinal) }
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
                        viewModel.selectTab(HomeTab.MEDIA)
                        scope.launch { pagerState.animateScrollToPage(HomeTab.MEDIA.ordinal) }
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
                        viewModel.selectTab(HomeTab.TOOLS)
                        scope.launch { pagerState.animateScrollToPage(HomeTab.TOOLS.ordinal) }
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
                    contentColor = if (colors.background == DarkAppColors.background) Color(0xFF000000) else Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
                )
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (HomeTab.entries[page]) {
                HomeTab.FILES -> {
                    if (fileManagerViewModel != null && onOpenFile != null) {
                        FileManagerScreen(
                            viewModel = fileManagerViewModel,
                            onOpenFile = onOpenFile,
                            onSettingsClick = onSettingsClick,
                            onToggleTheme = onToggleTheme,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                HomeTab.DOCUMENTS -> {
                    DocumentsTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        onSettingsClick = onSettingsClick,
                        onDocumentClick = onDocumentClick,
                        onShareDocument = onShareDocument,
                        openFolderLauncher = openFolderLauncher,
                        openDocumentLauncher = openDocumentLauncher,
                        onShowClearRecentsDialog = { showClearRecentsDialog = true },
                        onSelectDocForDetails = { selectedDocForDetails = it },
                        onOpenTrash = { fileManagerViewModel?.openTrash() },
                        trashCount = trashCount,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                HomeTab.MEDIA -> {
                    if (mediaViewModel != null) {
                        com.abhisheksati.mydoc.ui.media.MediaTabContent(
                            viewModel = mediaViewModel,
                            onOpenMedia = { item, list ->
                                onOpenMedia?.invoke(item, list)
                            },
                            onSettingsClick = onSettingsClick,
                            onToggleTheme = onToggleTheme,
                            isDarkTheme = isDarkTheme,
                            onOpenTrash = { fileManagerViewModel?.openTrash() },
                            trashCount = trashCount,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                HomeTab.TOOLS -> {
                    ToolsScreen(
                        viewModel = toolsViewModel,
                        onToolClick = { tool ->
                            if (tool == PdfTool.SCAN_TO_PDF) {
                                showCameraScanner = true
                            } else {
                                toolsViewModel.selectTool(tool)
                            }
                        },
                        onSettingsClick = onSettingsClick,
                        onToggleTheme = onToggleTheme,
                        isDarkTheme = isDarkTheme,
                        onOpenTrash = { fileManagerViewModel?.openTrash() },
                        trashCount = trashCount,
                        modifier = Modifier.fillMaxSize()
                    )
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

    // Recycle Bin Full Screen Sheet
    if (fileManagerViewModel != null && fileManagerUiState?.isTrashOpen == true) {
        com.abhisheksati.mydoc.ui.filemanager.TrashBinSheet(
            trashItems = fileManagerUiState!!.trashItems,
            onRestore = { fileManagerViewModel.restoreTrashItems(it) },
            onDeletePermanently = { fileManagerViewModel.permanentlyDeleteTrashItems(it) },
            onEmptyTrash = { fileManagerViewModel.emptyTrash() },
            onClose = { fileManagerViewModel.closeTrash() }
        )
    }
}
