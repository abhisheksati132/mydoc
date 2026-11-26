package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.DeviceCaptures
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FileActionSheetOnDeviceTest {
    @get:Rule(order = 0) val fontScale = com.abhisheksati.mydoc.testutil.AndroidFontScaleRule(
        onlyTestName = "largeMirroredActionHasButtonSemanticsAndActsOnce")
    @get:Rule(order = 1) val compose = createComposeRule()
    private val longName = "Project reference notes and supporting documents for the November design review final approved version.pdf"

    @Composable private fun Sheet(name: String, onCopy: () -> Unit = {}) {
        SingleItemActionBottomSheet(StorageItem(File("/fixture/$name")), false,
            {}, {}, onCopy, {}, {}, {}, {}, {}, {}, {}, {}, modifier = Modifier.width(288.dp))
    }

    @Test fun longFilenameExpandsAndCollapsesWhileCopyRemainsReachable() {
        var copies = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AppDesignSystemTheme { Sheet(longName) { copies++ } } }
        compose.onNodeWithText("Show full name").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Show less").performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(longName, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse("Expanded filename must not be clipped or ellipsized", layouts.single().hasVisualOverflow)
        DeviceCaptures.save("file-action-expanded-name",
            compose.onNodeWithText(longName).onParent().captureToImage().asAndroidBitmap())
        compose.onNodeWithText("Show less").performScrollTo().performClick()
        compose.onNodeWithText("Show full name").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Copy").performScrollTo().performClick()
        assertEquals(1, copies)
    }

    @Test fun shortFilenameNeedsNoExpansionControl() {
        compose.setContent { AppDesignSystemTheme { Sheet("Brief.pdf") } }
        compose.onNodeWithText("Brief.pdf").assertIsDisplayed()
        compose.onNodeWithText("Show full name").assertDoesNotExist()
    }

    @Test fun largeMirroredActionHasButtonSemanticsAndActsOnce() {
        var copies = 0
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppDesignSystemTheme { Sheet(longName) { copies++ } }
            }
        }
        compose.onNodeWithText("Copy").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp).performClick()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Copy").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("The dialog must actually render at double text size", 2f, layouts.single().layoutInput.density.fontScale, 0.01f)
        assertEquals(1, copies)
        DeviceCaptures.save("file-action-large-rtl")
    }
}
