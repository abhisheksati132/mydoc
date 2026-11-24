package com.abhisheksati.mydoc.tools.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePdfDecodeTest {
    @Test fun imagesBelowBudgetKeepTheirOriginalResolution() {
        assertEquals(1, editorImageSampleSize(800, 1200, 1600))
        assertEquals(1, editorImageSampleSize(1600, 800, 1600))
    }

    @Test fun dimensionsJustAboveBudgetAreSampledBeforeAllocation() {
        assertEquals(2, editorImageSampleSize(1601, 1200, 1600))
        assertEquals(2, editorImageSampleSize(1200, 3100, 1600))
    }

    @Test fun hugeHeadersAreNotLimitedToSixtyFourTimesSampling() {
        val sample = editorImageSampleSize(1_000_000, 500_000, 3000)
        assertTrue(sample > 64)
        assertTrue(1_000_000L <= sample.toLong() * 3000)
        assertEquals(0, sample and (sample - 1))
    }

    @Test fun integerMaximumDimensionsDoNotOverflow() {
        val sample = editorImageSampleSize(Int.MAX_VALUE, Int.MAX_VALUE, 1600)
        assertTrue(sample > 0)
        assertTrue(Int.MAX_VALUE.toLong() <= sample.toLong() * 1600)
    }
}
