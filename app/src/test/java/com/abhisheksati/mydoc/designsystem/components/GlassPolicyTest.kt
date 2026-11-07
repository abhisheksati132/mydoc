package com.abhisheksati.mydoc.designsystem.components

import org.junit.Assert.*
import org.junit.Test

class GlassPolicyTest {
    @Test fun contrastAndUnsupportedHardwareAlwaysUseSolidMaterial() {
        GlassEffectMode.entries.forEach { mode ->
            assertFalse(shouldUseLiveGlass(mode, GlassEnvironment(sdk = 30)))
            assertFalse(shouldUseLiveGlass(mode, GlassEnvironment(sdk = 35, increasedContrast = true)))
        }
    }
    @Test fun automaticModeHonorsPowerAndMemoryButExplicitOnRemainsAvailable() {
        listOf(GlassEnvironment(sdk = 35, powerSave = true), GlassEnvironment(sdk = 35, lowRam = true)).forEach {
            assertFalse(shouldUseLiveGlass(GlassEffectMode.AUTO, it))
            assertTrue(shouldUseLiveGlass(GlassEffectMode.ON, it))
        }
        assertTrue(shouldUseLiveGlass(GlassEffectMode.AUTO, GlassEnvironment(sdk = 31)))
        assertFalse(shouldUseLiveGlass(GlassEffectMode.OFF, GlassEnvironment(sdk = 35)))
    }
    @Test fun scrollDensityDoesNotResetAtEveryRowBoundary() {
        assertEquals(0f, glassScrollProgress(0, 0))
        assertEquals(0.5f, glassScrollProgress(0, 120))
        assertEquals(1f, glassScrollProgress(0, 1000))
        assertEquals(1f, glassScrollProgress(1, 0))
        assertEquals(1f, glassScrollProgress(100, 1))
    }
}
