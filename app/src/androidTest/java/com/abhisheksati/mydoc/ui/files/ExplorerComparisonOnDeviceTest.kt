package com.abhisheksati.mydoc.ui.files

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.data.local.ExplorerComparison
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.ComparisonStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class ExplorerComparisonOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private fun fixture() = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "comparison-${UUID.randomUUID()}").apply { check(mkdirs()) }

    @Test fun comparesContentsAndCanShowMatchingFilesWithoutWriting() {
        val root = fixture()
        val left = root.resolve("left").apply { mkdir() }
        val right = root.resolve("right").apply { mkdir() }
        left.resolve("same.txt").writeText("same")
        right.resolve("same.txt").writeText("same")
        left.resolve("changed.txt").writeText("AAAA")
        right.resolve("changed.txt").writeText("BBBB")
        try {
            compose.setContent { AppDesignSystemTheme { ExplorerComparisonSheet(listOf(left, right)) {} } }
            compose.onNodeWithText("Compare contents").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Matching files: 1").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Different contents or type: 1").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("changed.txt").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Hide matching files").performScrollTo().performClick()
            compose.onNodeWithText("same.txt").performScrollTo().assertIsDisplayed()
            assertEquals("AAAA", left.resolve("changed.txt").readText())
            assertEquals("BBBB", right.resolve("changed.txt").readText())
            assertEquals(2, left.listFiles()!!.size)
        } finally { root.deleteRecursively() }
    }

    @Test fun promptsForTwoDifferentFolders() {
        val root = fixture()
        try {
            compose.setContent { AppDesignSystemTheme { ExplorerComparisonSheet(listOf(root)) {} } }
            compose.onNodeWithText("Open two different folders in separate tabs, then return here.").assertIsDisplayed()
            compose.onNodeWithText("Compare contents").performScrollTo().assertIsNotEnabled()
        } finally { root.deleteRecursively() }
    }

    @Test fun actualSymlinkCannotProduceFalseMissingOrMatch() = runBlocking {
        val root = fixture()
        try {
            val left = root.resolve("left").apply { mkdir() }
            val right = root.resolve("right").apply { mkdir() }
            val outside = root.resolve("outside").apply { writeText("secret") }
            java.nio.file.Files.createSymbolicLink(left.resolve("file").toPath(), outside.toPath())
            right.resolve("file").writeText("secret")
            val result = ExplorerComparison().compare(left, right)
            assertTrue(result.incomplete)
            assertEquals(ComparisonStatus.UNKNOWN, result.entries.single().status)
        } finally { root.deleteRecursively() }
    }
    @Test fun redirectedParentCannotBeReportedAsMatching() = runBlocking {
        val root = fixture()
        try {
            val left = root.resolve("left").apply { mkdir() }
            val right = root.resolve("right").apply { mkdir() }
            val parent = left.resolve("nested").apply { mkdir() }
            val remote = root.resolve("outside").apply { mkdir() }
            val source = parent.resolve("file").apply { writeText("same") }
            remote.resolve("file").apply { writeText("same"); setLastModified(source.lastModified()) }
            right.resolve("nested").mkdir()
            right.resolve("nested/file").writeText("same")
            val result = ExplorerComparison(afterScan = {
                check(parent.renameTo(left.resolve("old")))
                java.nio.file.Files.createSymbolicLink(parent.toPath(), remote.toPath())
            }).compare(left, right)
            assertTrue(result.incomplete)
            assertEquals(ComparisonStatus.UNKNOWN, result.entries.single().status)
        } finally { root.deleteRecursively() }
    }

}
