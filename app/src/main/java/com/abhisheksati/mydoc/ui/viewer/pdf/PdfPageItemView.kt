package com.abhisheksati.mydoc.ui.viewer.pdf

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ui.viewer.controls.TileBitmapCache
import com.abhisheksati.mydoc.rendering.engine.SearchMatch
import com.abhisheksati.mydoc.rendering.engine.TextSelectionResult
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.abhisheksati.mydoc.designsystem.tokens.DocumentCanvasColors

/**
 * High-performance PDF page renderer with debounced progressive rendering,
 * centroid tile sharp-zoom overlays, search match highlights, and text selection quads.
 */
@Composable
internal fun PageItemView(
    pageIndex: Int,
    viewModel: ViewerViewModel,
    readingMode: ReadingMode,
    zoomScale: Float = 1f,
    searchMatches: List<SearchMatch>,
    activeMatch: SearchMatch? = null,
    selection: TextSelectionResult? = null,
    onLongPressPage: ((Int) -> Unit)? = null,
    onLongPressOffset: ((Int, Offset, Size) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val (origWidth, origHeight) = remember(pageIndex) { viewModel.getPageSize(pageIndex) }
    val rawRatio = if (origHeight > 0f && origWidth > 0f) origWidth / origHeight else 0.707f
    val aspectRatio = if (rawRatio.isFinite() && rawRatio > 0.05f) rawRatio else 0.707f

    // Base bitmap: Two-step load (instant cache hit / low-res preview, then 60ms fling-debounced 1.8x base)
    @Suppress("ProduceStateDoesNotAssignValue")
    val baseBitmap by produceState<Bitmap?>(initialValue = null, pageIndex, readingMode) {
        val cachedHigh = viewModel.getCachedPageBitmap(pageIndex, 1.8f)
        if (cachedHigh != null) {
            value = cachedHigh
            return@produceState
        }

        val lowRes = withContext(Dispatchers.Default) {
            viewModel.renderPageBitmap(pageIndex, 0.35f)
        }
        if (lowRes != null) {
            value = lowRes
        }

        delay(60)

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
        ReadingMode.INVERTED_DARK -> DocumentCanvasColors.blackPage // True AMOLED Black
        ReadingMode.SEPIA -> DocumentCanvasColors.sepiaPage
        ReadingMode.EYE_COMFORT -> DocumentCanvasColors.eyeComfortPage
        ReadingMode.NORMAL -> DocumentCanvasColors.pdfPage
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
                    onLongPress = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (onLongPressOffset != null) {
                            onLongPressOffset.invoke(pageIndex, offset, Size(size.width.toFloat(), size.height.toFloat()))
                        } else {
                            onLongPressPage?.invoke(pageIndex)
                        }
                    }
                )
            }
    ) {
        val bmp = baseBitmap
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                colorFilter = pageColorFilter,
                modifier = Modifier.fillMaxSize()
            )

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
                            drawRect(
                                color = DocumentCanvasColors.annotationOrange,
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top)
                            )
                            drawRect(
                                color = DocumentCanvasColors.annotationRed,
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        } else {
                            drawRect(
                                color = DocumentCanvasColors.searchHighlight,
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top)
                            )
                        }
                    }
                }
            }

            val sel = selection
            if (sel != null && sel.highlightRects.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / origWidth
                    val scaleY = size.height / origHeight
                    for (rect in sel.highlightRects) {
                        val left = rect.left * scaleX
                        val top = rect.top * scaleY
                        val right = rect.right * scaleX
                        val bottom = rect.bottom * scaleY
                        drawRect(
                            color = DocumentCanvasColors.annotationAccent,
                            topLeft = Offset(left, top),
                            size = Size((right - left).coerceAtLeast(4f), (bottom - top).coerceAtLeast(4f))
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
