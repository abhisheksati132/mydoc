package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
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
    showZoomControls: Boolean = true,
    zoomControlsAtTop: Boolean = false,
    onVerticalScroll: ((Float) -> Float)? = null,
    onHorizontalScroll: ((Float) -> Float)? = null,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val viewConfig = LocalViewConfiguration.current

    // Callers pass freshly-allocated lambdas on every recomposition. Keying the gesture
    // detector on them restarted it mid-pinch; read the latest through state instead.
    val latestOnHorizontalScroll by rememberUpdatedState(onHorizontalScroll)
    val latestOnVerticalScroll by rememberUpdatedState(onVerticalScroll)
    val latestOnToggleControls by rememberUpdatedState(onToggleControls)
    val latestOnPrevPage by rememberUpdatedState(onPrevPage)
    val latestOnNextPage by rememberUpdatedState(onNextPage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { newSize ->
                zoomState.viewportSize = newSize.toSize()
                zoomState.updateScaleAndOffset(zoomState.scale, zoomState.offset)
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clipToBounds()
            .pointerInput(
                zoomState,
                isContinuous,
                isDualPage,
                isDoubleTapEnabled
            ) {
                var motionJob: Job? = null
                var pendingSingleTapJob: Job? = null
                var lastTapTime = 0L
                var lastTapPosition = Offset.Zero
                val doubleTapTimeoutMs = 200L
                val doubleTapDistanceSlop = 48.0f // pixels

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    motionJob?.cancel()
                    pendingSingleTapJob?.cancel()
                    zoomState.stopAnimation()
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
                                    viewportSize = zoomState.viewportSize,
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

                            if (zoomState.scale > 1.0f && totalDragDistance >= touchSlop) {
                                pendingSingleTapJob?.cancel()
                                val scroll = if (isContinuous || isDualPage) {
                                    latestOnVerticalScroll?.invoke(-dragDelta.y / zoomState.scale) ?: 0f
                                } else 0f
                                val horizontalScroll = latestOnHorizontalScroll?.invoke(-dragDelta.x / zoomState.scale) ?: 0f
                                val panX = ZoomMath.remainingVerticalPan(dragDelta.x, horizontalScroll, zoomState.scale)
                                val panY = ZoomMath.remainingVerticalPan(dragDelta.y, scroll, zoomState.scale)
                                zoomState.updateScaleAndOffset(
                                    zoomState.scale,
                                    zoomState.offset + Offset(panX, panY)
                                )
                                // Zoom owns the gesture. In continuous mode the list gets the
                                // correctly scaled vertical distance explicitly, including diagonals.
                                // Paged readers keep panning until zoom is reset, never turning a
                                // page just because the finger reaches its edge.
                                change.consume()
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
                            motionJob = coroutineScope.launch {
                                if (zoomState.scale > 1.5f) {
                                    zoomState.animateTo(ZoomMath.MIN_SCALE, Offset.Zero)
                                } else {
                                    val targetOffset = ZoomMath.calculateCentroidOffset(
                                        currentOffset = Offset.Zero,
                                        currentScale = 1.0f,
                                        newScale = ZoomMath.DOUBLE_TAP_SCALE,
                                        centroid = downPosition,
                                        viewportSize = zoomState.viewportSize
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
                                            downPosition.x < width * 0.2f -> latestOnPrevPage()
                                            downPosition.x > width * 0.8f -> latestOnNextPage()
                                            else -> latestOnToggleControls()
                                        }
                                    } else {
                                        latestOnToggleControls()
                                    }
                                } else {
                                    latestOnToggleControls()
                                }
                            }
                        }
                    } else if (zoomState.scale > 1.0f) {
                        // Fling inertia on lift
                        val velocity = velocityTracker.calculateVelocity()
                        motionJob = coroutineScope.launch {
                            if ((isContinuous || isDualPage) && latestOnVerticalScroll != null) {
                                var previous = 0f
                                Animatable(0f).animateDecay(velocity.y, exponentialDecay()) {
                                    val delta = value - previous
                                    previous = value
                                    val consumed = latestOnVerticalScroll?.invoke(-delta / zoomState.scale) ?: 0f
                                    val remaining = ZoomMath.remainingVerticalPan(delta, consumed, zoomState.scale)
                                    zoomState.updateScaleAndOffset(
                                        zoomState.scale, zoomState.offset + Offset(0f, remaining)
                                    )
                                }
                            } else {
                                zoomState.fling(Offset(velocity.x, velocity.y))
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
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
        if (showZoomControls) {
            ViewerZoomControl(
                zoomState = zoomState,
                modifier = Modifier.align(if (zoomControlsAtTop) Alignment.TopEnd else Alignment.BottomEnd)
                    .padding(end = 16.dp, top = if (zoomControlsAtTop) 88.dp else 0.dp, bottom = if (zoomControlsAtTop) 0.dp else 88.dp)
            )
        }
    }
}
