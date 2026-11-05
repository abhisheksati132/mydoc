package com.abhisheksati.mydoc.data.repository

import com.abhisheksati.mydoc.domain.model.PartialStorageOperationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
import java.util.zip.ZipFile
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

    /**
     * R3: the archive was built in a private `.`-prefixed `.tmp` name that nothing ever removed. A
     * compression that failed or was cancelled left that file — and, when the rename fell back to a
     * copy, a half-written `.zip` beside it. Both look like output to the next listing.
     */
    @Test
    fun `a cancelled archive leaves nothing behind`() {
        lateinit var work: File
        var outcome: Result<*>? = null
        runBlocking {
            work = tempFolder.newFolder("zipcancel")
            val sources = listOf(
                File(work, "a.txt").apply { writeText("aaaaaaaaaa") },
                File(work, "b.txt").apply { writeText("bbbbbbbbbb") },
                File(work, "c.txt").apply { writeText("cccccccccc") }
            )
            val dest = File(work, "bundle.zip")

            // Cancelled as a child, not as the runBlocking scope: cancelling the test's own job
            // makes runBlocking rethrow before any assertion below can run.
            launch {
                outcome = runCatching {
                    repo().zip(sources, dest) { current, _ -> if (current == 2) cancel() }
                }
            }.join()
        }

        val captured = outcome ?: error("the zip call produced no outcome at all")
        assertTrue(
            "a cancelled zip must propagate cancellation, not return a result: $captured",
            captured.exceptionOrNull() is CancellationException
        )
        assertNothingButTheSourcesLeft(work)
    }

    @Test
    fun `a zip that fails midway takes its own output back`() = runBlocking {
        val work = tempFolder.newFolder("zipfail")
        val first = File(work, "a.txt").apply { writeText("aaaaaaaaaa") }
        val vanishing = File(work, "b.txt").apply { writeText("bbbbbbbbbb") }
        val third = File(work, "c.txt").apply { writeText("cccccccccc") }
        val dest = File(work, "bundle.zip")

        // The file disappears after the folder was enumerated but before it is read: the honest
        // outcome is a failure with no archive, not a truncated zip that opens on two entries.
        val result = repo().zip(listOf(first, vanishing, third), dest) { current, _ ->
            if (current == 1) vanishing.delete()
        }

        assertTrue("the vanished entry must be reported as a failure: $result", result.isFailure)
        assertNothingButTheSourcesLeft(work)
        assertTrue("the sources are never the archive's collateral", first.exists() && third.exists())
    }

    @Test
    fun `a zip that succeeds keeps its archive and drops the staging name`() = runBlocking {
        val work = tempFolder.newFolder("zipok")
        val sources = listOf(
            File(work, "a.txt").apply { writeText("hello") },
            File(work, "b.txt").apply { writeText("there") }
        )
        val dest = File(work, "bundle.zip")

        val result = repo().zip(sources, dest) { _, _ -> }

        val archive = result.getOrThrow()
        val names = ZipFile(archive).use { zip -> zip.entries().asSequence().map { it.name }.toSet() }
        assertEquals(setOf("a.txt", "b.txt"), names)
        assertNothingButTheSourcesLeft(work)
    }

    private fun assertNothingButTheSourcesLeft(work: File) {
        val strays = work.listFiles()?.filter { it.name.startsWith(".") || it.name.endsWith(".tmp") }
            ?.map { it.name }
            ?: emptyList()
        assertEquals("the staging name survived the run: $strays", emptyList<String>(), strays)
        val archive = File(work, "bundle.zip")
        if (archive.exists()) {
            // An archive that exists must be a whole one: a truncated zip is the failure this
            // guard exists to prevent, so reading it here is the assertion.
            ZipFile(archive).use { assertTrue("the archive holds no entries", it.size() > 0) }
        }
    }
}
