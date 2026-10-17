package com.abhisheksati.mydoc.tools.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

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
    val totalScannedDirectories: Int = 0
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
        private val TEMP_EXTENSIONS = setOf("tmp", "temp", "log", "bak", "cache", "crdownload", "part")
    }

    suspend fun scanDirectory(
        rootDir: File,
        onProgress: (scannedCount: Int, currentName: String) -> Unit = { _, _ -> }
    ): StorageCleanScanResult = withContext(Dispatchers.IO) {
        val emptyFolders = mutableListOf<File>()
        val largeFiles = mutableListOf<File>()
        val apkFiles = mutableListOf<File>()
        val tempFiles = mutableListOf<File>()

        // For duplicate detection: map size -> list of files
        val sizeCandidates = mutableMapOf<Long, MutableList<File>>()

        var fileCount = 0
        var dirCount = 0

        fun walk(dir: File) {
            val entries = try {
                dir.listFiles()
            } catch (_: Exception) {
                null
            } ?: return

            if (entries.isEmpty()) {
                emptyFolders.add(dir)
                return
            }

            dirCount++
            for (entry in entries) {
                if (entry.isDirectory) {
                    val subEntries = try { entry.listFiles() } catch (_: Exception) { null }
                    if (subEntries != null && subEntries.isEmpty()) {
                        emptyFolders.add(entry)
                    } else {
                        walk(entry)
                    }
                } else if (entry.isFile) {
                    fileCount++
                    if (fileCount % 50 == 0) {
                        onProgress(fileCount, entry.name)
                    }

                    val length = entry.length()
                    val ext = entry.extension.lowercase()

                    // Large file check
                    if (length >= LARGE_FILE_THRESHOLD_BYTES) {
                        largeFiles.add(entry)
                    }

                    // APK check
                    if (ext == "apk") {
                        apkFiles.add(entry)
                    }

                    // Temp / junk files
                    if (ext in TEMP_EXTENSIONS || entry.name.startsWith(".~") || entry.name.endsWith(".tmp")) {
                        tempFiles.add(entry)
                    }

                    // Potential duplicate candidate (files between 4KB and 500MB)
                    if (length in 4096..(500L * 1024 * 1024)) {
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

            val hashGroups = mutableMapOf<String, MutableList<File>>()
            for (file in files) {
                val hash = calculateQuickSha256(file) ?: continue
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
            totalScannedDirectories = dirCount
        )
    }

    suspend fun cleanFiles(files: List<File>): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        for (file in files) {
            try {
                if (file.isDirectory) {
                    file.delete()
                } else {
                    val len = file.length()
                    if (file.delete()) {
                        freedBytes += len
                    }
                }
            } catch (_: Exception) {
                // Ignore single file failures
            }
        }
        freedBytes
    }

    private fun calculateQuickSha256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                // For speed on big files, read up to first 2MB + last 64KB if > 2MB
                var totalRead = 0
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalRead > 2 * 1024 * 1024) break
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }
}
