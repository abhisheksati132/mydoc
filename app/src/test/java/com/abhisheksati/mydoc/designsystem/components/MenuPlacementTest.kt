package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuPlacementTest {
    @Test fun bottomRightAnchorOpensAboveAndStaysInsideWindow() {
        assertEquals(IntOffset(92, 342), menuPosition(
            IntRect(272, 600, 320, 648), IntSize(320, 650), IntSize(220, 258),
            LayoutDirection.Ltr, IntOffset.Zero, 8))
    }

    @Test fun rtlStartsAtAnchorsRightEdgeAndMirrorsHorizontalOffset() {
        assertEquals(IntOffset(130, 110), menuPosition(
            IntRect(200, 50, 280, 100), IntSize(500, 800), IntSize(140, 200),
            LayoutDirection.Rtl, IntOffset(10, 10), 8))
    }

    @Test fun oversizedMenuNeverProducesAnInvalidClampRange() {
        assertEquals(IntOffset(8, 8), menuPosition(
            IntRect(0, 0, 48, 48), IntSize(100, 100), IntSize(200, 300),
            LayoutDirection.Ltr, IntOffset.Zero, 8))
    }
}
