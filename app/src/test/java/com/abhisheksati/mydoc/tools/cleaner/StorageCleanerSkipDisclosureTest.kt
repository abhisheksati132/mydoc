package com.abhisheksati.mydoc.tools.cleaner

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

class StorageCleanerSkipDisclosureTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun sparseFile(name: String, size: Long): File {
        val file = tempFolder.newFile(name)
        RandomAccessFile(file, "rw").use { it.setLength(size) }
        return file
    }

    @Test
    fun oversizedFilesAreCountedNotSilentlyOmitted() = runBlocking {
        val root = tempFolder.newFolder("oversized_root")
        val over = StorageCleanerManager.MAX_DUPLICATE_SIZE_BYTES + 1
        val a = File(root, "huge_a.bin").also { RandomAccessFile(it, "rw").use { r -> r.setLength(over) } }
        val b = File(root, "huge_b.bin").also { RandomAccessFile(it, "rw").use { r -> r.setLength(over) } }

        val result = StorageCleanerManager().scanDirectory(root)

        assertEquals(2, result.duplicateScanSkips.oversized)
        assertEquals(0, result.duplicateScanSkips.unreadable)
        assertTrue("oversized files must never be hashed into duplicate groups", result.duplicateGroups.isEmpty())
        assertTrue(a.exists() && b.exists())
    }

    @Test
    fun unreadableHashIsCountedAndNeverFormsADuplicate() = runBlocking {
        val root = tempFolder.newFolder("unreadable_root")
        val content = ByteArray(8192) { (it % 64).toByte() }
        val locked = File(root, "locked.bin").apply { writeBytes(content) }
        File(root, "readable.bin").apply { writeBytes(content) }

        val manager = StorageCleanerManager(hasher = { file ->
            if (file.name == "locked.bin") null else "same-hash"
        })
        val result = manager.scanDirectory(root)

        assertEquals(1, result.duplicateScanSkips.unreadable)
        assertEquals(0, result.duplicateScanSkips.oversized)
        assertTrue("a file that failed hashing must not be declared a duplicate", result.duplicateGroups.isEmpty())
        assertTrue(locked.exists())
    }

    @Test
    fun readableDuplicatesStillDetectedWithZeroSkips() = runBlocking {
        val root = tempFolder.newFolder("clean_root")
        val content = ByteArray(8192) { (it % 64).toByte() }
        File(root, "orig.bin").apply { writeBytes(content) }
        File(root, "copy.bin").apply { writeBytes(content) }

        val result = StorageCleanerManager().scanDirectory(root)

        assertEquals(1, result.duplicateGroups.size)
        assertEquals(0, result.duplicateScanSkips.total)
    }

    @Test
    fun smallFilesBelowCandidateFloorAreNotReportedAsOversized() = runBlocking {
        val root = tempFolder.newFolder("small_root")
        File(root, "tiny.bin").apply { writeBytes(ByteArray(100)) }

        val result = StorageCleanerManager().scanDirectory(root)

        assertEquals(0, result.duplicateScanSkips.oversized)
        assertEquals(0, result.duplicateScanSkips.unreadable)
    }
}