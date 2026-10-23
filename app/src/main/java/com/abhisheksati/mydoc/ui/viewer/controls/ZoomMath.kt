package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Pure, deterministic mathematical functions for zoom calculations,
 * focal centroid transformations, bounds clamping, and rubber-banding.
 */
object ZoomMath {
    const val MIN_SCALE = 1.0f
    const val MAX_SCALE = 6.0f
    const val DOUBLE_TAP_SCALE = 2.5f

    /**
     * Calculates the new pan offset when scaling around an arbitrary centroid:
     * newOffset = centroid - (centroid - currentOffset) * (newScale / currentScale) + pan
     */
    fun calculateCentroidOffset(
        currentOffset: Offset,
        currentScale: Float,
        newScale: Float,
        centroid: Offset,
        pan: Offset = Offset.Zero
    ): Offset {
        if (currentScale <= 0f) return currentOffset
        val scaleRatio = newScale / currentScale
        return Offset(
            x = centroid.x - (centroid.x - currentOffset.x) * scaleRatio + pan.x,
            y = centroid.y - (centroid.y - currentOffset.y) * scaleRatio + pan.y
        )
    }

    /**
     * Calculates maximum pan offset in both axes based on viewport size and scale:
     * maxX = viewportWidth * (scale - 1) / 2
     * maxY = viewportHeight * (scale - 1) / 2
     */
    fun calculateMaxPan(viewportSize: Size, scale: Float): Offset {
        if (scale <= 1.0f || viewportSize.width <= 0f || viewportSize.height <= 0f) {
            return Offset.Zero
        }
        val maxX = (viewportSize.width * (scale - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (viewportSize.height * (scale - 1f) / 2f).coerceAtLeast(0f)
        return Offset(maxX, maxY)
    }

    /**
     * Clamps the pan offset to keep content within viewport bounds without revealing blank space.
     */
    fun clampOffset(offset: Offset, viewportSize: Size, scale: Float): Offset {
        val maxPan = calculateMaxPan(viewportSize, scale)
        return Offset(
            x = offset.x.coerceIn(-maxPan.x, maxPan.x),
            y = offset.y.coerceIn(-maxPan.y, maxPan.y)
        )
    }

    /**
     * Applies rubber-band dampening to offsets that exceed the clamp limits during active gestures.
     */
    fun rubberBandOffset(
        offset: Offset,
        viewportSize: Size,
        scale: Float,
        rubberBandFactor: Float = 0.35f
    ): Offset {
        val maxPan = calculateMaxPan(viewportSize, scale)
        fun applyBand(value: Float, max: Float): Float {
            return when {
                value > max -> max + (value - max) * rubberBandFactor
                value < -max -> -max + (value - (-max)) * rubberBandFactor
                else -> value
            }
        }
        return Offset(
            x = applyBand(offset.x, maxPan.x),
            y = applyBand(offset.y, maxPan.y)
        )
    }

    /**
     * Determines whether the content is currently at or beyond a horizontal edge.
     * Returns Pair(atLeftEdge, atRightEdge).
     */
    fun isAtHorizontalEdge(
        offsetX: Float,
        viewportWidth: Float,
        scale: Float,
        tolerance: Float = 6f
    ): Pair<Boolean, Boolean> {
        val maxPanX = (viewportWidth * (scale - 1f) / 2f).coerceAtLeast(0f)
        val atLeft = offsetX >= maxPanX - tolerance
        val atRight = offsetX <= -maxPanX + tolerance
        return Pair(atLeft, atRight)
    }
}
