package com.abhisheksati.mydoc.ui.files

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.data.local.*
import com.abhisheksati.mydoc.data.repository.StorageRepositoryImpl
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Fixtures live only inside a unique cache folder; these tests never touch user files or Trash. */
class ExplorerToolsOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fixture(): File = File(context.cacheDir, "explorer-tools-${UUID.randomUUID()}").apply { check(mkdirs()) }

    @Test fun archiveNavigatesWithoutWritingAndConfirmsExtraction() {
        val root = fixture()
        val zip = root.resolve("sample.zip")
        ZipOutputStream(zip.outputStream()).use { stream ->
            stream.putNextEntry(ZipEntry("docs/report.txt")); stream.write("report".toByteArray()); stream.closeEntry()
        }
        var extracted = false
        try {
            compose.setContent { AppDesignSystemTheme { ExplorerArchiveSheet(zip, {}, { extracted = true }) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("docs", substring = false).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("docs").performClick()
            compose.onNodeWithText("report.txt").assertIsDisplayed()
            assertFalse(root.resolve("docs").exists())
            compose.onNodeWithText("Extract all").performScrollTo().performClick()
            assertFalse(extracted)
            compose.onNodeWithText("Cancel").performClick()
            assertFalse(extracted)
            compose.onNodeWithText("Up one folder").performClick()
            compose.onNodeWithText("docs").assertIsDisplayed()
        } finally { root.deleteRecursively() }
    }

    @Test fun folderInsightsShowsCountsAndLargestFiles() {
        val root = fixture()
        root.resolve("pictures").mkdir()
        root.resolve("pictures/photo.jpg").writeText("picture")
        root.resolve(".hidden").writeText("hidden")
        try {
            compose.setContent { AppDesignSystemTheme { ExplorerFolderInsightsSheet(root, {}, {}) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Files: 2 · Folders: 1").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("pictures").assertIsDisplayed()
            compose.onNodeWithText("photo.jpg").performScrollTo().assertIsDisplayed()
            assertEquals("picture", root.resolve("pictures/photo.jpg").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun transferSheetStopsSafelyAndShowsProgress() {
        val center = ExplorerTransfers()
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val release = CompletableDeferred<Unit>()
        compose.setContent { AppDesignSystemTheme { ExplorerTransfersSheet(center) {} } }
        try {
            compose.runOnIdle { center.start(owner,
                ClipboardState(ClipboardOperation.COPY, listOf(File("a"), File("b"))), File("target"),
                { _, _, _ -> release.await(); Result.success(1) }) {} }
            compose.onNodeWithText("Stop after current item").performClick()
            compose.onNodeWithText("Stopping after the current item…").assertIsDisplayed()
            release.complete(Unit)
            compose.waitUntil(10_000) { center.state.value.first().status == TransferStatus.CANCELLED }
            compose.onNodeWithText("1 of 2 items completed").assertIsDisplayed()
            compose.onNodeWithText("Clear finished").performClick()
            compose.onNodeWithText("No transfers yet.", substring = true).assertIsDisplayed()
        } finally { release.complete(Unit); owner.cancel() }
    }

    @Test fun insightsSkipsActualAndroidSymlink() = runBlocking {
        val root = fixture()
        try {
            val folder = root.resolve("folder").apply { mkdir() }
            folder.resolve("inside").writeText("abc")
            val outside = root.resolve("outside").apply { writeText("outside") }
            java.nio.file.Files.createSymbolicLink(folder.resolve("link").toPath(), outside.toPath())
            val data = ExplorerFolderInsights().scan(folder)
            assertEquals(3L, data.totalBytes)
            assertEquals(1, data.fileCount)
            assertTrue(data.incomplete)
        } finally { root.deleteRecursively() }
    }

    @Test fun transferUsesRealCopyWithoutChangingSource() = runBlocking {
        val root = fixture()
        try {
            val source = root.resolve("source.txt").apply { writeText("original bytes") }
            val destination = root.resolve("destination").apply { mkdir() }
            val clipboard = ExplorerClipboard().apply { bind(this@runBlocking, StorageRepositoryImpl(context)) }
            clipboard.set(ClipboardState(ClipboardOperation.COPY, listOf(source)))
            assertTrue(clipboard.paste(destination))
            withTimeout(10_000) { while (clipboard.transfers.state.value.first().running) delay(10) }
            assertEquals(TransferStatus.COMPLETED, clipboard.transfers.state.value.first().status)
            assertEquals(source.readText(), destination.resolve("source.txt").readText())
            assertNull(clipboard.state.value)
        } finally { root.deleteRecursively() }
    }
}
