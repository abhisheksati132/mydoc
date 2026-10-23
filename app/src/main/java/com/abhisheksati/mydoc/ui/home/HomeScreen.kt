package com.abhisheksati.mydoc.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.abhisheksati.mydoc.designsystem.components.HomeBottomBar
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.ui.ToolRunnerSheet
import com.abhisheksati.mydoc.tools.ui.ToolsScreen
import com.abhisheksati.mydoc.tools.ui.components.CameraScanSheet
import com.abhisheksati.mydoc.tools.ui.components.SignPdfSheet
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import com.abhisheksati.mydoc.ui.documents.DocumentsScreen
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
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
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onDocumentClick: (DocumentFile) -> Unit,
    onOpenFile: ((File) -> Unit)? = null,
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
        val tab = HomeTab.entries.getOrNull(pagerState.currentPage) ?: HomeTab.DOCUMENTS
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

    // Navigate back to Documents tab when pressing Back on Tools tab
    BackHandler(
        enabled = currentTab != HomeTab.DOCUMENTS &&
            !showCameraScanner &&
            !showSignaturePad &&
            selectedDocForDetails == null &&
            documentToDelete == null
    ) {
        viewModel.selectTab(HomeTab.DOCUMENTS)
        scope.launch { pagerState.animateScrollToPage(HomeTab.DOCUMENTS.ordinal) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            HomeBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    viewModel.selectTab(tab)
                    scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                }
            )
        }
    ) { innerPadding ->
        // Edge-to-Edge Two Destination Pager: Documents | Tools
        HorizontalPager(
            state = pagerState,
            key = { HomeTab.entries[it].name },
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) { page ->
            when (HomeTab.entries[page]) {
                HomeTab.DOCUMENTS -> {
                    DocumentsScreen(
                        documents = uiState.documents,
                        recentDocs = uiState.recents,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                        sortOrder = uiState.sortOrder,
                        onSetSortOrder = { viewModel.setSortOrder(it) },
                        onDocumentClick = onDocumentClick,
                        onShareDocument = onShareDocument,
                        onShowDetails = { selectedDocForDetails = it },
                        onDeleteDocument = { documentToDelete = it },
                        onScanClick = { documentScanner() },
                        onSettingsClick = onSettingsClick,
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
