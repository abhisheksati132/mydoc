package com.abhisheksati.mydoc.data.repository

import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.FileSortOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertTrue(hash != null && hash.isNotEmpty())
        assertEquals(64, hash?.length) // SHA-256 is 64 hex characters

        // An unreadable file has no checksum; it must not return a string that could be
        // mistaken for one, or compare equal to another failure.
        assertNull(repo.calculateHash(File(renamedFile.parent, "never-existed.txt"), "SHA-256"))

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

    @Test
    fun testInvalidFileNames() = runBlocking {
        val root = tempFolder.root
        val repo = StorageRepositoryImpl(mockContext())

        // Names with illegal characters should fail
        assertTrue(repo.createDirectory(root, "invalid/name").isFailure)
        assertTrue(repo.createDirectory(root, "invalid:name").isFailure)
        assertTrue(repo.createDirectory(root, "invalid*name").isFailure)
        assertTrue(repo.createDirectory(root, "invalid?name").isFailure)
        assertTrue(repo.createFile(root, "invalid\"name.txt").isFailure)
        assertTrue(repo.createFile(root, "invalid<name.txt").isFailure)
        assertTrue(repo.createFile(root, "invalid|name.txt").isFailure)
        assertTrue(repo.createFile(root, "").isFailure)
        assertTrue(repo.createFile(root, ".").isFailure)
        assertTrue(repo.createFile(root, "..").isFailure)
    }

    @Test
    fun testCyclicCopyPrevention() = runBlocking {
        val root = tempFolder.root
        val repo = StorageRepositoryImpl(mockContext())

        val parentDir = repo.createDirectory(root, "ParentFolder").getOrThrow()
        val subDir = repo.createDirectory(parentDir, "SubFolder").getOrThrow()

        // Attempting to copy parent into its own child should be safely skipped without infinite recursion
        val copyRes = repo.copy(listOf(parentDir), subDir)
        assertTrue(copyRes.isSuccess)
        assertEquals(0, copyRes.getOrThrow()) // Skipped to prevent cyclic loop
    }

    @Test
    fun testTrashAndRestoreFlow() = runBlocking {
        val root = tempFolder.root
        val repo = StorageRepositoryImpl(mockContext())

        // 1. Create file to trash
        val testFile = File(root, "important_doc.pdf").apply { writeText("Important Data") }
        assertTrue(testFile.exists())

        // 2. Move to Trash
        val trashRes = repo.moveToTrash(listOf(testFile))
        assertTrue(trashRes.isSuccess)
        val trashedItems = trashRes.getOrThrow()
        assertEquals(1, trashedItems.size)
        assertFalse(testFile.exists()) // Original moved
        assertTrue(trashedItems[0].trashFile.exists()) // In trash

        // 3. Restore from Trash
        val restoreRes = repo.restoreFromTrash(trashedItems)
        assertTrue(restoreRes.isSuccess)
        assertEquals(1, restoreRes.getOrThrow())
        assertTrue(testFile.exists()) // Restored
        assertEquals("Important Data", testFile.readText())

        // 4. Move to Trash again and Empty
        val trashRes2 = repo.moveToTrash(listOf(testFile))
        assertTrue(trashRes2.isSuccess)
        val emptyRes = repo.emptyTrash()
        assertTrue(emptyRes.isSuccess)
        assertEquals(1, emptyRes.getOrThrow())
        assertFalse(testFile.exists())
    }

    @Test
    fun testListDirectorySortingAndHiddenFilter() = runBlocking {
        val root = tempFolder.root
        val repo = StorageRepositoryImpl(mockContext())
        val testDir = File(root, "SortTestDir").apply { mkdirs() }

        // Create folders and files
        File(testDir, "FolderB").mkdirs()
        File(testDir, "FolderA").mkdirs()
        File(testDir, ".hiddenFolder").mkdirs()

        val f1 = File(testDir, "apple.txt").apply { writeText("short") }
        val f2 = File(testDir, "banana.pdf").apply { writeText("much longer content to test size sorting") }
        val f3 = File(testDir, "cherry.jpg").apply { writeText("medium text content") }
        val hiddenFile = File(testDir, ".secret.txt").apply { writeText("secret") }

        // 1. Verify hidden filter
        val listNoHidden = repo.listDirectory(testDir, showHidden = false, FileSortOrder(FileSortField.NAME, isAscending = true))
        assertFalse(listNoHidden.any { it.name.startsWith(".") })
        assertEquals(5, listNoHidden.size) // 2 folders + 3 files

        val listWithHidden = repo.listDirectory(testDir, showHidden = true, FileSortOrder(FileSortField.NAME, isAscending = true))
        assertTrue(listWithHidden.any { it.name == ".secret.txt" })
        assertTrue(listWithHidden.any { it.name == ".hiddenFolder" })
        assertEquals(7, listWithHidden.size) // 3 folders + 4 files

        // 2. Directories always appear first
        assertTrue(listNoHidden[0].isDirectory)
        assertTrue(listNoHidden[1].isDirectory)
        assertFalse(listNoHidden[2].isDirectory)

        // 3. Name Ascending: FolderA, FolderB, then apple, banana, cherry
        val nameAsc = repo.listDirectory(testDir, showHidden = false, FileSortOrder(FileSortField.NAME, isAscending = true))
        assertEquals("FolderA", nameAsc[0].name)
        assertEquals("FolderB", nameAsc[1].name)
        assertEquals("apple.txt", nameAsc[2].name)
        assertEquals("banana.pdf", nameAsc[3].name)
        assertEquals("cherry.jpg", nameAsc[4].name)

        // 4. Name Descending
        val nameDesc = repo.listDirectory(testDir, showHidden = false, FileSortOrder(FileSortField.NAME, isAscending = false))
        assertEquals("FolderB", nameDesc[0].name)
        assertEquals("FolderA", nameDesc[1].name)
        assertEquals("cherry.jpg", nameDesc[2].name)
        assertEquals("banana.pdf", nameDesc[3].name)
        assertEquals("apple.txt", nameDesc[4].name)

        // 5. Size Ascending (files: apple < cherry < banana)
        val sizeAsc = repo.listDirectory(testDir, showHidden = false, FileSortOrder(FileSortField.SIZE, isAscending = true))
        val filesOnly = sizeAsc.filter { !it.isDirectory }
        assertEquals("apple.txt", filesOnly[0].name)
        assertEquals("cherry.jpg", filesOnly[1].name)
        assertEquals("banana.pdf", filesOnly[2].name)

        // 6. Type Ascending (extensions: jpg < pdf < txt)
        val typeAsc = repo.listDirectory(testDir, showHidden = false, FileSortOrder(FileSortField.TYPE, isAscending = true))
        val typeFilesOnly = typeAsc.filter { !it.isDirectory }
        assertEquals("cherry.jpg", typeFilesOnly[0].name)
        assertEquals("banana.pdf", typeFilesOnly[1].name)
        assertEquals("apple.txt", typeFilesOnly[2].name)
    }

    @Test
    fun testCopyNameCollisionHandling() = runBlocking {
        val root = tempFolder.root
        val repo = StorageRepositoryImpl(mockContext())

        val srcDir = File(root, "CollisionSrc").apply { mkdirs() }
        val targetDir = File(root, "CollisionTarget").apply { mkdirs() }

        val original = File(srcDir, "doc.pdf").apply { writeText("Version 1") }
        // Pre-create same name in target
        File(targetDir, "doc.pdf").apply { writeText("Existing target version") }

        // Copy original to targetDir with collision
        val copyRes = repo.copy(listOf(original), targetDir)
        assertTrue(copyRes.isSuccess)
        assertEquals(1, copyRes.getOrThrow())

        // Target should now contain original doc.pdf AND non-conflicting copy "doc (1).pdf"
        val existing = File(targetDir, "doc.pdf")
        val copy1 = File(targetDir, "doc (1).pdf")
        assertTrue(existing.exists())
        assertEquals("Existing target version", existing.readText())
        assertTrue(copy1.exists())
        assertEquals("Version 1", copy1.readText())
    }

    private fun mockContext(): android.content.Context {
        val root = tempFolder.root
        return object : android.content.ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File = File(root, "app_external").apply { mkdirs() }
            override fun getFilesDir(): File = File(root, "app_internal").apply { mkdirs() }
        }
    }
}
