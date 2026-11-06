package com.abhisheksati.mydoc.explorer

import com.abhisheksati.mydoc.data.local.ExplorerRename
import com.abhisheksati.mydoc.domain.model.ExplorerRenameOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.nio.file.Files

class ExplorerRenameTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun previewPreservesExtensionsAndDoesNotWrite() = runBlocking {
        val source = temp.newFile("photo.jpg").apply { writeText("original") }
        val plan = ExplorerRename().preview(listOf(source), ExplorerRenameOptions(prefix = "Trip_", numbered = true))
        assertTrue(plan.canApply)
        assertEquals("Trip_photo_001.jpg", plan.entries.single().target.name)
        assertTrue(source.exists())
        assertFalse(plan.entries.single().target.exists())
    }

    @Test fun collisionAndUnsafeNamesBlockWholePlan() = runBlocking {
        val a = temp.newFile("a.txt")
        temp.newFile("taken.txt")
        assertFalse(ExplorerRename().preview(listOf(a), ExplorerRenameOptions(find = "a", replacement = "taken")).canApply)
        assertFalse(ExplorerRename().preview(listOf(a), ExplorerRenameOptions(prefix = "../")).canApply)
    }

    @Test fun failedPublishRestoresEveryOriginalWithoutOverwriting() = runBlocking {
        val a = temp.newFile("a.txt").apply { writeText("A") }
        val b = temp.newFile("b.txt").apply { writeText("B") }
        var moves = 0
        val engine = ExplorerRename { from, to ->
            moves++
            if (moves == 4) throw IOException("injected publish failure")
            Files.move(from.toPath(), to.toPath())
        }
        val result = engine.apply(engine.preview(listOf(a, b), ExplorerRenameOptions(prefix = "new_")))
        assertFalse(result.success)
        assertTrue(result.unresolvedPaths.isEmpty())
        assertEquals("A", a.readText())
        assertEquals("B", b.readText())
    }

    @Test fun refusesSourceChangedSincePreview() = runBlocking {
        val a = temp.newFile("a.txt").apply { writeText("A") }
        val engine = ExplorerRename()
        val plan = engine.preview(listOf(a), ExplorerRenameOptions(prefix = "new_"))
        a.writeText("changed")
        assertFalse(engine.apply(plan).success)
        assertEquals("changed", a.readText())
        assertFalse(temp.root.resolve("new_a.txt").exists())
    }

    @Test fun successfulRenamePreservesBytesAndRemovesJournal() = runBlocking {
        val a = temp.newFile("a.txt").apply { writeText("A") }
        val journal = temp.newFolder("journals")
        val engine = ExplorerRename(journalDirectory = journal)
        val result = engine.apply(engine.preview(listOf(a), ExplorerRenameOptions(prefix = "new_")))
        assertTrue(result.success)
        assertFalse(a.exists())
        assertEquals("A", temp.root.resolve("new_a.txt").readText())
        assertTrue(engine.pendingRecovery().isEmpty())
    }

    @Test fun interruptedProcessLeavesDurableRecoveryDetails() = runBlocking {
        val a = temp.newFile("a.txt").apply { writeText("A") }
        val b = temp.newFile("b.txt").apply { writeText("B") }
        val journal = temp.newFolder("journals")
        var count = 0
        val engine = ExplorerRename(journalDirectory = journal) { from, to ->
            if (++count == 2) throw AssertionError("simulate process interruption")
            Files.move(from.toPath(), to.toPath())
        }
        try {
            engine.apply(engine.preview(listOf(a, b), ExplorerRenameOptions(prefix = "new_")))
            fail("Expected interruption")
        } catch (_: AssertionError) { }
        val records = ExplorerRename(journalDirectory = journal).pendingRecovery()
        assertEquals(2, records.size)
        assertTrue(records.any { it.contains(a.absolutePath) })
        assertTrue(temp.root.listFiles()!!.any { it.name.startsWith(".mydoc-rename-") && it.readText() == "A" })
        assertEquals("B", b.readText())
    }
    @Test fun failedRollbackKeepsJournalPointingAtActualFiles() = runBlocking {
        val a = temp.newFile("a.txt").apply { writeText("A") }
        val b = temp.newFile("b.txt").apply { writeText("B") }
        val journal = temp.newFolder("journals")
        var count = 0
        val engine = ExplorerRename(journalDirectory = journal) { from, to ->
            count++
            if (count == 4 || count >= 6) throw IOException("injected failure")
            Files.move(from.toPath(), to.toPath())
        }
        val result = engine.apply(engine.preview(listOf(a, b), ExplorerRenameOptions(prefix = "new_")))
        assertFalse(result.success)
        assertTrue(result.unresolvedPaths.isNotEmpty())
        val records = engine.pendingRecovery().joinToString("\n")
        result.unresolvedPaths.forEach { path ->
            assertTrue(java.io.File(path).exists())
            assertTrue(records.contains(path))
        }
    }

}
