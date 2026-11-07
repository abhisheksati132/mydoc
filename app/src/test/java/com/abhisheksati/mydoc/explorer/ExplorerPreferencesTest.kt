package com.abhisheksati.mydoc.explorer

import com.abhisheksati.mydoc.data.local.ExplorerPreferences
import com.abhisheksati.mydoc.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExplorerPreferencesTest {
    @Test fun persistsUnicodeSearchAndRelativeDateWindow() {
        var data: String? = null
        val store = ExplorerPreferences({ data }, { data = it })
        val now = 1_000_000_000L
        val options = ExplorerSearchOptions(name = "résumé", extension = "pdf", minBytes = 2048, recursive = true, modifiedAfter = now - 7 * 86_400_000L)
        assertTrue(store.saveSearch("Weekly PDFs", File("/documents"), options, now))
        val restored = ExplorerPreferences({ data }, {})
        val search = restored.savedSearches.value.single()
        assertEquals("Weekly PDFs", search.label)
        assertEquals("résumé", search.filters.name)
        assertEquals(7, search.modifiedWithinDays)
        assertEquals(now + 10 * 86_400_000L - 7 * 86_400_000L, search.optionsAt(now + 10 * 86_400_000L).modifiedAfter)
        assertEquals(2048L, search.filters.minBytes)
        assertTrue(search.filters.recursive)
        restored.deleteSearch(search.id)
        assertTrue(restored.savedSearches.value.isEmpty())
    }
    @Test fun folderViewsStayIndependentAndResetDoesNotDeleteFiles() {
        var data: String? = null
        val store = ExplorerPreferences({ data }, { data = it })
        val a = File("/a")
        val b = File("/b")
        val view = FolderViewPreferences(FileSortOrder(FileSortField.SIZE, false), true)
        store.setFolderView(a, view)
        assertEquals(view, store.folderView(a))
        assertEquals(null, store.folderView(b))
        assertEquals(view, ExplorerPreferences({ data }, {}).folderView(a))
        store.resetFolderView(a)
        assertEquals(null, store.folderView(a))
    }
    @Test fun rejectsInvalidSearchesAndHonorsCapacity() {
        val store = ExplorerPreferences()
        assertFalse(store.saveSearch(" ", File("/a"), ExplorerSearchOptions()))
        assertFalse(store.saveSearch("bad", File("/a"), ExplorerSearchOptions(minBytes = 2, maxBytes = 1)))
        repeat(50) { assertTrue(store.saveSearch("Search $it", File("/a"), ExplorerSearchOptions())) }
        assertFalse(store.saveSearch("overflow", File("/a"), ExplorerSearchOptions()))
        store.deleteSearch(store.savedSearches.value.first().id)
        assertTrue(store.saveSearch("one more", File("/a"), ExplorerSearchOptions()))
    }
    @Test fun boundsFolderHistoryAndIgnoresMalformedPayloads() {
        val store = ExplorerPreferences()
        repeat(201) { store.setFolderView(File("/folder$it"), FolderViewPreferences()) }
        assertEquals(200, store.folderViews.value.size)
        assertNull(store.folderView(File("/folder0")))
        assertTrue(ExplorerPreferences({ "version=bad\ncount=oops" }, {}).savedSearches.value.isEmpty())
        assertTrue(ExplorerPreferences({ "x".repeat(1_100_000) }, {}).folderViews.value.isEmpty())
    }
    @Test fun savingLaterDoesNotExpandExplicitDateWindow() {
        val store = ExplorerPreferences()
        val openedAt = 1_000_000_000L
        assertTrue(store.saveSearch("Last week", File("/a"), ExplorerSearchOptions(
            modifiedAfter = openedAt - 7 * 86_400_000L, modifiedWithinDays = 7), openedAt + 60_000L))
        assertEquals(7, store.savedSearches.value.single().modifiedWithinDays)
    }
    @Test fun failedWriteDoesNotPublishUnsavedPreferences() {
        val store = ExplorerPreferences(write = { throw java.io.IOException("failed") })
        assertFalse(store.saveSearch("search", File("/a"), ExplorerSearchOptions()))
        assertTrue(store.savedSearches.value.isEmpty())
        assertFalse(store.setFolderView(File("/a"), FolderViewPreferences()))
        assertTrue(store.folderViews.value.isEmpty())
    }

    @Test fun refreshingSameViewUpdatesEvictionOrderBeforeAndAfterRestart() {
        var data: String? = null
        val store = ExplorerPreferences({ data }, { data = it })
        repeat(200) { store.setFolderView(File("/folder$it"), FolderViewPreferences()) }
        store.setFolderView(File("/folder0"), FolderViewPreferences())
        store.saveSearch("test", File("/a"), ExplorerSearchOptions())
        val restored = ExplorerPreferences({ data }, {})
        store.setFolderView(File("/extra"), FolderViewPreferences())
        restored.setFolderView(File("/extra"), FolderViewPreferences())
        assertNotNull(store.folderView(File("/folder0")))
        assertNull(store.folderView(File("/folder1")))
        assertEquals(store.folderViews.value, restored.folderViews.value)
    }

}
