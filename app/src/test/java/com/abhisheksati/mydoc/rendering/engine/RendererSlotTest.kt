package com.abhisheksati.mydoc.rendering.engine

import com.abhisheksati.mydoc.domain.model.ReadingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * N-25: the viewer kept a native document alive for every file that was opened, because each
 * document URI got its own view model and nothing closed the engine of the one it replaced. The
 * slot is the ownership rule; these tests are the count of live engines after a run of documents.
 */
class RendererSlotTest {

    private class FakeRenderer(val label: String) : DocumentRenderer {
        var closes = 0
        override val pageCount: Int = 1
        override val isPasswordProtected: Boolean = false
        override val supportsPasswordAuthentication: Boolean = true
        override fun authenticatePassword(password: String) = false
        override fun getPageSize(pageIndex: Int) = 595f to 842f
        override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode) = null
        override fun search(query: String, isCancelled: () -> Boolean): List<SearchMatch> = emptyList()
        override fun getOutline(): List<OutlineItem> = emptyList()
        override fun close() { closes++ }
    }

    @Test
    fun openingFifteenDocumentsLeavesOneEngineAliveAndFourteenClosed() {
        val slot = RendererSlot()
        val engines = (1..15).map { FakeRenderer("doc$it") }
        engines.forEach { slot.replace(it) }

        assertEquals(1, slot.liveCount)
        assertSame(engines.last(), slot.current)
        assertEquals("every displaced engine was closed", 14, engines.take(14).sumOf { it.closes })
        assertEquals("the engine still in use was not closed", 0, engines.last().closes)
    }

    @Test
    fun releasingClosesTheHeldEngineAndLeavesNothingBehind() {
        val slot = RendererSlot()
        val engine = FakeRenderer("only")
        slot.replace(engine)
        slot.release()

        assertEquals(0, slot.liveCount)
        assertNull(slot.current)
        assertEquals(1, engine.closes)
    }

    @Test
    fun releasingTwiceDoesNotCloseTheSameEngineTwice() {
        val slot = RendererSlot()
        val engine = FakeRenderer("only")
        slot.replace(engine)
        slot.release()
        slot.release()
        assertEquals(1, engine.closes)
    }

    @Test
    fun replacingWithTheSameEngineKeepsItOpen() {
        // Reopening the same document hands back the engine that is already reading it; closing it
        // here would leave the viewer pointing at a released native document.
        val slot = RendererSlot()
        val engine = FakeRenderer("same")
        slot.replace(engine)
        assertSame(engine, slot.replace(engine))
        assertEquals(0, engine.closes)
    }

    @Test
    fun replacingWithNothingClosesWhatWasHeld() {
        val slot = RendererSlot()
        val engine = FakeRenderer("doomed")
        slot.replace(engine)
        slot.replace(null)
        assertEquals(0, slot.liveCount)
        assertEquals(1, engine.closes)
    }

    @Test
    fun anEngineThatThrowsOnCloseIsStillGivenUp() {
        val slot = RendererSlot()
        val noisy = object : DocumentRenderer {
            var attempts = 0
            override val pageCount: Int = 1
            override val isPasswordProtected: Boolean = false
            override fun authenticatePassword(password: String) = false
            override fun getPageSize(pageIndex: Int) = 595f to 842f
            override fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode) = null
            override fun search(query: String, isCancelled: () -> Boolean): List<SearchMatch> = emptyList()
            override fun getOutline(): List<OutlineItem> = emptyList()
            override fun close() {
                attempts++
                throw RuntimeException("native handle already gone")
            }
        }
        val next = FakeRenderer("next")
        slot.replace(noisy)
        slot.replace(next)
        assertSame("a failed close must not swallow the replacement", next, slot.current)
        assertEquals(1, noisy.attempts)
    }
}
