package com.abhisheksati.mydoc.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.*
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.HomeTab
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GlassDockOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowDockWithDoubleSizeTextHasThreeAccessibleSelectableTabs() {
        var selected by mutableStateOf(HomeTab.HOME)
        compose.setContent {
            val density = LocalDensity.current
            AppDesignSystemTheme {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                    LocalGlassEffectMode provides GlassEffectMode.OFF, LocalReducedMotion provides true) {
                    Box(Modifier.width(320.dp)) { BottomNavBar(selected, { selected = it }) }
                }
            }
        }
        val tabs = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        compose.onAllNodes(tabs).assertCountEquals(3)
        listOf("Home", "Library", "Tools").forEach { label ->
            val tab = compose.onNode(tabs and hasText(label))
            tab.assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            assertFalse(tab.fetchSemanticsNode().config.contains(SemanticsProperties.ContentDescription))
            tab.performClick().assertIsSelected()
        }
        compose.runOnIdle { assertEquals(HomeTab.TOOLS, selected) }
    }

    @Test fun clipboardReportsItsFullHeightAfterEntering() {
        var clipboard by mutableStateOf<com.abhisheksati.mydoc.domain.model.ClipboardState?>(null)
        var measured = 0
        compose.setContent {
            AppDesignSystemTheme {
                com.abhisheksati.mydoc.ui.files.FileManagerClipboardBar(
                    clipboard, {}, {}, onHeightChanged = { measured = it })
            }
        }
        compose.runOnIdle {
            clipboard = com.abhisheksati.mydoc.domain.model.ClipboardState(
                com.abhisheksati.mydoc.domain.model.ClipboardOperation.COPY,
                listOf(java.io.File("/fixture")))
        }
        compose.onNodeWithText("Paste").assertIsDisplayed()
        compose.runOnIdle { assertTrue("Visible clipboard must report a nonzero height", measured > 0) }
    }
}
