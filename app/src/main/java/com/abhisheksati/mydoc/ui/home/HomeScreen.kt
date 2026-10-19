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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CameraAlt
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(
        initialPage = currentTab.ordinal,
        pageCount = { HomeTab.entries.size }
    )

    // Synchronize pager swipe to viewModel
    LaunchedEffect(pagerState.currentPage) {
        val tab = HomeTab.entries.getOrNull(pagerState.currentPage) ?: HomeTab.FILES
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
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var showClearRecentsDialog by remember { mutableStateOf(false) }
    var selectedDocForDetails by remember { mutableStateOf<DocumentFile?>(null) }

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

    // Navigate back to FILES tab when pressing Back on secondary tabs
    BackHandler(enabled = currentTab != HomeTab.FILES && !showCameraScanner && !showSignaturePad && selectedDocForDetails == null && !showClearRecentsDialog && !(fileManagerUiState?.isTrashOpen == true)) {
        viewModel.selectTab(HomeTab.FILES)
        scope.launch { pagerState.animateScrollToPage(HomeTab.FILES.ordinal) }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingPillDock(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        viewModel.selectTab(tab)
                        scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentTab == HomeTab.DOCUMENTS,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            documentScanner()
                        },
                        shape = CircleShape,
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.textPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Scan Document",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            openDocumentLauncher.launch(SUPPORTED_MIME_TYPES)
                        },
                        shape = CircleShape,
                        containerColor = colors.accent,
                        contentColor = if (colors.background == DarkAppColors.background) Color(0xFF000000) else Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Open Document",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            key = { HomeTab.entries[it].name },
            beyondViewportPageCount = 1,
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
                            isActive = (currentTab == HomeTab.FILES),
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
                        onOpenTrash = {
                            fileManagerViewModel?.openTrash()
                        },
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
                            onOpenTrash = {
                                fileManagerViewModel?.openTrash()
                            },
                            trashCount = trashCount,
                            onMoveToTrash = { files, onComplete ->
                                fileManagerViewModel?.moveToTrashFiles(files, onComplete)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                HomeTab.TOOLS -> {
                    ToolsScreen(
                        viewModel = toolsViewModel,
                        onToolClick = { tool ->
                            if (tool == PdfTool.SCAN_TO_PDF) {
                                documentScanner()
                            } else {
                                toolsViewModel.selectTool(tool)
                            }
                        },
                        onSettingsClick = onSettingsClick,
                        onToggleTheme = onToggleTheme,
                        isDarkTheme = isDarkTheme,
                        onOpenTrash = {
                            fileManagerViewModel?.openTrash()
                        },
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

@Composable
fun FloatingPillDock(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val haptic = LocalHapticFeedback.current
    val isOled = colors.background == DarkAppColors.background

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isOled) Color(0xF2101217) else colors.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingDockItem(
                selected = currentTab == HomeTab.FILES,
                icon = if (currentTab == HomeTab.FILES) Icons.Filled.Folder else Icons.Outlined.Folder,
                label = "Files",
                accentColor = colors.accent,
                unselectedColor = colors.textSecondary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(HomeTab.FILES)
                }
            )
            FloatingDockItem(
                selected = currentTab == HomeTab.DOCUMENTS,
                icon = if (currentTab == HomeTab.DOCUMENTS) Icons.AutoMirrored.Filled.Article else Icons.AutoMirrored.Outlined.Article,
                label = "Docs",
                accentColor = colors.accent,
                unselectedColor = colors.textSecondary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(HomeTab.DOCUMENTS)
                }
            )
            FloatingDockItem(
                selected = currentTab == HomeTab.MEDIA,
                icon = if (currentTab == HomeTab.MEDIA) Icons.Filled.PermMedia else Icons.Outlined.PermMedia,
                label = "Media",
                accentColor = colors.accent,
                unselectedColor = colors.textSecondary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(HomeTab.MEDIA)
                }
            )
            FloatingDockItem(
                selected = currentTab == HomeTab.TOOLS,
                icon = if (currentTab == HomeTab.TOOLS) Icons.Filled.Build else Icons.Outlined.Build,
                label = "Tools",
                accentColor = colors.accent,
                unselectedColor = colors.textSecondary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(HomeTab.TOOLS)
                }
            )
        }
    }
}

@Composable
private fun FloatingDockItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    accentColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    val backgroundTarget = if (selected) accentColor.copy(alpha = 0.16f) else Color.Transparent
    val backgroundColor by animateColorAsState(
        targetValue = backgroundTarget,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockItemBackground"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 15.dp else 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (badgeText != null) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = accentColor,
                            contentColor = Color(0xFF000000)
                        ) {
                            Text(badgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) accentColor else unselectedColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) accentColor else unselectedColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        color = accentColor,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

