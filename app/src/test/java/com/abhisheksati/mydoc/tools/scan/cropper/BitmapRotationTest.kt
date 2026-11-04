package com.abhisheksati.mydoc.tools.scan.cropper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N-12: the rotate button turned the image inside the source's own box, which clips a landscape
 * photo, and every tap left the previous full-size bitmap alive.
 */
class BitmapRotationTest {

    @Test
    fun quarterTurnsSwapTheAxes() {
        listOf(90, -90, 270, 450).forEach { degrees ->
            assertTrue("expected $degrees to swap axes", BitmapRotation.swapsAxes(degrees))
            assertEquals(300 to 400, BitmapRotation.outputSize(400, 300, degrees))
        }
    }

    @Test
    fun halfAndFullTurnsKeepTheAxes() {
        listOf(0, 180, 360, -180).forEach { degrees ->
            assertFalse("expected $degrees to keep the axes", BitmapRotation.swapsAxes(degrees))
            assertEquals(400 to 300, BitmapRotation.outputSize(400, 300, degrees))
        }
    }

    @Test
    fun anOddAngleKeepsTheSourceBox() {
        // Only exact quarter turns swap; anything else is drawn inside the same frame, which is
        // what the deskew canvas assumes.
        assertFalse(BitmapRotation.swapsAxes(45))
        assertEquals(400 to 300, BitmapRotation.outputSize(400, 300, 45))
    }

    @Test
    fun aPortraitSourceBecomesLandscapeAfterOneTurnAndBackAfterTwo() {
        val first = BitmapRotation.outputSize(1080, 1920, 90)
        assertEquals(1920 to 1080, first)
        assertEquals(1080 to 1920, BitmapRotation.outputSize(first.first, first.second, 90))
    }

    @Test
    fun fourTurnsReturnToTheOriginalBox() {
        var size = 1600 to 1200
        repeat(4) { size = BitmapRotation.outputSize(size.first, size.second, 90) }
        assertEquals(1600 to 1200, size)
    }
}
