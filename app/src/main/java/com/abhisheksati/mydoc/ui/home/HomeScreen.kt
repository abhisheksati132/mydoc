package com.abhisheksati.mydoc.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.BottomNavBar
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.ui.ToolRunnerSheet
import com.abhisheksati.mydoc.tools.ui.ToolsScreen
import com.abhisheksati.mydoc.tools.ui.components.CameraScanSheet
import com.abhisheksati.mydoc.tools.ui.components.SignPdfSheet
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import com.abhisheksati.mydoc.ui.filemanager.FileManagerScreen
import com.abhisheksati.mydoc.ui.library.LibraryTabContent
import com.abhisheksati.mydoc.ui.storage.StorageViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.update.UpdateUiState
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    toolsViewModel: ToolsViewModel,
    fileManagerViewModel: FileManagerViewModel,
    storageViewModel: StorageViewModel,
    updateManager: UpdateManager? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onLibraryItemClick: (LibraryItem) -> Unit,
    onDocumentClick: (DocumentFile) -> Unit,
    onOpenFile: (File) -> Unit,
    onSettingsClick: () -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(
        initialPage = currentTab.ordinal,
        pageCount = { HomeTab.entries.size }
    )

    // Synchronize pager swipe to viewModel
    LaunchedEffect(pagerState.currentPage) {
        val tab = HomeTab.entries.getOrNull(pagerState.currentPage) ?: HomeTab.HOME
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
    var selectedDocForDetails by remember { mutableStateOf<DocumentFile?>(null) }
    var documentToDelete by remember { mutableStateOf<DocumentFile?>(null) }

    val documentScanner = com.abhisheksati.mydoc.tools.scan.rememberDocumentScanner(
        onPages = { uris ->
            showCameraScanner = false
            toolsViewModel.setInputUris(uris)
            toolsViewModel.selectTool(PdfTool.SCAN_TO_PDF)
        },
        onError = {
            showCameraScanner = true
        }
    )

    // Trigger instant scanning when launched from Quick Settings tile
    LaunchedEffect(Unit) {
        MainActivity.quickScanEvents.collect {
            documentScanner()
        }
    }

    // Navigate back to Home tab when pressing Back on other tabs
    BackHandler(
        enabled = currentTab != HomeTab.HOME &&
            !showCameraScanner &&
            !showSignaturePad &&
            selectedDocForDetails == null &&
            documentToDelete == null
    ) {
        viewModel.selectTab(HomeTab.HOME)
        scope.launch { pagerState.animateScrollToPage(HomeTab.HOME.ordinal) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val updateState = updateManager?.state?.collectAsState()?.value
                if (updateState is UpdateUiState.UpdateAvailable) {
                    val release = updateState.release
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update available: v${release.versionName}",
                                    style = AppTheme.typography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = colors.ink
                                )
                                Text(
                                    text = "Tap to update Stacks",
                                    style = AppTheme.typography.caption.copy(fontSize = 12.sp),
                                    color = colors.inkMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { scope.launch { updateManager.downloadAndInstall(release) } },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Update", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }
                } else if (updateState is UpdateUiState.Downloading) {
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading update…",
                                    style = AppTheme.typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                    color = colors.ink
                                )
                                Text(
                                    text = "${updateState.progressPercent}%",
                                    style = AppTheme.typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    color = colors.accent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { updateState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = colors.accent,
                                trackColor = colors.surfaceMuted
                            )
                        }
                    }
                } else if (updateState is UpdateUiState.ReadyToInstall) {
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update downloaded!",
                                    style = AppTheme.typography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = colors.ink
                                )
                                Text(
                                    text = "Ready to install v${updateState.release.versionName}",
                                    style = AppTheme.typography.caption.copy(fontSize = 12.sp),
                                    color = colors.inkMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (!updateManager.installerHelper.canInstallUnknownApps()) {
                                        val intent = updateManager.installerHelper.createManageUnknownAppSourcesIntent()
                                        context.startActivity(intent)
                                    } else {
                                        updateManager.installerHelper.installApk(updateState.apkFile)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Install", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }
                }

                BottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        viewModel.selectTab(tab)
                        scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                    }
                )
            }
        }
    ) { innerPadding ->
        // 4-Destination Edge-to-Edge Pager: Home | Library | Files | Tools
        HorizontalPager(
            state = pagerState,
            key = { HomeTab.entries[it].name },
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) { page ->
            when (HomeTab.entries[page]) {
                HomeTab.HOME -> {
                    HomeTabContent(
                        continueItems = uiState.continueItems,
                        recentItems = uiState.recentLibraryItems,
                        onItemClick = onLibraryItemClick,
                        onSearchClick = {
                            viewModel.selectTab(HomeTab.LIBRARY)
                            scope.launch { pagerState.animateScrollToPage(HomeTab.LIBRARY.ordinal) }
                        },
                        onSettingsClick = onSettingsClick,
                        onToggleTheme = onToggleTheme,
                        onClearRecent = { viewModel.clearAllRecents() },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                HomeTab.LIBRARY -> {
                    LibraryTabContent(
                        allItems = uiState.libraryItems,
                        recentlyAdded = uiState.recentlyAdded,
                        countsByKind = uiState.countsByKind,
                        onItemClick = onLibraryItemClick,
                        onSettingsClick = onSettingsClick,
                        onToggleTheme = onToggleTheme,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                HomeTab.FILES -> {
                    FileManagerScreen(
                        viewModel = fileManagerViewModel,
                        storageViewModel = storageViewModel,
                        onOpenFile = onOpenFile,
                        onSettingsClick = onSettingsClick,
                        onToggleTheme = onToggleTheme,
                        isDarkTheme = isDarkTheme,
                        isActive = currentTab == HomeTab.FILES,
                        modifier = Modifier.fillMaxSize()
                    )
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

    // Delete Confirmation Dialog
    if (documentToDelete != null) {
        val doc = documentToDelete!!
        AppDialog(
            isOpen = true,
            title = "Delete document",
            message = "Remove \"${doc.displayName}\" from your documents list?",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                viewModel.removeFromRecents(doc)
                documentToDelete = null
                Toast.makeText(context, "Document removed", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { documentToDelete = null }
        )
    }

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
