package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.abhisheksati.mydoc.ui.viewer.controls.ZoomMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomMathTest {

    @Test
    fun `calculateMaxPan at 1x scale is always zero`() {
        val viewport = Size(1080f, 1920f)
        val maxPan = ZoomMath.calculateMaxPan(viewport, 1.0f)
        assertEquals(0f, maxPan.x, 0.001f)
        assertEquals(0f, maxPan.y, 0.001f)
    }

    @Test
    fun `calculateMaxPan scales linearly with viewport size`() {
        val viewport = Size(1000f, 2000f)
        // At 2x: maxX = 1000 * (2 - 1) / 2 = 500; maxY = 2000 * (2 - 1) / 2 = 1000
        val maxPan2x = ZoomMath.calculateMaxPan(viewport, 2.0f)
        assertEquals(500f, maxPan2x.x, 0.001f)
        assertEquals(1000f, maxPan2x.y, 0.001f)

        // At 3x: maxX = 1000 * 2 / 2 = 1000; maxY = 2000 * 2 / 2 = 2000
        val maxPan3x = ZoomMath.calculateMaxPan(viewport, 3.0f)
        assertEquals(1000f, maxPan3x.x, 0.001f)
        assertEquals(2000f, maxPan3x.y, 0.001f)
    }

    @Test
    fun `clampOffset limits offset within maxPan boundaries`() {
        val viewport = Size(800f, 1200f)
        val scale = 2.0f // maxPan = (400, 600)

        // Inside bounds
        val inside = Offset(200f, -300f)
        val clampedInside = ZoomMath.clampOffset(inside, viewport, scale)
        assertEquals(200f, clampedInside.x, 0.001f)
        assertEquals(-300f, clampedInside.y, 0.001f)

        // Exceeding positive bounds
        val excessive = Offset(900f, 1000f)
        val clampedExcessive = ZoomMath.clampOffset(excessive, viewport, scale)
        assertEquals(400f, clampedExcessive.x, 0.001f)
        assertEquals(600f, clampedExcessive.y, 0.001f)

        // Exceeding negative bounds
        val negative = Offset(-800f, -900f)
        val clampedNegative = ZoomMath.clampOffset(negative, viewport, scale)
        assertEquals(-400f, clampedNegative.x, 0.001f)
        assertEquals(-600f, clampedNegative.y, 0.001f)
    }

    @Test
    fun `calculateCentroidOffset keeps touch point stationary during zoom`() {
        val centroid = Offset(300f, 400f)
        val initialOffset = Offset(0f, 0f)
        val initialScale = 1.0f
        val newScale = 2.0f

        val newOffset = ZoomMath.calculateCentroidOffset(
            currentOffset = initialOffset,
            currentScale = initialScale,
            newScale = newScale,
            centroid = centroid
        )

        // Formula: centroid - (centroid - offset) * (newScale / oldScale)
        // x: 300 - (300 - 0) * 2 = 300 - 600 = -300
        // y: 400 - (400 - 0) * 2 = 400 - 800 = -400
        assertEquals(-300f, newOffset.x, 0.001f)
        assertEquals(-400f, newOffset.y, 0.001f)
    }

    @Test
    fun `calculateCentroidOffset incorporates pan translation`() {
        val centroid = Offset(200f, 200f)
        val initialOffset = Offset(50f, 50f)
        val pan = Offset(10f, -20f)

        val newOffset = ZoomMath.calculateCentroidOffset(
            currentOffset = initialOffset,
            currentScale = 1.0f,
            newScale = 1.5f,
            centroid = centroid,
            pan = pan
        )

        // x: 200 - (200 - 50) * 1.5 + 10 = 200 - 225 + 10 = -15
        // y: 200 - (200 - 50) * 1.5 - 20 = 200 - 225 - 20 = -45
        assertEquals(-15f, newOffset.x, 0.001f)
        assertEquals(-45f, newOffset.y, 0.001f)
    }

    @Test
    fun `rubberBandOffset dampens values past limits`() {
        val viewport = Size(1000f, 1000f)
        val scale = 2.0f // maxPan = 500
        val factor = 0.35f

        val withinBounds = Offset(400f, 400f)
        val resultWithin = ZoomMath.rubberBandOffset(withinBounds, viewport, scale, factor)
        assertEquals(400f, resultWithin.x, 0.001f)

        val pastBounds = Offset(600f, 600f) // 100 beyond maxPan
        val resultPast = ZoomMath.rubberBandOffset(pastBounds, viewport, scale, factor)
        // 500 + 100 * 0.35 = 535
        assertEquals(535f, resultPast.x, 0.001f)
        assertEquals(535f, resultPast.y, 0.001f)
    }

    @Test
    fun `isAtHorizontalEdge accurately detects left and right boundaries`() {
        val viewportWidth = 1000f
        val scale = 2.0f // maxPanX = 500

        // In center
        val (left1, right1) = ZoomMath.isAtHorizontalEdge(0f, viewportWidth, scale)
        assertFalse(left1)
        assertFalse(right1)

        // At left edge (+500)
        val (left2, right2) = ZoomMath.isAtHorizontalEdge(500f, viewportWidth, scale)
        assertTrue(left2)
        assertFalse(right2)

        // At right edge (-500)
        val (left3, right3) = ZoomMath.isAtHorizontalEdge(-500f, viewportWidth, scale)
        assertFalse(left3)
        assertTrue(right3)
    }
}
