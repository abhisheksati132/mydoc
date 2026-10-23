package com.abhisheksati.mydoc.data.repository

import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryTest {

    @Test
    fun testLibraryItemKindFiltering() {
        val items = listOf(
            LibraryItem(uri = "file://1.pdf", name = "Doc.pdf", kind = MediaKind.DOCUMENT, mimeType = "application/pdf", sizeBytes = 1000L, lastModified = 100L),
            LibraryItem(uri = "file://2.jpg", name = "Photo.jpg", kind = MediaKind.PHOTO, mimeType = "image/jpeg", sizeBytes = 2000L, lastModified = 200L),
            LibraryItem(uri = "file://3.mp4", name = "Movie.mp4", kind = MediaKind.VIDEO, mimeType = "video/mp4", sizeBytes = 50000L, lastModified = 300L),
            LibraryItem(uri = "file://4.mp3", name = "Song.mp3", kind = MediaKind.AUDIO, mimeType = "audio/mpeg", sizeBytes = 5000L, lastModified = 400L)
        )

        val docs = items.filter { it.kind == MediaKind.DOCUMENT }
        assertEquals(1, docs.size)
        assertEquals("Doc.pdf", docs.first().name)

        val photos = items.filter { it.kind == MediaKind.PHOTO }
        assertEquals(1, photos.size)

        val videos = items.filter { it.kind == MediaKind.VIDEO }
        assertEquals(1, videos.size)

        val audio = items.filter { it.kind == MediaKind.AUDIO }
        assertEquals(1, audio.size)
    }

    @Test
    fun testLibraryItemSearch() {
        val items = listOf(
            LibraryItem(uri = "file://1.pdf", name = "Financial Report.pdf", kind = MediaKind.DOCUMENT, mimeType = "application/pdf", sizeBytes = 1000L, lastModified = 100L),
            LibraryItem(uri = "file://2.jpg", name = "Beach Vacation.jpg", kind = MediaKind.PHOTO, mimeType = "image/jpeg", sizeBytes = 2000L, lastModified = 200L),
            LibraryItem(uri = "file://3.mp4", name = "Report Presentation.mp4", kind = MediaKind.VIDEO, mimeType = "video/mp4", sizeBytes = 50000L, lastModified = 300L)
        )

        val query = "report"
        val matched = items.filter { it.name.contains(query, ignoreCase = true) }
        assertEquals(2, matched.size)
        assertTrue(matched.any { it.name == "Financial Report.pdf" })
        assertTrue(matched.any { it.name == "Report Presentation.mp4" })
    }

    @Test
    fun testLibraryItemProgressComputation() {
        val docItem = LibraryItem(
            uri = "file://1.pdf",
            name = "Doc.pdf",
            kind = MediaKind.DOCUMENT,
            mimeType = "application/pdf",
            sizeBytes = 1000L,
            lastModified = 100L,
            lastPage = 4,
            pageCount = 10,
            progressFraction = 0.5f
        )
        assertEquals("Page 5 of 10", docItem.formattedProgress)

        val videoItem = LibraryItem(
            uri = "file://2.mp4",
            name = "Video.mp4",
            kind = MediaKind.VIDEO,
            mimeType = "video/mp4",
            sizeBytes = 5000L,
            lastModified = 100L,
            lastPositionMs = 65000L,
            durationMs = 130000L,
            progressFraction = 0.5f
        )
        assertEquals("01:05 / 02:10", videoItem.formattedProgress)
    }
}
