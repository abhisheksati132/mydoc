package com.abhisheksati.mydoc.data.repository

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Batch 1 of the v1.7.2 audit, on the filesystem that actually matters.
 *
 * The JVM tests for these paths run on a desktop filesystem. Android's storage is the one with
 * FUSE, per-app mounts and delete semantics that differ, and this is the code that touches a
 * user's originals, so the round trips are repeated here against the app's own external files
 * directory — the same place the Recycle Bin lives in production.
 */
@RunWith(AndroidJUnit4::class)
class StorageMoveTrashOnDeviceTest {

    private lateinit var context: Context
    private lateinit var root: File
    private lateinit var repository: StorageRepositoryImpl

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(context.getExternalFilesDir(null), "move-trash-test").apply {
            deleteRecursively()
            mkdirs()
        }
        repository = StorageRepositoryImpl(context)
    }

    private fun writeTree(dir: File): Map<String, String> {
        File(dir, "nested/deep").mkdirs()
        val content = mapOf(
            "top.txt" to "TOP",
            "nested/mid.txt" to "MID",
            "nested/deep/leaf.txt" to "LEAF"
        )
        content.forEach { (relative, text) -> File(dir, relative).writeText(text) }
        return content
    }

    @Test
    fun aNestedFolderMoveLandsWholeAndRemovesTheSource() = runBlocking {
        val source = File(root, "source").apply { mkdirs() }
        val target = File(root, "target").apply { mkdirs() }
        val folder = File(source, "docs").apply { mkdir() }
        val content = writeTree(folder)

        val result = repository.move(listOf(folder), target)

        assertEquals("the move must report one item", 1, result.getOrNull())
        assertFalse("the source folder must be gone", folder.exists())
        content.forEach { (relative, text) ->
            val moved = File(target, "docs/$relative")
            assertTrue("$relative did not arrive", moved.isFile)
            assertEquals("$relative changed on the way", text, moved.readText())
        }
    }

    @Test
    fun trashThenRestoreReturnsTheSameBytes() = runBlocking {
        val source = File(root, "roundtrip").apply { mkdirs() }
        val folder = File(source, "docs").apply { mkdir() }
        val content = writeTree(folder)
        val originalPaths = content.keys.map { File(folder, it).absolutePath }

        val trashed = repository.moveToTrash(listOf(folder)).getOrNull()
        assertTrue("nothing reached the Recycle Bin", trashed?.isNotEmpty() == true)
        assertFalse("the original is still in place after trashing", folder.exists())

        val restored = repository.restoreFromTrash(trashed!!)
        assertEquals(1, restored.getOrNull())

        val restoredFolder = File(source, "docs")
        assertTrue("restore did not bring the folder back", restoredFolder.isDirectory)
        content.forEach { (relative, text) ->
            val file = File(restoredFolder, relative)
            assertTrue("$relative was not restored", file.isFile)
            assertEquals("$relative came back with different bytes", text, file.readText())
        }
        assertTrue("a restored path was not where the record said",
            originalPaths.first().startsWith(restoredFolder.absolutePath))
    }

    @Test
    fun emptyingTheBinRemovesTheContentItClaimsToRemove() = runBlocking {
        val source = File(root, "empty-bin").apply { mkdirs() }
        val file = File(source, "doomed.txt").apply { writeText("DOOMED") }

        val trashed = repository.moveToTrash(listOf(file)).getOrNull()
        assertTrue(trashed?.size == 1)
        val binFile = trashed!!.first().trashFile
        assertTrue("the bin copy is not where the record says it is", binFile.isFile)

        val emptied = repository.emptyTrash()
        assertEquals(1, emptied.getOrNull())
        assertFalse("emptyTrash reported success but the content is still on disk", binFile.exists())
    }

    @Test
    fun aMissingBinEntryIsReportedRatherThanRestoredSilently() = runBlocking {
        val source = File(root, "ghost").apply { mkdirs() }
        val file = File(source, "ghost.txt").apply { writeText("GHOST") }

        val trashed = repository.moveToTrash(listOf(file)).getOrNull()
        assertTrue(trashed?.size == 1)
        trashed!!.first().trashFile.delete()

        val result = repository.restoreFromTrash(trashed)
        assertTrue("a destroyed bin entry must not read as a restore", result.isFailure)
        assertEquals(
            "the failure must say that nothing was restored",
            0,
            (result.exceptionOrNull() as? com.abhisheksati.mydoc.domain.model
                .PartialStorageOperationException)?.completed
        )
    }
}
