package com.abhisheksati.mydoc.ui.viewer.controls

import org.junit.Assert.*
import org.junit.Test

class AdaptiveInkTest {
    @Test fun mixedBackgroundsGiveIndependentContrastingChoices() {
        assertTrue(preferLightInk(0.1f,null))
        assertFalse(preferLightInk(0.9f,null))
    }
    @Test fun smallLuminanceChangesCannotFlipInkBackAndForth() {
        assertTrue(preferLightInk(0.51f,true))
        assertFalse(preferLightInk(0.49f,false))
        assertFalse(preferLightInk(0.7f,true))
        assertTrue(preferLightInk(0.3f,false))
    }
}
