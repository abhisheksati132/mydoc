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
}
