package com.abhisheksati.mydoc.ui.search

import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotlightFilterTest {

    private fun item(name: String, kind: MediaKind): LibraryItem =
        LibraryItem("content://media/$name", name, kind, "application/octet-stream", 10L, 1L)

    @Test
    fun `Media matches every indexed non-document kind, not a fixed extension list`() {
        val media = listOf(
            item("photo.heic", MediaKind.PHOTO),
            item("anim.gif", MediaKind.PHOTO),
            item("clip.webm", MediaKind.VIDEO),
            item("voice.m4a", MediaKind.AUDIO),
            item("sound.ogg", MediaKind.AUDIO),
            item("old.jpg", MediaKind.PHOTO),
            item("old.mp4", MediaKind.VIDEO),
            item("old.mp3", MediaKind.AUDIO)
        )
        for (entry in media) {
            assertTrue("${entry.name} is indexed media and must match the Media chip",
                spotlightFilterMatches(entry, "Media"))
        }
        assertFalse(spotlightFilterMatches(item("report.pdf", MediaKind.DOCUMENT), "Media"))
    }

    @Test
    fun `document chips keep their registry-defined membership`() {
        assertTrue(spotlightFilterMatches(item("report.pdf", MediaKind.DOCUMENT), "PDF"))
        assertFalse(spotlightFilterMatches(item("report.pdf", MediaKind.DOCUMENT), "Word"))
        assertTrue(spotlightFilterMatches(item("notes.docx", MediaKind.DOCUMENT), "Word"))
    }

    @Test
    fun `All matches documents and media alike`() {
        assertTrue(spotlightFilterMatches(item("report.pdf", MediaKind.DOCUMENT), "All"))
        assertTrue(spotlightFilterMatches(item("clip.webm", MediaKind.VIDEO), "All"))
    }
}