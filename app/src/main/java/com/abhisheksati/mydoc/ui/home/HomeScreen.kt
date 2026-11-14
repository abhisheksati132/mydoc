package com.abhisheksati.mydoc.ui.home

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.abhisheksati.mydoc.designsystem.components.LocalFloatingChromeInset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.BottomNavBar
import dev.chrisbanes.haze.hazeSource
import com.abhisheksati.mydoc.designsystem.components.GlassMotionState
import com.abhisheksati.mydoc.designsystem.components.TrackGlassMotion
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage
import com.abhisheksati.mydoc.util.shareUris

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    toolsViewModel: ToolsViewModel,
    fileManagerViewModel: FileManagerViewModel,
    storageViewModel: StorageViewModel,
    onLibraryItemClick: (LibraryItem) -> Unit,
    onDocumentClick: (DocumentFile) -> Unit,
    onOpenFile: (File, List<File>?) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit,
    onSpotlightSearch: () -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier,
    updateManager: UpdateManager? = null,
    quickScanRequested: Boolean = false,
    onQuickScanShown: () -> Unit = {}
) {

    val uiState by viewModel.uiState.collectAsState()
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val resources = LocalResources.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    var libraryPageVisible by remember { mutableStateOf(false) }
    val selectedRecentUris by viewModel.selectedRecentUris.collectAsStateWithLifecycle()

    var showCameraScanner by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var toolSource by remember { mutableStateOf<File?>(null) }
    var selectedDocForDetails by remember { mutableStateOf<DocumentFile?>(null) }
    var documentToDelete by remember { mutableStateOf<DocumentFile?>(null) }

    val documentScanner = com.abhisheksati.mydoc.tools.scan.rememberDocumentScanner(
        onLaunchScanner = {
            showCameraScanner = true
        }
    )

    // The Quick Scan tile. MainActivity holds the request as state, so it reaches the scanner from
    // whatever screen was showing; this only has to act on it while Home is on screen.
    LaunchedEffect(quickScanRequested) {
        if (quickScanRequested) {
            documentScanner()
            onQuickScanShown()
        }
    }

    val homeHazeState = remember { dev.chrisbanes.haze.HazeState() }
    val libraryHazeState = remember { dev.chrisbanes.haze.HazeState() }
    val toolsHazeState = remember { dev.chrisbanes.haze.HazeState() }
    var launchToolPicker by rememberSaveable { mutableStateOf(false) }
    val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val libraryGlassMotion = remember { GlassMotionState() }
    val toolsGlassMotion = remember { GlassMotionState() }

    val tabs = remember { listOf(HomeTab.HOME, HomeTab.LIBRARY, HomeTab.TOOLS) }
    val reducedMotion = LocalReducedMotion.current
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(currentTab).coerceAtLeast(0),
        pageCount = { tabs.size }
    )

    SyncTabPager(
        pagerState = pagerState,
        currentTab = currentTab,
        tabs = tabs,
        reducedMotion = reducedMotion,
        onTabSettled = viewModel::selectTab
    )

    // Back exits a recents selection before it exits the tab, same as the Library.
    BackHandler(enabled = currentTab == HomeTab.HOME && selectedRecentUris.isNotEmpty()) {
        viewModel.clearRecentSelection()
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
    }

    val density = LocalDensity.current
    var footerHeight by remember { mutableIntStateOf(0) }
    val footerInset = if (footerHeight > 0) with(density) { footerHeight.toDp() } else 112.dp

    val fileManagerState by fileManagerViewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Root tabs share one horizontal axis with swipe gesture support and keep their own state
        // (scroll, search, selection) when you leave and come back.
        val tabStateHolder = rememberSaveableStateHolder()
        val isPagerScrollEnabled = when {
            currentTab == HomeTab.HOME -> selectedRecentUris.isEmpty()
            currentTab == HomeTab.LIBRARY -> !(libraryPageVisible || fileManagerState.isSelectionMode || fileManagerState.isBrowsingStorage)
            else -> true
        }
        CompositionLocalProvider(LocalFloatingChromeInset provides footerInset) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                userScrollEnabled = isPagerScrollEnabled,
                modifier = Modifier.fillMaxSize().hazeSource(homeHazeState)
            ) { page ->
                val tab = tabs[page]
                tabStateHolder.SaveableStateProvider(tab.name) {
                    when (tab) {
                        HomeTab.HOME -> {
                            HomeFeedPane(
                                continueItems = uiState.continueItems,
                                recentItems = uiState.recentLibraryItems,
                                onLibraryItemClick = onLibraryItemClick,
                                onLibraryItemDetails = { item ->
                                    selectedDocForDetails = item.toDocumentFile()
                                },
                                onRemoveFromRecents = { item ->
                                    viewModel.removeFromRecents(item)
                                },
                                onShareDocument = { item ->
                                    onShareDocument(item.toDocumentFile())
                                },
                                selectedRecentUris = selectedRecentUris,
                                onToggleRecentSelection = { item -> viewModel.toggleRecentSelection(item.uri) },
                                onSelectAllRecents = { items ->
                                    viewModel.selectAllRecents(items.map { it.uri })
                                },
                                onClearRecentSelection = { viewModel.clearRecentSelection() },
                                onShareSelectedRecents = { items ->
                                    val uris = items.map { Uri.parse(it.uri) }
                                    if (!shareUris(context, uris, resources.getString(R.string.home_share_label))) {
                                        context.showMessage(resources.getString(R.string.home_share_failed))
                                    }
                                },
                                onRemoveSelectedRecents = { items ->
                                    viewModel.removeLibraryItemsFromRecents(items)
                                },
                                onSpotlightSearch = onSpotlightSearch,
                                onSettingsClick = onSettingsClick,
                                onScanClick = { documentScanner() },
                                onNavigateToLibrary = { viewModel.selectTab(HomeTab.LIBRARY) },
                                errorMessage = uiState.errorMessage,
                                isLoading = uiState.isLoading,
                                onDismissError = { viewModel.clearError() },
                                listState = homeListState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        HomeTab.LIBRARY -> {
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
                                isActive = currentTab == HomeTab.LIBRARY && pagerState.currentPage == 1,
                                onOpenInTools = { toolSource = it },
                                onMergePdfs = { files ->
                                    toolsViewModel.selectTool(PdfTool.MERGE)
                                    toolsViewModel.setInputUris(files.map(Uri::fromFile))
                                },
                                hazeState = libraryHazeState,
                                glassMotion = libraryGlassMotion,
                                onPageVisibilityChange = { libraryPageVisible = it },
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
                                        launchToolPicker = true
                                        toolsViewModel.selectTool(tool)
                                    }
                                },
                                onSettingsClick = onSettingsClick,
                                isActive = currentTab == HomeTab.TOOLS && pagerState.currentPage == 2,
                                hazeState = toolsHazeState,
                                glassMotion = toolsGlassMotion,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

        }

        // FLOATING GLASS BOTTOM NAVIGATION & UPDATE BANNER
        Column(
            modifier = Modifier
                .onSizeChanged { footerHeight = it.height }
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
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
                                        text = stringResource(R.string.home_update_available, release.versionName),
                                        style = AppTheme.typography.body,
                                        color = colors.ink
                                    )
                                    Text(
                                        text = stringResource(R.string.home_tap_to_update),
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
                                    Text(stringResource(R.string.home_update), style = AppTheme.typography.captionStrong)
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
                                        text = stringResource(R.string.home_downloading_update),
                                        style = AppTheme.typography.label,
                                        color = colors.ink
                                    )
                                    Text(
                                        text = "${updateState.progressPercent}%",
                                        style = AppTheme.typography.metadata,
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
                                        text = stringResource(R.string.home_update_downloaded),
                                        style = AppTheme.typography.body,
                                        color = colors.ink
                                    )
                                    Text(
                                        text = stringResource(R.string.home_ready_install, updateState.release.versionName),
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
                                            updateManager.install(updateState.apkFile)
                                        }
                                    },
                                    shape = AppRadius.cardShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = colors.onAccent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(stringResource(R.string.home_install), style = AppTheme.typography.captionStrong)
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }

            if (currentTab != HomeTab.LIBRARY || !libraryPageVisible) BottomNavBar(
                currentTab = currentTab,
                onTabSelected = viewModel::selectTab,
                hazeState = homeHazeState
            )
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
    val docToDelete = documentToDelete
    if (docToDelete != null) {
        AppDialog(
            isOpen = true,
            title = stringResource(R.string.home_delete_title),
            message = stringResource(R.string.home_delete_message, docToDelete.displayName),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                viewModel.removeFromRecents(docToDelete)
                documentToDelete = null
                context.showMessage(resources.getString(R.string.home_document_removed))
            },
            onDismiss = { documentToDelete = null }
        )
    }

    toolSource?.let { source ->
        val extension = source.extension.lowercase(java.util.Locale.ROOT)
        val tools = when (extension) {
            "pdf" -> listOf(PdfTool.COMPRESS, PdfTool.SIGN_PDF, PdfTool.PDF_TO_IMAGES)
            "jpg", "jpeg", "png", "webp", "bmp", "gif" -> listOf(PdfTool.IMAGE_TO_PDF)
            "txt", "md", "csv", "json", "xml", "log" -> listOf(PdfTool.TEXT_TO_PDF)
            else -> emptyList()
        }
        AppAlertDialog(
            onDismissRequest = { toolSource = null },
            title = { Text(source.name, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
            text = {
                Column {
                    if (tools.isEmpty()) Text(stringResource(R.string.file_tools_no_match))
                    tools.forEach { tool ->
                        androidx.compose.material3.TextButton(onClick = {
                            toolSource = null
                            toolsViewModel.selectTool(tool, Uri.fromFile(source))
                        }) { Text(stringResource(tool.titleRes)) }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { toolSource = null }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // PDF Tool Runner Sheet
    val selectedTool by toolsViewModel.selectedTool.collectAsState()
    if (selectedTool != null) {
        ToolRunnerSheet(
            viewModel = toolsViewModel,
            launchInitialPicker = launchToolPicker,
            onInitialPickerLaunched = { launchToolPicker = false },
            onOpenResultFile = { file, _ ->
                val uri = Uri.fromFile(file)
                val inputUri = toolsViewModel.inputUris.value.firstOrNull()
                scope.launch {
                    val appInst = context.applicationContext as? MyDocApp
                    if (appInst != null) {
                        val res = appInst.documentRepository.registerOpenedDocument(uri)
                        res.onSuccess { doc ->
                            val toolSuffix = selectedTool?.defaultSuffix ?: "result"
                            val inputName = inputUri?.let { u ->
                                runCatching {
                                    var n: String? = null
                                    if (u.scheme == "content") {
                                        context.contentResolver.query(u, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                            if (cursor.moveToFirst()) {
                                                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                                if (idx != -1) n = cursor.getString(idx)
                                            }
                                        }
                                    } else if (u.scheme == "file") {
                                        n = File(u.path ?: "").name
                                    }
                                    n ?: u.lastPathSegment
                                }.getOrNull()
                            }?.substringBeforeLast('.')

                            val friendlyName = if (!inputName.isNullOrBlank()) {
                                "${inputName}_${toolSuffix}.${file.extension}"
                            } else {
                                doc.displayName
                            }
                            toolsViewModel.closeTool()
                            onDocumentClick(doc.copy(displayName = friendlyName))
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
                context.showMessage(resources.getString(R.string.home_signature_saved))
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

/**
 * Keeps the tab and its pager in sync without deadlocking them. The tab observer only fires once
 * the pager has fully settled: syncing on every currentPage change meant a Home→Tools animation
 * crossing Library rewrote currentTab mid-flight, which restarted the tab effect and cancelled
 * the animation — the pager froze between pages with the nav on the wrong tab. The tab effect
 * also snaps a fractional offset restored after process recreation, which nothing else settles.
 */
@Composable
internal fun SyncTabPager(
    pagerState: PagerState,
    currentTab: HomeTab,
    tabs: List<HomeTab>,
    reducedMotion: Boolean,
    onTabSettled: (HomeTab) -> Unit
) {
    val latestTab by rememberUpdatedState(currentTab)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress to pagerState.settledPage }
            .collectLatest { (scrolling, page) ->
                if (!scrolling) {
                    delay(32)
                    // A retargeted animation passes through a momentary settle; let the new
                    // scroll start before adopting its intermediate page as the selected tab.
                    val tab = tabs.getOrNull(page) ?: return@collectLatest
                    if (tab != latestTab) onTabSettled(tab)
                }
            }
    }
    LaunchedEffect(currentTab) {
        val targetPage = tabs.indexOf(currentTab)
        if (targetPage >= 0 &&
            (pagerState.currentPage != targetPage || pagerState.currentPageOffsetFraction != 0f)
        ) {
            if (reducedMotion || kotlin.math.abs(pagerState.currentPage - targetPage) > 1) {
                pagerState.scrollToPage(targetPage)
                return@LaunchedEffect
            }
            pagerState.animateScrollToPage(
                targetPage,
                animationSpec = com.abhisheksati.mydoc.designsystem.animation.AppMotion.snappy()
            )
        }
    }
}
