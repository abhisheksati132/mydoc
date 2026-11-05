package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * H-6: a full-document search used to hold the rendering lock for the whole scan, so scrolling a
 * large PDF froze while the workers looked for a phrase, and a search the reader had already
 * abandoned could still publish its results.
 */
@RunWith(AndroidJUnit4::class)
class PdfSearchCancellationOnDeviceTest {

    private companion object { const val PAGES = 1200 }

    private lateinit var context: Context
    private lateinit var file: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.getExternalFilesDir(null), "search-1200.pdf").apply { delete() }
        val doc = PdfDocument()
        val paint = Paint().apply { color = Color.BLACK; textSize = 9f }
        repeat(PAGES) { index ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(400, 300, index + 1).create())
            // Only the last page holds the needle, so every search has to walk all 400 pages.
            page.canvas.drawText(if (index == PAGES - 1) "needle" else "page $index of $PAGES", 40f, 60f, paint)
            // Enough text per page that walking the document is real work: the point of the test is
            // whether one scan can hold every render waiting, which needs a scan that lasts.
            repeat(24) { line ->
                page.canvas.drawText(
                    "The quick brown fox jumps over the lazy dog and keeps running across line $line",
                    20f,
                    90f + line * 14f,
                    paint
                )
            }
            doc.finishPage(page)
        }
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun renderer(): MuPdfRenderer {
        val renderer = MuPdfRenderer(filePath = file.absolutePath)
        assertTrue("fixture did not open", renderer.pageCount == PAGES)
        return renderer
    }

    @Test
    fun anAbandonedSearchReturnsNothingRatherThanAStaleResultList() {
        val renderer = renderer()
        val cancelled = renderer.search("needle") { true }
        val live = renderer.search("needle") { false }
        renderer.close()

        assertTrue("a cancelled search still produced ${cancelled.size} matches", cancelled.isEmpty())
        assertTrue("the same query should match when not cancelled", live.isNotEmpty())
    }

    @Test
    fun renderingIsNotHeldBehindTheWholeDocumentSearch() {
        val renderer = renderer()

        // Baseline first: the window the render has to slip into is derived from the real cost of
        // the scan on this device, not from a guessed constant.
        val scanStarted = System.nanoTime()
        renderer.search("needle") { false }
        val scanAloneMs = (System.nanoTime() - scanStarted) / 1_000_000
        assertTrue(
            "a 1,200 page scan took only ${scanAloneMs}ms — too quick to tell locking strategies apart",
            scanAloneMs > 1_000
        )

        val start = CountDownLatch(1)
        var searchFinishedAt = 0L
        var renderFinishedAt = 0L

        val searcher = Thread {
            start.await(5, TimeUnit.SECONDS)
            renderer.search("needle") { false }
            searchFinishedAt = System.nanoTime()
        }.apply { name = "doc-search"; start() }

        // Get the scan properly under way, then ask for a page. With one lock for the whole scan the
        // render can only complete after the search has; per-page locking lets it slip in between.
        Thread.sleep((scanAloneMs / 5).coerceIn(60, 200))
        start.countDown()
        Thread.sleep((scanAloneMs / 5).coerceIn(60, 200))
        val bitmap = renderer.renderPage(0, 1.5f, com.abhisheksati.mydoc.domain.model.ReadingMode.NORMAL)
        renderFinishedAt = System.nanoTime()

        searcher.join(TimeUnit.SECONDS.toMillis(120))
        renderer.close()

        assertTrue("the search never finished", !searcher.isAlive)
        assertTrue("renderPage produced nothing", bitmap != null)
        assertTrue(
            "render only completed after the whole ${scanAloneMs}ms scan: the search still holds " +
                "the rendering lock for the document",
            renderFinishedAt < searchFinishedAt
        )
        bitmap?.recycle()
    }
}
