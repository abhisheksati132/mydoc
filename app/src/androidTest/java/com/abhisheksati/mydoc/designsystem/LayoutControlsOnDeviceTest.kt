package com.abhisheksati.mydoc.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.ui.files.DirectoryPane
import com.abhisheksati.mydoc.ui.home.RecentFilesHeader
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LayoutControlsOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recentActionsFitAtLargeText() = verifyRecentActions(LayoutDirection.Ltr)
    @Test fun recentActionsFitWhenMirrored() = verifyRecentActions(LayoutDirection.Rtl)

    private fun verifyRecentActions(direction: LayoutDirection) {
        var browsed = false
        var toggled = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides direction) {
                AppDesignSystemTheme {
                    Box(Modifier.width(288.dp)) {
                        RecentFilesHeader(12, false, { browsed = true }, { toggled = true }, Modifier.testTag("header"))
                    }
                }
            }
        }
        val container = compose.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
        val browse = compose.onNodeWithText("Browse files").assertIsDisplayed()
        val toggle = compose.onNodeWithContentDescription("Show previews").assertIsDisplayed()
        listOf(browse, toggle).forEach {
            val bounds = it.fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= container.left && bounds.right <= container.right)
        }
        assertFalse(browse.fetchSemanticsNode().boundsInRoot.overlaps(toggle.fetchSemanticsNode().boundsInRoot))
        browse.performClick()
        toggle.performClick()
        assertTrue(browsed && toggled)
    }

    @Test fun visibleSortControlKeepsSortAndViewActionsWorking() {
        var chosen: FileSortField? = null
        var viewChanged = false
        compose.setContent {
            AppDesignSystemTheme {
                Box(Modifier.width(288.dp)) {
                    DirectoryPane(FileManagerUiState(File("/sdcard")), {}, {}, {}, { viewChanged = true }, { chosen = it }, {})
                }
            }
        }
        compose.onNodeWithContentDescription("Sort")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ascending"))
            .performClick()
        compose.onNodeWithText("Size").performClick()
        assertEquals(FileSortField.SIZE, chosen)
        compose.onNodeWithContentDescription("Grid view").performClick()
        assertTrue(viewChanged)
    }
}
