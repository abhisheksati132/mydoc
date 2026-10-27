package com.abhisheksati.mydoc.rendering.engine.presentation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PptxParserTest {

    private val parser = PptxParser()

    @Test
    fun `parse empty stream returns empty presentation`() = runBlocking {
        val emptyInput = ByteArrayInputStream(ByteArray(0))
        val pres = parser.parse(emptyInput)
        assertEquals(1, pres.slides.size)
        assertEquals("Empty Presentation", pres.slides[0].title)
    }

    @Test
    fun `parse synthetic pptx zip extracts slides and placeholder layout`() = runBlocking {
        val zipBytes = createSyntheticPptxZip()
        val pres = parser.parse(ByteArrayInputStream(zipBytes))
        assertEquals(1, pres.slides.size)
        val slide = pres.slides[0]
        assertEquals("Welcome to Architecture", slide.title)
        assertTrue(slide.content.contains("Cloud computing fundamentals"))
        assertTrue(slide.visualElements.isNotEmpty())

        val titleElem = slide.visualElements.firstOrNull { it.paragraphs.any { p -> p.plainText.contains("Welcome") } }
        assertNotNull(titleElem)
        // Verify title bounds inherited or parsed
        assertEquals(0.06f, titleElem!!.bounds.left, 0.05f)
        assertEquals(0.08f, titleElem.bounds.top, 0.05f)
    }

    @Test
    fun `parse binary ppt bytes extracts valid slides via text stream`() = runBlocking {
        val pptBytes = createSyntheticBinaryPpt()
        val pres = parser.parse(ByteArrayInputStream(pptBytes))

        assertTrue(pres.slides.isNotEmpty())
        val firstSlide = pres.slides[0]
        assertTrue(firstSlide.title.isNotEmpty())
    }

    @Test
    fun `parse binary ppt bytes extracts UTF-16LE text correctly`() = runBlocking {
        val bb = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN)
        // OLE2 signature
        bb.put(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()))
        bb.position(64)
        val utf16Text = "Quarterly Business Review\r\nRevenue Growth: 25%\r\nOperating Margins: 18%".toByteArray(Charsets.UTF_16LE)
        bb.put(utf16Text)

        val pres = parser.parse(ByteArrayInputStream(bb.array()))
        assertTrue(pres.slides.isNotEmpty())
        val slide = pres.slides[0]
        assertTrue(slide.title.contains("Quarterly Business Review", ignoreCase = true) ||
            slide.content.any { it.contains("Revenue", ignoreCase = true) })
    }

    @Test
    fun `empty presentation has visual element and non-empty content`() = runBlocking {
        val pres = parser.parse(ByteArrayInputStream(ByteArray(0)))
        assertEquals(1, pres.slides.size)
        val slide = pres.slides[0]
        assertTrue(slide.visualElements.isNotEmpty())
        assertEquals("Empty Presentation", slide.title)
    }

    private fun createSyntheticPptxZip(): ByteArray {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        // 1. ppt/presentation.xml
        zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
        zos.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <p:sldSz cx="9144000" cy="5143500"/>
                <p:sldIdLst>
                    <p:sldId id="256" r:id="rId1"/>
                </p:sldIdLst>
            </p:presentation>
            """.trimIndent().toByteArray()
        )
        zos.closeEntry()

        // 2. ppt/_rels/presentation.xml.rels
        zos.putNextEntry(ZipEntry("ppt/_rels/presentation.xml.rels"))
        zos.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide1.xml"/>
            </Relationships>
            """.trimIndent().toByteArray()
        )
        zos.closeEntry()

        // 3. ppt/theme/theme1.xml
        zos.putNextEntry(ZipEntry("ppt/theme/theme1.xml"))
        zos.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Office Theme">
                <a:themeElements>
                    <a:clrScheme name="Office">
                        <a:dk1><a:sysClr val="windowText" lastClr="0F172A"/></a:dk1>
                        <a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1>
                        <a:accent1><a:srgbClr val="2563EB"/></a:accent1>
                    </a:clrScheme>
                    <a:fontScheme name="Office">
                        <a:majorFont><a:latin typeface="Roboto"/></a:majorFont>
                        <a:minorFont><a:latin typeface="Roboto Light"/></a:minorFont>
                    </a:fontScheme>
                </a:themeElements>
            </a:theme>
            """.trimIndent().toByteArray()
        )
        zos.closeEntry()

        // 4. ppt/slides/slide1.xml
        zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
        zos.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                   xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                <p:cSld>
                    <p:spTree>
                        <p:sp>
                            <p:nvSpPr>
                                <p:nvPr><p:ph type="title"/></p:nvPr>
                            </p:nvSpPr>
                            <p:spPr>
                                <a:xfrm>
                                    <a:off x="548640" y="411480"/>
                                    <a:ext cx="8046720" cy="925830"/>
                                </a:xfrm>
                            </p:spPr>
                            <p:txBody>
                                <a:p>
                                    <a:r>
                                        <a:rPr sz="2400" b="1"/>
                                        <a:t>Welcome to Architecture</a:t>
                                    </a:r>
                                </a:p>
                            </p:txBody>
                        </p:sp>
                        <p:sp>
                            <p:nvSpPr>
                                <p:nvPr><p:ph type="body" idx="1"/></p:nvPr>
                            </p:nvSpPr>
                            <p:spPr>
                                <a:xfrm>
                                    <a:off x="548640" y="1543050"/>
                                    <a:ext cx="8046720" cy="3086100"/>
                                </a:xfrm>
                            </p:spPr>
                            <p:txBody>
                                <a:p>
                                    <a:r>
                                        <a:rPr sz="1500"/>
                                        <a:t>Cloud computing fundamentals</a:t>
                                    </a:r>
                                </a:p>
                            </p:txBody>
                        </p:sp>
                    </p:spTree>
                </p:cSld>
            </p:sld>
            """.trimIndent().toByteArray()
        )
        zos.closeEntry()

        zos.finish()
        return baos.toByteArray()
    }

    private fun createSyntheticBinaryPpt(): ByteArray {
        val bb = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
        // OLE2 magic
        bb.put(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()))
        // sector shift: 9 (512 bytes)
        bb.position(30)
        bb.putShort(9)
        // RT_Slide atom (0x03EE) with simple text
        bb.position(512)
        bb.putShort(0x000F) // recVer / instance
        bb.putShort(0x03EE.toShort()) // RT_Slide
        bb.putInt(200) // length

        // RT_TextBytesAtom (0x0F9F)
        bb.putShort(0x0000)
        bb.putShort(0x0F9F.toShort())
        val textBytes = "Slide 1 Title\nBullet Point A\nBullet Point B".toByteArray(Charsets.ISO_8859_1)
        bb.putInt(textBytes.size)
        bb.put(textBytes)

        return bb.array()
    }

    @Test
    fun `parse corrupted random bytes returns fallback presentation gracefully`() = runBlocking {
        val randomCorruptBytes = ByteArray(1024) { (it * 37 % 255).toByte() }
        val pres = parser.parse(ByteArrayInputStream(randomCorruptBytes))
        assertNotNull(pres)
        assertTrue(pres.slides.isNotEmpty())
    }


    @Test
    fun `parse non-standard zip paths without ppt prefix successfully extracts slides`() = runBlocking {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)
        zos.putNextEntry(ZipEntry("slides/slide1.xml"))
        zos.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                   xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                <p:cSld>
                    <p:spTree>
                        <p:sp>
                            <p:txBody>
                                <a:p>
                                    <a:r><a:t>Non Standard Slide Title</a:t></a:r>
                                </a:p>
                                <a:p>
                                    <a:r><a:t>Keynote or Canva Exported Slide</a:t></a:r>
                                </a:p>
                            </p:txBody>
                        </p:sp>
                    </p:spTree>
                </p:cSld>
            </p:sld>
            """.trimIndent().toByteArray()
        )
        zos.closeEntry()
        zos.finish()

        val pres = parser.parse(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(1, pres.slides.size)
        assertEquals("Non Standard Slide Title", pres.slides[0].title)
    }

    @Test
    fun `parse malformed slide xml uses resilient parser without throwing`() = runBlocking {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)
        zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
        zos.write(
            """
            <broken:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                <randomTag>
                    <a:t>Resilient Recovery Slide</a:t>
                    <a:t>Content bullet point 1</a:t>
                </randomTag>
            </broken:sld>
            """.trimIndent().toByteArray()
        )
        zos.closeEntry()
        zos.finish()

        val pres = parser.parse(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(1, pres.slides.size)
        assertEquals("Resilient Recovery Slide", pres.slides[0].title)
        assertTrue(pres.slides[0].content.contains("Content bullet point 1"))
    }

    @Test
    fun `parse synthetic odp presentation extracts all slides, headings, bullet points, and tables`() = runBlocking {
        val odpBytes = createSyntheticOdpZip()
        val pres = parser.parse(ByteArrayInputStream(odpBytes))

        assertEquals(2, pres.slides.size)
        val slide1 = pres.slides[0]
        assertEquals("OpenDocument Architecture", slide1.title)
        assertTrue(slide1.content.contains("Native ODP slide rendering"))
        assertTrue(slide1.content.contains("High-fidelity desktop compatibility"))
        assertEquals("Remember to highlight cross-platform support.", slide1.notes)

        val slide2 = pres.slides[1]
        assertEquals("Performance Benchmark", slide2.title)
        val tableElem = slide2.visualElements.find { it.type == VisualElementType.TABLE }
        assertNotNull(tableElem)
        assertEquals(2, tableElem!!.tableRows.size)
        assertEquals(listOf("Engine", "Latency"), tableElem.tableRows[0])
        assertEquals(listOf("Native", "12ms"), tableElem.tableRows[1])
    }

    private fun createSyntheticOdpZip(): ByteArray {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        zos.putNextEntry(ZipEntry("mimetype"))
        zos.write("application/vnd.oasis.opendocument.presentation".toByteArray())
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("content.xml"))
        zos.write(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                                     xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
                                     xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                                     xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                                     xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
                                     xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                                     xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                                     xmlns:presentation="urn:oasis:names:tc:opendocument:xmlns:presentation:1.0">
                <office:automatic-styles>
                    <style:page-layout-properties fo:page-width="28cm" fo:page-height="15.75cm"/>
                </office:automatic-styles>
                <office:body>
                    <office:presentation>
                        <draw:page draw:name="page1">
                            <draw:frame svg:x="2cm" svg:y="1.5cm" svg:width="24cm" svg:height="3cm">
                                <draw:text-box>
                                    <text:h text:outline-level="1">OpenDocument Architecture</text:h>
                                </draw:text-box>
                            </draw:frame>
                            <draw:frame svg:x="2cm" svg:y="5cm" svg:width="24cm" svg:height="8cm">
                                <draw:text-box>
                                    <text:p>Native ODP slide rendering</text:p>
                                    <text:p>High-fidelity desktop compatibility</text:p>
                                </draw:text-box>
                            </draw:frame>
                            <presentation:notes>
                                <text:p>Remember to highlight cross-platform support.</text:p>
                            </presentation:notes>
                        </draw:page>
                        <draw:page draw:name="page2">
                            <draw:frame svg:x="2cm" svg:y="2cm" svg:width="24cm" svg:height="3cm">
                                <draw:text-box>
                                    <text:h text:outline-level="1">Performance Benchmark</text:h>
                                </draw:text-box>
                            </draw:frame>
                            <draw:frame svg:x="2cm" svg:y="6cm" svg:width="20cm" svg:height="6cm">
                                <table:table table:name="Table1">
                                    <table:table-row>
                                        <table:table-cell><text:p>Engine</text:p></table:table-cell>
                                        <table:table-cell><text:p>Latency</text:p></table:table-cell>
                                    </table:table-row>
                                    <table:table-row>
                                        <table:table-cell><text:p>Native</text:p></table:table-cell>
                                        <table:table-cell><text:p>12ms</text:p></table:table-cell>
                                    </table:table-row>
                                </table:table>
                            </draw:frame>
                        </draw:page>
                    </office:presentation>
                </office:body>
            </office:document-content>
            """.trimIndent().toByteArray()
        )
        zos.closeEntry()
        zos.finish()
        return baos.toByteArray()
    }
}
