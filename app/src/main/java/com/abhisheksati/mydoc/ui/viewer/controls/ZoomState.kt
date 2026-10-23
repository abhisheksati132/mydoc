package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Hoisted zoom and pan state that survives orientation changes and handles
 * animated transitions (double-tap zoom, reset) and inertia fling.
 */
@Stable
class ZoomState(
    initialScale: Float = 1.0f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f
) {
    var scale by mutableFloatStateOf(initialScale)
        internal set

    var offset by mutableStateOf(Offset(initialOffsetX, initialOffsetY))
        internal set

    var viewportSize by mutableStateOf(Size.Zero)

    fun updateScaleAndOffset(newScale: Float, newOffset: Offset) {
        scale = newScale.coerceIn(ZoomMath.MIN_SCALE, ZoomMath.MAX_SCALE)
        offset = if (scale > 1.0f) {
            ZoomMath.clampOffset(newOffset, viewportSize, scale)
        } else {
            Offset.Zero
        }
    }

    fun updateScaleAndOffsetWithRubberBand(newScale: Float, newOffset: Offset) {
        scale = newScale.coerceIn(ZoomMath.MIN_SCALE, ZoomMath.MAX_SCALE)
        offset = if (scale > 1.0f) {
            ZoomMath.rubberBandOffset(newOffset, viewportSize, scale)
        } else {
            Offset.Zero
        }
    }

    suspend fun reset(animate: Boolean = false) {
        if (!animate) {
            scale = 1.0f
            offset = Offset.Zero
        } else {
            animateTo(1.0f, Offset.Zero)
        }
    }

    suspend fun animateTo(targetScale: Float, targetOffset: Offset, durationMs: Int = 200) {
        val clampedScale = targetScale.coerceIn(ZoomMath.MIN_SCALE, ZoomMath.MAX_SCALE)
        val clampedOffset = if (clampedScale > 1.0f) {
            ZoomMath.clampOffset(targetOffset, viewportSize, clampedScale)
        } else {
            Offset.Zero
        }

        coroutineScope {
            val scaleAnim = Animatable(scale)
            val offsetAnim = Animatable(offset, Offset.VectorConverter)

            launch {
                scaleAnim.animateTo(clampedScale, tween(durationMs)) {
                    scale = this.value
                }
            }
            launch {
                offsetAnim.animateTo(clampedOffset, tween(durationMs)) {
                    offset = this.value
                }
            }
        }
    }

    suspend fun fling(velocity: Offset) {
        if (scale <= 1.0f) return
        val maxPan = ZoomMath.calculateMaxPan(viewportSize, scale)
        coroutineScope {
            val animX = Animatable(offset.x)
            val animY = Animatable(offset.y)
            val decay = exponentialDecay<Float>(frictionMultiplier = 1.4f)

            launch {
                animX.animateDecay(velocity.x, decay) {
                    val clamped = value.coerceIn(-maxPan.x, maxPan.x)
                    offset = Offset(clamped, offset.y)
                    if (value != clamped) {
                        coroutineContext[kotlinx.coroutines.Job]?.cancel()
                    }
                }
            }
            launch {
                animY.animateDecay(velocity.y, decay) {
                    val clamped = value.coerceIn(-maxPan.y, maxPan.y)
                    offset = Offset(offset.x, clamped)
                    if (value != clamped) {
                        coroutineContext[kotlinx.coroutines.Job]?.cancel()
                    }
                }
            }
        }
    }

    companion object {
        val Saver: Saver<ZoomState, *> = listSaver(
            save = { listOf(it.scale, it.offset.x, it.offset.y) },
            restore = {
                ZoomState(
                    initialScale = it[0] as Float,
                    initialOffsetX = it[1] as Float,
                    initialOffsetY = it[2] as Float
                )
            }
        )
    }
}

@Composable
fun rememberZoomState(
    key: Any? = null
): ZoomState {
    return rememberSaveable(key, saver = ZoomState.Saver) {
        ZoomState()
    }
}
