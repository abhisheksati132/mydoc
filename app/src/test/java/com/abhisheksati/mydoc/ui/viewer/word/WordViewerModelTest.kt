package com.abhisheksati.mydoc.ui.viewer.word

import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxDocument
import com.abhisheksati.mydoc.rendering.engine.word.DocxRun
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * N-24 was "a damaged DOCX causes an endless loading spinner": the typed-failure branch set the
 * message and left `isLoading` true, so the reader stared at a spinner over an empty page and the
 * problem screen was never reached. These tests hold the loader to its contract — some terminal
 * outcome, always, with the parser's own classification preserved.
 */
class WordViewerModelTest {

    private fun stream() = ByteArrayInputStream("bytes".toByteArray())

    private fun doc(vararg blocks: DocxBlock) = DocxDocument(blocks.toList())

    private fun paragraph(text: String) = DocxBlock.Paragraph(listOf(DocxRun(text)))

    @Test
    fun aSuccessfulLoadEndsWithADocument() = runBlocking {
        val outcome = WordDocumentLoader.load({ stream() }) { doc(paragraph("hello")) }
        assertTrue(outcome is WordLoadOutcome.Loaded)
        assertEquals("hello", (outcome as WordLoadOutcome.Loaded).document.blocks.single().let { WordViewerModel.plainText(it) })
    }

    @Test
    fun everyFailureIsClassifiedAndNoneOfThemLeavesTheLoaderWithoutAnOutcome() = runBlocking {
        val cases = listOf(
            DocumentLoadException(LoadFailure.CORRUPT_DOCUMENT, "not a zip archive") to LoadFailure.CORRUPT_DOCUMENT,
            DocumentLoadException(LoadFailure.PASSWORD_PROTECTED, "encrypted") to LoadFailure.PASSWORD_PROTECTED,
            DocumentLoadException(LoadFailure.EXCEEDS_LIMITS, "too large") to LoadFailure.EXCEEDS_LIMITS,
            DocumentLoadException(LoadFailure.FEATURE_UNAVAILABLE, "charts") to LoadFailure.FEATURE_UNAVAILABLE,
            DocumentLoadException(LoadFailure.UNSUPPORTED_FORMAT, "odt") to LoadFailure.UNSUPPORTED_FORMAT
        )
        cases.forEach { (error, expected) ->
            val outcome = WordDocumentLoader.load({ stream() }) { throw error }
            assertEquals(
                "typed failure ${error::class.simpleName} / ${error.message}",
                WordLoadOutcome.Failed(expected, error),
                outcome
            )
        }
    }

    @Test
    fun anUnreadableFileIsAFailureAndNotAnEmptyDocument() = runBlocking {
        val outcome = WordDocumentLoader.load({ stream() }) { throw java.io.IOException("stream closed") }
        assertEquals(LoadFailure.CORRUPT_DOCUMENT, (outcome as WordLoadOutcome.Failed).failure)
    }

    @Test
    fun aPermissionDenialKeepsItsOwnClassification() = runBlocking {
        val outcome = WordDocumentLoader.load({ stream() }) {
            throw SecurityException("Permission Denial: opening that URI")
        }
        assertEquals(LoadFailure.PERMISSION_EXPIRED, (outcome as WordLoadOutcome.Failed).failure)
    }

    @Test
    fun aMissingStreamFailsRatherThanLoadingNothingForever() = runBlocking {
        val outcome = WordDocumentLoader.load({ null }) { doc() }
        assertEquals(LoadFailure.CORRUPT_DOCUMENT, (outcome as WordLoadOutcome.Failed).failure)
        assertEquals(WordDocumentLoader.COULD_NOT_OPEN, outcome.error?.message)
    }

