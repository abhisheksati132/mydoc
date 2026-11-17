package com.abhisheksati.mydoc.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Recency for cache trimming lives on the meta sidecar. The copy's own modification time is its
 * content identity: the text indexer skips re-extraction while it is unchanged, so merely opening
 * a document must not move it.
 */
class SavedCopyAccessStampTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun touchingACopyWithASidecarLeavesItsContentStampAlone() {
        val copy = File(tempFolder.root, "doc.pdf").apply { writeText("data") }
        val meta = File(tempFolder.root, OfflineCopyPolicy.metaFileFor(copy.name))
            .apply { writeText("size=4;modified=1") }
        assertTrue(copy.setLastModified(1_000L))
        assertTrue(meta.setLastModified(1_000L))

        DocumentStreamResolver.touchSavedCopy(copy)

        assertEquals(1_000L, copy.lastModified())
        assertTrue(meta.lastModified() > 1_000L)
    }

    @Test
    fun aCopyWithoutASidecarStillRecordsUse() {
        val copy = File(tempFolder.root, "legacy.pdf").apply { writeText("data") }
        assertTrue(copy.setLastModified(1_000L))

        DocumentStreamResolver.touchSavedCopy(copy)

        assertTrue(copy.lastModified() > 1_000L)
    }
}
