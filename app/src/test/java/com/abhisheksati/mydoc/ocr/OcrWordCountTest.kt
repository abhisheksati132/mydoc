package com.abhisheksati.mydoc.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N-22: the OCR result sheet counted words by splitting the whole text during composition, so every
 * scroll and every button press re-ran the split on text that cannot change while the sheet is open.
 * The counting is now a named function the sheet calls from `remember(text)`, which makes both the
 * result and the cost testable.
 */
class OcrWordCountTest {

    @Test
    fun ordinaryRunningTextCountsOneWordPerGap() {
        assertEquals(6, ocrWordCount("the quick brown fox jumps over"))
    }

    @Test
    fun runsOfWhitespaceDoNotInflateTheCount() {
        assertEquals(3, ocrWordCount("one \t\n  two\r\nthree   "))
    }

    @Test
    fun anEmptyOcrResultCountsZeroWords() {
        assertEquals(0, ocrWordCount(""))
        assertEquals(0, ocrWordCount("   \n\t  "))
    }

    @Test
    fun ocrTextWithNoSpacesIsOneWordNotZero() {
        // OCR of a seal or a license plate can come back as a single unbroken run; reporting "0 words"
        // for text the user can plainly see would read as a failed extraction.
        assertEquals(1, ocrWordCount("MH12AB1234"))
    }

    @Test
    fun countingALargeResultIsLinearAndCheap() {
        val text = buildString(2_000_000) {
            repeat(300_000) { append("word") ; append(' ') }
        }
        val started = System.nanoTime()
        assertEquals(300_000, ocrWordCount(text))
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue("counting 2 MB of OCR text took ${elapsedMs}ms", elapsedMs < 1_500)
    }
}
