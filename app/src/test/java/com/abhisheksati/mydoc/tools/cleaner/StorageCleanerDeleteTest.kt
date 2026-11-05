package com.abhisheksati.mydoc.tools.cleaner

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * R2: the scan refuses entries that resolve outside its root, but the delete is a separate call
 * handed a list the sheet has been holding since the scan. It has to apply the same rule, or a
 * folder that moved, was replaced by a link, or was never in the scan at all gets removed.
 */
class StorageCleanerDeleteTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private val cleaner = StorageCleanerManager()

    private fun emptyFolderIn(parent: File, name: String): File =
        File(parent, name).apply { mkdirs() }

    @Test
    fun anEmptyFolderInsideTheScannedRootIsRemoved() = runBlocking {
        val root = tempFolder.newFolder("scanroot")
        val candidate = emptyFolderIn(root, "old-project")

        val removed = cleaner.deleteEmptyDirectories(listOf(candidate), root)

        assertEquals(1, removed)
        assertTrue("the folder the user approved is still there", !candidate.exists())
    }

    @Test
    fun aFolderOutsideTheScannedRootIsLeftAlone() = runBlocking {
        val root = tempFolder.newFolder("scanroot2")
        val elsewhere = emptyFolderIn(File(root.parentFile!! , "elsewhere2"), "tax-records")

        val removed = cleaner.deleteEmptyDirectories(listOf(elsewhere), root)

        assertEquals("a folder outside the scan was deleted", 0, removed)
        assertTrue(elsewhere.exists())
    }

    @Test
    fun theScannedRootItselfIsNeverTheVictim() = runBlocking {
        val root = tempFolder.newFolder("scanroot3")

        val removed = cleaner.deleteEmptyDirectories(listOf(root), root)

        assertEquals(0, removed)
        assertTrue(root.isDirectory)
    }

    @Test
    fun aPathThatResolvesOutsideThroughTheParentIsRefused() = runBlocking {
        val root = tempFolder.newFolder("scanroot4")
        val outsideName = "elsewhere4"
        val outside = emptyFolderIn(root.parentFile!!, outsideName)
        val sneaky = File(root, "../$outsideName")

        val removed = cleaner.deleteEmptyDirectories(listOf(sneaky), root)

        assertEquals("the `..` id reached outside the root", 0, removed)
        assertTrue(outside.exists())
    }

    @Test
    fun aFolderThatIsNoLongerEmptyIsRefused() = runBlocking {
        val root = tempFolder.newFolder("scanroot5")
        val candidate = emptyFolderIn(root, "notes")
        File(candidate, "keep.txt").writeText("appeared after the scan")

        val removed = cleaner.deleteEmptyDirectories(listOf(candidate), root)

        assertEquals(0, removed)
        assertTrue("a folder that gained content must survive", candidate.exists())
        assertTrue(File(candidate, "keep.txt").exists())
    }

    @Test
    fun aProtectedSystemFolderIsRefusedEvenInsideTheRoot() = runBlocking {
        val root = tempFolder.newFolder("scanroot6")
        val alarms = emptyFolderIn(root, "Alarms")

        val removed = cleaner.deleteEmptyDirectories(listOf(alarms), root)

        assertEquals(0, removed)
        assertTrue(alarms.exists())
    }

    @Test
    fun nestedEmptyFoldersGoDeepestFirstAndTheParentFollows() = runBlocking {
        val root = tempFolder.newFolder("scanroot7")
        val child = emptyFolderIn(root, "a")
        val grandchild = emptyFolderIn(child, "b")

        val removed = cleaner.deleteEmptyDirectories(listOf(child, grandchild), root)

        assertEquals(2, removed)
        assertTrue(!child.exists() && !grandchild.exists())
    }
}
