package com.abhisheksati.mydoc.ui.files

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.model.StorageItem
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Exercises the production navigation host, including its saved preview back entry. */
class PreviewReaderNavigationOnDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun selectedPageOpensInReaderAndBackRestoresPreviewAfterRecreation() {
        val app = compose.activity.application as MyDocApp
        val file = File.createTempFile("continuity-", ".pdf", app.cacheDir)
        val pdf = android.graphics.pdf.PdfDocument()
        try {
            repeat(2) { index ->
                val page = pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(300, 420, index + 1).create())
                page.canvas.drawText("Continuity page ${index + 1}", 24f, 60f,
                    android.graphics.Paint().apply { textSize = 20f })
                pdf.finishPage(page)
            }
            file.outputStream().use(pdf::writeTo)
        } finally { pdf.close() }
        try {
            compose.runOnUiThread { compose.activity.showFilePreview(StorageItem(file)) }
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Page 1 of 2").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Next preview page").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Page 2 of 2").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Open").performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithContentDescription("Search text in document").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("2/2").assertIsDisplayed()
            compose.onNodeWithContentDescription("Navigate back").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Page 2 of 2").fetchSemanticsNodes().isNotEmpty() }
            compose.activityRule.scenario.recreate()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Page 2 of 2").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Close preview").performClick()
            compose.onNodeWithText("Quick preview").assertDoesNotExist()
        } finally {
            runBlocking { app.documentRepository.removeFromRecents(Uri.fromFile(file).toString()) }
            file.delete()
        }
    }

    @Test fun moreActionsReturnsToTheSameFileInTheExplorer() {
        val file = File.createTempFile("preview-actions-", ".txt", compose.activity.cacheDir)
        file.writeText("Preview action fixture")
        try {
            compose.onNodeWithText("Library").performClick()
            compose.runOnUiThread { compose.activity.showFilePreview(StorageItem(file)) }
            compose.onNodeWithText("More actions").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Rename").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(file.name).assertExists()
            com.abhisheksati.mydoc.testutil.pressSystemBack()
            compose.onNodeWithText("Rename").assertDoesNotExist()
        } finally { file.delete() }
    }
}
