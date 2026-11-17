package com.abhisheksati.mydoc.tools.engine

import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDMetadata
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

/**
 * stripMetadata has to remove authored metadata without touching accessibility data:
 * StructTreeRoot and MarkInfo are parsing/reading structure, not author identity. The
 * PDFBox pass also had to keep working when it ran on a document that carries an XMP
 * packet, which is the case that used to be untested.
 */
@RunWith(JUnit4::class)
class PdfMetadataStripPreservesTagsTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private fun taggedPdf(file: File) {
        PDDocument().use { doc ->
            doc.addPage(PDPage(PDRectangle.A4))
            val catalog = doc.documentCatalog
            catalog.cosObject.setItem(COSName.STRUCT_TREE_ROOT, COSDictionary())
            catalog.cosObject.setItem(COSName.MARK_INFO, COSDictionary().apply {
                setBoolean(COSName.getPDFName("Marked"), true)
            })
            val metadata = PDMetadata(doc)
            metadata.importXMPMetadata(XMP.toByteArray(Charsets.UTF_8))
            catalog.metadata = metadata
            doc.documentInformation = PDDocumentInformation().apply {
                author = "Ada Lovelace"
                title = "Draft notes"
                subject = "Subject"
                creator = "Creator app"
                producer = "Producer app"
                keywords = "secret"
            }
            doc.save(file)
        }
    }

    @Test
    fun `the fixture really carries authored metadata and tags`() {
        val input = File(tempFolder.root, "tagged.pdf").apply { parentFile.mkdirs() }.let {
            taggedPdf(it); it
        }
        PDDocument.load(input).use { doc ->
            val catalog = doc.documentCatalog
            assertNotNull(catalog.metadata)
            assertNotNull(catalog.cosObject.getDictionaryObject(COSName.STRUCT_TREE_ROOT))
            assertTrue(
                (catalog.cosObject.getDictionaryObject(COSName.MARK_INFO) as COSDictionary)
                    .getBoolean(COSName.getPDFName("Marked"), false)
            )
            assertEquals("Ada Lovelace", doc.documentInformation.author)
        }
    }

    @Test
    fun `authored metadata and xmp are cleared while tags survive`() {
        val input = File(tempFolder.root, "tagged.pdf").apply { parentFile.mkdirs() }.let {
            taggedPdf(it); it
        }
        val output = File(tempFolder.root, "stripped.pdf")

        stripAuthoredMetadata(input, output)

        PDDocument.load(output).use { doc ->
            assertEquals("the page itself must survive", 1, doc.numberOfPages)
            assertNull("XMP packet must be removed", doc.documentCatalog.metadata)
            val info = doc.documentInformation
            listOf(info.author, info.title, info.subject, info.creator, info.producer, info.keywords)
                .forEach { assertTrue("authored field must be cleared, was <$it>", it.isNullOrEmpty()) }

            val catalog = doc.documentCatalog.cosObject
            assertNotNull(
                "StructTreeRoot is reading structure and must survive",
                catalog.getDictionaryObject(COSName.STRUCT_TREE_ROOT)
            )
            assertTrue(
                "MarkInfo/Marked is the tagged flag and must survive",
                (catalog.getDictionaryObject(COSName.MARK_INFO) as COSDictionary)
                    .getBoolean(COSName.getPDFName("Marked"), false)
            )
        }
    }

    @Test
    fun `an encrypted document is refused before any output is written`() {
        val input = File(tempFolder.root, "encrypted.pdf").apply { parentFile.mkdirs() }
        PDDocument().use { doc ->
            doc.addPage(PDPage(PDRectangle.A4))
            doc.protect(
                StandardProtectionPolicy("owner-password", "user-password", AccessPermission()).apply {
                    encryptionKeyLength = 128
                }
            )
            doc.save(input)
        }
        val output = File(tempFolder.root, "stripped.pdf")

        val thrown = runCatching { stripAuthoredMetadata(input, output) }.exceptionOrNull()

        assertTrue("expected the guard to fire, got $thrown", thrown is IllegalArgumentException)
        assertFalse("no unencrypted copy may be left behind", output.exists())
    }

    private companion object {
        const val XMP = """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
<rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:creator>Ada Lovelace</dc:creator></rdf:Description>
</rdf:RDF></x:xmpmeta><?xpacket end="w"?>"""
    }
}
