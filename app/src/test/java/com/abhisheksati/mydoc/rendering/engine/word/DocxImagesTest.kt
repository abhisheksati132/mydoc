package com.abhisheksati.mydoc.rendering.engine.word

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxImagesTest {
    private fun document(body: String, target: String = "media/picture.png", external: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun part(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry()
            }
            part("word/document.xml", """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><w:body>$body</w:body></w:document>""")
            part("word/_rels/document.xml.rels", """<Relationships><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="$target" ${if (external) "TargetMode=\"External\"" else ""}/></Relationships>""")
            part("word/media/picture.png", "test-image-bytes")
        }
        return out.toByteArray()
    }
    private val picture = """<w:r><w:drawing><wp:inline><wp:extent cx="914400" cy="457200"/><wp:docPr id="1" name="Picture" descr="A landscape"/><a:graphic><a:blip r:embed="rId1"/></a:graphic></wp:inline></w:drawing></w:r>"""

    @Test fun imageOnlyParagraphSurvives() = runTest {
        val blocks = DocxParser().parse(document("<w:p>$picture</w:p>").inputStream()).blocks
        assertEquals(1, blocks.size)
        assertEquals("Image", blocks.single().javaClass.simpleName)
    }
    @Test fun textBeforeAndAfterPictureKeepsOrder() = runTest {
        val blocks = DocxParser().parse(document("<w:p><w:r><w:t>Before</w:t></w:r>$picture<w:r><w:t>After</w:t></w:r></w:p>").inputStream()).blocks
        assertEquals(listOf("Paragraph", "Image", "Paragraph"), blocks.map { it.javaClass.simpleName })
        assertEquals("Before", DocxBlock.plainTextOf(blocks.first()))
        assertEquals("After", DocxBlock.plainTextOf(blocks.last()))
    }

    @Test fun pictureRelationshipDimensionsAndDescriptionAreRead() = runTest {
        val image = DocxParser().parse(document("<w:p>$picture</w:p>").inputStream()).blocks.single() as DocxBlock.Image
        assertArrayEquals("test-image-bytes".toByteArray(), image.bytes)
        assertEquals(72f, image.widthPoints, 0.01f)
        assertEquals(36f, image.heightPoints, 0.01f)
        assertEquals("A landscape", image.description)
    }
    @Test fun externalImageIsNotFetchedAndRetainsPlaceholder() = runTest {
        val image = DocxParser().parse(document("<w:p>$picture</w:p>", "https://example.com/picture.png", true).inputStream()).blocks.single() as DocxBlock.Image
        assertNull(image.bytes)
    }
    @Test fun absolutePackagePathResolves() = runTest {
        val image = DocxParser().parse(document("<w:p>$picture</w:p>", "/word/media/picture.png").inputStream()).blocks.single() as DocxBlock.Image
        assertNotNull(image.bytes)
    }
    @Test fun escapingPackageRootDoesNotResolve() = runTest {
        val image = DocxParser().parse(document("<w:p>$picture</w:p>", "../../word/media/picture.png").inputStream()).blocks.single() as DocxBlock.Image
        assertNull(image.bytes)
    }
    @Test fun imageOnlyTableCellSurvives() = runTest {
        val table = DocxParser().parse(document("<w:tbl><w:tr><w:tc><w:p>$picture</w:p></w:tc></w:tr></w:tbl>").inputStream()).blocks.single() as DocxBlock.Table
        assertTrue(table.rows.single().single().blocks.single() is DocxBlock.Image)
    }

    @Test fun pictureDoesNotTurnOneListItemIntoTwo() = runTest {
        val body = "<w:p><w:pPr><w:numPr><w:numId w:val=\"1\"/></w:numPr></w:pPr><w:r><w:t>Before</w:t></w:r>$picture<w:r><w:t>After</w:t></w:r></w:p>"
        val blocks = DocxParser().parse(document(body).inputStream()).blocks
        assertEquals(1, blocks.filterIsInstance<DocxBlock.ListItem>().size)
        assertEquals("After", (blocks.last() as DocxBlock.Paragraph).fullText)
    }
}
