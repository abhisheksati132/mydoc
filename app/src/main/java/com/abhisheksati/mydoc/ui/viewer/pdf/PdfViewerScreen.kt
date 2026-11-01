package com.abhisheksati.mydoc.ui.viewer.pdf

import android.app.Activity
import android.app.SearchManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.ui.viewer.controls.DocumentToolsSheet
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import androidx.compose.ui.text.font.FontWeight
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.abhisheksati.mydoc.ui.viewer.controls.TileBitmapCache
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ui.viewer.controls.FastScrollbar
import com.abhisheksati.mydoc.ui.viewer.controls.OutlineDrawer
import com.abhisheksati.mydoc.ui.viewer.controls.PageGridSheet
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.ui.viewer.controls.PasswordDialog
import com.abhisheksati.mydoc.ui.viewer.controls.ReadingControlsBottomSheet
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import com.abhisheksati.mydoc.ui.viewer.controls.ThumbnailStrip
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerBottomBar
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerTopBar
import com.abhisheksati.mydoc.ui.viewer.controls.ZoomableViewport
import com.abhisheksati.mydoc.ui.viewer.controls.rememberZoomState
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    document: DocumentFile,
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToolsClick: (() -> Unit)? = null,
    onSelectTool: ((PdfTool) -> Unit)? = null,
    onDocumentUpdated: ((DocumentFile) -> Unit)? = null,
    onSlideshowClick: (() -> Unit)? = null,
    onReflowClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
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
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

    val pageBgColor = when (state.readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF000000)
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.EYE_COMFORT -> Color(0xFFF7F3E9)
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
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.jumpToPage(prev)
                        coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                    }
                },
                onNextPage = {
                    val next = (state.currentPage + 1).coerceAtMost(state.pageCount - 1)
                    if (next != state.currentPage) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    // Two-page spread for tablets and landscape
                    val pairCount = (state.pageCount + 1) / 2
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = contentPaddingValues,
                        modifier = Modifier.fillMaxSize()
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
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.pageCount, key = { it }) { pageIndex ->
                            PageItemView(
                                pageIndex = pageIndex,
                                viewModel = viewModel,
                                readingMode = state.readingMode,
                                zoomScale = zoomScale,
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

        // Clean, uncluttered Top Toolbar
        ViewerTopBar(
            title = document.displayName,
            currentPage = state.currentPage,
            pageCount = state.pageCount,
            visible = state.areControlsVisible && !state.isSearchOpen,
            isBookmarked = isCurrentBookmarked,
            onBackClick = onBackClick,
            onSearchClick = viewModel::openSearch,
            onToggleBookmark = {
                val added = app?.settingsRepository?.toggleBookmark(document.uri, state.currentPage) ?: false
                bookmarks = app?.settingsRepository?.getBookmarks(document.uri) ?: emptySet()
                Toast.makeText(context, if (added) "Bookmarked page ${state.currentPage + 1}" else "Bookmark removed", Toast.LENGTH_SHORT).show()
            },
            onShareClick = onShareClick,
            onToolsClick = { isToolsSheetOpen = true },
            onOcrClick = {
                if (!isOcrRunning) {
                    isOcrRunning = true
                    Toast.makeText(context, "Extracting text from page ${state.currentPage + 1}...", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        try {
                            val pageBmp = viewModel.renderPageBitmap(state.currentPage, 2.0f)
                            if (pageBmp != null) {
                                val text = ocrEngine.recognizeText(pageBmp)
                                if (text.isBlank()) {
                                    Toast.makeText(context, "No text detected on this page", Toast.LENGTH_SHORT).show()
                                } else {
                                    ocrExtractedText = text
                                }
                            } else {
                                Toast.makeText(context, "Unable to render page for OCR", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "No text detected on this page to read", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onSlideshowClick = onSlideshowClick,
            onReflowClick = onReflowClick,
            onDocumentDetailsClick = { isDocumentDetailsOpen = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Search Overlay
        SearchOverlay(
            visible = state.isSearchOpen,
            query = state.searchQuery,
            matchCount = state.searchMatches.size,
            currentMatchIndex = state.currentMatchIndex,
            onQueryChanged = viewModel::onSearchQueryChanged,
            onNextMatch = viewModel::nextSearchMatch,
            onPreviousMatch = viewModel::previousSearchMatch,
            onCloseSearch = viewModel::closeSearch,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Ergonomic Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            ThumbnailStrip(
                pageCount = state.pageCount,
                currentPage = state.currentPage,
                visible = state.isThumbnailStripVisible && state.areControlsVisible,
                onPageSelected = { page ->
                    viewModel.jumpToPage(page)
                    coroutineScope.launch {
                        if (state.isContinuousScroll) {
                            lazyListState.scrollToItem(page)
                        } else {
                            pagerState.scrollToPage(page)
                        }
                    }
                },
                renderThumbnail = { p, s -> viewModel.renderPageBitmap(p, s) }
            )

            ViewerBottomBar(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                visible = state.areControlsVisible,
                hasOutline = state.outline.isNotEmpty(),
                onOutlineClick = { viewModel.setOutlineDrawerOpen(true) },
                onPagesClick = { isThumbnailGridOpen = true },
                onAppearanceClick = { isReadingControlsOpen = true },
                onPageScrubbed = { targetPage ->
                    viewModel.jumpToPage(targetPage)
                    coroutineScope.launch {
                        if (state.isContinuousScroll) {
                            lazyListState.scrollToItem(targetPage)
                        } else {
                            pagerState.scrollToPage(targetPage)
                        }
                    }
                },
                sheetItems = state.outline,
                onSheetSelect = { pageIdx ->
                    viewModel.jumpToPage(pageIdx)
                    coroutineScope.launch {
                        if (state.isContinuousScroll) {
                            lazyListState.animateScrollToItem(pageIdx)
                        } else {
                            pagerState.animateScrollToPage(pageIdx)
                        }
                    }
                }
            )
        }

        // Consolidated Reading Controls Bottom Sheet
        ReadingControlsBottomSheet(
            isOpen = isReadingControlsOpen,
            onDismiss = { isReadingControlsOpen = false },
            readingMode = state.readingMode,
            onReadingModeChange = viewModel::setReadingMode,
            isContinuousScroll = state.isContinuousScroll,
            onScrollModeChange = viewModel::setContinuousScroll,
            isDualPageView = state.isDualPageView,
            onDualPageViewChange = viewModel::setDualPageView,
            currentBrightness = currentBrightness,
            onBrightnessChange = updateBrightness,
            isKeepScreenOn = isKeepScreenOn,
            onKeepScreenOnChange = { newKeep ->
                isKeepScreenOn = newKeep
                app?.settingsRepository?.setKeepScreenOn(newKeep)
            },
            document = document,
            isFitPage = state.isFitPage,
            onFitPageChange = viewModel::setFitPage
        )

        // Document Details Dialog
        if (isDocumentDetailsOpen) {
            com.abhisheksati.mydoc.ui.home.DocumentDetailsDialog(
                document = document,
                onDismiss = { isDocumentDetailsOpen = false },
                onOpen = { isDocumentDetailsOpen = false },
                onShare = { onShareClick() },
                onRemove = { isDocumentDetailsOpen = false }
            )
        }

        // 3-Column Page Thumbnail Grid Overview Sheet
        PageGridSheet(
            isOpen = isThumbnailGridOpen,
            pageCount = state.pageCount,
            currentPage = state.currentPage,
            bookmarks = bookmarks,
            onDismiss = { isThumbnailGridOpen = false },
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
            renderThumbnail = { p, s -> viewModel.renderPageBitmap(p, s) }
        )

        // Page Jump Dialog
        PageJumpDialog(
            isOpen = isPageJumpDialogOpen,
            currentPage = state.currentPage,
            pageCount = state.pageCount,
            onJump = { targetPage ->
                viewModel.jumpToPage(targetPage)
                coroutineScope.launch {
                    if (state.isContinuousScroll) {
                        lazyListState.scrollToItem(targetPage)
                    } else {
                        pagerState.scrollToPage(targetPage)
                    }
                }
            },
            onDismiss = { isPageJumpDialogOpen = false }
        )

        // Table of Contents & Bookmarks Drawer
        OutlineDrawer(
            outline = state.outline,
            bookmarks = bookmarks,
            isOpen = state.isOutlineDrawerOpen,
            onDismiss = { viewModel.setOutlineDrawerOpen(false) },
            onItemSelected = { page ->
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
            }
        )

        // Document In-Viewer Tools Sheet
        DocumentToolsSheet(
            isOpen = isToolsSheetOpen,
            onDismiss = { isToolsSheetOpen = false },
            document = document,
            onSelectTool = { tool ->
                if (onSelectTool != null) {
                    onSelectTool(tool)
                } else {
                    onToolsClick?.invoke()
                }
            }
        )

        // Password Dialog
        PasswordDialog(
            isOpen = state.isPasswordRequired,
            isIncorrect = state.isPasswordIncorrect,
            onSubmit = viewModel::authenticatePassword,
            onCancel = onBackClick
        )

        // OCR Result Modal Sheet
        if (ocrExtractedText != null) {
            com.abhisheksati.mydoc.ocr.OcrResultSheet(
                text = ocrExtractedText!!,
                onDismiss = { ocrExtractedText = null }
            )
        }

        // Text-to-Speech (TTS) Read Aloud floating playback controller
        com.abhisheksati.mydoc.ui.tts.DocumentTtsBar(
            ttsManager = ttsManager,
            documentText = currentTtsText,
            isVisible = isTtsVisible,
            onClose = { isTtsVisible = false },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Floating Contextual Action Pill (Copy, Share, Web Search, Read Aloud)
        if (selectedPageForActions != null) {
            val pageNum = selectedPageForActions!!
            val activeSelectionText = if (selectedTextState?.first == pageNum) selectedTextState?.second?.selectedText else null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 64.dp, start = 20.dp, end = 20.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = AppRadius.roundShape,
                    color = if (state.readingMode == ReadingMode.INVERTED_DARK) Color(0xF218181A) else Color(0xF8FFFFFF),
                    border = BorderStroke(0.5.dp, if (state.readingMode == ReadingMode.INVERTED_DARK) Color(0xFF2A2A2E) else Color(0xFFE2E2DD)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. Copy
                        FloatingPillItem(
                            icon = Icons.Outlined.ContentCopy,
                            label = if (activeSelectionText != null) "Copy" else "Copy Page",
                            tint = colors.accent
                        ) {
                            coroutineScope.launch {
                                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                                if (text.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("PDF Text", text))
                                    Toast.makeText(context, if (activeSelectionText != null) "Text copied to clipboard" else "Page ${pageNum + 1} text copied", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No text detected on this page", Toast.LENGTH_SHORT).show()
                                }
                                selectedPageForActions = null
                                selectedTextState = null
                            }
                        }

                        // 2. Share
                        FloatingPillItem(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            tint = Color(0xFF10B981)
                        ) {
                            coroutineScope.launch {
                                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                                if (text.isNotBlank()) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Text"))
                                } else {
                                    Toast.makeText(context, "No text to share", Toast.LENGTH_SHORT).show()
                                }
                                selectedPageForActions = null
                                selectedTextState = null
                            }
                        }

                        // 3. Web Search
                        FloatingPillItem(
                            icon = Icons.Outlined.TextFields,
                            label = "Search",
                            tint = Color(0xFF5B4FE8)
                        ) {
                            coroutineScope.launch {
                                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                                if (text.isNotBlank()) {
                                    val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                        putExtra(SearchManager.QUERY, text.take(150))
                                    }
                                    try {
                                        context.startActivity(searchIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "No text to search", Toast.LENGTH_SHORT).show()
                                }
                                selectedPageForActions = null
                                selectedTextState = null
                            }
                        }

                        // 4. Read Aloud
                        FloatingPillItem(
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                            label = "Speak",
                            tint = Color(0xFFAF52DE)
                        ) {
                            coroutineScope.launch {
                                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                                if (text.isNotBlank()) {
                                    currentTtsText = text
                                    isTtsVisible = true
                                    ttsManager.startReading(text)
                                } else {
                                    Toast.makeText(context, "No text to read", Toast.LENGTH_SHORT).show()
                                }
                                selectedPageForActions = null
                                selectedTextState = null
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable {
                                    selectedPageForActions = null
                                    selectedTextState = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", style = typography.caption.copy(fontWeight = FontWeight.Bold), color = colors.textSecondary)
                        }
                    }
                }
            }
        }

        // Page Contextual Action Sheet (Copy, Read, Share, OCR)
        if (selectedPageForActions != null) {
            val pageNum = selectedPageForActions!!
            AppBottomSheet(
                isOpen = true,
                onDismiss = { selectedPageForActions = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.paddingLarge)
                ) {
                    Text(
                        text = "Page ${pageNum + 1} Actions",
                        style = typography.title,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Copy Page Text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                coroutineScope.launch {
                                    val text = viewModel.getPageText(pageNum)
                                    if (text.isNotBlank()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Page ${pageNum + 1}", text))
                                        Toast.makeText(context, "Page ${pageNum + 1} text copied to clipboard", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No digital text on this page. Try OCR extract.", Toast.LENGTH_SHORT).show()
                                    }
                                    selectedPageForActions = null
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = colors.accent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Copy Page Text", style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text("Copy all digital text from this page to clipboard", style = typography.caption, color = colors.textSecondary)
                        }
                    }

                    // 2. Read Page Aloud (TTS)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                coroutineScope.launch {
                                    val text = viewModel.getPageText(pageNum)
                                    if (text.isNotBlank()) {
                                        currentTtsText = text
                                        isTtsVisible = true
                                    } else {
                                        Toast.makeText(context, "No text on this page to read", Toast.LENGTH_SHORT).show()
                                    }
                                    selectedPageForActions = null
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = colors.accent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Read Page Aloud (TTS)", style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text("Listen to this page with offline speech synthesizer", style = typography.caption, color = colors.textSecondary)
                        }
                    }

                    // 3. Share Page Text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                coroutineScope.launch {
                                    val text = viewModel.getPageText(pageNum)
                                    if (text.isNotBlank()) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Page ${pageNum + 1} Text"))
                                    } else {
                                        Toast.makeText(context, "No text on this page to share", Toast.LENGTH_SHORT).show()
                                    }
                                    selectedPageForActions = null
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = colors.accent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Share Page Text", style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text("Share page text to WhatsApp, Messages, or Email", style = typography.caption, color = colors.textSecondary)
                        }
                    }

                    // 4. OCR Extract Text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedPageForActions = null
                                coroutineScope.launch {
                                    val pageBmp = viewModel.renderPageBitmap(pageNum, 2.0f)
                                    if (pageBmp != null) {
                                        val ocr = ocrEngine.recognizeText(pageBmp)
                                        if (ocr.isNotBlank()) {
                                            ocrExtractedText = ocr
                                        } else {
                                            Toast.makeText(context, "No text found with OCR", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.TextFields, contentDescription = null, tint = colors.accent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Extract Text with OCR", style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text("Recognize printed or handwritten text on images", style = typography.caption, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingPillItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val typography = LocalAppTypography.current
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp), color = colors.textPrimary)
    }
}
