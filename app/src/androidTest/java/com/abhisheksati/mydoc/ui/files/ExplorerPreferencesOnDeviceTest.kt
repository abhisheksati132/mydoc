package com.abhisheksati.mydoc.ui.files

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.data.local.ExplorerPreferences
import com.abhisheksati.mydoc.data.repository.StorageRepositoryImpl
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.*
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

/** Private, uniquely named preference files and cache fixtures only. */
class ExplorerPreferencesOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun saveRecreateRunAndDeleteSearchWithoutTouchingFiles() {
        val token = UUID.randomUUID().toString()
        val root = File(context.cacheDir, "prefs-$token").apply { mkdirs() }
        val target = root.resolve("docs").apply { mkdir() }
        target.resolve("report.pdf").writeText("report bytes")
        target.resolve("notes.txt").writeText("notes bytes")
        val name = "explorer-test-$token"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        fun store() = ExplorerPreferences({ prefs.getString("data", null) }, { check(prefs.edit().putString("data", it).commit()) })
        val preferences = store()
        val owner = ViewModelStore()
        lateinit var browser: FileManagerViewModel
        var saving by mutableStateOf(true)
        try {
            compose.runOnIdle {
                browser = FileManagerViewModel(StorageRepositoryImpl(context), root, explorerPreferences = preferences)
                owner.put("browser", browser)
            }
            compose.setContent { AppDesignSystemTheme {
                if (saving) SaveExplorerSearchDialog({ saving = false }) { label ->
                    preferences.saveSearch(label, target, ExplorerSearchOptions(extension = "pdf", recursive = true))
                } else ExplorerSavedSearchesSheet(preferences, {}, browser::openSavedSearch)
            } }
            compose.onNodeWithText("Search name").performTextInput("My PDFs")
            compose.onNode(hasText("Save search") and hasClickAction()).performClick()
            compose.onNodeWithText("My PDFs").assertIsDisplayed()
            assertEquals("My PDFs", store().savedSearches.value.single().label)
            compose.onNodeWithText("Run search").performScrollTo().performClick()
            compose.waitUntil(10_000) { browser.uiState.value.advancedSearch != null && !browser.uiState.value.isLoading }
            assertEquals(target, browser.uiState.value.currentDirectory)
            assertEquals(listOf("report.pdf"), browser.uiState.value.filteredItems.map { it.name })
            compose.onNodeWithContentDescription("Delete saved search: My PDFs").performScrollTo().performClick()
            compose.onNodeWithText("No saved searches yet.", substring = true).assertIsDisplayed()
            assertTrue(store().savedSearches.value.isEmpty())
            assertEquals("report bytes", target.resolve("report.pdf").readText())
            assertEquals("notes bytes", target.resolve("notes.txt").readText())
        } finally {
            compose.runOnIdle { owner.clear() }
            context.deleteSharedPreferences(name)
            root.deleteRecursively()
        }
    }

    @Test fun folderViewPersistsAndResetLeavesFilesAlone() {
        val token = UUID.randomUUID().toString()
        val root = File(context.cacheDir, "views-$token").apply { mkdirs() }
        root.resolve("file.txt").writeText("original")
        val name = "explorer-views-test-$token"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val preferences = ExplorerPreferences({ prefs.getString("data", null) }, { check(prefs.edit().putString("data", it).commit()) })
        val owner = ViewModelStore()
        lateinit var browser: FileManagerViewModel
        try {
            preferences.setFolderView(root, FolderViewPreferences(FileSortOrder(FileSortField.SIZE, false), true))
            compose.runOnIdle {
                browser = FileManagerViewModel(StorageRepositoryImpl(context), root, explorerPreferences = preferences)
                owner.put("browser", browser)
                browser.startBrowsingStorage(root)
            }
            compose.waitUntil(10_000) { !browser.uiState.value.isLoading && browser.uiState.value.isBrowsingStorage }
            assertTrue(browser.uiState.value.isGridView)
            assertEquals(FileSortField.SIZE, browser.uiState.value.sortOrder.field)
            assertTrue(ExplorerPreferences({ prefs.getString("data", null) }, {}).folderView(root)!!.grid)
            compose.runOnIdle { browser.resetFolderView() }
            compose.waitUntil(10_000) { !browser.uiState.value.isGridView && browser.uiState.value.sortOrder.field == FileSortField.NAME }
            assertEquals(null, ExplorerPreferences({ prefs.getString("data", null) }, {}).folderView(root))
            assertEquals("original", root.resolve("file.txt").readText())
        } finally {
            compose.runOnIdle { owner.clear() }
            context.deleteSharedPreferences(name)
            root.deleteRecursively()
        }
    }
}
