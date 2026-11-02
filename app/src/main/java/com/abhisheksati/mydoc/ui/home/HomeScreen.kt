package com.abhisheksati.mydoc.ui.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.BottomNavBar
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
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
import com.abhisheksati.mydoc.ui.files.FilesScreen
import com.abhisheksati.mydoc.ui.storage.StorageViewModel
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.update.UpdateUiState
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import java.io.File
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    toolsViewModel: ToolsViewModel,
    fileManagerViewModel: FileManagerViewModel,
    storageViewModel: StorageViewModel,
    onLibraryItemClick: (LibraryItem) -> Unit,
    onDocumentClick: (DocumentFile) -> Unit,
    onOpenFile: (File) -> Unit,
    onSettingsClick: () -> Unit,
    onSpotlightSearch: () -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier,
    updateManager: UpdateManager? = null
) {

    val uiState by viewModel.uiState.collectAsState()
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    var showCameraScanner by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var selectedDocForDetails by remember { mutableStateOf<DocumentFile?>(null) }
    var documentToDelete by remember { mutableStateOf<DocumentFile?>(null) }

    val documentScanner = com.abhisheksati.mydoc.tools.scan.rememberDocumentScanner(
        onLaunchScanner = {
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
        enabled = currentTab != HomeTab.FILES &&
            !showCameraScanner &&
            !showSignaturePad &&
            selectedDocForDetails == null &&
            documentToDelete == null
    ) {
        viewModel.selectTab(HomeTab.FILES)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val updateState = updateManager?.state?.collectAsState()?.value
                val isUpdateActive = updateState is UpdateUiState.UpdateAvailable ||
                        updateState is UpdateUiState.Downloading ||
                        updateState is UpdateUiState.ReadyToInstall

                AnimatedVisibility(
                    visible = isUpdateActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    when (updateState) {
                        is UpdateUiState.UpdateAvailable -> {
                            val release = updateState.release
                            Surface(
                                color = colors.surface,
                                shape = AppRadius.largeShape,
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
                                            style = AppTheme.typography.bodyStrong,
                                            color = colors.ink
                                        )
                                        Text(
                                            text = "Tap to update mydoc",
                                            style = AppTheme.typography.caption,
                                            color = colors.inkMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { scope.launch { updateManager.downloadAndInstall(release) } },
                                        shape = AppRadius.cardShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.accent,
                                            contentColor = colors.onAccent
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Update", style = AppTheme.typography.captionStrong)
                                    }
                                }
                            }
                        }
                        is UpdateUiState.Downloading -> {
                            Surface(
                                color = colors.surface,
                                shape = AppRadius.largeShape,
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
                                            text = "Downloading update...",
                                            style = AppTheme.typography.label,
                                            color = colors.ink
                                        )
                                        Text(
                                            text = "${updateState.progressPercent}%",
                                            style = AppTheme.typography.captionStrong,
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
                        }
                        is UpdateUiState.ReadyToInstall -> {
                            Surface(
                                color = colors.surface,
                                shape = AppRadius.largeShape,
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
                                            style = AppTheme.typography.bodyStrong,
                                            color = colors.ink
                                        )
                                        Text(
                                            text = "Ready to install v${updateState.release.versionName}",
                                            style = AppTheme.typography.caption,
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
                                        shape = AppRadius.cardShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.accent,
                                            contentColor = colors.onAccent
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Install", style = AppTheme.typography.captionStrong)
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }

                BottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        viewModel.selectTab(tab)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Root tabs share one horizontal axis and keep their own state (scroll, search,
            // selection) when you leave and come back.
            val tabStateHolder = rememberSaveableStateHolder()
            AnimatedContent(
                targetState = currentTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    AppMotion.peerEnter(forward) togetherWith AppMotion.peerExit(forward)
                },
                label = "rootTab"
            ) { tab ->
                tabStateHolder.SaveableStateProvider(tab.name) {
                    when (tab) {
                        HomeTab.FILES -> {
                            FilesScreen(
                                viewModel = fileManagerViewModel,
                                homeViewModel = viewModel,
                                storageViewModel = storageViewModel,
                                onOpenFile = onOpenFile,
                                onLibraryItemClick = onLibraryItemClick,
                                onLibraryItemDetails = { item ->
                                    selectedDocForDetails = item.toDocumentFile()
                                },
                                onSettingsClick = onSettingsClick,
                                onSpotlightSearch = onSpotlightSearch,
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
                                modifier = Modifier.fillMaxSize()
                            )
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
                context.showMessage("Document removed")
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
                // selectTool resets the input list, so it has to run before the pages land.
                toolsViewModel.selectTool(PdfTool.SCAN_TO_PDF)
                toolsViewModel.setInputUris(uris)
            }
        )
    }

    // Signature Pad
    if (showSignaturePad) {
        SignPdfSheet(
            onDismiss = { showSignaturePad = false },
            onSignatureSaved = {
                showSignaturePad = false
                context.showMessage("Signature encrypted and saved.")
            }
        )
    }
}

private fun LibraryItem.toDocumentFile() = DocumentFile(
    uri = uri,
    displayName = name,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    lastOpenedTimestamp = lastModified,
    pageCount = pageCount,
    lastPage = lastPage,
    isFavorite = isFavorite
)
