package com.abhisheksati.mydoc.ui.files

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.data.local.ExplorerZipExtraction
import com.abhisheksati.mydoc.data.repository.StorageRepositoryImpl
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExplorerZipSelectionOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fixture() = File(context.cacheDir, "zip-selection-${UUID.randomUUID()}").apply { check(mkdirs()) }
    private fun zip(root: File): File = root.resolve("sample.zip").also { file ->
        ZipOutputStream(file.outputStream()).use { out ->
            for ((name, bytes) in listOf("docs/report.txt" to "preview bytes", "other.txt" to "other bytes")) {
                out.putNextEntry(ZipEntry(name)); out.write(bytes.toByteArray()); out.closeEntry()
            }
        }
    }

    @Test fun selectingFolderRequiresConfirmationWithoutExtractingDuringBrowse() {
        val root = fixture()
        val archive = zip(root)
        var selection: Set<String>? = null
        try {
            compose.setContent { AppDesignSystemTheme { ExplorerArchiveSheet(archive, {}, {},
                onExtractSelected = { selected, _ -> selection = selected }) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Select ZIP entry: docs/").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Select ZIP entry: docs/").performClick().assertIsOn()
            compose.onNodeWithText("Extract selected").performScrollTo().performClick()
            assertNull(selection)
            compose.onNode(hasText("Extract selected") and hasClickAction() and hasAnyAncestor(isDialog() and hasAnyDescendant(hasText("Cancel")))).performClick()
            assertEquals(setOf("docs/"), selection)
            assertEquals(listOf("sample.zip"), root.listFiles()!!.map { it.name })
        } finally { root.deleteRecursively() }
    }

    @Test fun openingMemberCreatesReadableContentUriWithSameBytes() {
        val root = fixture()
        val archive = zip(root)
        val cache = File(context.cacheDir, "shared/zip-preview-test-${UUID.randomUUID()}")
        val owner = ViewModelStore()
        lateinit var browser: FileManagerViewModel
        var opened: File? = null
        try {
            compose.runOnIdle {
                browser = FileManagerViewModel(StorageRepositoryImpl(context), root)
                owner.put("browser", browser)
            }
            compose.setContent { AppDesignSystemTheme { ExplorerArchiveSheet(archive, {}, {},
                onOpenMember = { member, stamp -> browser.openArchiveMember(archive, member, cache, stamp) { opened = it } },
                onExtractSelected = { _, _ -> }) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("docs", substring = false).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("docs").performClick()
            compose.onNodeWithText("report.txt").performClick()
            compose.waitUntil(10_000) { opened != null }
            val preview = requireNotNull(opened)
            assertEquals("preview bytes", preview.readText())
            assertEquals("txt", preview.extension)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", preview)
            assertEquals("content", uri.scheme)
            assertEquals("preview bytes", context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) })
            assertFalse(root.resolve("docs").exists())
        } finally {
            compose.runOnIdle { owner.clear() }
            cache.deleteRecursively()
            root.deleteRecursively()
        }
    }

    @Test fun selectedExtractionPreservesStructureAndSkipsOtherEntries() = runBlocking {
        val root = fixture()
        try {
            val archive = zip(root)
            val output = ExplorerZipExtraction().extract(archive, setOf("docs/"), root)
            assertEquals("preview bytes", output.resolve("docs/report.txt").readText())
            assertFalse(output.resolve("other.txt").exists())
            assertTrue(archive.exists())
        } finally { root.deleteRecursively() }
    }
}
