package com.abhisheksati.mydoc.data.local

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.util.DocumentIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The offline copy now carries a sidecar recording what it was copied from. These run against the
 * real `saved_docs` directory because the failure modes are file-shaped: a sidecar mistaken for a
 * cached document would be served to the reader as their file, and sidecars counted as documents
 * would push real ones out of the cache.
 */
@RunWith(AndroidJUnit4::class)
class OfflineCopySidecarOnDeviceTest {

    private lateinit var context: Context
    private lateinit var savedDocs: File

    private val uriString = "content://com.example.provider/document/offline-copy-test"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        savedDocs = File(context.filesDir, "saved_docs").apply { mkdirs() }
        savedDocs.listFiles()?.forEach { if (it.name.contains("offline-copy-test")) it.delete() }
    }

    private fun writeCopy(contents: String, suffix: String): Pair<File, File> {
        val name = DocumentIdentity.savedDocsName("$uriString$suffix", "report.pdf")
        val copy = File(savedDocs, name).apply { writeText(contents) }
        val meta = File(savedDocs, OfflineCopyPolicy.metaFileFor(name)).apply {
            writeText(OfflineCopyPolicy.format(OfflineCopyStamp(contents.length.toLong(), 1_700_000_000_000)))
        }
        return copy to meta
    }

    @Test
    fun theOfflineFallbackReturnsTheDocumentNotItsSidecar() {
        val uri = "content://com.example.provider/document/offline-copy-test-fallback"
        val name = DocumentIdentity.savedDocsName(uri, "report.pdf")
        val copy = File(savedDocs, name).apply { writeText("REAL DOCUMENT BYTES") }
        val meta = File(savedDocs, OfflineCopyPolicy.metaFileFor(name)).apply {
            writeText("size=19;modified=1700000000000")
        }
        // Make the sidecar the file that would win an unfiltered newest-first or plain-prefix match.
        meta.setLastModified(System.currentTimeMillis() + 60_000L)

        val served = DocumentStreamResolver.openStream(context, Uri.parse(uri))?.buffered()?.reader()?.readText()
        assertEquals("REAL DOCUMENT BYTES", served)
        assertTrue(copy.exists() && meta.exists())

        copy.delete()
        meta.delete()
    }

    @Test
    fun sidecarsDoNotConsumeDocumentSlotsAndLeaveWithTheirCopy() {
        val limit = DocumentStreamResolver.MAX_SAVED_DOCS_COUNT
        val created = (0..limit).map { index -> writeCopy("copy $index has content", "-$index") }
        assertEquals("one copy plus one sidecar per document", (limit + 1) * 2, created.size * 2)

        DocumentStreamResolver.trimSavedDocsCache(savedDocs)

        val survivors = created.filter { (copy, _) -> copy.exists() }
        assertTrue(
            "trimming kept only ${survivors.size} of ${limit + 1} copies; sidecars are being " +
                "counted as documents",
            survivors.size > limit / 2
        )
        survivors.forEach { (copy, meta) ->
            assertTrue("a surviving copy lost its stamp: ${copy.name}", meta.exists())
        }
        created.forEach { (copy, meta) ->
            runCatching { copy.delete() }
            runCatching { meta.delete() }
        }
    }
}
