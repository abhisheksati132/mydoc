package com.abhisheksati.mydoc.tools.cleaner

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageCleanerManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cleanerManager: StorageCleanerManager

    @Before
    fun setup() {
        cleanerManager = StorageCleanerManager()
    }

    @Test
    fun testEmptyFolderDetection() = runBlocking {
        val root = tempFolder.newFolder("clean_test_root")
        val emptyDir1 = File(root, "empty1").apply { mkdir() }
        val emptyDir2 = File(root, "empty2").apply { mkdir() }
        val nonEmptyDir = File(root, "non_empty").apply {
            mkdir()
            File(this, "sample.txt").writeText("Hello world")
        }

        val result = cleanerManager.scanDirectory(root)

        assertTrue(result.emptyFolders.any { it.name == "empty1" })
        assertTrue(result.emptyFolders.any { it.name == "empty2" })
        assertFalse(result.emptyFolders.any { it.name == "non_empty" })
    }

    @Test
    fun testTempFileAndApkDetection() = runBlocking {
        val root = tempFolder.newFolder("clean_temp_test")
        val tmpFile = File(root, "cache.tmp").apply { writeBytes(ByteArray(8192)) }
        val logFile = File(root, "debug.log").apply { writeBytes(ByteArray(4096)) }
        val apkFile = File(root, "app_v1.apk").apply { writeBytes(ByteArray(10240)) }
        val docFile = File(root, "doc.pdf").apply { writeBytes(ByteArray(5000)) }

        val result = cleanerManager.scanDirectory(root)

        assertTrue(result.tempFiles.any { it.name == "cache.tmp" })
        assertTrue(result.tempFiles.any { it.name == "debug.log" })
        assertTrue(result.apkFiles.any { it.name == "app_v1.apk" })
        assertFalse(result.tempFiles.any { it.name == "doc.pdf" })
    }

    @Test
    fun testDuplicateDetection() = runBlocking {
        val root = tempFolder.newFolder("clean_dup_test")
        val content = ByteArray(8192) { (it % 128).toByte() }

        val file1 = File(root, "orig.bin").apply { writeBytes(content) }
        val file2 = File(root, "copy.bin").apply { writeBytes(content) }
        val unique = File(root, "unique.bin").apply { writeBytes(ByteArray(8192) { 99.toByte() }) }

        val result = cleanerManager.scanDirectory(root)

        assertEquals(1, result.duplicateGroups.size)
        val dupGroup = result.duplicateGroups.first()
        assertEquals(8192L, dupGroup.size)
        assertEquals(1, dupGroup.duplicates.size)
        assertEquals(8192L, dupGroup.wastedBytes)
    }

    @Test
    fun testDuplicateDetectionComparesWholeFileNotJustPrefix() = runBlocking {
        val root = tempFolder.newFolder("clean_prefix_test")
        // Same size and an identical 2 MB prefix, different tail. A prefix-only hash
        // would call these duplicates and the cleaner would have deleted one.
        val sharedPrefix = ByteArray(2 * 1024 * 1024) { (it % 251).toByte() }
        val file1 = File(root, "keep.bin").apply {
            writeBytes(sharedPrefix + ByteArray(1024 * 1024) { 1 })
        }
        val file2 = File(root, "keep_too.bin").apply {
            writeBytes(sharedPrefix + ByteArray(1024 * 1024) { 2 })
        }

        val result = cleanerManager.scanDirectory(root)

        assertTrue(result.duplicateGroups.isEmpty())
        assertTrue(file1.exists() && file2.exists())
    }

    @Test
    fun testDeleteEmptyDirectoriesLeavesNonEmptyAlone() = runBlocking {
        val root = tempFolder.newFolder("clean_dir_test")
        val empty = File(root, "empty").apply { mkdir() }
        val nonEmpty = File(root, "non_empty").apply {
            mkdir()
            File(this, "keep.txt").writeText("keep")
        }

        val removed = cleanerManager.deleteEmptyDirectories(listOf(empty, nonEmpty))

        assertEquals(1, removed)
        assertFalse(empty.exists())
        assertTrue(nonEmpty.exists())
    }
}
