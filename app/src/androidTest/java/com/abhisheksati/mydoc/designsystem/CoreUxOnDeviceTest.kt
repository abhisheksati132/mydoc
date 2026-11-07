package com.abhisheksati.mydoc.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.ui.search.SpotlightSearchSheet
import com.abhisheksati.mydoc.ui.settings.SettingToggleRow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CoreUxOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchFieldKeepsItsAccessibleNameAfterTyping() {
        compose.setContent { AppDesignSystemTheme { SpotlightSearchSheet(emptyList(), {}, {}) } }
        val input = compose.onNode(hasSetTextAction())
        input.assertContentDescriptionEquals("Search files")
        input.performTextInput("report")
        input.assertContentDescriptionEquals("Search files").assertTextContains("report")
    }

    @Test fun settingExplanationIsNotClippedAtDoubleTextSize() {
        val explanation = "Pitch black background, saves battery on OLED"
        compose.setContent {
            val density = LocalDensity.current
            AppDesignSystemTheme {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    Box(Modifier.width(320.dp).padding(horizontal = 32.dp)) {
                        SettingToggleRow("Pure AMOLED Black", explanation, Icons.Outlined.Info, true, {})
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(explanation, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertEquals("Setting explanation must lay out every character", explanation.length,
            layouts.single().getLineEnd(layouts.single().lineCount - 1, visibleEnd = true))
    }
}
