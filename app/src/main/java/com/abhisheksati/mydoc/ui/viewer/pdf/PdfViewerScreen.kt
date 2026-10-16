package com.abhisheksati.mydoc.ui.viewer.pdf

import android.graphics.Bitmap
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ui.theme.Spacing
import com.abhisheksati.mydoc.ui.viewer.controls.OutlineDrawer
import com.abhisheksati.mydoc.ui.viewer.controls.PasswordDialog
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import com.abhisheksati.mydoc.ui.viewer.controls.ThumbnailStrip
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerBottomBar
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerTopBar
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@Composable
fun PdfViewerScreen(
    document: DocumentFile,
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentPage)
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.pageCount }
    )

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
        }

        // Top Toolbar
        ViewerTopBar(
            title = document.displayName,
            visible = state.areControlsVisible && !state.isSearchOpen,
            readingMode = state.readingMode,
            isContinuousScroll = state.isContinuousScroll,
            hasOutline = state.outline.isNotEmpty(),
            onBackClick = onBackClick,
            onSearchClick = viewModel::openSearch,
            onToggleReadingMode = viewModel::toggleReadingMode,
            onToggleScrollMode = viewModel::toggleScrollMode,
            onOutlineClick = { viewModel.setOutlineDrawerOpen(true) },
            onShareClick = onShareClick,
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
                    if (state.isContinuousScroll) {
                        // Launch scroll
                    }
                },
                renderThumbnail = { p, s -> viewModel.renderPageBitmap(p, s) }
            )

            ViewerBottomBar(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                visible = state.areControlsVisible,
                isThumbnailStripOpen = state.isThumbnailStripVisible,
                onPageJump = viewModel::jumpToPage,
                onToggleThumbnails = viewModel::toggleThumbnailStrip
            )
        }

        // Table of Contents Drawer
        OutlineDrawer(
            outline = state.outline,
            isOpen = state.isOutlineDrawerOpen,
            onDismiss = { viewModel.setOutlineDrawerOpen(false) },
            onItemSelected = viewModel::jumpToPage
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
