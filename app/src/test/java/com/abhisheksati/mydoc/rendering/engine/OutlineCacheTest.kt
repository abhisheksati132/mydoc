package com.abhisheksati.mydoc.rendering.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * N-30: an outline sidecar keyed only by filename kept serving yesterday's table of contents, and a
 * document whose outline disappeared kept the old sidecar alive.
 */
class OutlineCacheTest {

    private val dir = File(System.getProperty("java.io.tmpdir"), "outline-cache-${System.nanoTime()}")

    private fun pdf(content: String = "pdf-bytes"): File {
        dir.mkdirs()
        return File(dir, "deck.pdf").apply { writeText(content) }
    }

    private val sample = listOf(
        OutlineItem(title = "Agenda", pageIndex = 0, level = 0),
        OutlineItem(title = "Q3 | Revenue", pageIndex = 1, level = 1),
        OutlineItem(title = "Details", pageIndex = 2, level = 2)
    )

    @Test
    fun nestedLevelsSurviveARoundTrip() {
        val file = pdf()
        OutlineCache.saveOutlines(file, sample)
        val loaded = OutlineCache.loadOutlines(file)
        assertEquals(sample.map { it.level }, loaded.map { it.level })
        assertEquals(sample.map { it.pageIndex }, loaded.map { it.pageIndex })
    }

    @Test
    fun aTitleContainingTheFieldSeparatorIsNotMangledAnymore() {
        val file = pdf()
        OutlineCache.saveOutlines(file, sample)
        val loaded = OutlineCache.loadOutlines(file)
        assertEquals("Q3 | Revenue", loaded[1].title)
        assertFalse("titles must not be rewritten to survive the format", loaded[1].title.contains('/'))
    }

    @Test
    fun aRebuiltDocumentUnderTheSameNameDoesNotReuseTheOldOutline() {
        val file = pdf("first conversion")
        OutlineCache.saveOutlines(file, sample)
        assertEquals(3, OutlineCache.loadOutlines(file).size)

        // Same path, different content: the deck was re-converted after an edit.
        file.writeText("second conversion, a different length on purpose")
        assertTrue(
            "the stale sidecar was served again",
            OutlineCache.loadOutlines(file).isEmpty()
        )
        assertFalse("the untrusted sidecar should have been dropped", OutlineCache.sidecarFileFor(file).exists())
    }

    @Test
    fun anEmptyOutlineClearsThePreviousSidecar() {
        val file = pdf()
        OutlineCache.saveOutlines(file, sample)
        assertEquals(3, OutlineCache.loadOutlines(file).size)

        OutlineCache.saveOutlines(file, emptyList())
        assertTrue("a deleted outline must not linger", OutlineCache.loadOutlines(file).isEmpty())
        assertFalse(OutlineCache.sidecarFileFor(file).exists())
    }

    @Test
    fun aSidecarWithoutAStampIsNotTrusted() {
        val file = pdf()
        // Written by an older build: pipe separated, no stamp line.
        File(dir, "deck.pdf.outline").writeText("0|0|Agenda\n1|1|Q3 / Revenue\n")
        assertTrue("an unversioned sidecar must not be read", OutlineCache.loadOutlines(file).isEmpty())
    }

    @Test
    fun nothingCachedMeansNothingLoaded() {
        val file = pdf()
        assertEquals(emptyList<OutlineItem>(), OutlineCache.loadOutlines(file.absolutePath))
    }
}
