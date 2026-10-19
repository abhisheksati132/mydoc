package com.abhisheksati.mydoc.ui.viewer.pdf

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ui.viewer.controls.FastScrollbar
import com.abhisheksati.mydoc.ui.viewer.controls.OutlineDrawer
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.ui.viewer.controls.PasswordDialog
import com.abhisheksati.mydoc.ui.viewer.controls.ReadingControlsBottomSheet
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import com.abhisheksati.mydoc.ui.viewer.controls.ThumbnailStrip
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerBottomBar
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerTopBar
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfViewerScreen(
    document: DocumentFile,
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToolsClick: (() -> Unit)? = null,
    onDocumentUpdated: ((DocumentFile) -> Unit)? = null,
    onSlideshowClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val colors = LocalAppColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isPageJumpDialogOpen by remember { mutableStateOf(false) }
    var isReadingControlsOpen by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf<String?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }
    val ocrEngine = remember { com.abhisheksati.mydoc.ocr.OcrEngine(context) }

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

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentPage)
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.pageCount }
    )

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

    val pageBgColor = when (state.readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF000000)
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
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

            // Main viewport with double-tap zoom, pinch-pan, and tap-zones
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.isContinuousScroll, state.currentPage, state.pageCount) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (!state.isContinuousScroll && !state.isDualPageView) {
                                    val viewWidth = size.width.toFloat()
                                    if (offset.x < viewWidth * 0.2f) {
                                        // Left 20% zone: Previous page
                                        val prev = (state.currentPage - 1).coerceAtLeast(0)
                                        if (prev != state.currentPage) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.jumpToPage(prev)
                                            coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                                        }
                                    } else if (offset.x > viewWidth * 0.8f) {
                                        // Right 20% zone: Next page
                                        val next = (state.currentPage + 1).coerceAtMost(state.pageCount - 1)
                                        if (next != state.currentPage) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.jumpToPage(next)
                                            coroutineScope.launch { pagerState.animateScrollToPage(next) }
                                        }
                                    } else {
                                        viewModel.toggleControls()
                                    }
                                } else {
                                    viewModel.toggleControls()
                                }
                            },
                            onDoubleTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                zoomScale = if (zoomScale > 1.5f) 1f else 2.5f
                                panOffset = Offset.Zero
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                            if (zoomScale > 1f) {
                                val maxPan = 500f * (zoomScale - 1f)
                                panOffset = Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                                    y = (panOffset.y + pan.y).coerceIn(-maxPan, maxPan)
                                )
                            } else {
                                panOffset = Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            ) {
                val searchMatchesByPage = remember(state.searchMatches) {
                    state.searchMatches.groupBy { it.pageIndex }
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
                                        searchMatches = searchMatchesByPage[leftPage] ?: emptyList()
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    if (rightPage < state.pageCount) {
                                        PageItemView(
                                            pageIndex = rightPage,
                                            viewModel = viewModel,
                                            readingMode = state.readingMode,
                                            zoomScale = zoomScale,
                                            searchMatches = searchMatchesByPage[rightPage] ?: emptyList()
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
                                searchMatches = searchMatchesByPage[pageIndex] ?: emptyList()
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
                                searchMatches = searchMatchesByPage[pageIndex] ?: emptyList()
                            )
                        }
                    }
                }
            }

            // Draggable Fast Scrollbar on side showing page bubble
            FastScrollbar(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
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
            visible = state.areControlsVisible && !state.isSearchOpen,
            isBookmarked = isCurrentBookmarked,
            onBackClick = onBackClick,
            onShareClick = onShareClick,
            onToggleBookmark = {
                val added = app?.settingsRepository?.toggleBookmark(document.uri, state.currentPage) ?: false
                bookmarks = app?.settingsRepository?.getBookmarks(document.uri) ?: emptySet()
                Toast.makeText(context, if (added) "Bookmarked page ${state.currentPage + 1}" else "Bookmark removed", Toast.LENGTH_SHORT).show()
            },
            onToolsClick = onToolsClick,
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
            onSlideshowClick = onSlideshowClick,
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
                isThumbnailStripOpen = state.isThumbnailStripVisible,
                onSearchClick = viewModel::openSearch,
                onOutlineClick = { viewModel.setOutlineDrawerOpen(true) },
                onToggleThumbnails = viewModel::toggleThumbnailStrip,
                onOpenSettings = { isReadingControlsOpen = true },
                onPageTextClick = { isPageJumpDialogOpen = true },
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
            document = document
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
    searchMatches: List<com.abhisheksati.mydoc.rendering.engine.SearchMatch>
) {
    val (origWidth, origHeight) = remember(pageIndex) { viewModel.getPageSize(pageIndex) }
    val aspectRatio = if (origHeight > 0) origWidth / origHeight else 0.707f
    val targetScale = remember(zoomScale) {
        if (zoomScale > 1.2f) (1.8f * zoomScale).coerceAtMost(3.6f) else 1.8f
    }

    @Suppress("ProduceStateDoesNotAssignValue")
    val bitmap by produceState<Bitmap?>(initialValue = null, pageIndex, readingMode, targetScale) {
        // Step 1: Render low-resolution preview immediately for instant feedback
        val lowRes = withContext(Dispatchers.Default) {
            viewModel.renderPageBitmap(pageIndex, 0.35f)
        }
        value = lowRes

        // Step 2: Sharpen to full high resolution (adjusts dynamically with zoom)
        val highRes = withContext(Dispatchers.Default) {
            viewModel.renderPageBitmap(pageIndex, targetScale)
        }
        if (highRes != null) {
            value = highRes
        }
    }

    val itemBg = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF000000) // True AMOLED Black
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.NORMAL -> Color(0xFFFAFAFC)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(itemBg)
    ) {
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize()
            )

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

                        drawRect(
                            color = Color(0x66FFEB3B),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top)
                        )
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
