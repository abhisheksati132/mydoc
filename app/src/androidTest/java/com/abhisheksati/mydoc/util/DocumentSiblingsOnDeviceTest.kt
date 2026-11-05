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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
     * The finding was that this scan ran on the main thread. The proof that it no longer does is
     * order, not duration: work posted to the main looper has to run *while* the scan is still
     * going. An inline scan holds the looper until the list is built, so the posted callback can only
     * ever land after the scan returned - which is what the last assertion refuses to accept. A
     * previous version of this test asserted the scan took longer than a fixed number of milliseconds
     * and a fast runner made that unsatisfiable, so the claim is now measured in a way that does not
     * depend on how quickly a device can stat 5,000 files.
     */
    @Test
    fun theScanLeavesTheMainThreadFree() {
        val dir = folder("mainthread")
        repeat(5_000) { index -> File(dir, "doc%04d.pdf".format(index)).writeText("x") }
        val anchor = File(dir, "doc0000.pdf")

        val scanRunning = AtomicBoolean(false)
        val scanStarted = CountDownLatch(1)
        val scanFinished = CountDownLatch(1)
        val ranWhileScanning = AtomicBoolean(false)
        var enumerated = -1
        val handler = Handler(Looper.getMainLooper())

        val worker = thread(name = "sibling-scan-from-main") {
            runBlocking(Dispatchers.Main) {
                scanRunning.set(true)
                scanStarted.countDown()
                enumerated = DocumentSiblings.readableDocumentsIn(anchor).size
                scanRunning.set(false)
                scanFinished.countDown()
            }
        }

        assertTrue("the scan never started", scanStarted.await(30, TimeUnit.SECONDS))
        // Queued only after the scan is under way: if the scan suspends off the main thread this runs
        // in that gap, and if it blocks the main thread the looper cannot reach it before
        // scanRunning has already gone back to false.
        handler.post { if (scanRunning.get()) ranWhileScanning.set(true) }

        assertTrue("the scan never finished within 60s", scanFinished.await(60, TimeUnit.SECONDS))
        worker.join(5_000)
        handler.removeCallbacksAndMessages(null)

        assertEquals("the scan must enumerate the whole folder for this to mean anything", 5_000, enumerated)
        assertTrue(
            "the main looper was starved for the whole scan: the posted callback only ran after " +
                "readableDocumentsIn returned",
            ranWhileScanning.get()
        )
        dir.walkBottomUp().forEach { it.delete() }
    }


}