    @Test
    fun cancellationIsNotReportedAsAFailedDocument() {
        // The reader left; there is nothing to show an error for, and swallowing the cancellation
        // would let the abandoned load overwrite the state of the document they opened next.
        try {
            runBlocking {
                WordDocumentLoader.load({ stream() }) { throw CancellationException("cancelled") }
            }
            fail("CancellationException was swallowed into a failure outcome")
        } catch (expected: CancellationException) {
            // as intended
        }
    }

    @Test
    fun retryingAfterAFailureCanSucceed() = runBlocking {
        var attempts = 0
        val loader: () -> InputStream = {
            attempts += 1
            if (attempts == 1) throw java.io.IOException("first read failed") else stream()
        }
        val first = WordDocumentLoader.load(loader) { doc(paragraph("x")) }
        assertEquals(LoadFailure.CORRUPT_DOCUMENT, (first as WordLoadOutcome.Failed).failure)
        assertTrue(WordDocumentLoader.load(loader) { doc(paragraph("x")) } is WordLoadOutcome.Loaded)
        assertEquals(2, attempts)
    }

    @Test
    fun theStreamIsClosedOnEveryPath() = runBlocking {
        var closed = false
        val tracked = object : InputStream() {
            override fun read() = -1
            override fun close() { closed = true }
        }
        WordDocumentLoader.load({ tracked }) { throw DocumentLoadException(LoadFailure.CORRUPT_DOCUMENT, "bad") }
        assertTrue("the parser threw and the stream stayed open", closed)
    }

    @Test
    fun searchCountsOccurrencesRatherThanBlocks() {
        val blocks = listOf(
            paragraph("alpha beta"),
            paragraph("beta gamma beta"),
            DocxBlock.Bullet(listOf(DocxRun("delta"), DocxRun(" beta"))),
            DocxBlock.Table(listOf(listOf("x"), listOf("beta here")))
        )
        val matches = WordViewerModel.matches(blocks, "beta")
        assertEquals(5, matches.size)
        assertEquals(listOf(0, 1, 1, 2, 3), matches.map { it.blockIndex })
        assertEquals(6, matches.first().start)
        assertEquals(10, matches.first().end)
    }

    @Test
    fun searchIsCaseInsensitiveAndHandlesNoMatches() {
        val blocks = listOf(paragraph("Report 2026"))
        assertEquals(1, WordViewerModel.matches(blocks, "REPORT").size)
        assertEquals(emptyList<WordViewerModel.Match>(), WordViewerModel.matches(blocks, "nothing"))
        assertEquals(emptyList<WordViewerModel.Match>(), WordViewerModel.matches(blocks, "  "))
    }

    @Test
    fun aBlankQueryStopsMatchingBeforeTheWholeDocumentMatches() {
        val blocks = listOf(paragraph("anything at all"))
        assertTrue(WordViewerModel.matches(blocks, "").isEmpty())
    }

    @Test
    fun rangesAndOffsetsPointAtTheSameTextTheReaderSees() {
        val runs = listOf(DocxRun("Never "), DocxRun("mind"), DocxRun(" the gaps"))
        val blocks = listOf(DocxBlock.Paragraph(runs))
        val matches = WordViewerModel.matches(blocks, "mind the")
        assertEquals(1, matches.size)
        val ranges = WordViewerModel.rangesIn(blocks, 0, matches)
        val offsets = WordViewerModel.runOffsets(runs)
        assertEquals(listOf(0 until 6, 6 until 10, 10 until 19), offsets)
        assertTrue("the second run holds the match start", ranges.single().first in offsets[1])
        assertTrue("the third run holds the match end", ranges.single().last in offsets[2])
        assertTrue("the first run is untouched", offsets[0].last < ranges.single().first)
    }

    @Test
    fun aLongDocumentKeepsMatchOrderAcrossBlocks() {
        val blocks = (0 until 500).map { paragraph("line $it needs finding") }
        val matches = WordViewerModel.matches(blocks, "finding")
        assertEquals(500, matches.size)
        assertEquals((0 until 500).toList(), matches.map { it.blockIndex })
    }
}
