package com.abhisheksati.mydoc.ui.viewer.pdf

import android.app.Activity
import android.app.SearchManager
import com.abhisheksati.mydoc.designsystem.components.liquidGlass
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
                                        onLongPressPage = { selectedPageForActions = it }
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
                                            onLongPressPage = { selectedPageForActions = it }
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
                                onLongPressPage = { selectedPageForActions = it }
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
                                onLongPressPage = { selectedPageForActions = it }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 64.dp, start = 20.dp, end = 20.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .liquidGlass(
                            isDarkTheme = state.readingMode == ReadingMode.INVERTED_DARK,
                            shape = RoundedCornerShape(28.dp),
                            elevation = 8.dp
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. Copy
                        FloatingPillItem(
                            icon = Icons.Outlined.ContentCopy,
                            label = "Copy",
                            tint = colors.accent
                        ) {
                            coroutineScope.launch {
                                val text = viewModel.getPageText(pageNum)
                                if (text.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Page ${pageNum + 1}", text))
                                    Toast.makeText(context, "Page ${pageNum + 1} text copied", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No text detected on this page", Toast.LENGTH_SHORT).show()
                                }
                                selectedPageForActions = null
                            }
                        }

                        // 2. Share
                        FloatingPillItem(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            tint = Color(0xFF10B981)
                        ) {
                            coroutineScope.launch {
                                val text = viewModel.getPageText(pageNum)
                                if (text.isNotBlank()) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Page ${pageNum + 1}"))
                                } else {
                                    Toast.makeText(context, "No text to share", Toast.LENGTH_SHORT).show()
                                }
                                selectedPageForActions = null
                            }
                        }

                        // 3. Web Search
                        FloatingPillItem(
                            icon = Icons.Outlined.TextFields,
                            label = "Web Search",
                            tint = Color(0xFF5B4FE8)
                        ) {
                            coroutineScope.launch {
                                val text = viewModel.getPageText(pageNum)
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
                            }
                        }

                        // 4. Read Aloud
                        FloatingPillItem(
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                            label = "Speak",
                            tint = Color(0xFFAF52DE)
                        ) {
                            coroutineScope.launch {
                                val text = viewModel.getPageText(pageNum)
                                if (text.isNotBlank()) {
                                    currentTtsText = text
                                    isTtsVisible = true
                                } else {
                                    Toast.makeText(context, "No text to read", Toast.LENGTH_SHORT).show()
                                }
                                selectedPageForActions = null
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { selectedPageForActions = null },
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

/**
 * Page renderer with immediate low-resolution preview followed by progressive sharpening.
 * Fulfills: "Show content immediately: render a low-resolution page preview the instant a file opens, then sharpen it."
 */
@Composable
private fun PageItemView(
    pageIndex: Int,
    viewModel: ViewerViewModel,
    readingMode: ReadingMode,
    zoomScale: Float = 1f,
    searchMatches: List<com.abhisheksati.mydoc.rendering.engine.SearchMatch>,
    activeMatch: com.abhisheksati.mydoc.rendering.engine.SearchMatch? = null,
    onLongPressPage: ((Int) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val (origWidth, origHeight) = remember(pageIndex) { viewModel.getPageSize(pageIndex) }
    val rawRatio = if (origHeight > 0f && origWidth > 0f) origWidth / origHeight else 0.707f
    val aspectRatio = if (rawRatio.isFinite() && rawRatio > 0.05f) rawRatio else 0.707f

    // Base bitmap: Two-step load (0.35x instant thumbnail, then 1.8x high-res base)
    @Suppress("ProduceStateDoesNotAssignValue")
    val baseBitmap by produceState<Bitmap?>(initialValue = null, pageIndex, readingMode) {
        val lowRes = withContext(Dispatchers.Default) {
            viewModel.renderPageBitmap(pageIndex, 0.35f)
        }
        value = lowRes

        val highRes = withContext(Dispatchers.Default) {
            viewModel.renderPageBitmap(pageIndex, 1.8f)
        }
        if (highRes != null) {
            value = highRes
        }
    }

    // High-scale Sharp Zoom Tile state
    var pageLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var visibleRegion by remember { mutableStateOf<RectF?>(null) }
    var tileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tileRegion by remember { mutableStateOf<RectF?>(null) }

    val tileAlpha by animateFloatAsState(
        targetValue = if (tileBitmap != null) 1.0f else 0.0f,
        animationSpec = tween(120),
        label = "tileCrossfade"
    )

    // Debounced region rendering: starts 150ms after gesture settles when zoom > 1.3x
    LaunchedEffect(pageIndex, readingMode, zoomScale > 1.3f, visibleRegion) {
        if (zoomScale > 1.3f && visibleRegion != null) {
            val reg = visibleRegion!!
            if (reg.width() > 0.02f && reg.height() > 0.02f) {
                delay(150) // Debounce 150ms
                val docUri = viewModel.documentUri
                val key = TileBitmapCache.makeKey(docUri, pageIndex, reg, zoomScale, readingMode)
                val cached = TileBitmapCache.get(key)
                if (cached != null) {
                    tileBitmap = cached
                    tileRegion = reg
                } else {
                    val coords = pageLayoutCoordinates
                    if (coords != null && coords.isAttached) {
                        val pageW = coords.boundsInWindow().width
                        val overlapW = (reg.width() * pageW).toInt().coerceAtLeast(1)
                        val overlapH = (reg.height() * (pageW / aspectRatio.coerceAtLeast(0.05f))).toInt().coerceAtLeast(1)

                        // Cap to 24 MB and max dimension 4096 px
                        val maxPixels = (24 * 1024 * 1024) / 4
                        var targetW = overlapW.coerceAtMost(4096)
                        var targetH = overlapH.coerceAtMost(4096)
                        if (targetW.toLong() * targetH > maxPixels) {
                            val ratio = kotlin.math.sqrt(maxPixels.toDouble() / (targetW.toLong() * targetH))
                            targetW = (targetW * ratio).toInt().coerceAtLeast(1)
                            targetH = (targetH * ratio).toInt().coerceAtLeast(1)
                        }

                        val rendered = withContext(Dispatchers.Default) {
                            try {
                                viewModel.renderRegionBitmap(
                                    pageIndex = pageIndex,
                                    region = reg,
                                    pageWidthPx = pageW,
                                    outWidth = targetW,
                                    outHeight = targetH
                                )
                            } catch (_: Throwable) {
                                null
                            }
                        }
                        if (rendered != null) {
                            TileBitmapCache.put(key, rendered)
                            tileBitmap = rendered
                            tileRegion = reg
                        }
                    }
                }
            }
        } else {
            tileBitmap = null
            tileRegion = null
        }
    }

    DisposableEffect(pageIndex) {
        onDispose {
            tileBitmap = null
            tileRegion = null
        }
    }

    val itemBg = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF000000) // True AMOLED Black
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.EYE_COMFORT -> Color(0xFFF7F3E9)
        ReadingMode.NORMAL -> Color(0xFFFAFAFC)
    }

    val pageColorFilter = when (readingMode) {
        ReadingMode.INVERTED_DARK -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            ))
        )
        ReadingMode.SEPIA -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            ))
        )
        ReadingMode.EYE_COMFORT -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                1.0f,  0f,    0f,    0f, 0f,
                0f,    0.93f, 0f,    0f, 0f,
                0f,    0f,    0.82f, 0f, 0f,
                0f,    0f,    0f,    1f, 0f
            ))
        )
        ReadingMode.NORMAL -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(itemBg)
            .onGloballyPositioned { coordinates ->
                pageLayoutCoordinates = coordinates
                val root = coordinates.findRootCoordinates()
                val pageInWindow = coordinates.boundsInWindow()
                val windowRect = Rect(0f, 0f, root.size.width.toFloat(), root.size.height.toFloat())
                val overlap = pageInWindow.intersect(windowRect)
                if (overlap.width > 0f && overlap.height > 0f && pageInWindow.width > 0f && pageInWindow.height > 0f) {
                    val relL = ((overlap.left - pageInWindow.left) / pageInWindow.width).coerceIn(0f, 1f)
                    val relT = ((overlap.top - pageInWindow.top) / pageInWindow.height).coerceIn(0f, 1f)
                    val relR = ((overlap.right - pageInWindow.left) / pageInWindow.width).coerceIn(0f, 1f)
                    val relB = ((overlap.bottom - pageInWindow.top) / pageInWindow.height).coerceIn(0f, 1f)
                    visibleRegion = RectF(relL, relT, relR, relB)
                } else {
                    visibleRegion = null
                }
            }
            .pointerInput(pageIndex) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPressPage?.invoke(pageIndex)
                    }
                )
            }
    ) {
        val bmp = baseBitmap
        if (bmp != null && !bmp.isRecycled) {
            // Base layer: Always displays the stable 1.8x base bitmap
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                colorFilter = pageColorFilter,
                modifier = Modifier.fillMaxSize()
            )

            // Sharp zoom tile overlay: Displayed with smooth 120ms crossfade
            val tile = tileBitmap
            val reg = tileRegion
            if (tile != null && !tile.isRecycled && reg != null && tileAlpha > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dstLeft = reg.left * size.width
                    val dstTop = reg.top * size.height
                    val dstW = (reg.right - reg.left) * size.width
                    val dstH = (reg.bottom - reg.top) * size.height
                    drawImage(
                        image = tile.asImageBitmap(),
                        dstOffset = androidx.compose.ui.unit.IntOffset(dstLeft.toInt(), dstTop.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1)),
                        alpha = tileAlpha,
                        colorFilter = pageColorFilter
                    )
                }
            }

            // Draw search match highlights over the page
            if (searchMatches.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / origWidth
                    val scaleY = size.height / origHeight

                    for (match in searchMatches) {
                        val left = match.bounds.left * scaleX
                        val top = match.bounds.top * scaleY
                        val right = match.bounds.right * scaleX
                        val bottom = match.bounds.bottom * scaleY
                        val isCurrent = match == activeMatch

                        if (isCurrent) {
                            // High-visibility glowing orange highlight for current active match
                            drawRect(
                                color = Color(0xCCFF9800),
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top)
                            )
                            drawRect(
                                color = Color(0xFFFF5722),
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        } else {
                            // Soft translucent yellow for passive matches
                            drawRect(
                                color = Color(0x66FFEB3B),
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top)
                            )
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppProgressIndicator()
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
