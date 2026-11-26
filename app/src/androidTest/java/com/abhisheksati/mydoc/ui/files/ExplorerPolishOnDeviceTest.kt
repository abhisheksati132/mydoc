package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.*
import com.abhisheksati.mydoc.DeviceCaptures
import org.junit.Assert.*
import org.junit.Rule
import org.junit.ClassRule
import org.junit.Test
import java.io.File
import java.io.IOException

class ExplorerPolishOnDeviceTest {
    companion object {
        // Keep one real font setting for this batch; rapid per-test toggles race Android configuration delivery.
        @ClassRule @JvmField val fontScale = com.abhisheksati.mydoc.testutil.AndroidFontScaleRule()
    }
    @get:Rule val compose = createComposeRule()

    @Test fun detailsHaveComfortableCopyTargetsAndShowTheWholeChecksum() {
        val hash = "0123456789abcdef".repeat(4)
        compose.setContent { AppDesignSystemTheme {
            FilePropertiesDialog(FolderProperties(File("/fixture/Project references/report.pdf"), 1234, 1, 0, true, true), hash, {})
        } }
        compose.onNodeWithContentDescription("Copy Path").performScrollTo().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Copy Checksum").performScrollTo().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(hash, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse("Checksum must remain inspectable without ellipsis", layouts.single().hasVisualOverflow)
        assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0.01f)
        DeviceCaptures.save("explorer-details-large")
    }

    @Test fun transferErrorsExpandWithoutHidingTheDestinationAction() {
        var opened: File? = null
        val destination = File("/fixture/Completed exports")
        val failures = (1..8).map { TransferFailure("failed-$it.pdf", IOException("Insufficient space")) }
        compose.setContent { AppDesignSystemTheme {
            Column(Modifier.width(288.dp).height(400.dp).verticalScroll(rememberScrollState())) {
                ExplorerTransferCard(ExplorerTransfer(9, ClipboardOperation.COPY, destination, 8,
                    failed = 8, failures = failures, status = TransferStatus.FAILED), {}, { opened = it })
            }
        } }
        compose.onNodeWithText("failed-8.pdf").assertDoesNotExist()
        compose.onNodeWithText("Show all errors").performScrollTo().performClick()
        compose.onNodeWithText("failed-8.pdf").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Show fewer errors").performScrollTo().performClick()
        compose.onNodeWithText("failed-8.pdf").assertDoesNotExist()
        compose.onNodeWithText("Open destination folder").performScrollTo().performClick()
        assertEquals(destination, opened)
        DeviceCaptures.save("explorer-transfer-large", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test fun blankArchiveNameCannotSubmit() {
        var submissions = 0
        compose.setContent { AppDesignSystemTheme { CompressDialog("", {}, { submissions++ }) } }
        compose.onNodeWithText("Compress", useUnmergedTree = false).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("   ")
        compose.onNodeWithText("Compress", useUnmergedTree = false).assertIsNotEnabled()
        assertEquals(0, submissions)
    }

    @Test fun renameResetsForAnotherFileAndKeyboardSubmitsTheEditedName() {
        var file by mutableStateOf(File("/fixture/old.txt"))
        var submitted: String? = null
        compose.setContent { AppDesignSystemTheme { RenameDialog(file, {}, { submitted = it }) } }
        compose.onNode(hasSetTextAction()).performTextReplacement("draft.txt")
        compose.runOnIdle { file = File("/fixture/next.txt") }
        compose.onNode(hasSetTextAction()).assertTextContains("next.txt")
        compose.onNode(hasSetTextAction()).performTextReplacement("approved.txt")
        compose.onNode(hasSetTextAction()).performImeAction()
        assertEquals("approved.txt", submitted)
    }

    @Test fun newFolderKeepsItsLabelAndKeyboardSubmission() {
        var submitted: String? = null
        compose.setContent { AppDesignSystemTheme { NewFolderDialog({}, { submitted = it }) } }
        compose.onNode(hasSetTextAction()).performTextInput("References")
        compose.onNodeWithText("Folder name", useUnmergedTree = true).assertExists()
        compose.onNode(hasSetTextAction()).performImeAction()
        assertEquals("References", submitted)
    }

    @Test fun archiveDraftSurvivesRecreationAndKeyboardSubmission() {
        var submitted: String? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AppDesignSystemTheme { CompressDialog("Archive.zip", {}, { submitted = it }) } }
        compose.onNode(hasSetTextAction()).performTextReplacement("  Review.zip  ")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextContains("  Review.zip  ")
        compose.onNode(hasSetTextAction()).performImeAction()
        assertEquals("Review.zip", submitted)
    }
}
