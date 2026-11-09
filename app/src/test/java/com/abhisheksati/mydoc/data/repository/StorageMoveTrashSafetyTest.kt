package com.abhisheksati.mydoc.data.repository

import android.content.ContextWrapper
import com.abhisheksati.mydoc.data.local.dao.TrashDao
import com.abhisheksati.mydoc.data.local.entity.TrashEntity
import com.abhisheksati.mydoc.domain.model.PartialStorageOperationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Batch 1 of the v1.7.2 audit: the mutations that touch a user's originals.
 *
 * The defect was not that a move could fail. It was that a failed move took back the copy it had
 * just made after already deleting part of the source, so the content in that part existed
 * nowhere. Rename is forced to fail and named entries refuse deletion, so the cross-volume shape
 * of the bug is reproduced on any filesystem — including the CI runner, which is not the machine
 * this was written on.
 */
class StorageMoveTrashSafetyTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    /**
     * @param forceCopyPath when true, rename always fails, so every operation takes the
     *   copy → verify → delete route that used to destroy data.
     * @param refused entry names that refuse to be deleted.
     */
    private open class FakeMutator(
        val forceCopyPath: Boolean = true,
        /**
         * Predicates on the whole path, not just the name: the copy of a file has the same name
         * as its source, and a test that cannot tell them apart proves nothing about which one
         * was deleted.
         */
        val refuse: (File) -> Boolean = { false }
    ) : EntryMutator {
        val deleteAttempts = mutableListOf<File>()

        override fun rename(src: File, dest: File): Boolean {
            if (forceCopyPath) return false
            return DefaultEntryMutator.rename(src, dest)
        }

        override fun delete(entry: File): Boolean {
            deleteAttempts += entry
            if (refuse(entry)) return false
            return DefaultEntryMutator.delete(entry)
        }
    }

    private class InMemoryTrashDao : TrashDao {
        private val rows = MutableStateFlow<List<TrashEntity>>(emptyList())
        private var nextId = 1L

        override fun getAllTrashItems(): Flow<List<TrashEntity>> = rows
        override suspend fun getAllTrashItemsSync(): List<TrashEntity> = rows.value
        override suspend fun insert(item: TrashEntity): Long {
            val id = if (item.id != 0L) item.id else nextId++
            rows.value = listOf(item.copy(id = id)) + rows.value.filter { it.id != id }
            return id
        }

        override suspend fun getItemsOlderThan(thresholdTimestamp: Long): List<TrashEntity> =
            rows.value.filter { it.deletedTimestamp < thresholdTimestamp }

        override suspend fun deleteByIds(ids: List<Long>) {
            rows.value = rows.value.filterNot { it.id in ids }
        }

        override suspend fun clearAll() {
            rows.value = emptyList()
        }

        override suspend fun purgeOlderThan(thresholdTimestamp: Long) {
            rows.value = rows.value.filter { it.deletedTimestamp >= thresholdTimestamp }
        }
    }

    private fun repo(mutator: EntryMutator, dao: TrashDao? = null): StorageRepositoryImpl {
        val root = tempFolder.root
        val context = object : ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File = File(root, "app_external").apply { mkdirs() }
            override fun getFilesDir(): File = File(root, "app_internal").apply { mkdirs() }
        }
        return StorageRepositoryImpl(context, dao, mutator)
    }

    private fun partial(error: Throwable?): PartialStorageOperationException {
        assertTrue("expected a partial result, got $error", error is PartialStorageOperationException)
        return error as PartialStorageOperationException
    }

    private fun trashDir(): File = File(tempFolder.root, "app_external/.trash")

    @Test
    fun `a folder move keeps the copy when part of the source cannot be deleted`() = runBlocking {
        val source = tempFolder.newFolder("volume-a")
        val target = tempFolder.newFolder("volume-b")
        val folder = File(source, "notes").apply { mkdir() }
        File(folder, "alpha.txt").writeText("A".repeat(512))
        File(folder, "beta.txt").writeText("B".repeat(512))

        val mutator = FakeMutator(refuse = { it.name == "beta.txt" && it.parentFile == folder })
        val result = repo(mutator).move(listOf(folder), target)

        val copy = File(target, "notes")
        assertTrue("the destination was taken back after the source was half-deleted", copy.exists())
        assertTrue(
            "the child already removed from the source now exists nowhere",
            File(copy, "alpha.txt").exists()
        )
        assertFalse("alpha.txt must not still sit at the source as well", File(folder, "alpha.txt").exists())
        assertTrue(
            "the child that refused deletion must still be at the source",
            File(folder, "beta.txt").exists()
        )

        val failure = partial(result.exceptionOrNull())
        assertEquals(0, failure.completed)
        assertEquals(1, failure.failed)
        assertEquals(listOf("notes"), failure.retainedSources)
        assertNotNull(failure.cause)
    }

    @Test
    fun `a move whose source is untouched takes its own copy back`() = runBlocking {
        val source = tempFolder.newFolder("src-intact")
        val target = tempFolder.newFolder("dst-intact")
        val original = File(source, "locked.txt").apply { writeText("payload") }

        val result = repo(FakeMutator(refuse = { it == original })).move(listOf(original), target)

        assertFalse("an untouched original must not end up duplicated", File(target, "locked.txt").exists())
        assertTrue("the original must survive", original.exists())
        // Nothing completed and nothing is in two places, so this is an ordinary failure: it must
        // not be dressed up as a partial success, which would imply work the user can keep.
        assertFalse(
            "an untouched original is not a partial result",
            result.exceptionOrNull() is PartialStorageOperationException
        )
        assertTrue(original.readText() == "payload")
    }

    @Test
    fun `every child survives a move that could not finish`() = runBlocking {
        val source = tempFolder.newFolder("src-both")
        val target = tempFolder.newFolder("dst-both")
        val folder = File(source, "tree").apply { mkdir() }
        File(folder, "one.txt").writeText("111")
        File(folder, "two.txt").writeText("222")

        repo(FakeMutator(refuse = { it.name == "two.txt" && it.parentFile == folder }))
            .move(listOf(folder), target)

        for (name in listOf("one.txt", "two.txt")) {
            val inSource = File(folder, name).exists()
            val inCopy = File(target, "tree/$name").exists()
            assertTrue("$name exists nowhere after the move", inSource || inCopy)
        }
    }

    @Test
    fun `nothing is deleted from the source before the copy is in place`() = runBlocking {
        val source = tempFolder.newFolder("order-src")
        val target = tempFolder.newFolder("order-dst")
        val folder = File(source, "tree").apply { mkdir() }
        File(folder, "a.txt").writeText("A")
        File(folder, "b.txt").writeText("B")

        var copyCompleteAtFirstDelete: Boolean? = null
        val mutator = object : FakeMutator() {
            override fun delete(entry: File): Boolean {
                if (copyCompleteAtFirstDelete == null && entry.parentFile == folder) {
                    copyCompleteAtFirstDelete = File(target, "tree/a.txt").exists() &&
                        File(target, "tree/b.txt").exists() &&
                        File(target, "tree").isDirectory
                }
                return super.delete(entry)
            }
        }

        repo(mutator).move(listOf(folder), target)

        assertNotNull(
            "the source was never touched, so the ordering was not exercised",
            copyCompleteAtFirstDelete
        )
        assertTrue("a source child was deleted before the copy of the whole tree existed",
            copyCompleteAtFirstDelete == true)
    }

    @Test
    fun `a nested folder move preserves every child`() = runBlocking {
        val source = tempFolder.newFolder("deep-src")
        val target = tempFolder.newFolder("deep-dst")
        val tree = File(source, "project")
        File(tree, "a/b").mkdirs()
        File(tree, "a/b/leaf.txt").writeText("leaf")
        File(tree, "top.txt").writeText("top")

        val result = repo(FakeMutator(forceCopyPath = false)).move(listOf(tree), target)

        assertEquals(1, result.getOrNull())
        assertEquals("leaf", File(target, "project/a/b/leaf.txt").readText())
        assertEquals("top", File(target, "project/top.txt").readText())
        assertFalse("the source tree must be gone after a clean move", tree.exists())
    }

    @Test
    fun `a nested folder move over the copy path preserves every child`() = runBlocking {
        val source = tempFolder.newFolder("deep-src2")
        val target = tempFolder.newFolder("deep-dst2")
        val tree = File(source, "project")
        File(tree, "a/b").mkdirs()
        File(tree, "a/b/leaf.txt").writeText("leaf")
        File(tree, "top.txt").writeText("top")

        val result = repo(FakeMutator()).move(listOf(tree), target)

        assertEquals(1, result.getOrNull())
        assertEquals("leaf", File(target, "project/a/b/leaf.txt").readText())
        assertFalse("a clean copy move still removes the source", tree.exists())
    }

    @Test
    fun `an empty folder move still completes`() = runBlocking {
        val source = tempFolder.newFolder("empty-src")
        val target = tempFolder.newFolder("empty-dst")
        val empty = File(source, "nothing-here").apply { mkdir() }

        assertEquals(1, repo(FakeMutator()).move(listOf(empty), target).getOrNull())
        assertTrue(File(target, "nothing-here").isDirectory)
    }

    @Test
    fun `a source that disappears mid-batch is counted as failed`() = runBlocking {
        val source = tempFolder.newFolder("vanish-src")
        val target = tempFolder.newFolder("vanish-dst")
        val here = File(source, "here.txt").apply { writeText("H") }

        val result = repo(FakeMutator()).move(listOf(here, File(source, "gone.txt")), target)

        assertTrue(File(target, "here.txt").exists())
        val failure = partial(result.exceptionOrNull())
        assertEquals(1, failure.completed)
        assertEquals(1, failure.failed)
    }

    @Test
    fun `concurrent same name trash moves keep both copies`() = runBlocking {
        val dao = InMemoryTrashDao()
        val first = File(tempFolder.newFolder("first"), "notes.txt").apply { writeText("first") }
        val second = File(tempFolder.newFolder("second"), "notes.txt").apply { writeText("second") }
        val repository = repo(FakeMutator(), dao)

        val firstMove = async { repository.moveToTrash(listOf(first)).getOrThrow().single() }
        val secondMove = async { repository.moveToTrash(listOf(second)).getOrThrow().single() }
        val firstEntry = firstMove.await()
        val secondEntry = secondMove.await()

        assertTrue(firstEntry.trashFile != secondEntry.trashFile)
        assertEquals("first", firstEntry.trashFile.readText())
        assertEquals("second", secondEntry.trashFile.readText())
    }

    @Test
    fun `emptying the bin keeps the record of a file it could not delete`() = runBlocking {
        val dao = InMemoryTrashDao()
        val source = tempFolder.newFolder("trash-src")
        val keep = File(source, "keep.txt").apply { writeText("K") }
        val stuck = File(source, "stuck.txt").apply { writeText("S") }
        repo(FakeMutator(forceCopyPath = false), dao).moveToTrash(listOf(keep, stuck))
        assertEquals(2, dao.getAllTrashItemsSync().size)

        val stuckPath = dao.getAllTrashItemsSync().single { it.displayName == "stuck.txt" }.trashPath
        val result = repo(FakeMutator(refuse = { it.absolutePath == stuckPath }), dao).emptyTrash()

        val failure = partial(result.exceptionOrNull())
        assertEquals(1, failure.completed)
        assertEquals(1, failure.failed)
        assertEquals(
            "a record may only go with content that is provably gone",
            1,
            dao.getAllTrashItemsSync().size
        )
        assertTrue(
            "the surviving record still points at real content",
            File(dao.getAllTrashItemsSync().first().trashPath).exists()
        )
    }

    @Test
    fun `a trash batch reports items left behind instead of claiming success`() = runBlocking {
        val dao = InMemoryTrashDao()
        val source = tempFolder.newFolder("trash-partial")
        val folder = File(source, "half").apply { mkdir() }
        File(folder, "gone.txt").writeText("G")
        File(folder, "stuck.txt").writeText("S")

        val result = repo(FakeMutator(refuse = { it.name == "stuck.txt" && it.parentFile == folder }), dao)
            .moveToTrash(listOf(folder))

        val failure = partial(result.exceptionOrNull())
        assertEquals(listOf("half"), failure.retainedSources)
        assertTrue(
            "the bin copy is the only complete version and must stay",
            trashDir().listFiles()!!.any { it.isDirectory }
        )
        val rows = dao.getAllTrashItemsSync()
        assertEquals("the retained item keeps exactly one record", 1, rows.size)
        assertTrue(
            "that record must still point at the bin copy",
            File(rows.first().trashPath).exists()
        )
    }

    @Test
    fun `restoring a batch counts what could not be restored`() = runBlocking {
        val dao = InMemoryTrashDao()
        val source = tempFolder.newFolder("restore-src")
        val good = File(source, "good.txt").apply { writeText("G") }
        val bad = File(source, "bad.txt").apply { writeText("B") }
        repo(FakeMutator(forceCopyPath = false), dao).moveToTrash(listOf(good, bad))

        val rows = dao.getAllTrashItemsSync()
        assertEquals(2, rows.size)
        File(rows.first { it.displayName == "bad.txt" }.trashPath).delete()

        val result = repo(FakeMutator(forceCopyPath = false), dao).restoreFromTrash(rows.map { it.toDomain() })

        val failure = partial(result.exceptionOrNull())
        assertEquals(1, failure.completed)
        assertEquals(1, failure.failed)
        assertTrue("the restorable file is back where it came from", File(source, "good.txt").exists())
        assertEquals("a stale record may be dropped", 0, dao.getAllTrashItemsSync().size)
    }

    @Test
    fun `a restore that cannot reach the bin keeps every record`() = runBlocking {
        val dao = InMemoryTrashDao()
        val source = tempFolder.newFolder("restore-unavailable")
        val item = File(source, "keep-me.txt").apply { writeText("K") }
        repo(FakeMutator(forceCopyPath = false), dao).moveToTrash(listOf(item))
        val rows = dao.getAllTrashItemsSync()
        assertEquals(1, rows.size)

        // The bin lives under the app's external files dir; making that path a regular file is
        // how a removed or unmounted volume looks to getTrashDir(): it cannot be created at all.
        File(tempFolder.root, "app_external").deleteRecursively()
        File(tempFolder.root, "app_external").writeText("not a directory")

        val result = repo(FakeMutator(forceCopyPath = false), dao)
            .restoreFromTrash(rows.map { it.toDomain() })

        assertTrue("an unavailable bin must not be mistaken for lost content", result.isFailure)
        assertFalse(
            "an unavailable bin is not a partial success",
            result.exceptionOrNull() is PartialStorageOperationException
        )
        assertEquals(1, dao.getAllTrashItemsSync().size)
    }

    @Test
    fun `a cancelled move never destroys the only complete copy`() = runBlocking {
        val source = tempFolder.newFolder("cancel-src")
        val target = tempFolder.newFolder("cancel-dst")
        val folder = File(source, "tree").apply { mkdir() }
        File(folder, "one.txt").writeText("1")
        File(folder, "two.txt").writeText("2")
        File(folder, "three.txt").writeText("3")

        // Cancellation lands between two source deletions: one child is already gone, and the
        // copy is the only place it still exists. The old code deleted the copy on the way out.
        val mutator = object : FakeMutator() {
            override fun delete(entry: File): Boolean {
                val removed = super.delete(entry)
                if (removed && entry.name == "two.txt" && entry.parentFile == folder) {
                    throw kotlinx.coroutines.CancellationException("cancelled mid-deletion")
                }
                return removed
            }
        }

        val result = runCatching { repo(mutator).move(listOf(folder), target) }

        assertTrue("a cancelled move must not swallow the cancellation",
            result.exceptionOrNull() is kotlinx.coroutines.CancellationException)
        assertTrue(
            "the copy was taken back while part of the source was already gone",
            File(target, "tree").exists()
        )
        for (name in listOf("one.txt", "two.txt", "three.txt")) {
            assertTrue(
                "$name exists nowhere after the cancellation",
                File(folder, name).exists() || File(target, "tree/$name").exists()
            )
        }
    }
}
