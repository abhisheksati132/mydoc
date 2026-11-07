package com.abhisheksati.mydoc.data.repository

import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * The free-space guard belongs to the copy fallback, not to move() as a whole: a same-volume
 * rename writes no bytes, so a nearly full volume must not block it. A directory reporting 1 byte
 * usable reproduces that shape on any filesystem, including the CI runner.
 */
class StorageMoveSpaceTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private class LowSpaceDir(path: String) : File(path) {
        override fun getUsableSpace(): Long = 1L
    }

    private class FakeMutator(private val forceCopyPath: Boolean) : EntryMutator {
        override fun rename(src: File, dest: File): Boolean {
            if (forceCopyPath) return false
            return DefaultEntryMutator.rename(src, dest)
        }

        override fun delete(entry: File): Boolean = DefaultEntryMutator.delete(entry)
    }

    private fun repo(mutator: EntryMutator): StorageRepositoryImpl {
        val root = tempFolder.root
        val context = object : ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File = File(root, "app_external").apply { mkdirs() }
            override fun getFilesDir(): File = File(root, "app_internal").apply { mkdirs() }
        }
        return StorageRepositoryImpl(context, null, mutator)
    }

    @Test
    fun `a same-volume rename moves even when the volume reports no free space`() = runBlocking {
        val volume = tempFolder.newFolder("volume")
        val source = File(volume, "src").apply { mkdir() }
        val target = LowSpaceDir(File(volume, "dst").apply { mkdir() }.absolutePath)
        val file = File(source, "big.txt").apply { writeText("X".repeat(4096)) }

        val result = repo(FakeMutator(forceCopyPath = false)).move(listOf(file), target)

        assertEquals("a rename writes no bytes and must not need free space", 1, result.getOrNull())
        assertTrue(File(target, "big.txt").exists())
        assertFalse(file.exists())
    }

    @Test
    fun `the copy fallback is still refused on a full volume and the source stays intact`() = runBlocking {
        val volume = tempFolder.newFolder("volume")
        val source = File(volume, "src").apply { mkdir() }
        val target = LowSpaceDir(File(volume, "dst").apply { mkdir() }.absolutePath)
        val file = File(source, "big.txt").apply { writeText("payload".repeat(1024)) }

        val result = repo(FakeMutator(forceCopyPath = true)).move(listOf(file), target)

        assertTrue("a copy writes bytes and must still be refused", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(file.exists())
        assertEquals("payload".repeat(1024), file.readText())
        assertFalse("no partial copy may be left behind", File(target, "big.txt").exists())
    }
}