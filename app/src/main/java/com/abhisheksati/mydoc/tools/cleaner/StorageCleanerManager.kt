package com.abhisheksati.mydoc.tools.cleaner

import com.abhisheksati.mydoc.util.SecureLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class DuplicateGroup(
    val checksum: String,
    val size: Long,
    val original: File,
    val duplicates: List<File>
) {
    val wastedBytes: Long get() = size * duplicates.size
}

data class StorageCleanScanResult(
    val emptyFolders: List<File> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val largeFiles: List<File> = emptyList(),
    val apkFiles: List<File> = emptyList(),
    val tempFiles: List<File> = emptyList(),
    val totalScannedFiles: Int = 0,
    val totalScannedDirectories: Int = 0,
    /** True when the entry or time budget stopped the walk before it finished. */
    val truncated: Boolean = false
) {
    val totalReclaimableBytes: Long
        get() {
            val duplicateBytes = duplicateGroups.sumOf { it.wastedBytes }
            val tempBytes = tempFiles.sumOf { it.length() }
            val apkBytes = apkFiles.sumOf { it.length() }
            return duplicateBytes + tempBytes + apkBytes
        }
}

class StorageCleanerManager {

    companion object {
        const val LARGE_FILE_THRESHOLD_BYTES = 50L * 1024 * 1024 // 50MB
        const val FULL_SCAN_MAX_ENTRIES = 200_000
        const val FULL_SCAN_BUDGET_MS = 90_000L
        // "bak" deliberately excluded: backups are often the only copy a user has.
        // Active/partial downloads (.crdownload, .part) must NEVER be treated as disposable junk.
        val ACTIVE_DOWNLOAD_EXTENSIONS = setOf("crdownload", "part", "download")
        val TEMP_EXTENSIONS = setOf("tmp", "temp", "log", "cache")

        val PROTECTED_DIR_NAMES = setOf(
            "download", "downloads", "dcim", "documents", "movies", "music",
            "pictures", "podcasts", "ringtones", "alarms", "notifications",
            "android", "lost+found", ".trash", ".mydoc", "system volume information",
            ".android_secure", ".recycle"
        )

        fun isProtectedDirectory(dir: File): Boolean {
            val name = dir.name.lowercase()
            if (name in PROTECTED_DIR_NAMES) return true
            val canonicalName = try { dir.canonicalFile.name.lowercase() } catch (_: Exception) { name }
            if (canonicalName in PROTECTED_DIR_NAMES) return true
            val parentName = dir.parentFile?.name?.lowercase()
            if (parentName == "storage" || parentName == "emulated") return true
            return false
        }
    }

    fun isProtectedDirectory(dir: File): Boolean = Companion.isProtectedDirectory(dir)

