package com.abhisheksati.mydoc.rendering.engine.word

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxLayoutTest {
    private fun document(body: String, styles: String? = null): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun part(name: String, xml: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(xml.toByteArray()); zip.closeEntry()
            }
            part("word/document.xml", """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$body</w:body></w:document>""")
            if (styles != null) part("word/styles.xml", styles)
        }
        return out.toByteArray()
    }

    @Test fun explicitPageBreakPreservesTextOnBothSides() = runTest {
        val blocks = DocxParser().parse(document("""<w:p><w:r><w:t>Before</w:t><w:br w:type="page"/><w:t>After</w:t></w:r></w:p>""").inputStream()).blocks
        assertEquals(listOf("Paragraph", "PageBreak", "Paragraph"), blocks.map { it.javaClass.simpleName })
        assertEquals("Before", DocxBlock.plainTextOf(blocks.first()))
        assertEquals("After", DocxBlock.plainTextOf(blocks.last()))
    }

    @Test fun paragraphKeepsDirectAlignmentSpacingAndIndents() = runTest {
        val paragraph = DocxParser().parse(document("""<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:before="240" w:after="120"/><w:ind w:left="720" w:right="360"/></w:pPr><w:r><w:t>Centered</w:t></w:r></w:p>""").inputStream()).blocks.single() as DocxBlock.Paragraph
        assertEquals(DocxAlignment.CENTER, paragraph.layout.alignment)
        assertEquals(12f, paragraph.layout.before)
        assertEquals(6f, paragraph.layout.after)
        assertEquals(36f, paragraph.layout.left)
        assertEquals(18f, paragraph.layout.right)
    }

    @Test fun inheritedPropertiesCombineWithExplicitZeroAndFalseOverrides() = runTest {
        val styles = """<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:style w:styleId="Base"><w:pPr><w:jc w:val="center"/><w:spacing w:before="240" w:after="120"/><w:ind w:left="720"/><w:pageBreakBefore/></w:pPr></w:style><w:style w:styleId="Child"><w:basedOn w:val="Base"/><w:pPr><w:jc w:val="right"/></w:pPr></w:style></w:styles>"""
        val paragraph = DocxParser().parse(document("""<w:p><w:pPr><w:pStyle w:val="Child"/><w:spacing w:after="0"/><w:pageBreakBefore w:val="0"/></w:pPr><w:r><w:t>Override</w:t></w:r></w:p>""", styles).inputStream()).blocks.single() as DocxBlock.Paragraph
        assertEquals(DocxAlignment.RIGHT, paragraph.layout.alignment)
        assertEquals(12f, paragraph.layout.before)
        assertEquals(0f, paragraph.layout.after)
        assertEquals(36f, paragraph.layout.left)
        assertEquals(false, paragraph.layout.pageBreakBefore)
    }

    @Test fun headingsRetainParagraphProperties() = runTest {
        val heading = DocxParser().parse(document("""<w:p><w:pPr><w:pStyle w:val="Heading1"/><w:jc w:val="right"/><w:pageBreakBefore/></w:pPr><w:r><w:t>Heading</w:t></w:r></w:p>""").inputStream()).blocks.single() as DocxBlock.Heading
        assertEquals(DocxAlignment.RIGHT, heading.layout.alignment)
        assertEquals(true, heading.layout.pageBreakBefore)
    }

    @Test fun normalLineBreakDoesNotBecomePageBreak() = runTest {
        val paragraph = DocxParser().parse(document("""<w:p><w:r><w:t>First</w:t><w:br/><w:t>Second</w:t></w:r></w:p>""").inputStream()).blocks.single() as DocxBlock.Paragraph
        assertEquals("First\nSecond", paragraph.fullText)
    }

    @Test fun breakOnlyParagraphIsPreserved() = runTest {
        val blocks = DocxParser().parse(document("""<w:p><w:r><w:br w:type="page"/></w:r></w:p>""").inputStream()).blocks
        assertEquals(listOf(DocxBlock.PageBreak), blocks)
    }

    @Test fun listMarkerSurvivesPageBreakBeforeItsFirstText() = runTest {
        val blocks = DocxParser().parse(document("""<w:p><w:pPr><w:numPr><w:numId w:val="1"/></w:numPr></w:pPr><w:r><w:br w:type="page"/><w:t>List text</w:t></w:r></w:p>""").inputStream()).blocks
        assertEquals(DocxBlock.PageBreak, blocks.first())
        assertTrue(blocks.last() is DocxBlock.ListItem)
    }

    @Test fun emptyParagraphKeepsItsVerticalSpace() = runTest {
        val blocks = DocxParser().parse(document("""<w:p/><w:p><w:r><w:t>After blank line</w:t></w:r></w:p>""").inputStream()).blocks
        assertEquals(2, blocks.size)
        assertEquals("", (blocks.first() as DocxBlock.Paragraph).fullText)
    }

    @Test fun headingKeepsItsLevelAcrossPageBreakWithoutDuplicatingOutlineEntry() = runTest {
        val blocks = DocxParser().parse(document("""<w:p><w:pPr><w:pStyle w:val="Heading2"/></w:pPr><w:r><w:t>Before</w:t><w:br w:type="page"/><w:t>After</w:t></w:r></w:p>""").inputStream()).blocks
        val headings = blocks.filterIsInstance<DocxBlock.Heading>()
        assertEquals(2, headings.size)
        assertEquals(2, headings.last().level)
        assertFalse(headings.first().isContinuation)
        assertTrue(headings.last().isContinuation)
    }

    @Test fun cyclicStyleInheritanceTerminatesWithNearestProperties() = runTest {
        val styles = """<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:style w:styleId="A"><w:basedOn w:val="B"/><w:pPr><w:jc w:val="center"/></w:pPr></w:style><w:style w:styleId="B"><w:basedOn w:val="A"/><w:pPr><w:spacing w:before="200"/></w:pPr></w:style></w:styles>"""
        val paragraph = DocxParser().parse(document("""<w:p><w:pPr><w:pStyle w:val="A"/></w:pPr><w:r><w:t>Cycle</w:t></w:r></w:p>""", styles).inputStream()).blocks.single() as DocxBlock.Paragraph
        assertEquals(DocxAlignment.CENTER, paragraph.layout.alignment)
        assertEquals(10f, paragraph.layout.before)
    }
}
