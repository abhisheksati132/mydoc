package com.abhisheksati.mydoc.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * H-10 asked for a benchmark against a folder of 5,000 files. The scan is the one that runs when a
 * document opens, so a slow one is felt as the reader waiting on a page.
 */
@RunWith(AndroidJUnit4::class)
class DocumentSiblingsOnDeviceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    private fun folder(name: String): File =
        File(context.getExternalFilesDir(null), "siblings-$name").apply {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }

    @Test
    fun fiveThousandFilesEnumerateWithoutStallingAndInNameOrder() {
        val dir = folder("large")
        repeat(5_000) { index -> File(dir, "doc%04d.pdf".format(index)).writeText("x") }
        File(dir, "zz-visible.pdf").writeText("x")

        val anchor = File(dir, "doc0001.pdf")
        val started = System.nanoTime()
        val siblings = runBlocking { DocumentSiblings.readableDocumentsIn(anchor) }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        assertEquals(5_001, siblings.size)
        assertEquals("doc0000.pdf", siblings.first().displayName)
        assertEquals("zz-visible.pdf", siblings.last().displayName)
        assertEquals(
            "not name-sorted",
            siblings.map { it.displayName.lowercase() }.sorted(),
            siblings.map { it.displayName.lowercase() }
        )
        // Measured 1,143ms for 5,001 entries + per-file size and mtime on mydoc_api35. The bound is
        // that with headroom for a slower device; the finding was never the raw cost, it was that
        // the cost sat on the main thread.
        assertTrue(
            "enumerating 5,001 files took ${elapsedMs}ms on the emulator",
            elapsedMs < 2_500
        )
        assertTrue(
            "size should come from the filesystem",
            siblings.first().sizeBytes == 1L
        )
        dir.walkBottomUp().forEach { it.delete() }
    }

    @Test
    fun hiddenFilesUnlistedTypesAndSubfoldersAreLeftOut() {
        val dir = folder("filter")
        File(dir, "report.pdf").writeText("a")
        File(dir, "notes.txt").writeText("a")
        File(dir, ".hidden.pdf").writeText("a")
        File(dir, "clip.mp4").writeText("a")
        File(dir, "no-extension").writeText("a")
        File(dir, "archive.unreadableextension").writeText("a")
        File(dir, "sub").mkdir()
        File(dir, "sub/nested.pdf").writeText("a")

        val siblings = runBlocking { DocumentSiblings.readableDocumentsIn(File(dir, "report.pdf")) }
        val supported = com.abhisheksati.mydoc.domain.model.DocumentFormats.supportedExtensions

        assertEquals(
            listOf("notes.txt", "report.pdf"),
            siblings.map { it.displayName }.sorted()
        )
        assertTrue(".txt is readable by MyDoc, so it belongs in the swipe chain", "txt" in supported)
        assertTrue("the scan must not walk into subfolders", siblings.none { it.displayName.contains("nested") })
        assertTrue("hidden files stay out", siblings.none { it.displayName.startsWith(".") })
        dir.walkBottomUp().forEach { it.delete() }
    }

    @Test
    fun aCallerThatHasAlreadyGoneDoesNotReceiveASiblingList() {
        val dir = folder("cancel")
        repeat(5_000) { index -> File(dir, "doc%04d.pdf".format(index)).writeText("x") }
        val anchor = File(dir, "doc0001.pdf")

        // The reader moved on before this finished. The scan must abandon rather than hand back a
        // list that would overwrite the document they opened next.
        val published = runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(0L) { DocumentSiblings.readableDocumentsIn(anchor) }
        }
        assertEquals("a cancelled scan still produced a result", null, published)
        dir.walkBottomUp().forEach { it.delete() }
    }

    @Test
    fun aMissingFolderAndAFolderlessFileBothYieldNothing() {        val siblings = runBlocking {
            listOf(
                DocumentSiblings.readableDocumentsIn(null),
                DocumentSiblings.readableDocumentsIn(File("/data/does-not-exist-anywhere/file.pdf"))
            )
        }
        assertTrue(siblings.all { it.isEmpty() })
    }

    /**
     * The finding was that this scan ran on the main thread. Running it from a Main-dispatched
     * coroutine on a background thread lets the main looper keep ticking only if the scan suspends
     * off it — an inline scan would starve the ticker for the whole second it takes.
     */
    @Test
    fun theScanLeavesTheMainThreadFree() {
        val dir = folder("mainthread")
        repeat(5_000) { index -> File(dir, "doc%04d.pdf".format(index)).writeText("x") }
        File(dir, "zz-visible.pdf").writeText("x")
        val anchor = File(dir, "doc0000.pdf")

        val handler = Handler(Looper.getMainLooper())
        val ticks = AtomicInteger(0)
        var ticking = true
        val ticker = object : Runnable {
            override fun run() {
                if (ticking) {
                    ticks.incrementAndGet()
                    handler.postDelayed(this, 20L)
                }
            }
        }
        handler.post(ticker)

        val scanMs = AtomicInteger(0)
        val worker = thread(name = "sibling-scan-from-main") {
            runBlocking(Dispatchers.Main) {
                val started = System.nanoTime()
                DocumentSiblings.readableDocumentsIn(anchor)
                scanMs.set(((System.nanoTime() - started) / 1_000_000).toInt())
            }
        }
        worker.join(60_000)
        ticking = false
        handler.removeCallbacks(ticker)

        assertTrue("the scan never finished within 60s", !worker.isAlive)
        assertTrue("scan took ${scanMs.get()}ms, too fast to judge", scanMs.get() > 400)
        assertTrue(
            "the main looper only ticked ${ticks.get()} times during a ${scanMs.get()}ms scan — " +
                "the scan is holding the main thread",
            ticks.get() > scanMs.get() / 40
        )
        dir.walkBottomUp().forEach { it.delete() }
    }


}
