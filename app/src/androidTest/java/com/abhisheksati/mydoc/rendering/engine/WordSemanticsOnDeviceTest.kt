package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.rendering.engine.word.DocxConverter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The Word reader is only half of the pipeline: the blocks it produces are laid out by
 * [com.abhisheksati.mydoc.rendering.engine.word.DocxPdfRenderer] onto pages we paginate ourselves.
 * These tests convert a real package on device and read the text layer of the PDF back out, so what
 * is checked is what a reader sees - not the model the unit tests already cover.
 */
@RunWith(AndroidJUnit4::class)
class WordSemanticsOnDeviceTest {

    private lateinit var context: Context
    private lateinit var dir: File

    private val w = "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
    private val r = "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "word-semantics").apply {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }
    }

    @Test
    fun thePartsAroundDocumentXmlReachTheConvertedPage() = runBlocking {
        val docx = File(dir, "semantics.docx")
        docx.writeBytes(
            packageOf(
                body = "<w:p><w:r><w:t>Cites a footnote</w:t></w:r>" +
                    "<w:r><w:footnoteReference w:id=\"1\"/></w:r>" +
                    "<w:r><w:t xml:space=\"preserve\"> and ends</w:t></w:r>" +
                    "<w:r><w:t>.</w:t></w:r></w:p>" +
                    "<w:p><w:r><w:t>See </w:t></w:r>" +
                    "<w:hyperlink r:id=\"rId9\"><w:r><w:t>the repository</w:t></w:r></w:hyperlink>" +
                    "</w:p>" +
                    "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"5\"/></w:numPr></w:pPr>" +
                    "<w:r><w:t>numbered</w:t></w:r></w:p>" +
                    "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"1\"/><w:numId w:val=\"5\"/></w:numPr></w:pPr>" +
                    "<w:r><w:t>lettered</w:t></w:r></w:p>" +
                    "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"6\"/></w:numPr></w:pPr>" +
                    "<w:r><w:t>bulleted</w:t></w:r></w:p>" +
                    "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>outer</w:t></w:r></w:p>" +
                    "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>inner</w:t></w:r></w:p></w:tc></w:tr></w:tbl>" +
                    "</w:tc><w:tc><w:p><w:r><w:t>right</w:t></w:r></w:p></w:tc></w:tr></w:tbl>",
                rels = "<Relationship Id=\"rId9\" " +
                    "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\" " +
                    "Target=\"https://example.com/spec\" TargetMode=\"External\"/>",
                numbering = "<w:abstractNum w:abstractNumId=\"1\">" +
                    "<w:lvl w:ilvl=\"0\"><w:start w:val=\"1\"/><w:numFmt w:val=\"decimal\"/>" +
                    "<w:lvlText w:val=\"%1.\"/></w:lvl>" +
                    "<w:lvl w:ilvl=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"lowerLetter\"/>" +
                    "<w:lvlText w:val=\"%2)\"/></w:lvl></w:abstractNum>" +
                    "<w:abstractNum w:abstractNumId=\"2\">" +
                    "<w:lvl w:ilvl=\"0\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/>" +
                    "<w:lvlText w:val=\"" + Char(0xF0B7) + "\"/></w:lvl></w:abstractNum>" +
                    "<w:num w:numId=\"5\"><w:abstractNumId w:val=\"1\"/></w:num>" +
                    "<w:num w:numId=\"6\"><w:abstractNumId w:val=\"2\"/></w:num>",
                footnotes = "<w:footnote w:type=\"separator\" w:id=\"-1\">" +
                    "<w:p><w:r><w:t>-</w:t></w:r></w:p></w:footnote>" +
                    "<w:footnote w:id=\"1\"><w:p><w:r><w:t>the note text</w:t></w:r></w:p></w:footnote>"
            )
        )

        val converted = DocxConverter.convertDocxToPdf(context, Uri.fromFile(docx), docx.name)
        val pdf = (converted as OfficeConversion.Success).pdf
        val renderer = MuPdfRenderer(filePath = pdf.absolutePath)
        val extracted = try { renderer.getDocumentText() } finally { renderer.close() }
        val text = extracted.orEmpty()
        val flat = text.replace('\n', ' ').replace(Regex("\\s+"), " ")

        assertTrue("footnote text never reached the page: $flat", flat.contains("the note text"))
        assertTrue("decimal numbering lost: $flat", flat.contains("1. numbered"))
        assertTrue("lettered sub-level lost: $flat", flat.contains("a) lettered"))
        assertTrue("the Symbol-font bullet became a box: $flat", flat.contains("• bulleted"))
        assertTrue("the table around a nested table was destroyed: $flat",
            flat.contains("outer") && flat.contains("inner") && flat.contains("right"))
    }

    @Test
    fun runsThatMeetWithoutASpaceAreNotJoinedWithOne() = runBlocking {
        // Word splits a sentence into runs at every format change, and a split can land right before
        // punctuation. The wrapper works word by word, so it has to remember whether the document put
        // a space there or whether the runs simply touch.
        val docx = File(dir, "spacing.docx")
        docx.writeBytes(
            packageOf(
                body = "<w:p><w:r><w:t xml:space=\"preserve\">Read the </w:t></w:r>" +
                    "<w:r><w:rPr><w:b/><w:bCs/></w:rPr><w:t>manual</w:t></w:r>" +
                    "<w:r><w:rPr><w:b w:val=\"0\"/></w:rPr><w:t>,</w:t></w:r>" +
                    "<w:r><w:t xml:space=\"preserve\"> then the </w:t></w:r>" +
                    "<w:r><w:t>appendix</w:t></w:r>" +
                    "<w:r><w:t>.</w:t></w:r></w:p>"
            )
        )

        val converted = DocxConverter.convertDocxToPdf(context, Uri.fromFile(docx), docx.name)
        val pdf = (converted as OfficeConversion.Success).pdf
        val renderer = MuPdfRenderer(filePath = pdf.absolutePath)
        val extracted = try { renderer.getDocumentText() } finally { renderer.close() }
        val text = extracted.orEmpty()
        val flat = text.replace('\n', ' ').replace(Regex("\\s+"), " ")

        assertTrue(
            "the page does not say what the document said: $flat",
            flat.contains("Read the manual, then the appendix.")
        )
        // The wrapper used to join every run with a space of its own, which put a gap in front of
        // punctuation the document had written flush against the word before it.
        assertFalse("a space was invented before the comma: $flat", flat.contains("manual ,"))
        assertFalse("a space was invented before the full stop: $flat", flat.contains("appendix ."))
    }

    private fun packageOf(
        body: String,
        rels: String? = null,
        numbering: String? = null,
        footnotes: String? = null
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put(
                "word/document.xml",
                "<?xml version=\"1.0\"?><w:document $w $r><w:body>$body</w:body></w:document>"
            )
            rels?.let {
                put(
                    "word/_rels/document.xml.rels",
                    "<?xml version=\"1.0\"?><Relationships " +
                        "xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">$it</Relationships>"
                )
            }
            numbering?.let { put("word/numbering.xml", "<?xml version=\"1.0\"?><w:numbering $w>$it</w:numbering>") }
            footnotes?.let { put("word/footnotes.xml", "<?xml version=\"1.0\"?><w:footnotes $w>$it</w:footnotes>") }
        }
        return out.toByteArray()
    }
}
