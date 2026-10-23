package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Reusable viewport container for document reading that owns scale, offset,
 * pinch-to-zoom around centroid, inertia fling, clamped edge panning, and tap dispatching.
 */
@Composable
fun ZoomableViewport(
    zoomState: ZoomState,
    isContinuous: Boolean,
    isDualPage: Boolean,
    onToggleControls: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
    isDoubleTapEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val viewConfig = LocalViewConfiguration.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { newSize ->
                zoomState.viewportSize = newSize.toSize()
            }
            .pointerInput(
                zoomState,
                isContinuous,
                isDualPage,
                isDoubleTapEnabled,
                onToggleControls,
                onPrevPage,
                onNextPage
            ) {
                var pendingSingleTapJob: Job? = null
                var lastTapTime = 0L
                var lastTapPosition = Offset.Zero
                val doubleTapTimeoutMs = 200L
                val doubleTapDistanceSlop = 48.0f // pixels

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val downTime = System.currentTimeMillis()
                    val downPosition = down.position
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var isMultiTouch = false
                    var totalDragDistance = 0f
                    val touchSlop = viewConfig.touchSlop

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val pressedPointers = event.changes.filter { it.pressed }

                        if (pressedPointers.isEmpty()) {
                            // All fingers lifted
                            break
                        }

                        if (pressedPointers.size >= 2) {
                            // Two-finger pinch and pan
                            isMultiTouch = true
                            pendingSingleTapJob?.cancel()

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)

                            if (zoomChange != 1.0f || panChange != Offset.Zero) {
                                val currentScale = zoomState.scale
                                val newScale = (currentScale * zoomChange).coerceIn(
                                    ZoomMath.MIN_SCALE,
                                    ZoomMath.MAX_SCALE
                                )

                                val targetOffset = ZoomMath.calculateCentroidOffset(
                                    currentOffset = zoomState.offset,
                                    currentScale = currentScale,
                                    newScale = newScale,
                                    centroid = centroid,
                                    pan = panChange
                                )

                                zoomState.updateScaleAndOffsetWithRubberBand(newScale, targetOffset)

                                // Consume all pinch changes so child pager/list does not scroll
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pressedPointers.size == 1 && !isMultiTouch) {
                            // One-finger interaction
                            val change = pressedPointers.first()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val dragDelta = change.positionChange()
                            totalDragDistance += dragDelta.getDistance()

                            if (zoomState.scale > 1.0f) {
                                if (isContinuous || isDualPage) {
                                    // In continuous or dual-page mode:
                                    // Consume horizontal drags to pan the zoomed view.
                                    // Pass vertical drags to the LazyColumn so scrolling continues across pages!
                                    if (abs(dragDelta.x) > 0.5f) {
                                        val newOffsetX = zoomState.offset.x + dragDelta.x
                                        zoomState.updateScaleAndOffset(
                                            newScale = zoomState.scale,
                                            newOffset = Offset(newOffsetX, zoomState.offset.y)
                                        )
                                        change.consume()
                                    }
                                } else {
                                    // In paged mode (HorizontalPager):
                                    // Pan zoomed page. When at horizontal edge, allow pager to swipe!
                                    val (atLeft, atRight) = ZoomMath.isAtHorizontalEdge(
                                        offsetX = zoomState.offset.x,
                                        viewportWidth = zoomState.viewportSize.width,
                                        scale = zoomState.scale
                                    )

                                    val isSwipingPastLeft = atLeft && dragDelta.x > 0
                                    val isSwipingPastRight = atRight && dragDelta.x < 0

                                    if (isSwipingPastLeft || isSwipingPastRight) {
                                        // Let HorizontalPager take the swipe
                                    } else {
                                        // Consume both axes to pan zoomed content within bounds
                                        val newOffset = zoomState.offset + dragDelta
                                        zoomState.updateScaleAndOffset(zoomState.scale, newOffset)
                                        change.consume()
                                    }
                                }
                            } else {
                                // Scale == 1.0: Do not consume drag. Let child list or pager scroll naturally.
                            }
                        }
                    }

                    // Touch up
                    if (isMultiTouch) {
                        // Pinch ended: snap back from rubber-band
                        zoomState.updateScaleAndOffset(zoomState.scale, zoomState.offset)
                    } else if (totalDragDistance < touchSlop) {
                        // Considered a TAP (not a drag)
                        val now = System.currentTimeMillis()
                        val isDoubleTap = isDoubleTapEnabled &&
                            (now - lastTapTime) < doubleTapTimeoutMs &&
                            (downPosition - lastTapPosition).getDistance() < doubleTapDistanceSlop

                        if (isDoubleTap) {
                            // Cancel pending single tap
                            pendingSingleTapJob?.cancel()
                            lastTapTime = 0L

                            // Execute double-tap zoom
                            coroutineScope.launch {
                                if (zoomState.scale > 1.5f) {
                                    zoomState.animateTo(ZoomMath.MIN_SCALE, Offset.Zero)
                                } else {
                                    val targetOffset = ZoomMath.calculateCentroidOffset(
                                        currentOffset = Offset.Zero,
                                        currentScale = 1.0f,
                                        newScale = ZoomMath.DOUBLE_TAP_SCALE,
                                        centroid = downPosition
                                    )
                                    zoomState.animateTo(ZoomMath.DOUBLE_TAP_SCALE, targetOffset)
                                }
                            }
                        } else {
                            // Register single tap with short 200ms debounce
                            lastTapTime = now
                            lastTapPosition = downPosition

                            pendingSingleTapJob?.cancel()
                            pendingSingleTapJob = coroutineScope.launch {
                                delay(doubleTapTimeoutMs)
                                // Dispatch single tap action
                                if (!isContinuous && !isDualPage && zoomState.scale <= 1.05f) {
                                    val width = zoomState.viewportSize.width
                                    if (width > 0f) {
                                        when {
                                            downPosition.x < width * 0.2f -> onPrevPage()
                                            downPosition.x > width * 0.8f -> onNextPage()
                                            else -> onToggleControls()
                                        }
                                    } else {
                                        onToggleControls()
                                    }
                                } else {
                                    onToggleControls()
                                }
                            }
                        }
                    } else if (zoomState.scale > 1.0f) {
                        // Fling inertia on lift
                        val velocity = velocityTracker.calculateVelocity()
                        coroutineScope.launch {
                            zoomState.fling(Offset(velocity.x, velocity.y))
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.offset.x
                    translationY = zoomState.offset.y
                }
        ) {
            content()
        }
    }
}
