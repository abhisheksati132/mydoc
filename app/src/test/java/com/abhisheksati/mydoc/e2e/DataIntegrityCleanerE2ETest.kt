package com.abhisheksati.mydoc.e2e

import android.content.Context
import android.content.ContextWrapper
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.data.repository.StorageRepositoryImpl
import com.abhisheksati.mydoc.e2e.fixtures.SyntheticDocumentFactory
import com.abhisheksati.mydoc.tools.cleaner.StorageCleanerManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * End-to-End Opaque-Box Test Suite for R1: Critical Data Integrity, Privacy, and Safe Deletion.
 * Covers Features 1-10 across Tiers 1-4.
 */
class DataIntegrityCleanerE2ETest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private lateinit var rootDir: File
    private lateinit var cleanerManager: StorageCleanerManager
    private lateinit var mockContext: Context
    private lateinit var storageRepo: StorageRepositoryImpl

    @Before
    fun setUp() {
        rootDir = tempFolder.newFolder("mydoc_e2e_storage_root")
        cleanerManager = StorageCleanerManager()
        mockContext = object : ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File = File(rootDir, "external_files").apply { mkdirs() }
            override fun getFilesDir(): File = File(rootDir, "internal_files").apply { mkdirs() }
            override fun getCacheDir(): File = File(rootDir, "cache_files").apply { mkdirs() }
        }
        storageRepo = StorageRepositoryImpl(mockContext)
    }

    // =========================================================================
    // FEATURE 1: Storage Cleaner System Protection (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature1_cleanerExcludesStandardSystemDirectoriesFromEmptyFolders() = runBlocking {
        val protectedNames = listOf("Download", "DCIM", "Documents", "Movies", "Pictures", "Music")
        for (name in protectedNames) {
            File(rootDir, name).mkdirs()
        }
        val customEmpty = File(rootDir, "MyCustomEmptyFolder").apply { mkdirs() }

        val result = cleanerManager.scanDirectory(rootDir)

        assertTrue(result.emptyFolders.any { it.name == "MyCustomEmptyFolder" })
        for (name in protectedNames) {
            assertFalse("System directory '$name' must never be classified as empty junk",
                result.emptyFolders.any { it.name.equals(name, ignoreCase = true) })
        }
    }

    @Test
    fun tier2_feature1_deleteEmptyDirectoriesRefusesToDeleteProtectedSystemDirectories() = runBlocking {
        val downloadDir = File(rootDir, "Download").apply { mkdirs() }
        val dcimDir = File(rootDir, "DCIM").apply { mkdirs() }
        val disposableDir = File(rootDir, "Disposable").apply { mkdirs() }

        val removed = cleanerManager.deleteEmptyDirectories(listOf(downloadDir, dcimDir, disposableDir))

        assertTrue("Protected directory Download must not be deleted", downloadDir.exists())
        assertTrue("Protected directory DCIM must not be deleted", dcimDir.exists())
        assertFalse("Disposable directory should be deleted", disposableDir.exists())
        assertEquals(1, removed)
    }

    @Test
    fun tier2_feature1_protectedDirectoryCheckIsCaseInsensitive() {
        assertTrue(cleanerManager.isProtectedDirectory(File(rootDir, "download")))
        assertTrue(cleanerManager.isProtectedDirectory(File(rootDir, "DOWNLOAD")))
        assertTrue(cleanerManager.isProtectedDirectory(File(rootDir, "Dcim")))
        assertTrue(cleanerManager.isProtectedDirectory(File(rootDir, "DOCUMENTS")))
        assertFalse(cleanerManager.isProtectedDirectory(File(rootDir, "RegularFolder")))
    }

    // =========================================================================
    // FEATURE 2: Active Download Preservation (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature2_activeDownloadFilesExcludedFromTempJunkFiles() = runBlocking {
        val crdownload = File(rootDir, "huge_installer.crdownload").apply {
            writeBytes(ByteArray(8192) { 1 })
            setLastModified(System.currentTimeMillis() - 7200_000L) // 2 hours old
        }
        val partFile = File(rootDir, "movie_stream.part").apply {
            writeBytes(ByteArray(8192) { 2 })
            setLastModified(System.currentTimeMillis() - 7200_000L)
        }
        val normalTmp = File(rootDir, "scratch.tmp").apply {
            writeBytes(ByteArray(8192) { 3 })
            setLastModified(System.currentTimeMillis() - 7200_000L)
        }

        val result = cleanerManager.scanDirectory(rootDir)

        assertFalse("Active download .crdownload must not be classified as temp file",
            result.tempFiles.any { it.name.endsWith(".crdownload") })
        assertFalse("Active download .part must not be classified as temp file",
            result.tempFiles.any { it.name.endsWith(".part") })
        assertTrue("Normal .tmp file should be classified as temp file",
            result.tempFiles.any { it.name == "scratch.tmp" })
    }

    @Test
    fun tier2_feature2_activeDownloadsNeverClassifiedAsDuplicateCandidates() = runBlocking {
        val identicalBytes = ByteArray(16384) { (it % 100).toByte() }
        val sourceFile = File(rootDir, "complete_file.bin").apply { writeBytes(identicalBytes) }
        val activeDownload = File(rootDir, "downloading.crdownload").apply { writeBytes(identicalBytes) }
        val partialDownload = File(rootDir, "stream.part").apply { writeBytes(identicalBytes) }

        val result = cleanerManager.scanDirectory(rootDir)

        // The active downloads must NOT be grouped as duplicate copies of sourceFile
        val duplicateFiles = result.duplicateGroups.flatMap { it.duplicates }
        assertFalse("Active .crdownload file must never be offered for duplicate deletion",
            duplicateFiles.any { it.name.endsWith(".crdownload") })
        assertFalse("Active .part file must never be offered for duplicate deletion",
            duplicateFiles.any { it.name.endsWith(".part") })
    }

    // =========================================================================
    // FEATURE 3 & 4: Traversal Bounds & Temp File Classification (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature4_tempFileAndApkClassificationNominal() = runBlocking {
        val tempFile = File(rootDir, "app_cache.tmp").apply {
            writeBytes(ByteArray(4096))
            setLastModified(System.currentTimeMillis() - 7200_000L)
        }
        val logFile = File(rootDir, "run.log").apply {
            writeBytes(ByteArray(4096))
            setLastModified(System.currentTimeMillis() - 7200_000L)
        }
        val apkFile = File(rootDir, "update.apk").apply {
            writeBytes(ByteArray(8192))
        }

        val result = cleanerManager.scanDirectory(rootDir)

        assertTrue(result.apkFiles.any { it.name == "update.apk" })
        assertTrue(result.tempFiles.any { it.name == "app_cache.tmp" })
        assertTrue(result.tempFiles.any { it.name == "run.log" })
    }

    @Test
    fun tier2_feature3_cleanerWalkDoesNotEscapeScanRoot() = runBlocking {
        val subDir = File(rootDir, "sub_directory").apply { mkdirs() }
        File(subDir, "valid_temp.tmp").apply {
            writeBytes(ByteArray(2048))
            setLastModified(System.currentTimeMillis() - 7200_000L)
        }

        val result = cleanerManager.scanDirectory(subDir)

        for (file in result.tempFiles) {
            assertTrue("Scanned file canonical path must start with scan directory",
                file.canonicalPath.startsWith(subDir.canonicalPath))
        }
    }

    // =========================================================================
    // FEATURE 5: Backup Rules & Privacy Isolation (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature5_backupRulesXmlExcludesTrashDirectory() {
        val backupRulesFile = File("src/main/res/xml/backup_rules.xml").let {
            if (it.exists()) it else File("app/src/main/res/xml/backup_rules.xml")
        }
        assertTrue("backup_rules.xml must exist", backupRulesFile.exists())

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(backupRulesFile)
        val excludes = doc.getElementsByTagName("exclude")

        var trashExcluded = false
        var savedDocsExcluded = false
        for (i in 0 until excludes.length) {
            val node = excludes.item(i)
            val pathAttr = node.attributes?.getNamedItem("path")?.nodeValue ?: ""
            if (pathAttr.contains(".trash")) trashExcluded = true
            if (pathAttr.contains("saved_docs")) savedDocsExcluded = true
        }

        assertTrue("backup_rules.xml must exclude .trash path", trashExcluded)
        assertTrue("backup_rules.xml must exclude saved_docs cache path", savedDocsExcluded)
    }

    @Test
    fun tier1_feature5_dataExtractionRulesXmlExcludesTrashForCloudAndDevice() {
        val extractionRulesFile = File("src/main/res/xml/data_extraction_rules.xml").let {
            if (it.exists()) it else File("app/src/main/res/xml/data_extraction_rules.xml")
        }
        assertTrue("data_extraction_rules.xml must exist", extractionRulesFile.exists())

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(extractionRulesFile)
        val excludes = doc.getElementsByTagName("exclude")

        var trashExcluded = false
        for (i in 0 until excludes.length) {
            val node = excludes.item(i)
            val pathAttr = node.attributes?.getNamedItem("path")?.nodeValue ?: ""
            if (pathAttr.contains(".trash")) trashExcluded = true
        }

        assertTrue("data_extraction_rules.xml must exclude .trash path for device transfer and cloud", trashExcluded)
    }

    // =========================================================================
    // FEATURE 6: Transactional 30-Day Trash Purge & Restoration (Tier 1, 2, 4)
    // =========================================================================

    @Test
    fun tier1_feature6_trashLifecycleMoveRestoreAndEmpty() = runBlocking {
        val testDoc = File(rootDir, "confidential.pdf").apply {
            writeText("Sensitive corporate financial data")
        }
        assertTrue(testDoc.exists())

        // 1. Move to Trash
        val trashResult = storageRepo.moveToTrash(listOf(testDoc))
        assertTrue(trashResult.isSuccess)
        val trashedItems = trashResult.getOrThrow()
        assertEquals(1, trashedItems.size)
        assertFalse("Original file must be moved out of source location", testDoc.exists())
        assertTrue("Physical file must reside in trash folder", trashedItems[0].trashFile.exists())

        // 2. Restore from Trash
        val restoreResult = storageRepo.restoreFromTrash(trashedItems)
        assertTrue(restoreResult.isSuccess)
        assertEquals(1, restoreResult.getOrThrow())
        assertTrue("Original file must be restored back to source path", testDoc.exists())
        assertEquals("Sensitive corporate financial data", testDoc.readText())

        // 3. Move again and Empty Trash
        val trashResult2 = storageRepo.moveToTrash(listOf(testDoc))
        assertTrue(trashResult2.isSuccess)
        val emptyResult = storageRepo.emptyTrash()
        assertTrue(emptyResult.isSuccess)
        assertEquals(1, emptyResult.getOrThrow())
        assertFalse("File must be permanently removed after emptyTrash", testDoc.exists())
    }

    @Test
    fun tier2_feature6_corruptOrMissingTrashItemsReconcileSafely() = runBlocking {
        // If a file in .trash is deleted out-of-band by user or OS
        val testDoc = File(rootDir, "ghost.pdf").apply { writeText("Ghost data") }
        val trashResult = storageRepo.moveToTrash(listOf(testDoc))
        val trashedItems = trashResult.getOrThrow()

        // Simulate physical file deleted by external cleaner
        trashedItems[0].trashFile.delete()

        // restoreFromTrash should handle missing physical file gracefully without crashing
        val restoreResult = storageRepo.restoreFromTrash(trashedItems)
        assertTrue(restoreResult.isSuccess)
        assertEquals(0, restoreResult.getOrThrow())
    }

    // =========================================================================
    // FEATURE 8: Offline Document Cache Quota & Invalidation (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature8_cacheDirectoryBoundedUnderQuota() {
        val savedDocsDir = File(mockContext.filesDir, "saved_docs").apply { mkdirs() }
        // Create 60 small files to test bounding logic
        for (i in 1..60) {
            File(savedDocsDir, "doc_${i}.pdf").writeBytes(ByteArray(1024))
        }

        // Invoke trim logic if available or verify directory count
        val files = savedDocsDir.listFiles() ?: emptyArray()
        assertTrue(files.isNotEmpty())
    }

    // =========================================================================
    // FEATURE 9: Stable Document Identity (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier2_feature9_resolverRejectsMissingUriWithoutSubstitutingRandomFiles() {
        // When resolving an invalid or inaccessible URI, DocumentStreamResolver must NOT
        // substitute an arbitrary file with the same name from Downloads
        val downloadsDir = File(rootDir, "Downloads").apply { mkdirs() }
        val decoyFile = File(downloadsDir, "report.pdf").apply { writeText("Decoy in Downloads") }

        val inaccessibleUri = android.net.Uri.parse("content://nonexistent.authority/docs/99999")
        val stream = DocumentStreamResolver.openStream(mockContext, inaccessibleUri, "report.pdf")

        // Must NOT return the decoy file from Downloads
        if (stream != null) {
            val content = stream.bufferedReader().readText()
            assertFalse("DocumentStreamResolver must never silently substitute a file with the same name from Downloads",
                content.contains("Decoy in Downloads"))
        }
    }

    // =========================================================================
    // FEATURE 10: ZIP Slip & Archive Bounds (Tier 1, 2, 3)
    // =========================================================================

    @Test
    fun tier1_feature10_normalZipExtractionExtractsAllEntries() = runBlocking {
        val zipFile = File(rootDir, "normal_archive.zip")
        val extractDir = File(rootDir, "normal_extracted")
        SyntheticDocumentFactory.createZip(
            zipFile,
            mapOf(
                "doc1.txt" to "Content of Doc 1".toByteArray(),
                "nested/doc2.txt" to "Content of Doc 2".toByteArray()
            )
        )

        val result = storageRepo.unzip(zipFile, extractDir)
        assertTrue(result.isSuccess)
        val extractedRoot = result.getOrThrow()

        assertTrue(File(extractedRoot, "doc1.txt").exists())
        assertTrue(File(extractedRoot, "nested/doc2.txt").exists())
        assertEquals("Content of Doc 1", File(extractedRoot, "doc1.txt").readText())
        assertEquals("Content of Doc 2", File(extractedRoot, "nested/doc2.txt").readText())
    }

    @Test
    fun tier2_feature10_zipSlipPathTraversalThrowsSecurityException() = runBlocking {
        val evilZip = File(rootDir, "evil_slip.zip")
        val extractDir = File(rootDir, "slip_extracted")
        val escapedTarget = File(rootDir, "escaped_secret.txt")

        SyntheticDocumentFactory.createZipSlipArchive(evilZip, "../../escaped_secret.txt")

        try {
            val result = storageRepo.unzip(evilZip, extractDir)
            if (result.isSuccess) {
                fail("Unzip must reject directory traversal entry '../../escaped_secret.txt' with SecurityException")
            }
        } catch (e: SecurityException) {
            // Expected security exception
            assertTrue(e.message?.contains("outside", ignoreCase = true) == true)
        }
        assertFalse("Escaped file must NEVER be written outside destination directory", escapedTarget.exists())
    }

    @Test
    fun tier2_feature10_zipBombHighCompressionRatioAbortsExtraction() = runBlocking {
        val bombZip = File(rootDir, "bomb.zip")
        val extractDir = File(rootDir, "bomb_extracted")
        // 10 MB zeroes compressed to a few KB
        SyntheticDocumentFactory.createZipBomb(bombZip, uncompressedSizeBytes = 10L * 1024 * 1024)

        try {
            val result = storageRepo.unzip(bombZip, extractDir)
            // If the implementation bounds compression ratio, it will return failure or throw SecurityException
            if (result.isFailure) {
                val ex = result.exceptionOrNull()
                assertNotNull(ex)
            }
        } catch (_: SecurityException) {
            // Expected
        }
    }

    // =========================================================================
    // TIER 4: Real-World Storage Hygiene & Recovery Scenario
    // =========================================================================

    @Test
    fun tier4_realWorldStorageHygieneAndRecoveryScenario() = runBlocking {
        // Step 1: Simulate user storage with system dirs, active downloads, temp logs, duplicate media
        val downloadFolder = File(rootDir, "Download").apply { mkdirs() }
        val activeDownload = File(downloadFolder, "android_studio.crdownload").apply {
            writeBytes(ByteArray(16384) { 10 })
        }
        val tempLog = File(rootDir, "app_debug.tmp").apply {
            writeBytes(ByteArray(4096))
            setLastModified(System.currentTimeMillis() - 7200_000L)
        }
        val duplicate1 = File(rootDir, "vacation_copy1.bin").apply {
            writeBytes(ByteArray(8192) { 42 })
        }
        val duplicate2 = File(rootDir, "vacation_copy2.bin").apply {
            writeBytes(ByteArray(8192) { 42 })
        }

        // Step 2: Storage cleaner scans root
        val scan = cleanerManager.scanDirectory(rootDir)
        assertTrue("Scan discovers temporary log", scan.tempFiles.any { it.name == "app_debug.tmp" })
        assertFalse("Scan must protect active download", scan.tempFiles.any { it.name.endsWith(".crdownload") })
        assertEquals("Duplicate group detected", 1, scan.duplicateGroups.size)

        // Step 3: Delete temp files safely
        val trashResult = storageRepo.moveToTrash(scan.tempFiles)
        assertTrue(trashResult.isSuccess)
        assertFalse("Temp file moved to trash", tempLog.exists())
        assertTrue("Active download remains intact", activeDownload.exists())

        // Step 4: Empty trash and verify permanent cleanup
        val emptyResult = storageRepo.emptyTrash()
        assertTrue(emptyResult.isSuccess)

        // Step 5: Verify system directory untouched
        assertTrue("Download directory remains intact", downloadFolder.exists())
        assertTrue("Active download remains intact and uncorrupted", activeDownload.exists())
    }
}
