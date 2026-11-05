package com.abhisheksati.mydoc.ui.viewer.text

import com.abhisheksati.mydoc.domain.model.LoadFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction

/**
 * N-27 was `readLines()` on a stream of any size: a log file was copied whole into the heap, then
 * copied again for read-aloud. The reader now has a stated budget and says when it stopped short.
 */
class TextViewerModelTest {

    private companion object {
        // Early enough that a fast machine cannot have finished the document, late enough that
        // the loader is definitely inside its read loop when the cancellation lands.
        const val CANCEL_AT_CHUNK = 3
    }

    private fun bytes(text: String) = ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))

    private fun load(text: String, budget: TextBudget = TextBudget.DEFAULT) = runBlocking {
        TextViewerModel.load({ bytes(text) }, budget) as TextLoadOutcome.Loaded
    }

    @Test
    fun theBudgetIsStatedNotDiscovered() {
        // The viewer's memory profile is a promise to the reader; changing these numbers changes
        // what a 20,000-line log looks like on a low-end phone, so they are pinned deliberately.
        assertEquals(8L * 1024 * 1024, TextBudget.DEFAULT.maxBytes)
        assertEquals(50_000, TextBudget.DEFAULT.maxLines)
        assertEquals(10_000, TextBudget.DEFAULT.maxLineChars)
        assertTrue(TextBudget.noteFor(TextBudget.DEFAULT).contains("8 MB"))
    }

    @Test
    fun aSmallFileArrivesWhole() {
        val loaded = load("first line\nsecond line\n")
        assertEquals(listOf("first line", "second line"), loaded.lines)
        assertFalse("a file inside the budget must not claim truncation", loaded.truncated)
    }

    @Test
    fun anEmptyFileIsNotEmptyLinesOfSomethingElse() {
        val loaded = load("")
        assertEquals(0, loaded.bytesRead)
        assertTrue(loaded.lines.isEmpty())
        assertFalse(loaded.truncated)
    }

    @Test
    fun aFileLargerThanTheLineBudgetIsStoppedAndSaidSo() {
        val text = (1..50).joinToString("\n") { "line $it" }
        val loaded = load(text, TextBudget(maxBytes = 100_000, maxLines = 10, maxLineChars = 200))
        assertEquals(10, loaded.lines.size)
        assertEquals((1..10).map { "line $it" }, loaded.lines)
        assertTrue("the reader was shown a prefix and not told", loaded.truncated)
    }

    @Test
    fun aFileLargerThanTheByteBudgetIsStoppedAndSaidSo() {
        val line = "x".repeat(100)
        val text = (1..100).joinToString("\n") { line }
        val loaded = load(text, TextBudget(maxBytes = 500, maxLines = 10_000, maxLineChars = 200))
        assertTrue(loaded.truncated)
        assertTrue("read past the byte budget: ${loaded.bytesRead}", loaded.bytesRead <= 500 + 8192)
        assertTrue(loaded.lines.size < 100)
    }

    @Test
    fun aSingleVeryLongLineIsCutRatherThanHeldWhole() {
        val text = "A" + "B".repeat(5_000) + "\nafter\n"
        val loaded = load(text, TextBudget(maxBytes = 1_000_000, maxLines = 100, maxLineChars = 50))
        assertEquals(2, loaded.lines.size)
        assertEquals(50 + TextViewerModel.CUT.length, loaded.lines.first().length)
        assertTrue(loaded.lines.first().endsWith(TextViewerModel.CUT))
        assertEquals("after", loaded.lines[1])
        assertTrue(loaded.truncated)
    }

    @Test
    fun multibyteContentSurvivesTheChunkedRead() {
        val loaded = load("héllo wörld\n日本語のテキスト\nemoji: 🙂\n")
        assertEquals(
            listOf("héllo wörld", "日本語のテキスト", "emoji: 🙂"),
            loaded.lines
        )
        assertTrue("bytes, not characters, were counted", loaded.bytesRead > 40)
    }

    @Test
    fun invalidEncodingIsDecodedLenientlyRatherThanFailingTheDocument() {
        val broken = byteArrayOf(0x61, 0x62, 0xFF.toByte(), 0xFE.toByte(), 0x0A, 0x63)
        val outcome = runBlocking {
            TextViewerModel.load({ ByteArrayInputStream(broken) })
        }
        val loaded = outcome as TextLoadOutcome.Loaded
        assertEquals(2, loaded.lines.size)
        assertTrue("the bytes around the bad one survived: ${loaded.lines[0]}", loaded.lines[0].startsWith("ab"))
        assertTrue("the bad byte became a replacement character, not a lost line",
            loaded.lines[0].contains('�'))
        assertEquals("c", loaded.lines[1])
        assertFalse(loaded.truncated)
    }

    @Test
    fun aStreamThatCannotBeOpenedFailsInsteadOfLoadingAnEmptyDocument() {
        val outcome = runBlocking { TextViewerModel.load({ null }) }
        assertEquals(LoadFailure.CORRUPT_DOCUMENT, (outcome as TextLoadOutcome.Failed).failure)
        assertEquals(TextViewerModel.COULD_NOT_OPEN, outcome.error?.message)
    }

    @Test
    fun aFailingStreamKeepsAPermissionDenialDistinctFromCorruption() {
        val outcome = runBlocking {
            TextViewerModel.load({ throw SecurityException("Permission Denial: not granted") })
        }
        assertEquals(LoadFailure.PERMISSION_EXPIRED, (outcome as TextLoadOutcome.Failed).failure)
    }

    @Test
    fun cancellationPropagatesRatherThanBecomingAFailedDocument() {
        // A reader who leaves the screen has not met a broken file. Turning the cancellation into
        // Failed() would let an abandoned load draw an error over whatever they opened next.
        val outcome = runCatching {
            runBlocking { TextViewerModel.load({ throw CancellationException("left the viewer") }) }
        }
        assertTrue("cancellation was swallowed into an outcome", outcome.exceptionOrNull() is CancellationException)
    }

    @Test
    fun aLongReadStopsWhenTheCoroutineIsCancelled() {
        // The cancel has to be issued from inside the read, not from a delay the read is hoped to
        // outlast: the previous version slept 60ms, cancelled, and asserted the loader had noticed -
        // which passed on my machine and failed on CI's faster one, where the whole document was
        // already loaded by then. Here the stream cancels the loader's own job on its third chunk, so
        // the loader is provably mid-document when the cancellation lands and its next cancellation
        // checkpoint has to throw. Nothing depends on how fast the machine is.
        val huge = (1..300_000).joinToString("\n") { "line $it with some text in it" }
        var reads = 0
        var cancelledAtChunk = -1
        val scanJob = Job()

        val stream = object : InputStream() {
            private val source = ByteArrayInputStream(huge.toByteArray(Charsets.UTF_8))
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                reads++
                if (reads == CANCEL_AT_CHUNK && cancelledAtChunk < 0) {
                    cancelledAtChunk = reads
                    scanJob.cancel()
                }
                return source.read(b, off, len)
            }

            override fun read(): Int = source.read()
            override fun available(): Int = source.available()
            override fun close() {
                source.close()
            }
        }

        var cancellationReachedCaller = false
        var completedAnyway = false
        runBlocking {
            launch(scanJob + Dispatchers.IO) {
                try {
                    TextViewerModel.load({ stream }, TextBudget(50_000_000, 10_000_000, 200))
                    completedAnyway = true
                } catch (expected: CancellationException) {
                    cancellationReachedCaller = true
                }
            }.join()
        }

        assertTrue(
            "the loader consumed the whole document before the cancel could be issued " +
                "(the stream was read $reads times)",
            cancelledAtChunk == CANCEL_AT_CHUNK
        )
        assertFalse("the read ran to completion despite the cancellation", completedAnyway)
        assertTrue("the cancel was swallowed inside the loader", cancellationReachedCaller)
    }

    @Test
    fun searchMatchesWholeLinesContainingTheTermInOrder() {
        val lines = listOf("alpha", "Beta gamma", "beta again", "delta")
        assertEquals(listOf(1, 2), TextViewerModel.matchingLines(lines, "beta"))
        assertTrue(TextViewerModel.matchingLines(lines, "").isEmpty())
        assertTrue(TextViewerModel.matchingLines(lines, "zzz").isEmpty())
    }

    @Test
    fun theReaderUnderneathIsClosedEvenWhenTheBudgetStopsEarly() {
        var closed = false
        val tracked = object : InputStream() {
            private val source = ByteArrayInputStream("one\ntwo\nthree\n".toByteArray())
            override fun read() = source.read()
            override fun read(b: ByteArray, off: Int, len: Int) = source.read(b, off, len)
            override fun available() = source.available()
            override fun close() { closed = true }
        }
        runBlocking {
            val budget = TextBudget(maxBytes = 6, maxLines = 10, maxLineChars = 50)
            val outcome = TextViewerModel.load({ tracked }, budget)
            assertTrue(outcome is TextLoadOutcome.Loaded)
            assertTrue((outcome as TextLoadOutcome.Loaded).truncated)
        }
        assertTrue("the budget stopped the read and the stream stayed open", closed)
    }
}
