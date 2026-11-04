package com.abhisheksati.mydoc.data.repository

import com.abhisheksati.mydoc.domain.model.PartialStorageOperationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * These four operations touch the user's originals, so the guarantee that matters is not that
 * they succeed but that they never lie: no duplicate left behind after a move, no half-extracted
 * folder reported as a failure that wrote nothing, no batch counted as pasted when items were
 * skipped. Each test below failed against the previous implementation.
 */
class StorageOperationSafetyTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private fun repo(): StorageRepositoryImpl {
        val root = tempFolder.root
        val context = object : android.content.ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File = File(root, "app_external").apply { mkdirs() }
            override fun getFilesDir(): File = File(root, "app_internal").apply { mkdirs() }
        }
        return StorageRepositoryImpl(context)
    }

    @Test
    fun `a copy counts the items that actually landed`() = runBlocking {
        val source = tempFolder.newFolder("source")
        val target = tempFolder.newFolder("target")
        File(source, "alpha.txt").writeText("A")
        File(source, "gamma.txt").writeText("G")
        val vanished = File(source, "beta.txt")

        val result = repo().copy(listOf(File(source, "alpha.txt"), vanished, File(source, "gamma.txt")), target)

        val error = result.exceptionOrNull()
        assertTrue("a split batch must not read as success: $result", error is PartialStorageOperationException)
        error as PartialStorageOperationException
        assertEquals(2, error.completed)
        assertEquals(1, error.failed)
        assertTrue(File(target, "alpha.txt").exists())
        assertTrue(File(target, "gamma.txt").exists())
        assertFalse("the item that could not be read must not appear copied", File(target, "beta.txt").exists())
    }

    @Test
    fun `a fully successful copy still reports its count`() = runBlocking {
        val source = tempFolder.newFolder("source2")
        val target = tempFolder.newFolder("target2")
        File(source, "one.txt").writeText("1")
        File(source, "two.txt").writeText("2")

        val result = repo().copy(listOf(File(source, "one.txt"), File(source, "two.txt")), target)

        assertEquals(2, result.getOrThrow())
    }

    @Test
    fun `a move never leaves the file in two places`() = runBlocking {
        val source = tempFolder.newFolder("source3")
        val target = tempFolder.newFolder("target3")
        val original = File(source, "locked.txt")
        original.writeText("only one copy may exist")

        // An open handle lets the copy succeed but blocks rename and delete on Windows, which is
        // exactly the state the old code reported as "Pasted 1 item" while the original stayed.
        FileChannel.open(original.toPath(), StandardOpenOption.READ).use { held ->
            val result = repo().move(listOf(original), target)

            val landed = target.listFiles()?.any { it.name.startsWith("locked.txt") } == true
            assertFalse(
                "a move that copied but could not delete left two copies: ${result.exceptionOrNull()}",
                original.exists() && landed
            )
            if (result.isFailure && !landed) {
                assertTrue("the original must survive a move that did not complete", original.exists())
            }
        }
    }

    @Test
    fun `an extraction that fails halfway takes its own output back`() = runBlocking {
        val work = tempFolder.newFolder("zipdir")
        val archive = File(work, "bomb.zip")
        val out = File(work, "out")

        ZipOutputStream(archive.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("first.txt"))
            zos.write("written before the failure".toByteArray())
            zos.closeEntry()
            // Six megabytes of zeros compresses to a few kilobytes: past the 5 MB floor the
            // ratio check stops this archive mid-entry, which is the halfway failure.
            zos.putNextEntry(ZipEntry("second.txt"))
            zos.write(ByteArray(6 * 1024 * 1024))
            zos.closeEntry()
        }

        val result = repo().unzip(archive, out)

        assertTrue("the bomb must still be refused", result.isFailure)
        assertFalse(
            "entries written before the failure were left behind as a half-extracted folder",
            File(out, "first.txt").exists()
        )
        assertFalse(File(out, "second.txt").exists())
    }
}
