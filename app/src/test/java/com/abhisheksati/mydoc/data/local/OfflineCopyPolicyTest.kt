package com.abhisheksati.mydoc.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * H-9: the offline copy was judged current on file size alone, so an edit that kept the length left
 * the app serving an outdated copy of the user's document with no way for it to notice.
 */
class OfflineCopyPolicyTest {

    private fun stamp(size: Long, modified: Long) = OfflineCopyStamp(size, modified)

    @Test
    fun aSameSizeEditIsDetectedByModificationTime() {
        val before = stamp(2_048, 1_700_000_000_000)
        val afterEdit = stamp(2_048, 1_700_000_900_000)
        assertTrue(
            "a copy that is the same length but older was still considered current",
            OfflineCopyPolicy.needsRefresh(before, 2_048, afterEdit)
        )
    }

    @Test
    fun anUnchangedDocumentKeepsItsCopy() {
        val same = stamp(2_048, 1_700_000_000_000)
        assertFalse(OfflineCopyPolicy.needsRefresh(same, 2_048, same))
    }

    @Test
    fun aChangedLengthIsRefreshedEvenWithoutAModificationTime() {
        val recorded = stamp(2_048, 0)
        val source = stamp(3_000, 0)
        assertTrue(OfflineCopyPolicy.needsRefresh(recorded, 2_048, source))
    }

    @Test
    fun aCopyWhoseLengthNoLongerMatchesTheSourceIsRefreshed() {
        // Either the document moved on or the copy was cut short while being written.
        val recorded = stamp(2_048, 1_700_000_000_000)
        assertTrue(OfflineCopyPolicy.needsRefresh(recorded, 1_024, stamp(2_048, 1_700_000_000_000)))
    }

    @Test
    fun missingAndEmptyCopiesAreAlwaysRebuilt() {
        val source = stamp(2_048, 1_700_000_000_000)
        assertTrue(OfflineCopyPolicy.needsRefresh(source, 0, source))
        assertTrue(OfflineCopyPolicy.needsRefresh(null, 0, source))
        assertTrue(OfflineCopyPolicy.needsRefresh(null, 2_048, source))
    }

    @Test
    fun aCopyWithoutAStampIsRebuiltOnceAndThenCarriesOne() {
        // Upgrading from the size-only rule: the first open rewrites the copy and records what it
        // came from, so every later open can tell stale from current.
        assertTrue(OfflineCopyPolicy.needsRefresh(null, 5_000, stamp(5_000, 1_700_000_000_000)))
        val written = stamp(5_000, 1_700_000_000_000)
        assertFalse(OfflineCopyPolicy.needsRefresh(written, 5_000, written))
    }

    @Test
    fun aDocumentWithNoKnownSizeOrModificationTimeStillReadsOffline() {
        // Refusing here would break the point of the copy: offline reading after the grant dies.
        assertFalse(OfflineCopyPolicy.needsRefresh(stamp(0, 0), 4_096, stamp(0, 0)))
    }

    @Test
    fun theStampFormatSurvivesARoundTrip() {
        val original = stamp(1_234_567, 1_700_000_000_000)
        val text = OfflineCopyPolicy.format(original)
        assertEquals(original, OfflineCopyPolicy.parse(text))
        assertEquals("size=0;modified=0", OfflineCopyPolicy.format(stamp(0, 0)))
    }

    @Test
    fun anUnreadableStampIsTreatedAsAbsentRatherThanCrashing() {
        listOf("", "nonsense", "size=abc;modified=1", "size=1", "modified=1", "size=-;modified=-").forEach {
            assertNull("parsed garbage as ${OfflineCopyPolicy.parse(it)}", OfflineCopyPolicy.parse(it))
        }
    }

    @Test
    fun sidecarsAreNamedAfterTheirCopyAndRecognisedBySuffix() {
        val copy = "a1b2c3-report.pdf"
        val meta = OfflineCopyPolicy.metaFileFor(copy)
        assertEquals("a1b2c3-report.pdf.meta", meta)
        assertTrue(OfflineCopyPolicy.isMetaFile(meta))
        assertFalse("a document must not be mistaken for bookkeeping", OfflineCopyPolicy.isMetaFile(copy))
    }
}