    suspend fun scanDirectory(
        rootDir: File,
        maxEntries: Int = FULL_SCAN_MAX_ENTRIES,
        timeBudgetMs: Long = FULL_SCAN_BUDGET_MS,
        onProgress: (scannedCount: Int, currentName: String) -> Unit = { _, _ -> }
    ): StorageCleanScanResult = withContext(Dispatchers.IO) {
        val scanJob = coroutineContext[Job]
        val emptyFolders = mutableListOf<File>()
        val largeFiles = mutableListOf<File>()
        val apkFiles = mutableListOf<File>()
        val tempFiles = mutableListOf<File>()

        // For duplicate detection: map size -> list of files
        val sizeCandidates = mutableMapOf<Long, MutableList<File>>()

        val deadline = if (timeBudgetMs == Long.MAX_VALUE) Long.MAX_VALUE
        else android.os.SystemClock.elapsedRealtime() + timeBudgetMs
        var outOfBudget = false
        var fileCount = 0
        var dirCount = 0

        val canonicalRoot = try { rootDir.canonicalFile.canonicalPath } catch (_: Exception) { rootDir.absolutePath }

        fun walk(dir: File) {
            if (outOfBudget) return
            if (scanJob?.isActive == false) {
                outOfBudget = true
                return
            }
            val canonicalDir = try { dir.canonicalFile.canonicalPath } catch (_: Exception) { null } ?: return
            if (!canonicalDir.startsWith(canonicalRoot + File.separator) && canonicalDir != canonicalRoot) {
                return
            }
            if (fileCount + dirCount >= maxEntries || android.os.SystemClock.elapsedRealtime() > deadline) {
                outOfBudget = true
                return
            }
            val entries = try {
                dir.listFiles()
            } catch (_: Exception) {
                null
            } ?: return

            if (entries.isEmpty()) {
                if (!isProtectedDirectory(dir)) {
                    emptyFolders.add(dir)
                }
                return
            }

            dirCount++
            for (entry in entries) {
                if (outOfBudget) break
                val canonicalEntry = try { entry.canonicalFile.canonicalPath } catch (_: Exception) { null } ?: continue
                if (!canonicalEntry.startsWith(canonicalRoot + File.separator) && canonicalEntry != canonicalRoot) {
                    continue
                }

                if (entry.isDirectory) {
                    val subEntries = try { entry.listFiles() } catch (_: Exception) { null }
                    if (subEntries != null && subEntries.isEmpty()) {
                        if (!isProtectedDirectory(entry)) {
                            emptyFolders.add(entry)
                        }
                    } else {
                        walk(entry)
                    }
                } else if (entry.isFile) {
                    fileCount++
                    if (fileCount % 120 == 0) {
                        onProgress(fileCount, entry.name)
                    }

                    val length = entry.length()
                    val ext = entry.extension.lowercase()
                    val isDownload = ext in ACTIVE_DOWNLOAD_EXTENSIONS ||
                        entry.name.endsWith(".crdownload", ignoreCase = true) ||
                        entry.name.endsWith(".part", ignoreCase = true)

                    // Large file check
                    if (length >= LARGE_FILE_THRESHOLD_BYTES) {
                        largeFiles.add(entry)
                    }

                    // APK check
                    if (ext == "apk") {
                        apkFiles.add(entry)
                    }

                    // Temp / junk files (exclude active/download files)
                    if (!isDownload && (ext in TEMP_EXTENSIONS || entry.name.startsWith(".~") || entry.name.endsWith(".tmp", ignoreCase = true))) {
                        tempFiles.add(entry)
                    }

                    // Potential duplicate candidate (files between 4KB and 500MB, never active downloads)
                    if (!isDownload && length in 4096..(500L * 1024 * 1024)) {
                        val list = sizeCandidates.getOrPut(length) { mutableListOf() }
                        list.add(entry)
                    }
                }
            }
        }

        walk(rootDir)

        // Process duplicates: only hash files that share identical sizes
        val duplicateGroups = mutableListOf<DuplicateGroup>()
        for ((size, files) in sizeCandidates) {
            if (files.size < 2) continue
            if (android.os.SystemClock.elapsedRealtime() > deadline) break
            if (scanJob?.isActive == false) break

            val hashGroups = mutableMapOf<String, MutableList<File>>()
            for (file in files) {
                if (android.os.SystemClock.elapsedRealtime() > deadline) break
                if (scanJob?.isActive == false) break
                // Full-file hash. A prefix hash calls distinct files duplicates and the
                // cleaner then deletes one of them, so partial hashing is not acceptable.
                val hash = calculateSha256(file) ?: continue
                hashGroups.getOrPut(hash) { mutableListOf() }.add(file)
            }

            for ((hash, matchingFiles) in hashGroups) {
                if (matchingFiles.size > 1) {
                    // First file is treated as original, rest are duplicates
                    duplicateGroups.add(
                        DuplicateGroup(
                            checksum = hash,
                            size = size,
                            original = matchingFiles.first(),
                            duplicates = matchingFiles.drop(1)
                        )
                    )
                }
            }
        }

        // Sort large files descending
        largeFiles.sortByDescending { it.length() }

        StorageCleanScanResult(
            emptyFolders = emptyFolders,
            duplicateGroups = duplicateGroups,
            largeFiles = largeFiles,
            apkFiles = apkFiles,
            tempFiles = tempFiles,
            totalScannedFiles = fileCount,
            totalScannedDirectories = dirCount,
            truncated = outOfBudget
        )
    }

    /**
     * Removes directories that are still empty at the moment of the call. Files are never
     * touched here: the cleaner moves them to Trash so they can be restored.
     */
    suspend fun deleteEmptyDirectories(directories: List<File>, withinRoot: File? = null): Int = withContext(Dispatchers.IO) {
        // The scan refuses anything that resolves outside its root; the delete is a separate call
        // with a list the UI has held for as long as the sheet has been open, so it re-checks here.
        val canonicalRoot = withinRoot?.let { runCatching { it.canonicalFile.canonicalPath }.getOrNull() }
        var removed = 0
        // Deepest first so a parent that only contains other empty parents also goes.
        for (dir in directories.sortedByDescending { it.absolutePath.length }) {
            if (isProtectedDirectory(dir)) continue
            val canonical = runCatching { dir.canonicalFile.canonicalPath }.getOrNull() ?: continue
            if (canonicalRoot != null) {
                if (canonical == canonicalRoot) continue
                if (!canonical.startsWith(canonicalRoot + File.separator)) continue
            }
            try {
                if (dir.isDirectory && dir.listFiles()?.isEmpty() == true && dir.delete()) {
                    removed++
                }
            } catch (e: Exception) {
                // One unreadable folder must not stop the batch; it is reported, not swallowed.
                SecureLog.warn("StorageCleaner", "could not remove an empty folder", e) { dir.name }
            }
        }
        removed
    }

    private fun calculateSha256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }
}
