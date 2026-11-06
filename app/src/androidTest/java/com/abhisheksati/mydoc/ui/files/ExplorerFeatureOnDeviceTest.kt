package com.abhisheksati.mydoc.ui.files

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions
import com.abhisheksati.mydoc.domain.model.ExplorerRenamePlan
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Dialog-only tests. No production Trash, offline documents or shared-storage files are mutated. */
class ExplorerFeatureOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchCollectsCombinedFilters() {
        var submitted: ExplorerSearchOptions? = null
        compose.setContent { AppDesignSystemTheme { ExplorerSearchDialog(File("/storage/emulated/0/Documents"), null, {}, { submitted = it }) } }
        compose.onNodeWithText("Name contains").performTextInput("report")
        compose.onNodeWithText("Extension (for example, pdf)").performTextInput("pdf")
        compose.onNodeWithText("Minimum size in KiB").performTextInput("2")
        compose.onNodeWithContentDescription("Include subfolders").performScrollTo().performClick()
        compose.onNodeWithText("Search", useUnmergedTree = false).performClick()
        compose.runOnIdle {
            assertEquals("report", submitted?.name)
            assertEquals("pdf", submitted?.extension)
            assertEquals(2048L, submitted?.minBytes)
            assertTrue(submitted?.recursive == true)
        }
    }

    @Test fun renameShowsPreviewBeforeAnyFilesystemChanges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.cacheDir, "explorer-ui-${System.nanoTime()}").apply { mkdirs() }
        val source = folder.resolve("photo.jpg").apply { writeText("original") }
        var submitted: ExplorerRenamePlan? = null
        try {
            compose.setContent { AppDesignSystemTheme { ExplorerRenameDialog(listOf(source), {}, { submitted = it }) } }
            compose.onNodeWithText("Add before the name").performTextInput("Trip_")
            compose.waitUntil(10_000) { compose.onAllNodesWithText("→ Trip_photo.jpg").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("→ Trip_photo.jpg").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Rename").performClick()
            compose.runOnIdle {
                assertTrue(submitted?.canApply == true)
                assertTrue(source.exists())
                assertFalse(folder.resolve("Trip_photo.jpg").exists())
                assertEquals("original", source.readText())
            }
        } finally { folder.deleteRecursively() }
    }
}
