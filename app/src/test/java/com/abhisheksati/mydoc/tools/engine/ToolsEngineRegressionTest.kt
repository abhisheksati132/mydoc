package com.abhisheksati.mydoc.tools.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ToolsEngineRegressionTest {

    // T05: a heading-only builder was never empty, so an image-only PDF was a "successful"
    // extraction. The fix returns null when no page yielded text.
    @Test
    fun buildExtractedMarkdown_returnsNullWhenNoPageHasText() {
        assertNull(buildExtractedMarkdown("Scanned", emptyList(), failedOcrPages = listOf(1, 2)))
    }

    @Test
    fun buildExtractedMarkdown_keepsSourcePageNumbersAndNotesFailedOcr() {
        val markdown = buildExtractedMarkdown(
            title = "Report",
            pages = listOf(1 to "first", 3 to "third"),
            failedOcrPages = listOf(3)
        )!!
        assertTrue(markdown.startsWith("# Report"))
        assertTrue(markdown.contains("## Page 1"))
        assertTrue(markdown.contains("## Page 3"))
        assertFalse(markdown.contains("## Page 2"))
        assertTrue(markdown.contains("OCR could not read page 3"))
    }

    // T06: the OCR and raster fallbacks now share the pixel ceiling the other raster paths use.
    @Test
    fun cappedRenderScale_neverExceedsThePixelCeiling() {
        val scale = cappedRenderScale(100_000f, 50_000f, 1.5f)
        assertTrue(100_000f * scale <= 4096f + 0.01f)
        assertEquals(1.5f, cappedRenderScale(200f, 300f, 1.5f), 0.0001f)
    }

    // T07: coerceIn(80f, visW * 0.85f) threw for a 72 pt wide page. The placement must exist and
    // stay inside the page in every rotation (rotation swaps the visible width and height).
    @Test
    fun layoutSignature_fitsNarrowPageInEveryRotation() {
        for ((visW, visH) in listOf(72f to 200f, 200f to 72f)) {
            val placement = layoutSignature(
                visW = visW,
                visH = visH,
                scale = 0.28f,
                xPercent = 0.9f,
                yPercent = 0.9f,
                aspectRatio = 3f
            )
            assertTrue(placement.width.isFinite() && placement.height.isFinite())
            assertTrue(placement.width <= visW * 0.85f + 0.001f)
            assertTrue(placement.height <= visH * 0.85f + 0.001f)
            assertTrue(placement.x >= 0f && placement.x + placement.width <= visW + 0.001f)
            assertTrue(placement.y >= 0f && placement.y + placement.height <= visH + 0.001f)
        }
    }

    @Test
    fun layoutSignature_capsATallStampByHeight() {
        val placement = layoutSignature(200f, 72f, 1f, 0.5f, 0.5f, aspectRatio = 0.2f)
        assertTrue(placement.height <= 72f * 0.85f + 0.001f)
        assertTrue(placement.width <= 200f * 0.85f + 0.001f)
        assertEquals(5.4f, placement.y, 0.001f)
    }

    // T10: staging must stop at the byte cap instead of copying until the cache partition fills.
    @Test
    fun copyStagedInput_throwsWhenTheStreamExceedsTheByteCap() = runBlocking {
        val output = ByteArrayOutputStream()
        val failure = runCatching {
            copyStagedInput(ByteArrayInputStream(ByteArray(9000)), output, maxBytes = 5000)
        }.exceptionOrNull()
        assertTrue("expected IOException, got $failure", failure is IOException)
        assertTrue(output.size() <= 5000)
    }

    @Test
    fun copyStagedInput_copiesEverythingUnderTheCap() = runBlocking {
        val payload = ByteArray(4000) { (it % 251).toByte() }
        val output = ByteArrayOutputStream()
        copyStagedInput(ByteArrayInputStream(payload), output, maxBytes = 5000)
        assertTrue(payload.contentEquals(output.toByteArray()))
    }

    // T10: Cancel used to be ignored until EOF because the copy loop never checked the coroutine.
    @Test
    fun copyStagedInput_stopsAfterCancellationDuringABlockingRead() = runBlocking {
        val blocked = CountDownLatch(1)
        val release = CountDownLatch(1)
        var reads = 0
        val longStream = object : InputStream() {
            override fun read(): Int = 7

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                reads++
                if (reads > 5000) return -1
                if (reads == 2) {
                    blocked.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
                b[off] = 7
                return 1
            }
        }

        val output = ByteArrayOutputStream()
        val job = launch(Dispatchers.IO) {
            copyStagedInput(longStream, output, maxBytes = 1_000_000)
        }
        assertTrue("worker never reached the blocking read", blocked.await(10, TimeUnit.SECONDS))
        job.cancel()
        release.countDown()
        job.join()

        assertTrue("job should end cancelled", job.isCancelled)
        // The old copyTo never checked cancellation and copied all 5000 bytes to EOF.
        assertTrue("copy must stop after cancellation, got ${output.size()} bytes", output.size() < 5000)
    }
}
