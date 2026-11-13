package com.abhisheksati.mydoc.ui.viewer.word

import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxRun
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F16: the reflow viewer's block index must never reach the paginated viewer of the same URI
 * as a resume page. The shared ViewState carries the mapped page; the block position lives
 * under a mode-distinct key that only reflow consumes.
 */
class WordReflowProgressTest {

    private val uri = "content://test/document.docx"

    private fun paragraphs(count: Int): List<DocxBlock> =
        List(count) { i -> DocxBlock.Paragraph(listOf(DocxRun("paragraph $i"))) }

    @Test
    fun paginatedResume_ignoresStoredReflowBlockIndex() {
        val blocks = paragraphs(100)
        val pagination = WordReflowPagination(blocks)
        assertEquals(25, pagination.effectivePageCount)

        val blockIndex = 37
        val persisted = WordReflowProgress.viewStateFor(uri, blockIndex, pagination, ReadingMode.NORMAL)

        // The shared view state must hold the PAGE for block 37, not the block index itself.
        assertEquals(9, persisted.lastPage)

        // The paginated viewer resumes from that shared state. With the old persistence
        // (lastPage = raw block index 37) this resume lands on page 24, the wrong end of the
        // document; with the fix it lands on page 9.
        val resumedPage = ViewerViewModel.resumePage(persisted.lastPage, totalPages = 25)
        assertEquals(9, resumedPage)
        assertNotEquals(ViewerViewModel.resumePage(blockIndex, totalPages = 25), resumedPage)
    }

    @Test
    fun reflowResume_consumesTheModeDistinctBlockIndex() {
        val blocks = paragraphs(100)
        val pagination = WordReflowPagination(blocks)
        val persisted = WordReflowProgress.viewStateFor(uri, 37, pagination, ReadingMode.NORMAL)

        val resumed = WordReflowProgress.resumeBlockIndex(
            storedBlockIndex = 37,
            savedViewState = persisted,
            pagination = pagination,
            blockCount = blocks.size
        )
        assertEquals(37, resumed)
    }

    @Test
    fun reflowResume_withoutStoredBlock_mapsTheSavedPageBackToABlock() {
        val blocks = paragraphs(100)
        val pagination = WordReflowPagination(blocks)

        val resumed = WordReflowProgress.resumeBlockIndex(
            storedBlockIndex = null,
            savedViewState = ViewState(uri = uri, lastPage = 9),
            pagination = pagination,
            blockCount = blocks.size
        )
        assertEquals(36, resumed)
    }

    @Test
    fun reflowResume_clampsOutOfRangePositions() {
        val pagination = WordReflowPagination(paragraphs(100))
        assertEquals(
            99,
            WordReflowProgress.resumeBlockIndex(500, null, pagination, blockCount = 100)
        )
        assertEquals(
            0,
            WordReflowProgress.resumeBlockIndex(null, null, pagination, blockCount = 100)
        )
        assertEquals(
            0,
            WordReflowProgress.resumeBlockIndex(10, null, pagination, blockCount = 0)
        )
    }

    @Test
    fun reflowKey_isModeDistinctFromTheSharedViewState() {
        val key = WordReflowProgress.keyFor(uri)
        assertNotEquals(uri, key)
        assertTrue(key.startsWith("reflow_block:"))
        assertTrue(key.endsWith(uri))
    }

    @Test
    fun pageBreaks_driveThePageMapping() {
        val blocks = listOf(
            DocxBlock.Paragraph(listOf(DocxRun("a"))),
            DocxBlock.Paragraph(listOf(DocxRun("b"))),
            DocxBlock.PageBreak,
            DocxBlock.Paragraph(listOf(DocxRun("c"))),
            DocxBlock.Paragraph(listOf(DocxRun("d"))),
            DocxBlock.PageBreak,
            DocxBlock.Paragraph(listOf(DocxRun("e")))
        )
        val pagination = WordReflowPagination(blocks)
        assertEquals(3, pagination.effectivePageCount)
        assertEquals(0, pagination.pageForBlockIndex(0))
        assertEquals(1, pagination.pageForBlockIndex(3))
        assertEquals(2, pagination.pageForBlockIndex(6))
        assertEquals(3, pagination.blockForPageIndex(1))
        assertEquals(6, pagination.blockForPageIndex(2))

        // A block index past the last break still persists as a valid page, never as itself.
        val persisted = WordReflowProgress.viewStateFor(uri, 6, pagination, ReadingMode.NORMAL)
        assertEquals(2, persisted.lastPage)
        assertEquals(2, ViewerViewModel.resumePage(persisted.lastPage, totalPages = 3))
    }

    @Test
    fun resumePage_clampsToThePageRangeAndDefaultsToZero() {
        assertEquals(0, ViewerViewModel.resumePage(null, totalPages = 10))
        assertEquals(9, ViewerViewModel.resumePage(99, totalPages = 10))
        assertEquals(0, ViewerViewModel.resumePage(-3, totalPages = 10))
        assertEquals(0, ViewerViewModel.resumePage(5, totalPages = 0))
    }
}