package com.abhisheksati.mydoc.ui.files

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.data.archive.*
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.archive.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveWorkspaceOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun encryptedZipPasswordFieldAcceptsTheWholePasswordAndPreviewsContent() {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-zip-password-").toFile()
        var preview: File? = null
        try {
            val source = File(root, "secret.txt").apply { writeText("Encrypted ZIP preview") }
            val password = "fixture-zip-password"
            val archive = runBlocking { NativeArchiveEngine(context).create(listOf(source), root, "protected.zip", ArchiveCreateOptions(), password.toCharArray()).first() }
            compose.setContent { AppDesignSystemTheme { ArchiveWorkspaceSheet(archive, {}, { preview = it }, {}) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Password").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Password").performTextInput(password)
            compose.onNodeWithText("secret.txt").performScrollTo().performClick()
            compose.waitUntil(10_000) { preview != null }
            assertEquals(source.readText(), preview!!.readText())
        } finally { preview?.parentFile?.deleteRecursively(); root.deleteRecursively() }
    }

    @Test fun archiveSwitchesHaveLabelsAndWholeRowTouchTargets() {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-accessibility-").toFile()
        try {
            val source = File(root, "source.txt").apply { writeText("Fixture") }
            compose.setContent { AppDesignSystemTheme { ArchiveCreateSheet(listOf(source), root, {}, {}) } }
            compose.onNodeWithText("Format: ZIP").performClick()
            compose.onNodeWithText("7Z").performClick()
            compose.onNodeWithText("Password (optional)").performTextInput("fixture-password")
            compose.onNode(hasText("Encrypt filenames") and isToggleable()).performScrollTo().assertIsOff().performClick().assertIsOn()
            val bounds = compose.onNode(hasText("Encrypt filenames") and isToggleable()).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.height >= 48 * context.resources.displayMetrics.density)
        } finally { root.deleteRecursively() }
    }

    @Test fun batchIntegrityChecksQueueAndFinishBothArchives() {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-batch-").toFile()
        val center = ArchiveJobs.get(context)
        runBlocking { center.awaitHistory() }
        try {
            val sources = (1..2).map { number -> File(root, "sample$number.zip").apply {
                ZipOutputStream(outputStream()).use { zip -> zip.putNextEntry(ZipEntry("sample.txt")); zip.write("Fixture $number".toByteArray()); zip.closeEntry() }
            } }
            val before = center.state.value.map { it.id }.toSet()
            compose.setContent { AppDesignSystemTheme { ArchiveCreateSheet(sources, root, {}, {}) } }
            compose.onNodeWithText("Check each archive").performScrollTo().performClick()
            compose.waitUntil(20_000) { center.state.value.count { it.id !in before && !it.active } == 2 }
            val results = center.state.value.filter { it.id !in before }
            assertTrue(results.toString(), results.all { it.status == ArchiveJobStatus.COMPLETED })
        } finally { root.deleteRecursively() }
    }

    @Test fun browserSearchesSelectsAndPreviewsNestedFiles() {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-ui-").toFile()
        var opened: File? = null
        try {
            val archive = File(root, "reference.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                for ((name, text) in listOf("docs/report.txt" to "Reference preview", "other.txt" to "Other")) {
                    zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry()
                }
            }
            compose.setContent { AppDesignSystemTheme { ArchiveWorkspaceSheet(archive, {}, { opened = it }, {}) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("docs").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Search this archive").performTextInput("report")
            compose.onNodeWithContentDescription("Select archive entry: docs/report.txt").performClick().assertIsOn()
            compose.onNodeWithText("report.txt").performClick()
            compose.waitUntil(10_000) { opened != null }
            assertEquals("Reference preview", opened!!.readText())
            assertFalse(File(root, "docs").exists())
        } finally { opened?.parentFile?.parentFile?.takeIf { it.name.endsWith("extracted") }?.deleteRecursively(); root.deleteRecursively() }
    }

    @Test fun passwordPromptRecoversAndOpensEncryptedListing() {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-ui-").toFile()
        try {
            val source = File(root, "secret.txt").apply { writeText("Fixture") }
            val password = "fixture-password".toCharArray()
            val archive = runBlocking { NativeArchiveEngine(context).create(listOf(source), root, "protected.7z", ArchiveCreateOptions(format = ArchiveFormat.SEVEN_ZIP, encryptNames = true), password).first() }
            password.fill('\u0000')
            compose.setContent { AppDesignSystemTheme { ArchiveWorkspaceSheet(archive, {}, {}, {}) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("This archive needs a password.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Password").performTextInput("fixture-password")
            compose.onNodeWithText("Unlock").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("secret.txt").fetchSemanticsNodes().isNotEmpty() }
        } finally { root.deleteRecursively() }
    }

    @Test fun creationSheetQueuesVerifiedOutputThroughForegroundService() {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-ui-").toFile()
        val center = ArchiveJobs.get(context)
        runBlocking { center.awaitHistory() }
        var queued = false
        try {
            val source = File(root, "fixture.txt").apply { writeText("Background archive") }
            val before = center.state.value.map { it.id }.toSet()
            compose.setContent { AppDesignSystemTheme { ArchiveCreateSheet(listOf(source), root, {}, { queued = true }) } }
            compose.onNodeWithText("Add to queue").performScrollTo().performClick()
            compose.waitUntil(20_000) { queued && center.state.value.any { it.id !in before && !it.active } }
            val result = center.state.value.first { it.id !in before }
            assertEquals(result.toString(), ArchiveJobStatus.COMPLETED, result.status)
            assertEquals("fixture.zip", result.outputs.single().name)
            runBlocking { NativeArchiveEngine(context).test(result.outputs.single()) }
        } finally { root.deleteRecursively() }
    }
}
