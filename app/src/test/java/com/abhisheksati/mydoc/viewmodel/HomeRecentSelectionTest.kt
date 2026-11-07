package com.abhisheksati.mydoc.viewmodel

import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRecentSelectionTest {

    private fun item(uri: String) = LibraryItem(
        uri = uri, name = "$uri.pdf", kind = MediaKind.DOCUMENT,
        mimeType = "application/pdf", sizeBytes = 10L, lastModified = 1L
    )

    @Test fun selectedRowThatBecameTheHeroIsPruned() {
        val recents = listOf(item("a"), item("b"))
        assertEquals(emptySet<String>(), pruneRecentSelection(setOf("a"), recents, heroUri = "a"))
    }

    @Test fun selectionOfVisibleNonHeroRowsSurvives() {
        val recents = listOf(item("a"), item("b"), item("c"))
        assertEquals(setOf("b", "c"), pruneRecentSelection(setOf("a", "b", "c"), recents, heroUri = "a"))
    }

    @Test fun rowsRemovedFromRecentsArePruned() {
        assertEquals(setOf("b"), pruneRecentSelection(setOf("a", "b"), listOf(item("b")), heroUri = null))
    }

    @Test fun clearedRecentsLeaveNoSelection() {
        assertEquals(emptySet<String>(), pruneRecentSelection(setOf("a", "b"), emptyList(), heroUri = null))
    }
}