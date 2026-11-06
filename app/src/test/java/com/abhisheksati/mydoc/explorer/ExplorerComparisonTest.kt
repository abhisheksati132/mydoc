package com.abhisheksati.mydoc.explorer

import com.abhisheksati.mydoc.data.local.ExplorerComparison
import com.abhisheksati.mydoc.domain.model.ComparisonStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExplorerComparisonTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun detectsChangedBytesEvenWithSameLengthAndTimestamp() = runBlocking {
        val left = temp.newFolder()
        val right = temp.newFolder()
        left.resolve("same").writeText("same")
        right.resolve("same").writeText("same")
        val a = left.resolve("changed").apply { writeText("AAAA") }
        val b = right.resolve("changed").apply { writeText("BBBB"); setLastModified(a.lastModified()) }
        val before = a.readBytes()
        val result = ExplorerComparison().compare(left, right)
        assertEquals(ComparisonStatus.MATCHING, result.entries.first { it.path == "same" }.status)
        assertEquals(ComparisonStatus.CHANGED, result.entries.first { it.path == "changed" }.status)
        assertArrayEquals(before, a.readBytes())
        assertEquals("BBBB", b.readText())
        assertFalse(result.incomplete)
    }
    @Test fun includesHiddenNestedAndMissingFilesAndTypeConflicts() = runBlocking {
        val left = temp.newFolder()
        val right = temp.newFolder()
        left.resolve("nested").mkdir()
        left.resolve("nested/.hidden").writeText("x")
        right.resolve("only-right").writeText("r")
        left.resolve("type").mkdir()
        right.resolve("type").writeText("file")
        val results = ExplorerComparison().compare(left, right).entries.associate { it.path to it.status }
        assertEquals(ComparisonStatus.LEFT_ONLY, results["nested/.hidden"])
        assertEquals(ComparisonStatus.RIGHT_ONLY, results["only-right"])
        assertEquals(ComparisonStatus.CHANGED, results["type"])
    }
    @Test fun contentBudgetNeverClaimsUnverifiedFilesMatch() = runBlocking {
        val left = temp.newFolder()
        val right = temp.newFolder()
        left.resolve("same").writeText("same")
        right.resolve("same").writeText("same")
        val result = ExplorerComparison(maxReadBytes = 1).compare(left, right)
        assertTrue(result.incomplete)
        assertEquals(ComparisonStatus.UNKNOWN, result.entries.single().status)
    }
    @Test fun limitedScanCannotClaimOtherSideHasMissingFiles() = runBlocking {
        val left = temp.newFolder()
        val right = temp.newFolder()
        left.resolve("a").writeText("a")
        left.resolve("b").writeText("b")
        right.resolve("c").writeText("c")
        val result = ExplorerComparison(maxEntries = 1).compare(left, right)
        assertTrue(result.incomplete)
        assertEquals(ComparisonStatus.UNKNOWN, result.entries.first { it.path == "c" }.status)
    }
    @Test fun changedOrRemovedEntriesBecomeUnknownBeforeClassification() = runBlocking {
        val left = temp.newFolder()
        val right = temp.newFolder()
        left.resolve("changed-size").writeText("before")
        right.resolve("changed-size").writeText("short")
        left.resolve("removed").writeText("removed")
        val result = ExplorerComparison(afterScan = {
            left.resolve("changed-size").writeText("a different size")
            left.resolve("removed").delete()
        }).compare(left, right)
        assertTrue(result.incomplete)
        assertTrue(result.entries.all { it.status == ComparisonStatus.UNKNOWN })
    }

    @Test fun cancelledComparisonDoesNotReturnResults() = runBlocking {
        val left = temp.newFolder()
        val right = temp.newFolder()
        try {
            ExplorerComparison(afterScan = { throw kotlinx.coroutines.CancellationException("cancelled") }).compare(left, right)
            fail("Cancellation was swallowed")
        } catch (_: kotlinx.coroutines.CancellationException) { }
    }

}
