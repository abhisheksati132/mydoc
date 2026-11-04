package com.abhisheksati.mydoc.rendering.engine

import com.abhisheksati.mydoc.domain.model.LoadFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * H-7: the size budget has to be applied before the buffer exists, and a memory failure must not be
 * announced as a file that uses unimplemented features.
 */
class OfficeSourceReaderTest {

    private fun bytes(size: Int) = ByteArray(size) { (it % 251).toByte() }

    private fun ready(source: OfficeSource): ByteArray =
        (source as? OfficeSource.Ready)?.bytes ?: error("expected Ready, got $source")

    @Test
    fun aStreamExactlyAsLongAsClaimedIsReturnedInFull() {
        val data = bytes(64)
        val result = OfficeSourceReader.readBounded(ByteArrayInputStream(data), 64, 1_000)
        assertEquals(64, ready(result).size)
        assertTrue(data.contentEquals(ready(result)))
    }

    @Test
    fun aStreamShorterThanClaimedYieldsOnlyTheBytesThatExist() {
        val result = OfficeSourceReader.readBounded(ByteArrayInputStream(bytes(5)), 50, 1_000)
        assertEquals(5, ready(result).size)
    }

    @Test
    fun aStreamLongerThanClaimedGrowsAndStillReadsInFull() {
        val data = bytes(300)
        val result = OfficeSourceReader.readBounded(ByteArrayInputStream(data), 10, 1_000)
        assertEquals(300, ready(result).size)
        assertTrue(data.contentEquals(ready(result)))
    }

    @Test
    fun aStreamPastTheBudgetIsRefusedRatherThanBuffered() {
        val result = OfficeSourceReader.readBounded(ByteArrayInputStream(bytes(4_000)), 4_000, 1_000)
        assertTrue(
            "expected refusal, got $result",
            result is OfficeSource.Failed && result.failure == OfficeSourceFailure.TOO_LARGE
        )
    }

    @Test
    fun anAbsurdLengthClaimDoesNotDriveTheAllocation() {
        // 10 GB claimed against a 1 KB budget, with a stream that only holds 8 bytes. Allocating
        // from the claim would ask for a ten-gigabyte array before ever looking at the stream.
        val result = OfficeSourceReader.readBounded(
            ByteArrayInputStream(bytes(8)),
            declaredSize = 10L * 1024 * 1024 * 1024,
            maxBytes = 1_024
        )
        assertEquals(8, ready(result).size)
    }

    @Test
    fun anEmptyStreamIsReportedAsEmptyNotUnreadable() {
        val result = OfficeSourceReader.readBounded(ByteArrayInputStream(bytes(0)), 0, 1_000)
        assertTrue(result is OfficeSource.Failed && result.failure == OfficeSourceFailure.EMPTY)
    }

    @Test
    fun tooLargeAndOutOfMemoryAreAnnouncedDifferently() {
        val tooLarge = OfficeSource.Failed(OfficeSourceFailure.TOO_LARGE).asConversionFailure()
        val outOfMemory = OfficeSource.Failed(OfficeSourceFailure.OUT_OF_MEMORY).asConversionFailure()

        assertEquals(LoadFailure.EXCEEDS_LIMITS, tooLarge.failure)
        assertEquals(LoadFailure.EXCEEDS_LIMITS, outOfMemory.failure)
        assertTrue(tooLarge.message.contains("larger than MyDoc can convert"))
        assertTrue(outOfMemory.message.contains("ran out of memory"))

        // Neither may blame unimplemented features, which is what the old single `null` returned.
        assertFalse(tooLarge.message.contains("features"))
        assertFalse(outOfMemory.message.contains("features"))
    }

    @Test
    fun anUnreadableDocumentIsAnnouncedAsUnreadable() {
        val failure = OfficeSource.Failed(OfficeSourceFailure.UNREADABLE).asConversionFailure()
        assertEquals(LoadFailure.CONVERSION_FAILED, failure.failure)
        assertTrue(failure.message.contains("could not read"))
    }
}
