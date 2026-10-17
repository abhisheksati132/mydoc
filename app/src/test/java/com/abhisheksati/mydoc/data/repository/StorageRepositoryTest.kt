package com.abhisheksati.mydoc.data.repository

import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.FileSortOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageRepositoryTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun testFileAndDirectoryCRUD() = runBlocking {
        val root = tempFolder.root
        // Create an anonymous instance for testing without Android Context dependencies for file I/O
        val repo = StorageRepositoryImpl(mockContext())

        // 1. Create Directory
        val dirRes = repo.createDirectory(root, "TestFolder")
        assertTrue(dirRes.isSuccess)
        val testDir = dirRes.getOrThrow()
        assertTrue(testDir.exists())
        assertTrue(testDir.isDirectory)

        // 2. Create File
        val fileRes = repo.createFile(testDir, "test.txt")
        assertTrue(fileRes.isSuccess)
        val testFile = fileRes.getOrThrow()
        assertTrue(testFile.exists())
        testFile.writeText("Hello MyDoc File Manager")

        // 3. Rename File
        val renameRes = repo.rename(testFile, "renamed.txt")
        assertTrue(renameRes.isSuccess)
        val renamedFile = renameRes.getOrThrow()
        assertEquals("renamed.txt", renamedFile.name)
        assertFalse(testFile.exists())
        assertTrue(renamedFile.exists())

        // 4. Calculate SHA-256 Checksum
        val hash = repo.calculateHash(renamedFile, "SHA-256")
        assertTrue(hash.isNotEmpty())
        assertEquals(64, hash.length) // SHA-256 is 64 hex characters

        // 5. Copy File
        val copyDestDir = repo.createDirectory(root, "CopyTarget").getOrThrow()
        val copyRes = repo.copy(listOf(renamedFile), copyDestDir)
        assertTrue(copyRes.isSuccess)
        assertEquals(1, copyRes.getOrThrow())
        val copiedFile = File(copyDestDir, "renamed.txt")
        assertTrue(copiedFile.exists())
        assertTrue(renamedFile.exists()) // Original still exists

        // 6. Move File
        val moveDestDir = repo.createDirectory(root, "MoveTarget").getOrThrow()
        val moveRes = repo.move(listOf(renamedFile), moveDestDir)
        assertTrue(moveRes.isSuccess)
        assertEquals(1, moveRes.getOrThrow())
        val movedFile = File(moveDestDir, "renamed.txt")
        assertTrue(movedFile.exists())
        assertFalse(renamedFile.exists()) // Original moved

        // 7. Folder Properties
        val props = repo.getFolderProperties(moveDestDir)
        assertEquals(1, props.totalFilesCount)
        assertEquals(0, props.totalFoldersCount)
        assertTrue(props.totalSizeBytes > 0)

        // 8. Delete
        val delRes = repo.delete(listOf(testDir, copyDestDir, moveDestDir))
        assertTrue(delRes.isSuccess)
        assertEquals(3, delRes.getOrThrow())
        assertFalse(testDir.exists())
        assertFalse(copyDestDir.exists())
        assertFalse(moveDestDir.exists())
    }

    @Test
    fun testZipAndUnzipArchive() = runBlocking {
        val root = tempFolder.root
        val repo = StorageRepositoryImpl(mockContext())

        // 1. Create folder with sample files
        val sourceFolder = File(root, "SourceFiles")
        sourceFolder.mkdirs()
        val file1 = File(sourceFolder, "sample1.txt").apply { writeText("Hello from Zip 1") }
        val file2 = File(sourceFolder, "sample2.txt").apply { writeText("Hello from Zip 2") }

        // 2. Compress to ZIP
        val targetZip = File(root, "archive.zip")
        val zipRes = repo.zip(listOf(file1, file2), targetZip)
        assertTrue(zipRes.isSuccess)
        val createdZip = zipRes.getOrThrow()
        assertTrue(createdZip.exists())
        assertTrue(createdZip.length() > 0)

        // 3. Extract ZIP
        val extractDir = File(root, "Extracted")
        val unzipRes = repo.unzip(createdZip, extractDir)
        assertTrue(unzipRes.isSuccess)
        val dest = unzipRes.getOrThrow()
        assertTrue(dest.exists())

        val extractedFile1 = File(dest, "sample1.txt")
        val extractedFile2 = File(dest, "sample2.txt")
        assertTrue(extractedFile1.exists())
        assertTrue(extractedFile2.exists())
        assertEquals("Hello from Zip 1", extractedFile1.readText())
        assertEquals("Hello from Zip 2", extractedFile2.readText())

        // 4. Test Search Files
        val searchHits = repo.searchFiles(extractDir, "sample1", recursive = false)
        assertEquals(1, searchHits.size)
        assertEquals("sample1.txt", searchHits[0].name)
    }


    private fun mockContext(): android.content.Context {
        // Simple proxy / mock for context if needed for getStorageVolumes
        return object : android.content.ContextWrapper(null) {}
    }
}
