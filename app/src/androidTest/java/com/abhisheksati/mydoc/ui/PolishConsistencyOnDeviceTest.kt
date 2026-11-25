package com.abhisheksati.mydoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.DeviceCaptures
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.ui.files.FileGridCell
import com.abhisheksati.mydoc.ui.files.FileRow
import com.abhisheksati.mydoc.ui.files.FilesSectionHeader
import com.abhisheksati.mydoc.ui.files.FileManagerClipboardBar
import com.abhisheksati.mydoc.ui.files.FilesSelectionBar
import com.abhisheksati.mydoc.domain.model.ClipboardState
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import com.abhisheksati.mydoc.ui.search.SpotlightSearchSheet
import com.abhisheksati.mydoc.ui.settings.SettingToggleRow
import com.abhisheksati.mydoc.ui.settings.SettingsCategory
import com.abhisheksati.mydoc.ui.settings.SettingsCategoryIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PolishConsistencyOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val document = LibraryItem("file:///fixture/report.pdf", "Quarterly report.pdf", MediaKind.DOCUMENT,
        "application/pdf", 12000, 1700000000000)

    @Composable private fun Frame(large: Boolean = false, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else 1f),
            LocalLayoutDirection provides if (large) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            AppDesignSystemTheme {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) { content() }
            }
        }
    }

    @Test fun searchOptionsBrowseAndClearKeepWorking() {
        var browsed = false
        compose.setContent { Frame { SpotlightSearchSheet(listOf(document), {}, {}, onBrowseFolders = { browsed = true }) } }
        compose.onNode(hasSetTextAction()).performTextInput("missing")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Clear search and filters").performClick()
        compose.onNodeWithText(document.name).assertIsDisplayed()
        compose.onNodeWithContentDescription("Search options").performClick()
        compose.onNodeWithText("Browse files").performClick()
        assertTrue(browsed)
    }

    @Test fun searchResultsStayReadableAtLargeTextAndRtl() {
        var opened = false
        compose.setContent { Frame(true) { SpotlightSearchSheet(listOf(document), { opened = true }, {}) } }
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText(document.name).assertIsDisplayed()
        DeviceCaptures.save("polish-search-large-rtl", compose.onRoot().captureToImage().asAndroidBitmap())
        compose.onNodeWithText(document.name).performClick()
        assertTrue(opened)
    }

    @Test fun settingsCategoriesRemainReachableWithLargeTextAndRtl() {
        var chosen: SettingsCategory? = null
        compose.setContent { Frame(true) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { SettingsCategoryIndex({ chosen = it }) }
        } }
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        DeviceCaptures.save("polish-settings-large-rtl", compose.onRoot().captureToImage().asAndroidBitmap())
        compose.onNodeWithText("Storage & files").performScrollTo().performClick()
        assertEquals(SettingsCategory.STORAGE, chosen)
    }

    @Test fun settingsSwitchHasOneToggleAndChangesOnce() {
        var changes = 0
        var enabled by mutableStateOf(false)
        compose.setContent { Frame {
            SettingToggleRow("Use glass", "Adapt surfaces to the page", Icons.Outlined.Palette, enabled,
                { enabled = it; changes++ })
        } }
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        compose.onNode(isToggleable()).performClick().assertIsOn()
        assertEquals(1, changes)
    }

    @Test fun fileGridStillPublishesItsSelectionState() {
        val item = StorageItem(File("/fixture/Project brief.pdf"), sizeBytes = 12000)
        var selected by mutableStateOf(false)
        compose.setContent { Frame(true) {
            Box(Modifier.width(220.dp)) { FileGridCell(item, selected, true, { selected = !selected }, {}) }
        } }
        compose.onNode(isToggleable()).performClick().assertIsOn()
    }

    @Test fun fileRowsAndSectionActionsFitLargeText() {
        var browsed = false
        compose.setContent { Frame(true) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                FilesSectionHeader("Recently added documents", meta = "24 files", actionLabel = "Browse files", onAction = { browsed = true })
                FileRow(StorageItem(File("/fixture/Project brief.pdf"), sizeBytes = 12000), false, false, {}, {}, {})
            }
        } }
        compose.onNodeWithText("Browse files").performScrollTo().performClick()
        assertTrue(browsed)
        compose.onNodeWithText("Project brief.pdf").performScrollTo().assertIsDisplayed()
        DeviceCaptures.save("polish-files-large-rtl", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test fun fileSizeKeepsItsNumberBeforeItsUnitWhenMirrored() {
        val item = StorageItem(File("/fixture/Brief.pdf"), sizeBytes = 12000)
        compose.setContent { Frame(true) { FileRow(item, false, false, {}, {}, {}) } }
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(item.formattedSize, useUnmergedTree = true).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(androidx.compose.ui.text.style.ResolvedTextDirection.Ltr, layouts.single().getParagraphDirection(0))
        val text = layouts.single()
        val unitOffset = item.formattedSize.indexOf("KB")
        assertTrue("The number must be drawn before its unit", text.getBoundingBox(0).left < text.getBoundingBox(unitOffset).left)
        DeviceCaptures.save("polish-file-size-rtl", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test fun pasteActionsFitLargeTextAndMirroredLayout() {
        var pasted = false
        var cancelled = false
        compose.setContent { Frame(true) { Box(Modifier.width(288.dp)) {
            FileManagerClipboardBar(ClipboardState(ClipboardOperation.COPY, listOf(File("/fixture/Brief.pdf"))),
                { cancelled = true }, { pasted = true }, destinationName = "Documents and project references")
        } } }
        val cancel = compose.onNodeWithText("Cancel").assertIsDisplayed()
        val paste = compose.onNodeWithText("Paste").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        assertFalse(cancel.fetchSemanticsNode().boundsInRoot.overlaps(paste.fetchSemanticsNode().boundsInRoot))
        DeviceCaptures.save("polish-paste-large-rtl", compose.onRoot().captureToImage().asAndroidBitmap())
        cancel.performClick()
        paste.performClick()
        assertTrue(cancelled && pasted)
    }

    @Test fun activeTransferOffersDetailsAndDisablesPaste() {
        var details = false
        var cancelled = false
        compose.setContent { Frame {
            FileManagerClipboardBar(ClipboardState(ClipboardOperation.COPY, listOf(File("/fixture/Brief.pdf"))),
                { cancelled = true }, {}, isTransferring = true, onShowTransfers = { details = true })
        } }
        compose.onNodeWithText("Paste").assertIsNotEnabled()
        compose.onNodeWithText("Details").performClick()
        assertTrue(details)
        assertFalse(cancelled)
    }

    @Test fun selectionActionsRemainUsableAtLargeText() {
        var copied = false
        var shared = false
        compose.setContent { Frame(true) { Column(Modifier.width(288.dp).verticalScroll(rememberScrollState())) {
            FilesSelectionBar(2, 3, {}, {}, { shared = true }, { copied = true }, {}, null, {}, {})
        } } }
        compose.onNodeWithText("Copy").performScrollTo().performClick()
        compose.onNodeWithText("Share").performScrollTo().performClick()
        assertTrue(copied && shared)
    }
}
