package com.abhisheksati.mydoc.explorer

import com.abhisheksati.mydoc.data.local.ExplorerSearch
import com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExplorerSearchTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun combinesFiltersAndHonorsFolderScope() = runBlocking {
        val root = temp.newFolder()
        root.resolve("report.pdf").writeText("12345")
        root.resolve("report.txt").writeText("12345")
        root.resolve("small.pdf").writeText("1")
        root.resolve("child").mkdir()
        root.resolve("child/report.pdf").writeText("12345")
        val options = ExplorerSearchOptions(name = "report", extension = "PDF", minBytes = 3, maxBytes = 8)
        assertEquals(listOf("report.pdf"), ExplorerSearch().search(root, options, false).items.map { it.name })
        assertEquals(2, ExplorerSearch().search(root, options.copy(recursive = true), false).items.size)
    }

    @Test fun excludesHiddenAndReportsResultLimit() = runBlocking {
        val root = temp.newFolder()
        root.resolve(".private.txt").writeText("hidden")
        repeat(4) { root.resolve("file$it.txt").writeText("visible") }
        val result = ExplorerSearch(maxResults = 2).search(root, ExplorerSearchOptions(), false)
        assertEquals(2, result.items.size)
        assertTrue(result.incomplete)
        assertTrue(result.items.none { it.name.startsWith(".") })
    }

    @Test fun rejectsInvalidRangesAndFiltersModifiedTime() = runBlocking {
        assertFalse(ExplorerSearchOptions(minBytes = 20, maxBytes = 10).isValid)
        val root = temp.newFolder()
        root.resolve("old.txt").apply { writeText("old"); setLastModified(1_000) }
        assertTrue(ExplorerSearch().search(root, ExplorerSearchOptions(modifiedAfter = 2_000), false).items.isEmpty())
    }
}
