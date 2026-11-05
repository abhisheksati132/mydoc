package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * H-6: a full-document search used to hold the rendering lock for the whole scan, so scrolling a
 * large PDF froze while the workers looked for a phrase, and a search the reader had already
 * abandoned could still publish its results.
 */
@RunWith(AndroidJUnit4::class)
class PdfSearchCancellationOnDeviceTest {

    private companion object {
        const val PAGES = 1200

        // The scan consults its predicate on every page, so asking for a page on the third pass
        // leaves 1,197 pages of work still running - that gap is the window the render needs.
        const val RENDER_AT_PAGE = 3

        // The correct answer is "straight away": the render only waits for the gap between two
        // pages. Nothing but a lock held across the whole scan can make this expire.
        const val REQUEST_WINDOW_SECONDS = 20L
    }

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
            // Only the last page holds the needle, so every search has to walk all 1,200 pages.
            page.canvas.drawText(if (index == PAGES - 1) "needle" else "page $index of $PAGES", 40f, 60f, paint)
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
        val renderRequested = CountDownLatch(1)
        val renderFinished = CountDownLatch(1)
        var predicateCalls = 0
        var rendered: Bitmap? = null
        var renderedDuringSearch = false

        val renderThread = thread(name = "page-render-during-search") {
            if (renderRequested.await(REQUEST_WINDOW_SECONDS, TimeUnit.SECONDS)) {
                rendered = renderer.renderPage(0, 1.5f, com.abhisheksati.mydoc.domain.model.ReadingMode.NORMAL)
                renderFinished.countDown()
            }
        }

        // search() asks the cancellation predicate between every page, and that call sits outside the
        // rendering lock. Asking for a page from inside the callback therefore only succeeds if the
        // lock is taken one page at a time: with the old whole-document lock the callback ran while
        // the search held the lock, the render blocked behind the remaining 1,197 pages, and this
        // wait expired. Nothing here depends on how fast the device is.
        val matches = renderer.search("needle") {
            predicateCalls++
            if (predicateCalls == RENDER_AT_PAGE) {
                renderRequested.countDown()
                renderedDuringSearch = renderFinished.await(REQUEST_WINDOW_SECONDS, TimeUnit.SECONDS)
            }
            false
        }
        renderThread.join(TimeUnit.SECONDS.toMillis(REQUEST_WINDOW_SECONDS * 3))
        renderer.close()

        assertTrue("renderPage produced nothing", rendered != null)
        rendered?.recycle()
        assertEquals(
            "the predicate should be consulted once per page; the scan walked only $predicateCalls",
            PAGES, predicateCalls
        )
        assertTrue("the search found nothing, although the last page holds the needle", matches.isNotEmpty())
        assertTrue(
            "renderPage could not complete while the scan still had ${PAGES - RENDER_AT_PAGE} pages to " +
                "walk: the search holds the rendering lock for the whole document",
            renderedDuringSearch
        )
    }
}
