package com.abhisheksati.mydoc.tools.engine

import org.junit.Assert.*
import org.junit.Test

class StitchedImageLayoutTest {
    @Test fun extremelyTallDocumentsReduceTheActualRenderScaleBelowTheNormalFloor() {
        val layout = stitchedImageLayout(4500f, 1_000_000f, 3f, 0.5f, 24_000_000)
        assertTrue(layout.scale < 0.5f)
        assertTrue(layout.width.toLong() * layout.height <= 24_000_000)
        assertTrue(4500 * layout.scale <= layout.width + 0.01f)
        assertTrue(1_000_000 * layout.scale <= layout.height + 0.01f)
        // 100 pages must fit collectively; rounding each pixmap's height must not clip the end.
        assertTrue(100 * (10_000 * layout.scale) <= layout.height + 0.01f)
    }

    @Test fun ordinaryDocumentsRetainTheirChosenResolution() {
        val layout = stitchedImageLayout(600f, 800f, 1.5f, 0.5f, 24_000_000)
        assertEquals(900, layout.width)
        assertEquals(1200, layout.height)
        assertEquals(1.5f, layout.scale, 0.00001f)
    }

    @Test fun aOnePixelNarrowAxisStillObeysTheBudget() {
        val layout = stitchedImageLayout(1f, 1_000_000_000f, 3f, 0.5f, 24_000_000)
        assertTrue(layout.width >= 1 && layout.height >= 1)
        assertTrue(layout.width.toLong() * layout.height <= 24_000_000)
        assertTrue(1_000_000_000f * layout.scale <= layout.height + 1f)
    }
}
