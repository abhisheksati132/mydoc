package com.abhisheksati.mydoc.tools.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C11: every raster path caps the rendered page at 4096px on its longest side. The asset
 * extraction OCR path used to render at a fixed 2x with no ceiling, so an oversized page was
 * an OutOfMemoryError instead of a reduced-resolution render.
 */
class CappedRenderScaleTest {

    @Test
    fun oversizedPage_isReducedToFitThePixelCeiling() {
        val scale = cappedRenderScale(widthPt = 3000f, heightPt = 4000f, baseScale = 2f)
        assertTrue("scale must be reduced below the base", scale < 2f)
        assertEquals(4096f, 4000f * scale, 0.5f)
        assertTrue(3000f * scale <= 4096f)
    }

    @Test
    fun ordinaryPage_keepsItsChosenScale() {
        assertEquals(1.5f, cappedRenderScale(595f, 842f, 1.5f), 0.0001f)
        assertEquals(2f, cappedRenderScale(612f, 792f, 2f), 0.0001f)
    }

    @Test
    fun pageExactlyAtTheCeiling_isNotReduced() {
        assertEquals(2f, cappedRenderScale(2048f, 2048f, 2f), 0.0001f)
    }

    @Test
    fun customCeiling_isHonoured() {
        assertEquals(1f, cappedRenderScale(1000f, 500f, 2f, maxPx = 1000f), 0.0001f)
    }

    @Test
    fun extremeAspectRatio_capsTheLongSideNotTheShort() {
        val scale = cappedRenderScale(widthPt = 100f, heightPt = 20000f, baseScale = 2f)
        assertEquals(4096f, 20000f * scale, 0.5f)
        assertTrue(100f * scale < 4096f)
    }
}