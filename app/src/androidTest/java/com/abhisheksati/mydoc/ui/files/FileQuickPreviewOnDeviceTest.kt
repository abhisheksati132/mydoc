package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.StorageItem
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FileQuickPreviewOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val item = StorageItem(File("/sdcard/Documents/Research/Term two/Project notes – final annotated review.pdf"),
        sizeBytes = 4096L, lastModified = 1700000000000L)

    @Test fun fileActionsExposePreviewWithoutOpeningTheFile() {
        var previewed = false
        var opened = false
        compose.setContent { AppDesignSystemTheme {
            SingleItemActionBottomSheet(
                item = item, isPinned = false, onDismissRequest = {}, onOpen = { opened = true },
                onCopy = {}, onMove = {}, onRename = {}, onDelete = {}, onTogglePin = {},
                onExtractZip = {}, onCompressToZip = {}, onDetails = {}, onShare = {},
                onPreview = { previewed = true }
            )
        } }
        compose.onNodeWithText("Quick preview").performScrollTo().performClick()
        assertTrue(previewed)
        assertFalse(opened)
    }

    @Test fun previewShowsTheFullNameAndLocationAndCanOpenTheFile() {
        var opened = false
        compose.setContent { AppDesignSystemTheme { FileQuickPreviewSheet(item, {}, { opened = true }, {}) } }
        compose.onNodeWithText(item.name).assertExists()
        compose.onNodeWithText(item.path).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open").performClick()
        assertTrue(opened)
    }

    @Test fun moreActionsDoNotOpenTheFile() {
        var opened = false
        var more = false
        compose.setContent { AppDesignSystemTheme { FileQuickPreviewSheet(item, {}, { opened = true }, { more = true }) } }
        compose.onNodeWithText("More actions").performClick()
        assertTrue(more)
        assertFalse(opened)
    }

    @Test fun closeDismissesThePreview() {
        var closed = false
        compose.setContent { AppDesignSystemTheme { FileQuickPreviewSheet(item, { closed = true }, {}, {}) } }
        compose.onNodeWithContentDescription("Close preview").performClick()
        assertTrue(closed)
    }

    @Test fun detailsAndActionsRemainReadableAtLargeText() = verifyLargeText(LayoutDirection.Ltr)
    @Test fun detailsAndActionsRemainReadableWhenMirrored() = verifyLargeText(LayoutDirection.Rtl)

    private fun verifyLargeText(direction: LayoutDirection) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides direction) {
                AppDesignSystemTheme { FileQuickPreviewSheet(item, {}, {}, {}) }
            }
        }
        listOf(item.name, item.path).forEach { value ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(value, useUnmergedTree = true).performScrollTo()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("Preview details must wrap in full", layouts.single().hasVisualOverflow)
        }
        compose.onNodeWithText("Open").assertIsDisplayed()
        compose.onNodeWithText("More actions").assertIsDisplayed()
    }
}
