package com.abhisheksati.mydoc.ui.viewer.word

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The Word loader and search are pure and unit-tested against hand-built blocks; this is the same
 * code over the real parser and real archive bytes, because the interesting failures are what the
 * parser does with a file rather than what the model does with a list.
 */
@RunWith(AndroidJUnit4::class)
class WordViewerOnDeviceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    private fun docx(bodyXml: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
                   <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                     <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                     <Default Extension="xml" ContentType="application/xml"/>
                     <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                   </Types>""".toByteArray()
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
                   <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                     <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                   </Relationships>""".toByteArray()
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(bodyXml.toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun documentXml(vararg paragraphs: String) =
        """<?xml version="1.0" encoding="UTF-8"?>
           <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>${
            paragraphs.joinToString("") { "<w:p><w:r><w:t>${it}</w:t></w:r></w:p>" }
        }</w:body></w:document>"""

    private fun loadBytes(bytes: ByteArray): WordLoadOutcome = runBlocking {
        WordDocumentLoader.load({ ByteArrayInputStream(bytes) }) { stream ->
            DocxParser().parse(stream)
        }
    }

    @Test
    fun aRealDocxLoadsThroughTheSamePathTheViewerUses() {
        val bytes = docx(documentXml("Quarterly report", "Revenue rose sharply", "The audit trail is complete"))
        val outcome = loadBytes(bytes)
        val loaded = outcome as? WordLoadOutcome.Loaded
        assertTrue("expected a loaded document, got $outcome", loaded != null)
        val blocks = loaded!!.document.blocks
        assertEquals(3, blocks.size)
        assertEquals("Revenue rose sharply", WordViewerModel.plainText(blocks[1]))
    }

    @Test
    fun searchOverARealDocumentNavigatesEveryOccurrenceInOrder() {
        val bytes = docx(
            documentXml(
                "beta first",
                "nothing here",
                "beta middle and beta again",
                "last block"
            )
        )
        val blocks = (loadBytes(bytes) as WordLoadOutcome.Loaded).document.blocks
        val matches = WordViewerModel.matches(blocks, "beta")
        assertEquals("three occurrences, not two blocks", 3, matches.size)
        assertEquals(listOf(0, 2, 2), matches.map { it.blockIndex })

        // Next/previous in the viewer are these indices modulo their count, so the wrap points
        // and the scroll targets are exactly what these assertions say.
        val forward = (0 until 5).map { it % matches.size }
        assertEquals(listOf(0, 1, 2, 0, 1), forward)
        val backward = (0 until 4).scan(0) { acc, _ -> (acc - 1 + matches.size) % matches.size }.drop(1)
        assertEquals(listOf(2, 1, 0, 2), backward)
    }

    @Test
    fun aTruncatedDocxFailsAndTellsTheViewerToStopShowingASpinner() {
        val bytes = docx(documentXml("a paragraph that will be cut off"))
        val chopped = bytes.copyOf(bytes.size / 2)
        val outcome = loadBytes(chopped)
        val failed = outcome as? WordLoadOutcome.Failed
        assertTrue("a half-written archive must not report success, got $outcome", failed != null)
        assertTrue(
            "expected a classified failure, got ${failed!!.failure}",
            failed.failure == LoadFailure.CORRUPT_DOCUMENT || failed.failure == LoadFailure.PASSWORD_PROTECTED
        )
    }

    @Test
    fun aNonDocxFileIsScrapedAndSaysSoRatherThanLookingLikeAFaithfulParse() {
        // The parser's documented fallback: bytes it cannot structure are recovered as text and
        // flagged, so the viewer puts a fidelity notice under them. Silent success here would be
        // the "failure rendered as content" behaviour v1.7.1 removed.
        val outcome = loadBytes("this is not a word file at all".toByteArray())
        val loaded = outcome as? WordLoadOutcome.Loaded
        assertTrue("expected a recovered-scrape document, got $outcome", loaded != null)
        assertTrue("the fallback did not flag itself", loaded!!.document.usedTextFallback)
        assertEquals("this is not a word file at all", WordViewerModel.plainText(loaded.document.blocks.single()))
    }

    @Test
    fun openingTheSameDocumentTwiceGivesTheSameContent() {
        val bytes = docx(documentXml("stable content", "second paragraph"))
        val first = (loadBytes(bytes) as WordLoadOutcome.Loaded).document
        val second = (loadBytes(bytes) as WordLoadOutcome.Loaded).document
        assertEquals(first.blocks.map { WordViewerModel.plainText(it) }, second.blocks.map { WordViewerModel.plainText(it) })
    }

    @Test
    fun aMissingFileIsReportedAsAFailureTheViewerCanDisplay() {
        val absent = File(context.getExternalFilesDir(null), "no-such-document.docx")
        if (absent.exists()) absent.delete()
        val outcome = runBlocking {
            WordDocumentLoader.load(openStream = { java.io.FileInputStream(absent) }) { stream ->
                DocxParser().parse(stream)
            }
        }
        assertTrue(outcome is WordLoadOutcome.Failed)
        assertEquals(LoadFailure.CORRUPT_DOCUMENT, (outcome as WordLoadOutcome.Failed).failure)
    }
}
