package com.abhisheksati.mydoc.ui.viewer.pdf

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ui.theme.Spacing
import com.abhisheksati.mydoc.ui.viewer.controls.BrightnessDialog
import com.abhisheksati.mydoc.ui.viewer.controls.OutlineDrawer
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.ui.viewer.controls.PasswordDialog
import com.abhisheksati.mydoc.ui.viewer.controls.PrintHelper
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isPageJumpDialogOpen by remember { mutableStateOf(false) }

    var bookmarks by remember {
        mutableStateOf(app?.settingsRepository?.getBookmarks(document.uri) ?: emptySet())
    }
    val isCurrentBookmarked = bookmarks.contains(state.currentPage)

    var isBrightnessDialogOpen by remember { mutableStateOf(false) }
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
                if (state.isContinuousScroll) {
                    viewModel.onPageChanged(page)
                }
            }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (!state.isContinuousScroll) {
                    viewModel.onPageChanged(page)
                }
            }
    }

    val pageBgColor = when (state.readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF000000)
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.NORMAL -> Color(0xFF141414)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBgColor)
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.errorMessage ?: "Unknown error",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(Spacing.large)
                )
            }
        } else if (state.pageCount > 0) {
            // Main viewport with pinch zoom and pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { viewModel.toggleControls() },
                            onDoubleTap = {
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
                if (state.isContinuousScroll) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.pageCount) { pageIndex ->
                            PageItemView(
                                pageIndex = pageIndex,
                                viewModel = viewModel,
                                readingMode = state.readingMode,
                                searchMatches = state.searchMatches.filter { it.pageIndex == pageIndex }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
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
                                searchMatches = state.searchMatches.filter { it.pageIndex == pageIndex }
                            )
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No pages found in this document.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(Spacing.large)
                )
            }
        }

        // Top Toolbar
        ViewerTopBar(
            title = document.displayName,
            visible = state.areControlsVisible && !state.isSearchOpen,
            readingMode = state.readingMode,
            isContinuousScroll = state.isContinuousScroll,
            hasOutline = state.outline.isNotEmpty(),
            isKeepScreenOn = isKeepScreenOn,
            isBookmarked = isCurrentBookmarked,
            onBackClick = onBackClick,
            onSearchClick = viewModel::openSearch,
            onToggleReadingMode = viewModel::toggleReadingMode,
            onToggleScrollMode = viewModel::toggleScrollMode,
            onOutlineClick = { viewModel.setOutlineDrawerOpen(true) },
            onShareClick = onShareClick,
            onToggleBookmark = {
                val added = app?.settingsRepository?.toggleBookmark(document.uri, state.currentPage) ?: false
                bookmarks = app?.settingsRepository?.getBookmarks(document.uri) ?: emptySet()
                Toast.makeText(context, if (added) "Bookmarked page ${state.currentPage + 1}" else "Bookmark removed", Toast.LENGTH_SHORT).show()
            },
            onBrightnessClick = { isBrightnessDialogOpen = true },
            onOpenWithClick = {
                val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                    val mime = document.mimeType.ifEmpty { "application/pdf" }
                    setDataAndType(Uri.parse(document.uri), mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(sendIntent, "Open with"))
                } catch (_: Exception) {
                    Toast.makeText(context, "No app found to open this document", Toast.LENGTH_SHORT).show()
                }
            },
            onPrintClick = {
                PrintHelper.printDocument(context, document)
            },
            onToggleKeepScreenOn = {
                val newKeep = !isKeepScreenOn
                isKeepScreenOn = newKeep
                app?.settingsRepository?.setKeepScreenOn(newKeep)
            },
            onToggleOrientation = {
                val activity = context as? Activity
                if (activity != null) {
                    val isLandscape = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    activity.requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
            },
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

        // Bottom Controls
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
                isThumbnailStripOpen = state.isThumbnailStripVisible,
                onPageJump = { page ->
                    viewModel.jumpToPage(page)
                    coroutineScope.launch {
                        if (state.isContinuousScroll) {
                            lazyListState.scrollToItem(page)
                        } else {
                            pagerState.scrollToPage(page)
                        }
                    }
                },
                onToggleThumbnails = viewModel::toggleThumbnailStrip,
                onPageTextClick = { isPageJumpDialogOpen = true }
            )
        }

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

        // Brightness Dimmer Dialog
        BrightnessDialog(
            isOpen = isBrightnessDialogOpen,
            currentBrightness = currentBrightness,
            onBrightnessChanged = updateBrightness,
            onDismiss = { isBrightnessDialogOpen = false }
        )

        // Password Dialog
        PasswordDialog(
            isOpen = state.isPasswordRequired,
            isIncorrect = state.isPasswordIncorrect,
            onSubmit = viewModel::authenticatePassword,
            onCancel = onBackClick
        )
    }
}

@Composable
private fun PageItemView(
    pageIndex: Int,
    viewModel: ViewerViewModel,
    readingMode: ReadingMode,
    searchMatches: List<com.abhisheksati.mydoc.rendering.engine.SearchMatch>
) {
    val (origWidth, origHeight) = remember(pageIndex) { viewModel.getPageSize(pageIndex) }
    val aspectRatio = if (origHeight > 0) origWidth / origHeight else 0.707f

    val bitmap by produceState<Bitmap?>(initialValue = null, pageIndex, readingMode) {
        value = withContext(Dispatchers.Default) {
            // Render at high resolution for crystal clarity
            viewModel.renderPageBitmap(pageIndex, 1.8f)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(
                if (readingMode == ReadingMode.INVERTED_DARK) Color.Black
                else if (readingMode == ReadingMode.SEPIA) Color(0xFFF4ECD8)
                else Color.White
            )
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
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
