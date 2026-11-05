package com.abhisheksati.mydoc.rendering.engine.word

import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The Word engine used to report a list as bullets, drop hyperlink targets, and let a table inside a
 * cell erase the rows of the table around it. Each test here is a document that fails on the previous
 * parser: they describe the OOXML the reader must honour, not the shape of our model.
 *
 * Fixtures are written as real packages - `[Content_Types].xml` aside, the same parts Word ships:
 * `word/document.xml`, `word/_rels/document.xml.rels`, `word/numbering.xml`, `word/styles.xml`,
 * `word/footnotes.xml` and `word/endnotes.xml`.
 */
class DocxSemanticsTest {

    private val ns = "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
        "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""

    private fun docx(
        document: String,
        rels: String? = null,
        numbering: String? = null,
        styles: String? = null,
        footnotes: String? = null,
        endnotes: String? = null
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, body: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("word/document.xml", "<?xml version=\"1.0\"?><w:document $ns><w:body>$document</w:body></w:document>")
            rels?.let { put("word/_rels/document.xml.rels", it) }
            numbering?.let { put("word/numbering.xml", it) }
            styles?.let { put("word/styles.xml", it) }
            footnotes?.let { put("word/footnotes.xml", it) }
            endnotes?.let { put("word/endnotes.xml", it) }
        }
        return out.toByteArray()
    }

    private fun parse(bytes: ByteArray): List<DocxBlock> = runBlocking {
        DocxParser().parse(ByteArrayInputStream(bytes)).blocks
    }

    @Test
    fun aHyperlinkKeepsItsTargetFromTheRelationshipPart() {
        val blocks = parse(
            docx(
                document = "<w:p><w:r><w:t>see </w:t></w:r>" +
                    "<w:hyperlink r:id=\"rId2\"><w:r><w:t>the spec</w:t></w:r></w:hyperlink>" +
                    "<w:r><w:t> now</w:t></w:r></w:p>",
                rels = """<?xml version="1.0"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId2" Target="https://example.com/spec" TargetMode="External"/>
                    </Relationships>"""
            )
        )
        val runs = (blocks.single() as DocxBlock.Paragraph).runs
        assertEquals("see the spec now", runs.joinToString("") { it.text })
        assertEquals("the linked text must carry no target", null, runs.first { it.text == "see " }.linkTarget)
        assertEquals("https://example.com/spec", runs.first { it.text == "the spec" }.linkTarget)
    }

    @Test
    fun aNumberedListIsNumberedAndCountsUp() {
        val blocks = parse(
            docx(
                document = "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"7\"/></w:numPr></w:pPr>" +
                    "<w:r><w:t>first</w:t></w:r></w:p>" +
                    "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"7\"/></w:numPr></w:pPr>" +
                    "<w:r><w:t>second</w:t></w:r></w:p>",
                numbering = """<?xml version="1.0"?><w:numbering $ns>
                    <w:abstractNum w:abstractNumId="3">
                      <w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/>
                        <w:pPr><w:ind w:left="720"/></w:pPr></w:lvl>
                      <w:lvl w:ilvl="1"><w:start w:val="1"/><w:numFmt w:val="lowerLetter"/><w:lvlText w:val="%2)"/></w:lvl>
                    </w:abstractNum>
                    <w:num w:numId="7"><w:abstractNumId w:val="3"/></w:num>
                  </w:numbering>"""
            )
        )
        val items = blocks.filterIsInstance<DocxBlock.ListItem>()
        assertEquals(2, items.size)
        assertEquals(listOf("1.", "2."), items.map { it.marker })
        assertTrue("a decimal numbering definition is ordered, not a bullet", items.all { it.ordered })
    }

    @Test
    fun aDeeperListLevelRestartsAfterTheParentAdvances() {
        fun item(level: Int, text: String) =
            "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"$level\"/><w:numId w:val=\"7\"/></w:numPr></w:pPr>" +
                "<w:r><w:t>$text</w:t></w:r></w:p>"
        val blocks = parse(
            docx(
                document = item(0, "a") + item(1, "a1") + item(1, "a2") + item(0, "b") + item(1, "b1"),
                numbering = """<?xml version="1.0"?><w:numbering $ns>
                    <w:abstractNum w:abstractNumId="3">
                      <w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/></w:lvl>
                      <w:lvl w:ilvl="1"><w:start w:val="1"/><w:numFmt w:val="lowerLetter"/><w:lvlText w:val="%2)"/></w:lvl>
                    </w:abstractNum>
                    <w:num w:numId="7"><w:abstractNumId w:val="3"/></w:num>
                  </w:numbering>"""
            )
        )
        val items = blocks.filterIsInstance<DocxBlock.ListItem>()
        assertEquals(listOf("1.", "a)", "b)", "2.", "a)"), items.map { it.marker })
    }

    @Test
    fun aTableInsideACellKeepsTheTableAroundIt() {
        val outer = "<w:tbl><w:tr><w:tc>" +
            "<w:p><w:r><w:t>outer cell</w:t></w:r></w:p>" +
            "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>inner A</w:t></w:r></w:p></w:tc>" +
            "<w:tc><w:p><w:r><w:t>inner B</w:t></w:r></w:p></w:tc></w:tr></w:tbl>" +
            "</w:tc><w:tc><w:p><w:r><w:t>right cell</w:t></w:r></w:p></w:tc></w:tr></w:tbl>"
        val blocks = parse(docx(document = outer))
        assertEquals(1, blocks.filterIsInstance<DocxBlock.Table>().size)
        val table = blocks.filterIsInstance<DocxBlock.Table>().single()
        assertEquals(
            "the inner table used to clear the outer table's rows, so half the document vanished",
            listOf("outer cell", "right cell"),
            table.rows.single().map { it.text.lines().first() }
        )
        val nested = (table.rows.single().first().blocks).filterIsInstance<DocxBlock.Table>().single()
        assertEquals(listOf("inner A", "inner B"), nested.rows.single().map { it.text })
    }

    @Test
    fun aRunThatSwitchesBoldOffStaysRegular() {
        val blocks = parse(
            docx(
                document = "<w:p><w:r><w:rPr><w:b/><w:bCs/></w:rPr><w:t>loud</w:t></w:r>" +
                    "<w:r><w:rPr><w:b w:val=\"0\"/></w:rPr><w:t>quiet</w:t></w:r></w:p>"
            )
        )
        val runs = (blocks.single() as DocxBlock.Paragraph).runs
        assertTrue("loud", runs.first { it.text == "loud" }.isBold)
        assertFalse(
            "w:val=\"0\" turns the property off; the old reader read any <w:b> as on",
            runs.first { it.text == "quiet" }.isBold
        )
    }

    @Test
    fun aHeadingIsFoundThroughItsStyleChainNotItsEnglishId() {
        // A German template gives the paragraph a style id that is not "Heading1"; the outline level
        // in styles.xml is what actually says it is a heading.
        val blocks = parse(
            docx(
                document = "<w:p><w:pPr><w:pStyle w:val=\"berschrift1\"/></w:pPr><w:r><w:t>Zwischenuberschrift</w:t></w:r></w:p>",
                styles = """<?xml version="1.0"?><w:styles $ns>
                    <w:style w:type="paragraph" w:styleId="berschrift1">
                      <w:name w:val="heading 1"/><w:basedOn w:val="Standard"/><w:uiPriority w:val="9"/>
                      <w:pPr><w:outlineLvl w:val="0"/></w:pPr>
                    </w:style>
                  </w:styles>"""
            )
        )
        val heading = blocks.single() as DocxBlock.Heading
        assertEquals(1, heading.level)
        assertEquals("Zwischenuberschrift", heading.text)
    }

    @Test
    fun anOutlineLevelOnTheParagraphItselfMakesAHeading() {
        val blocks = parse(
            docx(document = "<w:p><w:pPr><w:outlineLvl w:val=\"2\"/></w:pPr><w:r><w:t>third level</w:t></w:r></w:p>")
        )
        assertEquals(3, (blocks.single() as DocxBlock.Heading).level)
    }

    @Test
    fun aSymbolFontBulletLevelRendersABulletNotAPrivateUseGlyph() {
        // Taken from a real Word document, where every bullet level says
        // `<w:numFmt w:val="bullet"/><w:lvlText w:val="&#xF0B7;"/>` and the font is Symbol. The
        // character is a font instruction: drawn with the document's own typeface it is an empty box.
        val symbolBullet = Char(0xF0B7).toString() // Symbol font's bullet: a glyph, not a character
        val blocks = parse(
            docx(
                document = (0 until 2).joinToString("") {
                    "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"1\"/></w:numPr></w:pPr>" +
                        "<w:r><w:t>point $it</w:t></w:r></w:p>"
                },
                numbering = """<?xml version="1.0"?><w:numbering $ns>
                    <w:abstractNum w:abstractNumId="0">
                      <w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="bullet"/>
                        <w:lvlText w:val="$symbolBullet"/><w:rPr><w:rFonts w:ascii="Symbol"/></w:rPr></w:lvl>
                    </w:abstractNum>
                    <w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num>
                  </w:numbering>"""
            )
        )
        val items = blocks.filterIsInstance<DocxBlock.ListItem>()
        assertEquals(2, items.size)
        assertTrue(
            "a marker the reader cannot draw is worse than the generic bullet it replaces",
            items.all { it.marker == "•" }
        )
    }

    @Test
    fun anAuthorChosenBulletGlyphIsNotReplaced() {
        // Only the undrawable private-use glyphs fall back. A list whose bullet really is a dash, a
        // square or an "o" is the author's typography and has to stay on the page.
        val blocks = parse(
            docx(
                document = "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"1\"/></w:numPr></w:pPr>" +
                    "<w:r><w:t>item</w:t></w:r></w:p>",
                numbering = """<?xml version="1.0"?><w:numbering $ns>
                    <w:abstractNum w:abstractNumId="0">
                      <w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="bullet"/>
                        <w:lvlText w:val="-"/></w:lvl>
                    </w:abstractNum>
                    <w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num>
                  </w:numbering>"""
            )
        )
        assertEquals("-", blocks.filterIsInstance<DocxBlock.ListItem>().single().marker)
    }

    @Test
    fun anEmptyParagraphDoesNotBecomeADocumentOfNothing() {
        val thrown = runCatching { parse(docx(document = "<w:p><w:r><w:t></w:t></w:r></w:p>")) }.exceptionOrNull()
        assertTrue("expected a typed load failure, got $thrown", thrown is DocumentLoadException)
        assertEquals(LoadFailure.EMPTY_DOCUMENT, (thrown as DocumentLoadException).failure)
    }

    @Test
    fun aFootnoteIsReadFromItsOwnPartAndNumberedByItsCitation() {
        val blocks = parse(
            docx(
                document = "<w:p><w:r><w:t>Claim</w:t></w:r><w:r><w:footnoteReference w:id=\"3\"/></w:r>" +
                    "<w:r><w:t> follows</w:t></w:r></w:p>",
                footnotes = """<?xml version="1.0"?><w:footnotes $ns>
                    <w:footnote w:type="separator" w:id="-1"><w:p><w:r><w:t>-</w:t></w:r></w:p></w:footnote>
                    <w:footnote w:type="continuationSeparator" w:id="0"><w:p><w:r><w:t>|</w:t></w:r></w:p></w:footnote>
                    <w:footnote w:id="3"><w:p><w:r><w:footnoteRef/><w:t xml:space="preserve">Source: </w:t></w:r>
                      <w:r><w:t>the annex</w:t></w:r></w:p></w:footnote>
                  </w:footnotes>"""
            )
        )
        val body = blocks.filterNot { it is DocxBlock.ListItem }
        assertEquals("the reference number belongs where the reader saw the superscript",
            "Claim[1] follows", (body.single() as DocxBlock.Paragraph).fullText)
        val notes = blocks.filterIsInstance<DocxBlock.ListItem>()
        assertEquals("the separator notes are page furniture, not content", 1, notes.size)
        assertEquals("[1]", notes.single().marker)
        assertEquals("Source: the annex", notes.single().fullText)
    }

    @Test
    fun aNoteCitedTwiceKeepsOneNumberAndOneEntry() {
        val blocks = parse(
            docx(
                document = "<w:p><w:r><w:t>a</w:t></w:r><w:r><w:footnoteReference w:id=\"7\"/></w:r>" +
                    "<w:r><w:t>b</w:t></w:r><w:r><w:footnoteReference w:id=\"9\"/></w:r>" +
                    "<w:r><w:t>c</w:t></w:r><w:r><w:footnoteReference w:id=\"7\"/></w:r></w:p>",
                footnotes = """<?xml version="1.0"?><w:footnotes $ns>
                    <w:footnote w:id="7"><w:p><w:r><w:t>seven</w:t></w:r></w:p></w:footnote>
                    <w:footnote w:id="9"><w:p><w:r><w:t>nine</w:t></w:r></w:p></w:footnote>
                  </w:footnotes>"""
            )
        )
        val paragraph = blocks.first() as DocxBlock.Paragraph
        assertEquals("the OOXML id is not the number the reader is shown", "a[1]b[2]c[1]", paragraph.fullText)
        val notes = blocks.filterIsInstance<DocxBlock.ListItem>()
        assertEquals(listOf("[1]", "[2]"), notes.map { it.marker })
        assertEquals(listOf("seven", "nine"), notes.map { it.fullText })
    }

    @Test
    fun anEndnoteFollowsTheFootnotesAtTheEndOfTheDocument() {
        val blocks = parse(
            docx(
                document = "<w:p><w:r><w:t>Body</w:t></w:r><w:r><w:footnoteReference w:id=\"1\"/></w:r>" +
                    "<w:r><w:footnoteReference w:id=\"2\"/></w:r></w:p>" +
                    "<w:p><w:r><w:t>More</w:t></w:r><w:r><w:endnoteReference w:id=\"5\"/></w:r></w:p>",
                footnotes = """<?xml version="1.0"?><w:footnotes $ns>
                    <w:footnote w:id="1"><w:p><w:r><w:t>first note</w:t></w:r></w:p></w:footnote>
                  </w:footnotes>""",
                endnotes = """<?xml version="1.0"?><w:endnotes $ns>
                    <w:endnote w:id="5"><w:p><w:r><w:t>an end note</w:t></w:r></w:p></w:endnote>
                  </w:endnotes>"""
            )
        )
        val notes = blocks.filterIsInstance<DocxBlock.ListItem>()
        assertEquals(listOf("[1]", "[1]"), notes.map { it.marker })
        assertEquals(listOf("first note", "an end note"), notes.map { it.fullText })
        assertEquals(
            "notes are collected after every paragraph of the body",
            2, blocks.indexOfFirst { it === notes.first() }
        )
    }
}
