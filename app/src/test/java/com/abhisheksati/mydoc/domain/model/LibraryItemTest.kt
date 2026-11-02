package com.abhisheksati.mydoc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LibraryItemTest {

    private fun item(
        uri: String,
        name: String,
        kind: MediaKind = MediaKind.DOCUMENT,
        sizeBytes: Long = 1024L
    ) = LibraryItem(
        uri = uri,
        name = name,
        kind = kind,
        mimeType = "application/octet-stream",
        sizeBytes = sizeBytes,
        lastModified = 100L
    )

    @Test
    fun identityKey_matchesAcrossUriSchemes_forTheSameFile() {
        val scanned = item("file:///storage/emulated/0/Download/report.pdf", "report.pdf")
        val openedViaMediaStore = item("content://media/external/downloads/42", "report.pdf")

        assertEquals(scanned.identityKey, openedViaMediaStore.identityKey)
    }

    @Test
    fun identityKey_separatesDifferentFiles() {
        val a = item("file:///a.pdf", "a.pdf", sizeBytes = 100L)
        val b = item("file:///b.pdf", "b.pdf", sizeBytes = 200L)

        assertNotEquals(a.identityKey, b.identityKey)
    }

    @Test
    fun identityKey_separatesSameNameAcrossMediaKinds() {
        val asDocument = item("file:///clip.mp4", "clip.mp4", kind = MediaKind.DOCUMENT)
        val asVideo = item("content://media/external/videos/media/9", "clip.mp4", kind = MediaKind.VIDEO)

        assertNotEquals(asDocument.identityKey, asVideo.identityKey)
    }
}
