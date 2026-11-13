package com.abhisheksati.mydoc.tools.cleaner

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

class StorageCleanerCategoryDedupeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun sharedFileIsCountedInOneCategoryOnly() {
        val dupApk = tempFolder.newFile("shared.apk").apply { writeBytes(ByteArray(8192)) }
        val keeper = tempFolder.newFile("keeper.bin").apply { writeBytes(ByteArray(8192)) }
        val plainApk = tempFolder.newFile("plain.apk").apply { writeBytes(ByteArray(4096)) }
        val bigVideo = tempFolder.newFile("big.mp4").apply { writeBytes(ByteArray(16384)) }
        val temp = tempFolder.newFile("junk.tmp").apply { writeBytes(ByteArray(2048)) }

        val result = StorageCleanScanResult(
            duplicateGroups = listOf(DuplicateGroup("hash", 8192, keeper, listOf(dupApk))),
            largeFiles = listOf(dupApk, bigVideo),
            apkFiles = listOf(dupApk, plainApk),
            tempFiles = listOf(temp)
        )

        val deduped = result.dedupedCategories()

        assertFalse("duplicate copy must stay attributed to the duplicate subtitle", deduped.apkFiles.contains(dupApk))
        assertFalse("duplicate copy must not be counted again as a large file", deduped.largeFiles.contains(dupApk))
        assertEquals(listOf(plainApk), deduped.apkFiles)
        assertEquals(listOf(bigVideo), deduped.largeFiles)
        assertEquals(listOf(temp), deduped.tempFiles)

        val categoryBytes = deduped.apkFiles.sumOf { it.length() } +
            deduped.largeFiles.sumOf { it.length() } +
            deduped.tempFiles.sumOf { it.length() } +
            result.duplicateGroups.sumOf { it.wastedBytes }
        val unionBytes = listOf(dupApk, plainApk, bigVideo, temp).distinctBy { it.absolutePath }.sumOf { it.length() }
        assertEquals("subtitle bytes must sum to the union, not double-count shared files", unionBytes, categoryBytes)
    }

    @Test
    fun duplicateApkCopyIsExcludedFromApkSubtitleAfterRealScan() = runBlocking {
        val root = tempFolder.newFolder("apk_dup_root")
        val content = ByteArray(8192) { (it % 32).toByte() }
        File(root, "app_v1.apk").apply { writeBytes(content) }
        File(root, "app_v1_copy.apk").apply { writeBytes(content) }

        val result = StorageCleanerManager().scanDirectory(root)

        assertEquals(2, result.apkFiles.size)
        assertEquals(1, result.duplicateGroups.size)
        val deduped = result.dedupedCategories()
        assertEquals("one APK is a duplicate copy and belongs to the duplicate subtitle", 1, deduped.apkFiles.size)
        assertEquals(result.duplicateGroups.first().original.absolutePath, deduped.apkFiles.single().absolutePath)
    }

    @Test
    fun largeApkIsAttributedToApkCategoryNotLargeSubtitle() = runBlocking {
        val root = tempFolder.newFolder("large_apk_root")
        val big = File(root, "big.apk")
        RandomAccessFile(big, "rw").use { it.setLength(StorageCleanerManager.LARGE_FILE_THRESHOLD_BYTES) }

        val result = StorageCleanerManager().scanDirectory(root)

        assertTrue(result.largeFiles.any { it.name == "big.apk" })
        assertTrue(result.apkFiles.any { it.name == "big.apk" })
        val deduped = result.dedupedCategories()
        assertTrue(deduped.apkFiles.any { it.name == "big.apk" })
        assertFalse("large APK bytes are already in the APK subtitle", deduped.largeFiles.any { it.name == "big.apk" })
    }
}