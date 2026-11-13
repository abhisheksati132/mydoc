package com.abhisheksati.mydoc.ui.viewer.controls

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F12: layout-mode toggles are only shown for viewers that implement them. The reflow, grid and
 * text viewers persist these prefs but never read them, so the capability defaults must not
 * claim support — a claimed-but-unimplemented mode is a dead control in the appearance sheet.
 */
class ViewerCapabilitiesTest {

    @Test
    fun layoutModes_areNotClaimedByDefault() {
        val capabilities = ViewerCapabilities()
        assertFalse(capabilities.supportsContinuousScroll)
        assertFalse(capabilities.supportsDualPageView)
    }

    @Test
    fun layoutModes_canBeDeclaredByAViewerThatImplementsThem() {
        val capabilities = ViewerCapabilities(
            supportsContinuousScroll = true,
            supportsDualPageView = true
        )
        assertTrue(capabilities.supportsContinuousScroll)
        assertTrue(capabilities.supportsDualPageView)
    }
}