package com.abhisheksati.mydoc.data.repository

import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * A failed ZIP extraction must not lose the fact that its rollback also failed: a REPLACE
 * whose restore fails can leave the original only in its .bak_* file, and the old code threw
 * that failure away and reported only the extraction error. These tests pin both halves:
 * every rollback step runs and its failure is collected, and the surfaced error names the
 * recovery files while keeping the original failure as the cause.
 */
class ZipRollbackRecoveryTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private fun repo(): StorageRepositoryImpl {
        val root = tempFolder.root
        val context = object : ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File = File(root, "app_external").apply { mkdirs() }
            override fun getFilesDir(): File = File(root, "app_internal").apply { mkdirs() }
        }
        return StorageRepositoryImpl(context, null)
    }

    @Test
    fun `rollback runs newest first and collects a failing step without skipping the rest`() = runBlocking {
        val order = mutableListOf<String>()
        val restoreFailure = IOException("Could not restore report.pdf from its backup")

        val failures = repo().runZipRollbackActions(listOf(
            { order += "oldest" },
            { order += "newest"; throw restoreFailure },
            { order += "middle" }
        ))

        assertEquals(listOf("middle", "newest", "oldest"), order)
        assertEquals(listOf<Throwable>(restoreFailure), failures)
    }

    @Test
    fun `a clean rollback reports no failures`() = runBlocking {
        val failures = repo().runZipRollbackActions(listOf({}, {}))
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `without rollback problems the original failure is returned unchanged`() {
        val original = IOException("The archive is corrupt")
        assertSame(original, repo().zipFailureWithRollbackDetails(original, emptyList()))
    }

    @Test
    fun `with rollback problems the surfaced error names the recovery files and keeps the cause`() {
        val original = IOException("The archive is corrupt")
        val rollbackFailure = IOException("Could not restore report.pdf from its backup")

        val surfaced = repo().zipFailureWithRollbackDetails(original, listOf(rollbackFailure))

        assertNotSame("a recovery warning must not read like a plain extraction failure", original, surfaced)
        assertSame("the original failure stays attached as the cause", original, surfaced.cause)
        assertTrue(surfaced.message!!.contains(".bak_"))
        assertTrue(surfaced.message!!.contains(".tmp_"))
        assertTrue(surfaced.message!!.contains("could not be put back"))
    }
}
