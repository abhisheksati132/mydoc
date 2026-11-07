package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.abhisheksati.mydoc.data.local.ExplorerTransfers
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import org.junit.Rule
import org.junit.Test

class TransferFeedbackOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun destinationActionRemainsUsableAtLargeText() {
        val folder = File("/sdcard/Documents/Project reports/Completed exports")
        var opened: File? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AppDesignSystemTheme {
                    Column(Modifier.width(288.dp).height(400.dp).verticalScroll(rememberScrollState())) {
                        ExplorerTransferCard(ExplorerTransfer(1, ClipboardOperation.COPY, folder, 1,
                            completed = 1, status = TransferStatus.COMPLETED), {}, { opened = it })
                    }
                }
            }
        }
        compose.onNodeWithText("Open destination folder").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(folder, opened)
    }

    @Test fun stopCannotBeRequestedAgainWhileFinishingTheCurrentItem() {
        var stopped = false
        compose.setContent { AppDesignSystemTheme {
            ExplorerTransferCard(ExplorerTransfer(1, ClipboardOperation.CUT, File("/sdcard/target"), 2,
                currentName = "large-folder", status = TransferStatus.STOPPING), { stopped = true })
        } }
        compose.onNodeWithText("Stop after current item").assertIsNotEnabled()
        compose.onNodeWithText("Stopping after the current item…").assertIsDisplayed()
        assertFalse(stopped)
    }

    @Test fun failedTransferDoesNotLookLikeCompletedProgress() {
        val center = runBlocking {
            ExplorerTransfers().also { it.start(this,
                ClipboardState(ClipboardOperation.COPY, listOf(File("source.txt"))), File("destination"),
                { _, _, _ -> Result.failure(IOException("Not enough free space")) }) {} }
        }
        compose.setContent { AppDesignSystemTheme { ExplorerTransfersSheet(center) {} } }
        compose.onNodeWithText("Could not complete transfer").assertIsDisplayed()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(0)
    }

    @Test fun stoppedTransferExplainsHowToContinue() {
        val center = runBlocking {
            ExplorerTransfers().also { center -> center.start(this,
                ClipboardState(ClipboardOperation.COPY, listOf(File("first.txt"), File("second.txt"))), File("destination"),
                { _, _, _ -> center.cancel(); Result.success(1) }) {} }
        }
        compose.setContent { AppDesignSystemTheme { ExplorerTransfersSheet(center) {} } }
        compose.onNodeWithText("Review the source and destination before continuing with unfinished files.")
            .performScrollTo().assertIsDisplayed()
    }
}
