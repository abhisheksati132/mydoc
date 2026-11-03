package com.abhisheksati.mydoc.ui.viewer.pdf

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.designsystem.tokens.DocumentCanvasColors
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.ui.viewer.controls.FastScrollbar
import com.abhisheksati.mydoc.ui.viewer.controls.ZoomableViewport
import com.abhisheksati.mydoc.ui.viewer.controls.rememberZoomState
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    document: DocumentFile,
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToolsClick: (() -> Unit)? = null,
    onSelectTool: ((PdfTool) -> Unit)? = null,
    onDocumentUpdated: ((DocumentFile) -> Unit)? = null,
    onSlideshowClick: (() -> Unit)? = null,
    onReflowClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()

    val zoomState = rememberZoomState(key = document.uri)
    val zoomScale = zoomState.scale

    // Reset zoom when page or viewing layout mode changes
    LaunchedEffect(state.currentPage, state.isContinuousScroll, state.isDualPageView) {
        zoomState.reset()
    }
    var isPageJumpDialogOpen by remember { mutableStateOf(false) }
    var isReadingControlsOpen by remember { mutableStateOf(false) }
    var isThumbnailGridOpen by remember { mutableStateOf(false) }
    var isToolsSheetOpen by remember { mutableStateOf(false) }
    var isDocumentDetailsOpen by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf<String?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }
    val ocrEngine = remember { com.abhisheksati.mydoc.ocr.OcrEngine(context) }
    val ttsManager = remember { com.abhisheksati.mydoc.tools.tts.DocumentTtsManager(context) }
    DisposableEffect(ocrEngine, ttsManager) {
        onDispose {
            ocrEngine.close()
            ttsManager.shutdown()
        }
    }
    var isTtsVisible by remember { mutableStateOf(false) }
    var currentTtsText by remember { mutableStateOf("") }
    var selectedPageForActions by remember { mutableStateOf<Int?>(null) }
    var selectedTextState by remember { mutableStateOf<Pair<Int, com.abhisheksati.mydoc.rendering.engine.TextSelectionResult>?>(null) }

    val handleLongPressOffset: (Int, Offset, Size) -> Unit = { pIdx, offset, itemSize ->
        coroutineScope.launch {
            val (w, h) = viewModel.getPageSize(pIdx)
            if (w > 0 && h > 0 && itemSize.width > 0 && itemSize.height > 0) {
                val pdfX = (offset.x / itemSize.width) * w
                val pdfY = (offset.y / itemSize.height) * h
                val sel = viewModel.selectTextAt(pIdx, android.graphics.PointF(pdfX, pdfY))
                if (sel != null && sel.selectedText.isNotBlank()) {
                    selectedTextState = Pair(pIdx, sel)
                    selectedPageForActions = pIdx
                    return@launch
                }
            }
            selectedTextState = null
            selectedPageForActions = pIdx
        }
    }

    val reopenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { newUri: Uri? ->
        if (newUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    newUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.updateDocumentUri(newUri) { updatedDoc ->
                onDocumentUpdated?.invoke(updatedDoc)
            }
        }
    }

    var bookmarks by remember {
        mutableStateOf(app?.settingsRepository?.getBookmarks(document.uri) ?: emptySet())
    }
    val isCurrentBookmarked = bookmarks.contains(state.currentPage)

    var currentBrightness by remember { mutableFloatStateOf(-1f) }
    val updateBrightness: (Float) -> Unit = { b ->
        currentBrightness = b
        val activity = context as? Activity
        val lp = activity?.window?.attributes
        if (lp != null) {
            lp.screenBrightness = b
            activity.window.attributes = lp
        }
    }

    var isKeepScreenOn by remember {
        mutableStateOf(app?.settingsRepository?.keepScreenOn?.value ?: true)
    }

    val currentView = LocalView.current
    DisposableEffect(isKeepScreenOn) {
        currentView.keepScreenOn = isKeepScreenOn
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val activity = context as? Activity
            val lp = activity?.window?.attributes
            if (lp != null) {
                lp.screenBrightness = -1f
                activity.window.attributes = lp
            }
        }
    }

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (state.pageCount > 0) state.currentPage.coerceIn(0, state.pageCount - 1) else 0
    )
    val pagerState = rememberPagerState(
        initialPage = if (state.pageCount > 0) state.currentPage.coerceIn(0, state.pageCount - 1) else 0,
        pageCount = { state.pageCount.coerceAtLeast(1) }
    )
    val viewerHazeState = remember { dev.chrisbanes.haze.HazeState() }

    // Reading immersion: hide system bars when controls are hidden, restore on exit
    val activity = context as? Activity
    val window = activity?.window
    val insetsController = remember(window) {
        if (window != null) {
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else null
    }

    LaunchedEffect(state.areControlsVisible, insetsController) {
        if (insetsController != null) {
            if (state.areControlsVisible) {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(insetsController) {
        onDispose {
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Auto-hide controls quickly when user starts active scrolling for maximum reading immersion
    val isScrolling = lazyListState.isScrollInProgress || pagerState.isScrollInProgress
    LaunchedEffect(isScrolling) {
        if (isScrolling && state.areControlsVisible) {
            kotlinx.coroutines.delay(350L)
            if (lazyListState.isScrollInProgress || pagerState.isScrollInProgress) {
                viewModel.setControlsVisible(false)
            }
        }
    }

    // Volume key page turning listener
    LaunchedEffect(Unit) {
        MainActivity.pageTurnEvents.collect { isNext ->
            val target = if (isNext) {
                (state.currentPage + 1).coerceAtMost(state.pageCount - 1)
            } else {
                (state.currentPage - 1).coerceAtLeast(0)
            }
            if (target != state.currentPage) {
                viewModel.jumpToPage(target)
                if (state.isContinuousScroll) {
                    lazyListState.animateScrollToItem(target)
                } else {
                    pagerState.animateScrollToPage(target)
                }
            }
        }
    }

    // Sync current page with viewmodel
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { page ->
                if (state.isContinuousScroll && !state.isDualPageView) {
                    viewModel.onPageChanged(page)
                }
            }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (!state.isContinuousScroll && !state.isDualPageView) {
                    viewModel.onPageChanged(page)
                }
            }
    }

    // Auto-scroll viewer when navigating through search matches
    LaunchedEffect(state.currentMatchIndex) {
        if (state.currentMatchIndex in state.searchMatches.indices) {
            val targetPage = state.searchMatches[state.currentMatchIndex].pageIndex
            if (state.isContinuousScroll) {
                lazyListState.animateScrollToItem(targetPage)
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    // Restore saved reading progress when document loading completes
    var hasRestoredSavedPosition by remember(document.uri) { mutableStateOf(false) }
    LaunchedEffect(state.isLoading, state.pageCount) {
        if (!state.isLoading && state.pageCount > 0 && !hasRestoredSavedPosition) {
            val target = state.currentPage.coerceIn(0, state.pageCount - 1)
            if (target > 0) {
                if (state.isContinuousScroll) {
                    lazyListState.scrollToItem(target)
                } else {
                    pagerState.scrollToPage(target)
                }
            }
            hasRestoredSavedPosition = true
        }
    }

    // Synchronize scroll position when switching between continuous scroll and page-by-page pager
    LaunchedEffect(state.isContinuousScroll) {
        if (state.pageCount > 0 && hasRestoredSavedPosition) {
            val target = state.currentPage.coerceIn(0, state.pageCount - 1)
            if (state.isContinuousScroll) {
                if (lazyListState.firstVisibleItemIndex != target) {
                    lazyListState.scrollToItem(target)
                }
            } else {
                if (pagerState.currentPage != target) {
                    pagerState.scrollToPage(target)
                }
            }
        }
    }

    // Condensing top bar calculation: collapses into floating glass pill on vertical scroll
    val isCondensed by remember {
        androidx.compose.runtime.derivedStateOf {
            if (state.isContinuousScroll) {
                lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 80
            } else {
                pagerState.currentPage > 0
            }
        }
    }

    val pageBgColor = when (state.readingMode) {
        ReadingMode.INVERTED_DARK -> DocumentCanvasColors.blackPage
        ReadingMode.SEPIA -> DocumentCanvasColors.sepiaPage
        ReadingMode.EYE_COMFORT -> DocumentCanvasColors.eyeComfortPage
        ReadingMode.NORMAL -> colors.background
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBgColor)
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppProgressIndicator()
            }
        } else if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(AppSpacing.paddingLarge),
                contentAlignment = Alignment.Center
            ) {
                AppEmptyState(
                    title = if (state.isPermissionExpired) "Document inaccessible" else "Document could not be displayed",
                    message = state.errorMessage ?: "The file could not be read from storage. Please select it to restore access.",
                    actionLabel = "Select file",
                    onAction = {
                        reopenLauncher.launch(arrayOf("application/pdf", "*/*"))
                    }
                )
            }
        } else if (state.pageCount > 0) {
            val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val topContentPadding = statusBarsTop + 56.dp + AppSpacing.paddingSmall
            val bottomContentPadding = navBarsBottom + 56.dp + AppSpacing.paddingSmall
            val contentPaddingValues = PaddingValues(
                top = topContentPadding,
                bottom = bottomContentPadding,
                start = AppSpacing.paddingSmall,
                end = AppSpacing.paddingSmall
            )

            // Main viewport with unified gesture handling, pinch-pan around centroid, and inertia
            ZoomableViewport(
                zoomState = zoomState,
                isContinuous = state.isContinuousScroll,
                isDualPage = state.isDualPageView,
                isDoubleTapEnabled = app?.settingsRepository?.doubleTapZoom?.value ?: true,
                onToggleControls = viewModel::toggleControls,
                onPrevPage = {
                    val prev = (state.currentPage - 1).coerceAtLeast(0)
                    if (prev != state.currentPage) {
                        viewModel.jumpToPage(prev)
                        coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                    }
                },
                onNextPage = {
                    val next = (state.currentPage + 1).coerceAtMost(state.pageCount - 1)
                    if (next != state.currentPage) {
                        viewModel.jumpToPage(next)
                        coroutineScope.launch { pagerState.animateScrollToPage(next) }
                    }
                }
            ) {
                val searchMatchesByPage = remember(state.searchMatches) {
                    state.searchMatches.groupBy { it.pageIndex }
                }
                val activeMatch = remember(state.searchMatches, state.currentMatchIndex) {
                    state.searchMatches.getOrNull(state.currentMatchIndex)
                }

                if (state.isDualPageView) {
                    val pairCount = (state.pageCount + 1) / 2
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = contentPaddingValues,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(viewerHazeState)
                    ) {
                        items(pairCount, key = { it }) { pairIndex ->
                            val leftPage = pairIndex * 2
                            val rightPage = leftPage + 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacing.paddingSmall),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PageItemView(
                                        pageIndex = leftPage,
                                        viewModel = viewModel,
                                        readingMode = state.readingMode,
                                        zoomScale = zoomScale,
                                        isFitPage = state.isFitPage,
                                        searchMatches = searchMatchesByPage[leftPage] ?: emptyList(),
                                        activeMatch = activeMatch,
                                        selection = if (selectedTextState?.first == leftPage) selectedTextState?.second else null,
                                        onLongPressPage = { selectedPageForActions = it },
                                        onLongPressOffset = handleLongPressOffset
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    if (rightPage < state.pageCount) {
                                        PageItemView(
                                            pageIndex = rightPage,
                                            viewModel = viewModel,
                                            readingMode = state.readingMode,
                                            zoomScale = zoomScale,
                                            isFitPage = state.isFitPage,
                                            searchMatches = searchMatchesByPage[rightPage] ?: emptyList(),
                                            activeMatch = activeMatch,
                                            selection = if (selectedTextState?.first == rightPage) selectedTextState?.second else null,
                                            onLongPressPage = { selectedPageForActions = it },
                                            onLongPressOffset = handleLongPressOffset
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                        }
                    }
                } else if (state.isContinuousScroll) {
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = contentPaddingValues,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(viewerHazeState)
                    ) {
                        items(state.pageCount, key = { it }) { pageIndex ->
                            PageItemView(
                                pageIndex = pageIndex,
                                viewModel = viewModel,
                                readingMode = state.readingMode,
                                zoomScale = zoomScale,
                                isFitPage = state.isFitPage,
                                searchMatches = searchMatchesByPage[pageIndex] ?: emptyList(),
                                activeMatch = activeMatch,
                                selection = if (selectedTextState?.first == pageIndex) selectedTextState?.second else null,
                                onLongPressPage = { selectedPageForActions = it },
                                onLongPressOffset = handleLongPressOffset
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        key = { it },
                        contentPadding = contentPaddingValues,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PageItemView(
                                pageIndex = pageIndex,
                                viewModel = viewModel,
                                readingMode = state.readingMode,
                                zoomScale = zoomScale,
                                isFitPage = state.isFitPage,
                                searchMatches = searchMatchesByPage[pageIndex] ?: emptyList(),
                                activeMatch = activeMatch,
                                selection = if (selectedTextState?.first == pageIndex) selectedTextState?.second else null,
                                onLongPressPage = { selectedPageForActions = it },
                                onLongPressOffset = handleLongPressOffset
                            )
                        }
                    }
                }
            }

            // Draggable Fast Scrollbar on side showing live thumbnail preview bubble
            FastScrollbar(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                renderThumbnail = { p, s -> viewModel.renderPageBitmap(p, s) },
                onPageSelected = { targetPage ->
                    viewModel.jumpToPage(targetPage)
                    coroutineScope.launch {
                        if (state.isContinuousScroll) {
                            lazyListState.scrollToItem(targetPage)
                        } else {
                            pagerState.scrollToPage(targetPage)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = topContentPadding, bottom = bottomContentPadding)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppEmptyState(
                    title = "No pages found",
                    message = "This document does not contain readable pages.",
                    actionLabel = "Go back",
                    onAction = onBackClick
                )
            }
        }

        // Top bar, search bar, thumbnail scrubber, and bottom bar controls
        PdfViewerControlsOverlay(
            document = document,
            state = state,
            viewModel = viewModel,
            isCurrentBookmarked = isCurrentBookmarked,
            onBackClick = onBackClick,
            onShareClick = onShareClick,
            onToggleBookmark = {
                val added = app?.settingsRepository?.toggleBookmark(document.uri, state.currentPage) ?: false
                bookmarks = app?.settingsRepository?.getBookmarks(document.uri) ?: emptySet()
                context.showMessage(if (added) "Bookmarked page ${state.currentPage + 1}" else "Bookmark removed")
            },
            onToolsClick = { isToolsSheetOpen = true },
            onOcrClick = {
                if (!isOcrRunning) {
                    isOcrRunning = true
                    context.showMessage("Extracting text from page ${state.currentPage + 1}...")
                    coroutineScope.launch {
                        try {
                            val pageBmp = viewModel.renderPageBitmap(state.currentPage, 2.0f)
                            if (pageBmp != null) {
                                val text = ocrEngine.recognizeText(pageBmp)
                                if (text.isBlank()) {
                                    context.showMessage("No text detected on this page")
                                } else {
                                    ocrExtractedText = text
                                }
                            } else {
                                context.showMessage("Unable to render page for OCR")
                            }
                        } catch (e: Exception) {
                            context.showMessage("OCR failed: ${e.message}")
                        } finally {
                            isOcrRunning = false
                        }
                    }
                }
            },
            onTtsClick = {
                coroutineScope.launch {
                    val pageText = viewModel.getPageText(state.currentPage)
                    if (pageText.isNotBlank()) {
                        currentTtsText = pageText
                        isTtsVisible = true
                    } else {
                        val pageBmp = viewModel.renderPageBitmap(state.currentPage, 1.5f)
                        if (pageBmp != null) {
                            val ocr = ocrEngine.recognizeText(pageBmp)
                            if (ocr.isNotBlank()) {
                                currentTtsText = ocr
                                isTtsVisible = true
                            } else {
                                context.showMessage("No text detected on this page to read")
                            }
                        }
                    }
                }
            },
            onSlideshowClick = onSlideshowClick,
            onReflowClick = onReflowClick,
            onDocumentDetailsClick = { isDocumentDetailsOpen = true },
            onOpenOutlineDrawer = { viewModel.setOutlineDrawerOpen(true) },
            onOpenThumbnailGrid = { isThumbnailGridOpen = true },
            onOpenReadingControls = { isReadingControlsOpen = true },
            isCondensed = isCondensed,
            onPageSelected = { targetPage ->
                viewModel.jumpToPage(targetPage)
                coroutineScope.launch {
                    if (state.isContinuousScroll) {
                        lazyListState.scrollToItem(targetPage)
                    } else {
                        pagerState.scrollToPage(targetPage)
                    }
                }
            },
            hazeState = viewerHazeState
        )

        // Modal Sheets, Drawers & Dialogs
        PdfViewerDialogsAndSheets(
            document = document,
            state = state,
            bookmarks = bookmarks,
            isReadingControlsOpen = isReadingControlsOpen,
            onDismissReadingControls = { isReadingControlsOpen = false },
            onReadingModeChange = viewModel::setReadingMode,
            onScrollModeChange = viewModel::setContinuousScroll,
            onDualPageViewChange = viewModel::setDualPageView,
            currentBrightness = currentBrightness,
            onBrightnessChange = updateBrightness,
            isKeepScreenOn = isKeepScreenOn,
            onKeepScreenOnChange = { newKeep ->
                isKeepScreenOn = newKeep
                app?.settingsRepository?.setKeepScreenOn(newKeep)
            },
            onFitPageChange = viewModel::setFitPage,
            isDocumentDetailsOpen = isDocumentDetailsOpen,
            onDismissDocumentDetails = { isDocumentDetailsOpen = false },
            onShareClick = { onShareClick() },
            isThumbnailGridOpen = isThumbnailGridOpen,
            onDismissThumbnailGrid = { isThumbnailGridOpen = false },
            onPageSelected = { targetPage ->
                viewModel.jumpToPage(targetPage)
                coroutineScope.launch {
                    if (state.isContinuousScroll) {
                        lazyListState.scrollToItem(targetPage)
                    } else {
                        pagerState.scrollToPage(targetPage)
                    }
                }
            },
            renderThumbnail = { p, s -> viewModel.renderPageBitmap(p, s) },
            isPageJumpDialogOpen = isPageJumpDialogOpen,
            onDismissPageJumpDialog = { isPageJumpDialogOpen = false },
            onJumpToPage = { targetPage ->
                viewModel.jumpToPage(targetPage)
                coroutineScope.launch {
                    if (state.isContinuousScroll) {
                        lazyListState.scrollToItem(targetPage)
                    } else {
                        pagerState.scrollToPage(targetPage)
                    }
                }
            },
            onDismissOutlineDrawer = { viewModel.setOutlineDrawerOpen(false) },
            onSelectOutlineItem = { page ->
                viewModel.jumpToPage(page)
                coroutineScope.launch {
                    if (state.isContinuousScroll) {
                        lazyListState.scrollToItem(page)
                    } else {
                        pagerState.scrollToPage(page)
                    }
                }
            },
            onRemoveBookmark = { page ->
                app?.settingsRepository?.toggleBookmark(document.uri, page)
                bookmarks = app?.settingsRepository?.getBookmarks(document.uri) ?: emptySet()
            },
            isToolsSheetOpen = isToolsSheetOpen,
            onDismissToolsSheet = { isToolsSheetOpen = false },
            onSelectTool = { tool ->
                if (onSelectTool != null) {
                    onSelectTool(tool)
                } else {
                    onToolsClick?.invoke()
                }
            },
            onAuthenticatePassword = viewModel::authenticatePassword,
            onCancelPassword = onBackClick,
            ocrExtractedText = ocrExtractedText,
            onDismissOcr = { ocrExtractedText = null },
            isTtsVisible = isTtsVisible,
            onCloseTts = { isTtsVisible = false },
            currentTtsText = currentTtsText,
            ttsManager = ttsManager,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Floating Contextual Action Pill (Copy, Share, Web Search, Read Aloud) & Action Sheet
        PdfContextualActionsHost(
            pageNum = selectedPageForActions,
            activeSelectionText = if (selectedTextState?.first == selectedPageForActions) selectedTextState?.second?.selectedText else null,
            readingMode = state.readingMode,
            viewModel = viewModel,
            ocrEngine = ocrEngine,
            ttsManager = ttsManager,
            onStartTts = { text ->
                currentTtsText = text
                isTtsVisible = true
            },
            onOcrResult = { ocr ->
                ocrExtractedText = ocr
            },
            onClose = {
                selectedPageForActions = null
                selectedTextState = null
            }
        )
    }
}
