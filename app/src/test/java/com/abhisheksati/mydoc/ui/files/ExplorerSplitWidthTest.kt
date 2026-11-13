package com.abhisheksati.mydoc.ui.files

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorerSplitWidthTest {

    @Test
    fun mediumWidthClass_qualifiesForSplit() {
        assertTrue(explorerSupportsSplit(600.dp))
        assertTrue(explorerSupportsSplit(673.dp))
        assertTrue(explorerSupportsSplit(841.dp))
        assertTrue(explorerSupportsSplit(900.dp))
    }

    @Test
    fun compactWidths_staySinglePane() {
        assertFalse(explorerSupportsSplit(599.dp))
        assertFalse(explorerSupportsSplit(411.dp))
        assertFalse(explorerSupportsSplit(320.dp))
    }
}