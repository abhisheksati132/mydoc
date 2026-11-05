package com.abhisheksati.mydoc.rendering.engine.word

import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import com.abhisheksati.mydoc.domain.model.LoadFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxParserTest {

    private val parser = DocxParser()

    @Test
    fun parse_emptyStream_reportsEmptyRatherThanProse() = runTest {
        // An empty file used to come back as a one-paragraph document whose text was the
        // sentence "Document is empty." — indistinguishable from real content.
        val failure = runCatching { parser.parse(ByteArrayInputStream(ByteArray(0))) }
            .exceptionOrNull() as? DocumentLoadException
        assertNotNull(failure)
        assertEquals(LoadFailure.EMPTY_DOCUMENT, failure?.failure)
    }

    @Test
    fun parse_rawAsciiText_extractsFallbackBlocks() = runTest {
        val sampleText = "Hello world!\nThis is a plain document fallback test.\nFinal line."
        val doc = parser.parse(ByteArrayInputStream(sampleText.toByteArray(Charsets.UTF_8)))
        assertTrue(doc.blocks.isNotEmpty())
        // Promoting the first short string to a heading invented structure the container never
        // promised; recovered text is now paragraphs plus an honest fallback flag.
        assertTrue(doc.blocks.none { it is DocxBlock.Heading })
        assertTrue(doc.usedTextFallback)
        val paragraphs = doc.blocks.filterIsInstance<DocxBlock.Paragraph>()
        assertTrue(paragraphs.any { it.fullText.contains("This is a plain document fallback test.") })
    }

    @Test
    fun parse_docxWithParagraphsAndFormatting_extractsBlocksAndRuns() = runTest {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:pPr>
                            <w:pStyle w:val="Heading1"/>
                        </w:pPr>
                        <w:r>
                            <w:t>Project Title</w:t>
                        </w:r>
                    </w:p>
                    <w:p>
                        <w:r>
                            <w:rPr>
                                <w:b/>
                            </w:rPr>
                            <w:t>Bold Text </w:t>
                        </w:r>
                        <w:r>
                            <w:rPr>
                                <w:i/>
                            </w:rPr>
                            <w:t>Italic Text </w:t>
                        </w:r>
                        <w:r>
                            <w:rPr>
                                <w:u w:val="single"/>
                            </w:rPr>
                            <w:t>Underlined Text</w:t>
                        </w:r>
                    </w:p>
                    <w:tbl>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>Cell 1</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Cell 2</w:t></w:r></w:p></w:tc>
                        </w:tr>
                    </w:tbl>
                </w:body>
            </w:document>
        """.trimIndent()

        val docxZip = createDocxZip(xmlContent)
        val doc = parser.parse(ByteArrayInputStream(docxZip))

        assertTrue(doc.blocks.isNotEmpty())

        // Check Heading
        val heading = doc.blocks.firstOrNull { it is DocxBlock.Heading } as? DocxBlock.Heading
        assertTrue(heading != null)
        assertEquals("Project Title", heading?.text)

        // Check Paragraph and formatted runs
        val paragraph = doc.blocks.firstOrNull { it is DocxBlock.Paragraph } as? DocxBlock.Paragraph
        assertTrue(paragraph != null)
        assertTrue(paragraph?.runs?.any { it.isBold && it.text.contains("Bold") } == true)
        assertTrue(paragraph?.runs?.any { it.isItalic && it.text.contains("Italic") } == true)
        assertTrue(paragraph?.runs?.any { it.isUnderline && it.text.contains("Underlined") } == true)

        // Check Table
        val table = doc.blocks.firstOrNull { it is DocxBlock.Table } as? DocxBlock.Table
        assertTrue(table != null)
        assertEquals(1, table?.rows?.size)
        assertEquals(listOf("Cell 1", "Cell 2"), table?.rows?.get(0)?.map { it.text })
    }

    @Test
    fun parse_odtWithHeadingsAndLists_extractsBlocks() = runTest {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0">
                <office:body>
                    <office:text>
                        <text:h text:outline-level="1">OpenDocument Heading</text:h>
                        <text:p>Standard paragraph in ODT format.</text:p>
                        <text:list>
                            <text:list-item>
                                <text:p>Bullet item 1</text:p>
                            </text:list-item>
                        </text:list>
                    </office:text>
                </office:body>
            </office:document-content>
        """.trimIndent()

        val odtZip = createOdtZip(xmlContent)
        val doc = parser.parse(ByteArrayInputStream(odtZip))

        assertTrue(doc.blocks.isNotEmpty())
        val heading = doc.blocks.firstOrNull { it is DocxBlock.Heading } as? DocxBlock.Heading
        assertTrue(heading != null)
        assertEquals("OpenDocument Heading", heading?.text)

        val bullet = doc.blocks.firstOrNull { it is DocxBlock.ListItem } as? DocxBlock.ListItem
        assertTrue(bullet != null)
        assertEquals("Bullet item 1", bullet?.runs?.firstOrNull()?.text)
    }

    private fun createDocxZip(documentXml: String): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(documentXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return bos.toByteArray()
    }

    private fun createOdtZip(contentXml: String): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            zos.putNextEntry(ZipEntry("content.xml"))
            zos.write(contentXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return bos.toByteArray()
    }
}
