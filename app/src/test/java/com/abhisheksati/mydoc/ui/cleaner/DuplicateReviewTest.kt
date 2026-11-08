package com.abhisheksati.mydoc.ui.cleaner

import com.abhisheksati.mydoc.tools.cleaner.DuplicateGroup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class DuplicateReviewTest {
    @Test fun nothingIsSelectedUntilTheUserChoosesAKeeper() {
        val group=DuplicateGroup("hash",3,File("a"),listOf(File("b")))
        assertTrue(duplicateCopies(listOf(group),emptyMap()).isEmpty())
        assertTrue(duplicateCopies(listOf(group),mapOf("hash" to "not-a-member")).isEmpty())
        assertEquals(listOf(File("a")),duplicateCopies(listOf(group),mapOf("hash" to File("b").absolutePath)))
    }
    @Test fun changedCopiesAndMissingKeepersAreNeverApprovedForTrash() = runBlocking {
        val dir=kotlin.io.path.createTempDirectory("duplicates-").toFile()
        try {
            val a=File(dir,"a").apply { writeText("same") }
            val b=File(dir,"b").apply { writeText("same") }
            val hash=MessageDigest.getInstance("SHA-256").digest(a.readBytes()).joinToString(""){"%02x".format(it)}
            val group=DuplicateGroup(hash,4,a,listOf(b))
            val keep=mapOf(hash to b.absolutePath)
            assertEquals(setOf(a.absolutePath),verifiedDuplicateCopies(listOf(group),keep))
            a.writeText("else")
            assertTrue(verifiedDuplicateCopies(listOf(group),keep).isEmpty())
            a.writeText("same");b.delete()
            assertTrue(verifiedDuplicateCopies(listOf(group),keep).isEmpty())
            assertTrue(a.exists())
        } finally { dir.deleteRecursively() }
    }
}
