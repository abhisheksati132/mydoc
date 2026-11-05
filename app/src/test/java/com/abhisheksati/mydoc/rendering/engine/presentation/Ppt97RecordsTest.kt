package com.abhisheksati.mydoc.rendering.engine.presentation

import com.abhisheksati.mydoc.rendering.engine.Ole2File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The binary PowerPoint reader, run against decks PowerPoint itself saved as ".ppt" - one with
 * plain ASCII text and one whose title needs accented letters and an em dash and whose body needs
 * Japanese. Both were written by the COM automation in `make-ppt97-unicode.ps1` and
 * `make-legacy-fixtures.ps1`, not assembled by hand, because the point of these tests is that the
 * record tree of a real file is read as a tree.
 */
class Ppt97RecordsTest {

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }

    private fun documentStream(container: ByteArray): ByteArray {
        val ole = Ole2File(container)
        return ole.getStream("PowerPoint Document") ?: error("no PowerPoint Document stream in the fixture")
    }

    @Test
    fun `a three slide deck arrives as three slides`() {
        val slides = Ppt97Records.slides(documentStream(fixture("real-ppt97.ppt")))
        assertEquals("the record tree did not give three slides", 3, slides.size)
        assertEquals(
            listOf("Slide title 1", "Slide title 2", "Slide title 3"),
            slides.map { it.blocks.first() }
        )
        slides.forEach { slide ->
            val flat = slide.blocks.joinToString(" ")
            assertTrue("the bullets of a slide went missing: $flat", flat.contains("bullet one"))
            assertTrue("the bullets of a slide went missing: $flat", flat.contains("bullet three"))
        }
    }

    @Test
    fun `the master's placeholder text is not read as a slide`() {
        val slides = Ppt97Records.slides(documentStream(fixture("real-ppt97.ppt")))
        val everything = slides.joinToString(" ") { it.blocks.joinToString(" ") }
        assertFalse(
            "the master's own placeholder text was taken for slide content",
            everything.contains("Click to edit Master")
        )
        assertFalse("a font table was read as text", everything.lowercase().contains("wingdings"))
    }

    @Test
    fun `text outside Windows-1252 arrives in its own characters`() {
        val slides = Ppt97Records.slides(documentStream(fixture("real-ppt97-unicode.ppt")))
        assertEquals(1, slides.size)
        val title = slides.first().blocks.first()
        // "Café résumé naïve — 2026": the accents and the em dash are the point, since PowerPoint
        // switches to the wide-character atom for exactly these and a Latin-1 reader mangles them.
        assertEquals("Caf\u00e9 r\u00e9sum\u00e9 na\u00efve \u2014 2026", title)
        val body = slides.first().blocks.joinToString(" ")
        assertTrue("the Japanese body text was lost: $body", body.contains("\u65e5\u672c\u8a9e"))
    }

    @Test
    fun `bytes that are not a record tree yield no slides`() {
        // A truncated header and a body that claims more bytes than the stream holds must stop the
        // walk where it runs out of file, not invent records from the middle of a payload.
        val junk = ByteArray(4096) { (it % 251).toByte() }
        assertEquals(emptyList<Any>(), Ppt97Records.slides(junk))
        assertEquals(emptyList<Any>(), Ppt97Records.slides(ByteArray(0)))
    }
}
