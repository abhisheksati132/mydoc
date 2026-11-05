package com.abhisheksati.mydoc.rendering.engine.word

import com.abhisheksati.mydoc.domain.model.LoadFailure
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Word 97 text, read from a document Word itself saved as ".doc" - heading, body, a hyperlink, bold
 * and regular runs, two list items and a 2x2 table.
 *
 * The point of the fixture is that the piece table, not the author of the test, decides where the
 * text is and how it is encoded.
 */
class Word97TextTest {

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }

    private fun paragraphs(bytes: ByteArray): Pair<DocxDocument, List<String>> {
        val doc = runBlocking { DocxParser().parse(ByteArrayInputStream(bytes)) }
        return doc to doc.blocks.mapNotNull { DocxBlock.plainTextOf(it) }
    }

    @Test
    fun `the piece table gives the text back in order and unscrambled`() {
        val (doc, lines) = paragraphs(fixture("real-word97.doc"))
        assertFalse("the reader fell through to the byte scan", doc.usedTextFallback)

        val joined = lines.joinToString("\n")
        listOf(
            "Chapter one",
            "Bold words",
            "then regular words",
            "first bullet",
            "second bullet"
        ).forEach { expected ->
            assertTrue("missing " + expected + " in the recovered text", joined.contains(expected))
        }

        assertTrue(
            "the heading has to come before the list, which the byte scan could not promise",
            joined.indexOf("Chapter one") < joined.indexOf("first bullet")
        )
        assertTrue(joined.indexOf("Bold words") < joined.indexOf("first bullet"))
    }

    @Test
    fun `a field shows its result and not its instruction`() {
        val (_, lines) = paragraphs(fixture("real-word97.doc"))
        val joined = lines.joinToString(" ")
        // Word stores a hyperlink as a field: instruction, separator, result. The page shows the
        // result, so that is all a reader should get.
        assertTrue("the sentence should read as the page says: " + joined,
            joined.contains("mentions https://example.com/spec the specification"))
        assertFalse("the field code leaked into the text: " + joined, joined.contains("HYPERLINK"))
    }

    @Test
    fun `table cells become their own lines and the footnote stays a footnote`() {
        val (_, lines) = paragraphs(fixture("real-word97.doc"))
        val joined = lines.joinToString("\n")
        listOf("Header A", "Header B", "value one", "value two").forEach { cell ->
            assertTrue("missing table cell " + cell, joined.contains(cell))
        }
        // Headers, footers and footnotes are not read from a binary .doc, and the disclosure says
        // so - the text must not appear here and be claimed as recovered.
        assertFalse(joined.contains("A footnote written by Word itself"))
    }

    @Test
    fun `no Word structure mark survives into the text a reader sees`() {
        val (_, lines) = paragraphs(fixture("real-word97.doc"))
        assertTrue("some paragraphs were recovered", lines.isNotEmpty())
        lines.forEach { line ->
            line.forEach { ch ->
                val code = ch.code
                val allowed = code == 0x09 || code == 0x0A || code >= 0x20
                assertTrue("control character $code in the recovered text", allowed)
            }
        }
    }

    @Test
    fun `a container that is not a Word document is still refused rather than emptied`() {
        // 0xA5EC is the FIB signature; without it the piece table cannot be located, and the old
        // scrape is the honest last resort rather than a silently blank document.
        val notWord = ByteArray(4096) { (it % 251).toByte() }
        val failure = runCatching { runBlocking { DocxParser().parse(ByteArrayInputStream(notWord)) } }
            .exceptionOrNull()
        assertTrue("expected a typed failure, got $failure", failure is com.abhisheksati.mydoc.domain.model.DocumentLoadException)
        assertEquals(
            "a container with no Word signature is refused, not shown as an empty document",
            LoadFailure.UNSUPPORTED_FORMAT,
            (failure as com.abhisheksati.mydoc.domain.model.DocumentLoadException).failure
        )
    }
}
