package com.abhisheksati.mydoc.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    @Test fun recentFileActionsAndSelectionRemainAvailableInListView() {
        val file = com.abhisheksati.mydoc.domain.model.LibraryItem(
            "file:///fixture/Quarterly-report.pdf", "Quarterly-report.pdf",
            com.abhisheksati.mydoc.domain.model.MediaKind.DOCUMENT, "application/pdf", 4096, 1L)
        var selected by androidx.compose.runtime.mutableStateOf(false)
        var details = 0
        var shares = 0
        var removals = 0
        compose.setContent {
            AppDesignSystemTheme {
                com.abhisheksati.mydoc.ui.home.RecentFileRow(file, selected, selected,
                    { selected = !selected }, { selected = true }, { details++ }, { shares++ }, { removals++ })
            }
        }
        fun menu() = compose.onNodeWithContentDescription("More options for Quarterly-report.pdf").performClick()
        menu(); compose.onNodeWithText("Details").performClick()
        menu(); compose.onNodeWithText("Share").performClick()
        menu(); compose.onNodeWithText("Remove from recents").performClick()
        compose.runOnIdle { assertEquals(1, details); assertEquals(1, shares); assertEquals(1, removals) }
        compose.onNodeWithText(file.name).performTouchInput { longClick() }
        compose.onNodeWithText(file.name).assertIsOn().performClick()
        compose.runOnIdle { assertFalse(selected) }
    }

    @Test fun indexedSearchFiltersCanBeClearedAndFolderBrowsingRemainsAvailable() {
        val pdf = com.abhisheksati.mydoc.domain.model.LibraryItem("file:///report.pdf", "report.pdf",
            com.abhisheksati.mydoc.domain.model.MediaKind.DOCUMENT, "application/pdf", 4096, 1L)
        val photo = pdf.copy(uri = "file:///photo.jpg", name = "photo.jpg", kind = com.abhisheksati.mydoc.domain.model.MediaKind.PHOTO, mimeType = "image/jpeg")
        var browsed = 0
        var dismissed = 0
        compose.setContent { AppDesignSystemTheme {
            SpotlightSearchSheet(listOf(pdf, photo), {}, { dismissed++ }, onBrowseFolders = { browsed++ })
        } }
        compose.onNodeWithText("Media").performScrollTo().performClick()
        compose.onNodeWithText("report.pdf").assertDoesNotExist()
        compose.onNodeWithText("photo.jpg").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("missing")
        compose.onNodeWithText("Clear search and filters").performClick()
        compose.onNodeWithText("report.pdf").assertIsDisplayed()
        compose.onNodeWithText("Browse files").performClick()
        compose.runOnIdle { assertEquals(1, browsed); assertEquals(1, dismissed) }
    }
}
