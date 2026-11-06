package com.abhisheksati.mydoc.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.FileSortOrder
import com.abhisheksati.mydoc.domain.model.FolderProperties
import com.abhisheksati.mydoc.domain.model.PartialStorageOperationException
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.domain.repository.StorageRepository
import com.abhisheksati.mydoc.util.SecureLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


import com.abhisheksati.mydoc.data.local.dao.TrashDao
import com.abhisheksati.mydoc.data.local.entity.TrashEntity
import com.abhisheksati.mydoc.domain.model.TrashItem
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

enum class ZipCollisionStrategy {
    RENAME,
    SKIP,
    REPLACE
}

class StorageRepositoryImpl(
    private val context: Context,
    private val trashDao: TrashDao? = null,
    /**
     * The one place a file is removed from the disk. Injected so the partial-deletion cases that
     * used to destroy data can be reproduced on any filesystem: whether an OS refuses a delete is
     * not something a test can rely on, but whether the code survives one is.
     */
    private val entryMutator: EntryMutator = DefaultEntryMutator
) : StorageRepository {

    /**
     * One lock for every mutating batch. Two pastes aimed at the same folder used to race on
     * the auto-rename probe, so both could pick "report (1).pdf" and one overwrote the other.
     */
    private val mutationLock = Mutex()

    private companion object {
        /** A write that fills the last of a volume leaves a file that cannot be told from a good one. */
        const val RESERVED_FREE_BYTES = 16L * 1024 * 1024
    }


    override suspend fun getStorageVolumes(): List<StorageVolume> = withContext(Dispatchers.IO) {
        val volumes = mutableListOf<StorageVolume>()

        // 1. Primary Internal Storage
        try {
            val primaryDir = Environment.getExternalStorageDirectory()
            if (primaryDir.exists()) {
                val stat = StatFs(primaryDir.path)
                val total = stat.totalBytes
                val free = stat.availableBytes
                volumes.add(
                    StorageVolume(
                        title = "Internal Storage",
                        path = primaryDir.absolutePath,
                        totalBytes = total,
                        freeBytes = free,
                        isPrimary = true
                    )
                )
            }
        } catch (e: SecurityException) {
            android.util.Log.w("StorageRepository", "Permission denied accessing primary storage volume", e)
        } catch (e: Exception) {
            android.util.Log.w("StorageRepository", "Error resolving primary storage volume", e)
        }

        // 2. Secondary SD Card / USB OTG
        try {
            val externalDirs = context.getExternalFilesDirs(null)
            for (dir in externalDirs) {
                if (dir != null && !Environment.isExternalStorageEmulated(dir)) {
                    val root = getVolumeRoot(dir)
                    if (root != null && root.exists() && root.canRead()) {
                        val stat = StatFs(root.path)
                        val total = stat.totalBytes
                        val free = stat.availableBytes
                        // Avoid duplicates
                        if (volumes.none { it.path == root.absolutePath }) {
                            volumes.add(
                                StorageVolume(
                                    title = "SD Card",
                                    path = root.absolutePath,
                                    totalBytes = total,
                                    freeBytes = free,
                                    isPrimary = false
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.w("StorageRepository", "Permission denied accessing external secondary volumes", e)
        } catch (e: Exception) {
            android.util.Log.w("StorageRepository", "Error resolving secondary storage volumes", e)
        }

        volumes
    }

    private fun getVolumeRoot(externalFilesDir: File): File? {
        val path = externalFilesDir.absolutePath
        val androidIndex = path.indexOf("/Android/")
        if (androidIndex != -1) {
            val rootPath = path.substring(0, androidIndex)
            return File(rootPath)
        }
        return null
    }

    override suspend fun listDirectory(
        directory: File,
        showHidden: Boolean,
        sortOrder: FileSortOrder
    ): List<StorageItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext emptyList()
        }

        val rawFiles = directory.listFiles() ?: return@withContext emptyList()

        val items = rawFiles
            .filter { file ->
                if (!showHidden && file.name.startsWith(".")) false else true
            }
            .map { file ->
                val itemCount = if (file.isDirectory) {
                    file.list()?.size ?: 0
                } else 0
                StorageItem(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    lastModified = file.lastModified(),
                    itemCount = itemCount,
                    isHidden = file.name.startsWith(".")
                )
            }

        // Directories first, then sorted by field
        items.sortedWith { a, b ->
            if (a.isDirectory != b.isDirectory) {
                return@sortedWith if (a.isDirectory) -1 else 1
            }

            val cmp = when (sortOrder.field) {
                FileSortField.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                FileSortField.DATE -> a.lastModified.compareTo(b.lastModified)
                FileSortField.SIZE -> {
                    if (a.isDirectory) a.itemCount.compareTo(b.itemCount)
                    else a.sizeBytes.compareTo(b.sizeBytes)
                }
                FileSortField.TYPE -> a.extension.compareTo(b.extension, ignoreCase = true)
            }

            if (sortOrder.isAscending) cmp else -cmp
        }
    }

    private fun isValidFileName(name: String): Boolean {
        if (name.isEmpty() || name == "." || name == "..") return false
        val illegalChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')
        return !name.any { it in illegalChars }
    }

    override suspend fun createDirectory(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanName = name.trim()
            if (cleanName.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Name cannot be empty"))
            if (!isValidFileName(cleanName)) return@withContext Result.failure(IllegalArgumentException("Name contains invalid characters"))
            val target = File(parent, cleanName)
            if (target.exists()) {
                return@withContext Result.failure(IllegalStateException("A file or folder with this name already exists"))
            }
            if (target.mkdirs()) {
                Result.success(target)
            } else {
                Result.failure(Exception("Failed to create directory"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun createFile(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanName = name.trim()
            if (cleanName.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Name cannot be empty"))
            if (!isValidFileName(cleanName)) return@withContext Result.failure(IllegalArgumentException("Name contains invalid characters"))
            val target = File(parent, cleanName)
            if (target.exists()) {
                return@withContext Result.failure(IllegalStateException("File already exists"))
            }
            if (target.createNewFile()) {
                Result.success(target)
            } else {
                Result.failure(Exception("Failed to create file"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun rename(source: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanName = newName.trim()
            if (cleanName.isEmpty()) return@withContext Result.failure(IllegalArgumentException("New name cannot be empty"))
            if (!isValidFileName(cleanName)) return@withContext Result.failure(IllegalArgumentException("New name contains invalid characters"))
            val parent = source.parentFile
                ?: return@withContext Result.failure(IllegalStateException("Cannot determine parent directory"))
            val target = File(parent, cleanName)
            if (target.exists() && target.absolutePath != source.absolutePath) {
                return@withContext Result.failure(IllegalStateException("A file with that name already exists"))
            }
            if (source.renameTo(target)) {
                Result.success(target)
            } else {
                Result.failure(Exception("Rename failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun delete(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            for (file in files) {
                if (file.exists()) {
                    val deleted = if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                    if (deleted) count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun copy(
        sources: List<File>,
        targetDir: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext Result.failure(IllegalArgumentException("Target must be a directory"))
        }
        if (!hasRoomFor(targetDir, totalBytesOf(sources))) {
            return@withContext Result.failure(
                java.io.IOException("Not enough free space on ${targetDir.name} for this copy")
            )
        }

        val total = sources.size
        var copied = 0
        var failed = 0
        var firstError: Throwable? = null
        val targetCanon = targetDir.canonicalPath

        mutationLock.withLock {
            for (src in sources) {
                currentCoroutineContext().ensureActive()
                if (!src.exists()) {
                    failed++
                    continue
                }
                if (src.isDirectory) {
                    val srcCanon = src.canonicalPath
                    if (targetCanon == srcCanon || targetCanon.startsWith(srcCanon + File.separator)) {
                        continue // Prevent cyclic copy into self/child
                    }
                }
                val dest = getNonConflictingFile(targetDir, src.name)
                // One bad item must not abort the batch or hide what was already written.
                runCatching {
                    if (src.isDirectory) src.copyRecursively(dest, overwrite = true)
                    else src.copyTo(dest, overwrite = true)
                }.onSuccess {
                    copied++
                    onProgress(copied, total)
                }.onFailure { error ->
                    discardPartial(dest)
                    failed++
                    firstError = firstError ?: error
                }
            }
        }
        batchResult(copied, failed, firstError, "Nothing was copied")
    }

    override suspend fun move(
        sources: List<File>,
        targetDir: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext Result.failure(IllegalArgumentException("Target must be a directory"))
        }
        if (!hasRoomFor(targetDir, totalBytesOf(sources))) {
            return@withContext Result.failure(
                java.io.IOException("Not enough free space on ${targetDir.name} for this move")
            )
        }

        val total = sources.size
        var moved = 0
        var failed = 0
        var firstError: Throwable? = null
        val retained = mutableListOf<String>()
        val targetCanon = targetDir.canonicalPath

        mutationLock.withLock {
            for (src in sources) {
                currentCoroutineContext().ensureActive()
                if (!src.exists()) {
                    failed++
                    continue
                }
                if (src.isDirectory) {
                    val srcCanon = src.canonicalPath
                    if (targetCanon == srcCanon || targetCanon.startsWith(srcCanon + File.separator)) {
                        continue // Prevent cyclic move into self/child
                    }
                }
                val dest = getNonConflictingFile(targetDir, src.name)

                if (entryMutator.rename(src, dest)) {
                    moved++
                    onProgress(moved, total)
                    continue
                }

                // Cross-volume or locked-source fallback, in three stages: copy, prove the copy,
                // then delete the original entry by entry. The copy is taken back only while the
                // original is provably untouched; if part of the original was already removed, the
                // copy is the surviving version of that part and destroying it was the data-loss
                // bug this replaces.
                val copyError = copyAndVerify(src, dest)
                if (copyError != null) {
                    discardPartial(dest)
                    failed++
                    firstError = firstError ?: copyError
                    continue
                }
                val before = snapshotSource(src)
                val survivors = deleteSourceEntries(src)
                if (survivors.isEmpty()) {
                    moved++
                    onProgress(moved, total)
                } else {
                    failed++
                    if (!resolvePartialSource(src, dest, before)) {
                        retained += src.name
                        firstError = firstError ?: java.io.IOException(
                            "Copied ${src.name} but ${survivors.first().name} could not be removed from ${src.parent}"
                        )
                    } else {
                        firstError = firstError ?: java.io.IOException("Could not remove ${src.name}")
                    }
                }
            }
        }
        batchResult(moved, failed, firstError, "Nothing was moved", retained)
    }

    /**
     * A batch result. A fully successful run reports the count; a run where nothing completed
     * reports the real error; a split reports both counts so the UI cannot say "Pasted 10 items"
     * after three of them failed.
     */
    private fun batchResult(
        completed: Int,
        failed: Int,
        firstError: Throwable?,
        nothingDoneMessage: String,
        retainedSources: List<String> = emptyList()
    ): Result<Int> = when {
        failed == 0 -> Result.success(completed)
        completed == 0 && retainedSources.isEmpty() ->
            Result.failure(firstError ?: IllegalStateException(nothingDoneMessage))
        else -> Result.failure(
            PartialStorageOperationException(completed, failed, firstError, retainedSources)
        )
    }

    /** Bytes the sources will occupy. Checked before writing: a copy that dies at 98% full
     *  leaves a truncated file the user cannot tell from a complete one. */
    private fun totalBytesOf(sources: List<File>): Long = sources.sumOf { src ->
        if (!src.exists()) 0L
        else if (src.isDirectory) src.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        else src.length()
    }

    private fun hasRoomFor(dir: File, bytes: Long): Boolean =
        bytes <= 0L || dir.usableSpace - RESERVED_FREE_BYTES > bytes

    /** Remove whatever a failed write left behind, so a partial file is never mistaken for the item. */
    private fun discardPartial(dest: File) {
        if (dest.isDirectory) dest.walkBottomUp().forEach { entryMutator.delete(it) }
        else entryMutator.delete(dest)
    }

    /**
     * Every entry under [root] with its length, keyed by relative path. Taken before a source is
     * deleted so that, if deletion fails, the code can tell "the original is untouched" from
     * "the original has been half-removed" — the difference between a redundant copy and the only
     * complete copy that exists.
     */
    private fun snapshotSource(root: File): Map<String, Long> {
        if (!root.isDirectory) return mapOf("" to if (root.isFile) root.length() else -1L)
        return root.walkTopDown().associate { node ->
            node.relativeTo(root).path to if (node.isDirectory) -1L else node.length()
        }
    }

    /**
     * Copies [src] into [dest] and proves the copy is whole before anything is deleted. A
     * recursive copy that stopped early is still reported as success by some filesystems, so a
     * directory is verified entry by entry rather than by the return value.
     */
    private fun copyAndVerify(src: File, dest: File): Throwable? =
        runCatching {
            if (src.isDirectory) src.copyRecursively(dest, overwrite = true) else src.copyTo(dest, overwrite = true)
            verifyCopy(src, dest)
        }.fold(onSuccess = { null }, onFailure = { it })

    private fun verifyCopy(src: File, dest: File) {
        if (src.isDirectory) {
            src.walkTopDown().forEach { node ->
                val rel = node.relativeTo(src).path
                val mirror = if (rel.isEmpty()) dest else File(dest, rel)
                if (!mirror.exists()) throw java.io.IOException("Copied tree is missing $rel")
                if (node.isFile && mirror.length() != node.length()) {
                    throw java.io.IOException("Copy of $rel is ${mirror.length()} bytes, source is ${node.length()}")
                }
            }
        } else if (!dest.isFile || dest.length() != src.length()) {
            throw java.io.IOException("Copied file size does not match ${src.name}")
        }
    }

    /**
     * Removes the source one entry at a time, deepest first, and returns what survived. Deleting a
     * tree in a single call used to hide the partial case, and the partial case is the only one
     * where discarding the destination destroys data.
     */
    private suspend fun deleteSourceEntries(src: File): List<File> {
        if (!src.isDirectory) return if (entryMutator.delete(src)) emptyList() else listOf(src)
        val survivors = mutableListOf<File>()
        src.walkTopDown().filter { it != src }
            .sortedByDescending { it.absolutePath.length }
            .forEach { node ->
                currentCoroutineContext().ensureActive()
                if (!entryMutator.delete(node)) survivors += node
            }
        if (!entryMutator.delete(src)) survivors += src
        return survivors
    }

    /**
     * The decision the old fallback got backwards. A verified copy is redundant only while the
     * original still stands exactly as it was; once any part of the original is gone, the copy is
     * the surviving version of those parts and taking it back would delete user data.
     */
    private fun resolvePartialSource(src: File, dest: File, before: Map<String, Long>): Boolean {
        val after = snapshotSource(src)
        if (after == before) {
            discardPartial(dest)
            return true
        }
        return false
    }

    private fun getNonConflictingFile(parent: File, originalName: String): File {
        var file = File(parent, originalName)
        if (!file.exists()) return file

        val nameWithoutExt = file.nameWithoutExtension
        val ext = file.extension
        val extSuffix = if (ext.isNotEmpty()) ".$ext" else ""

        var counter = 1
        while (file.exists()) {
            file = File(parent, "$nameWithoutExt ($counter)$extSuffix")
            counter++
        }
        return file
    }

    override suspend fun getFolderProperties(directory: File): FolderProperties = withContext(Dispatchers.IO) {
        var totalSize = 0L
        var filesCount = 0
        var foldersCount = 0

        fun scan(dir: File) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory) {
                    foldersCount++
                    scan(child)
                } else {
                    filesCount++
                    totalSize += child.length()
                }
            }
        }

        scan(directory)

        FolderProperties(
            file = directory,
            totalSizeBytes = totalSize,
            totalFilesCount = filesCount,
            totalFoldersCount = foldersCount,
            isReadable = directory.canRead(),
            isWritable = directory.canWrite()
        )
    }

    override suspend fun calculateHash(file: File, algorithm: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canRead() || file.isDirectory) {
                return@withContext null
            }
            val digest = MessageDigest.getInstance(algorithm)
            val buffer = ByteArray(65536)
            FileInputStream(file).use { input ->
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            val hashBytes = digest.digest()
            val hex = StringBuilder()
            for (b in hashBytes) {
                hex.append(String.format(Locale.US, "%02x", b))
            }
            hex.toString()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            SecureLog.warn("StorageRepository", "checksum failed for a cached document", e) { file.name }
            null
        }
    }

    override suspend fun zip(
        sources: List<File>,
        destinationZip: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (sources.isEmpty()) throw IllegalArgumentException("No files selected for compression")
            val parentDir = destinationZip.parentFile ?: destinationZip
            val destFile = getNonConflictingFile(parentDir, destinationZip.name)
            destFile.parentFile?.mkdirs()

            data class ZipTaskEntry(val file: File, val entryName: String)
            val entriesToCompress = mutableListOf<ZipTaskEntry>()

            for (source in sources) {
                if (!source.exists()) continue
                if (source.isDirectory) {
                    val baseDirName = source.name
                    val dirFiles = source.walkTopDown().toList()
                    for (f in dirFiles) {
                        val relativePath = f.relativeTo(source).path.replace('\\', '/')
                        val entryName = if (relativePath.isEmpty()) {
                            "$baseDirName/"
                        } else if (f.isDirectory) {
                            "$baseDirName/$relativePath/"
                        } else {
                            "$baseDirName/$relativePath"
                        }
                        entriesToCompress.add(ZipTaskEntry(f, entryName))
                    }
                } else {
                    entriesToCompress.add(ZipTaskEntry(source, source.name))
                }
            }

            val total = entriesToCompress.size
            var current = 0

            // Copy and move refuse up front when the volume cannot hold the payload; this path asked
            // for no room at all, so a full disk was discovered halfway through writing.
            var payloadBytes = 0L
            for (task in entriesToCompress) {
                if (task.file.isFile) payloadBytes += task.file.length()
            }
            if (!hasRoomFor(parentDir, payloadBytes)) {
                throw java.io.IOException("Not enough free space on ${parentDir.name} for this archive")
            }

            // Build the archive under a private name. A compression that dies halfway used to
            // leave a real .zip on disk that opens as a truncated archive — and it also left the
            // private name itself behind, because nothing took it back.
            // A fixed ".name.tmp" name truncated whatever already sat at that path - a file the
            // listing never shows, silently overwritten. The staging name is unique per run, so it
            // cannot collide with anything and is always ours to remove.
            val staging = File.createTempFile(".${destFile.name}.", ".tmp", parentDir)
            val destPreExisted = destFile.exists()
            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(staging), 32 * 1024)).use { zos ->
                    val buffer = ByteArray(32 * 1024)
                    for (task in entriesToCompress) {
                        if (task.file.isDirectory) {
                            val dirEntry = ZipEntry(if (task.entryName.endsWith("/")) task.entryName else "${task.entryName}/")
                            dirEntry.time = task.file.lastModified()
                            zos.putNextEntry(dirEntry)
                            zos.closeEntry()
                        } else {
                            val fileEntry = ZipEntry(task.entryName)
                            fileEntry.time = task.file.lastModified()
                            fileEntry.size = task.file.length()
                            zos.putNextEntry(fileEntry)
                            FileInputStream(task.file).use { fis ->
                                var len: Int
                                while (fis.read(buffer).also { len = it } > 0) {
                                    zos.write(buffer, 0, len)
                                }
                            }
                            zos.closeEntry()
                        }
                        current++
                        currentCoroutineContext().ensureActive()
                        onProgress(current, total)
                    }
                }
                if (!staging.renameTo(destFile)) {
                    staging.copyTo(destFile, overwrite = true)
                    discardPartial(staging)
                }
                destFile
            } catch (t: Throwable) {
                // Cancellation lands here too. A half-written archive is not the user's output, and
                // staging is always this run's file now, so it always goes.
                discardPartial(staging)
                if (!destPreExisted) discardPartial(destFile)
                throw t
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
        }
    }

    override suspend fun unzip(
        zipFile: File,
        destinationDir: File,
        onProgress: (extractedFiles: Int) -> Unit
    ): Result<File> = unzip(zipFile, destinationDir, ZipCollisionStrategy.RENAME, onProgress)

    suspend fun unzip(
        zipFile: File,
        destinationDir: File,
        collisionStrategy: ZipCollisionStrategy,
        onProgress: (extractedFiles: Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        // Hoisted out of runCatching so the failure path below can see what this run created.
        // One token per extraction: millisecond timestamps collide when two archives are unpacked
        // together, and a shared temp name corrupts both results.
        val stagingToken = java.util.UUID.randomUUID().toString()
        val createdDuringRun = mutableListOf<File>()
        runCatching {
            if (!zipFile.exists() || !zipFile.isFile) throw IllegalArgumentException("Zip file does not exist")
            if (!destinationDir.exists()) destinationDir.mkdirs()

            val canonicalDest = destinationDir.canonicalFile.canonicalPath
            var extractedCount = 0
            var totalExtractedBytes = 0L
            val maxAllowedBytes = 1024L * 1024L * 1024L // 1 GB uncompressed limit
            val maxAllowedFiles = 10_000 // 10,000 files limit
            val zipFileSize = zipFile.length().coerceAtLeast(1024L)
            val buffer = ByteArray(32 * 1024)

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile), 32 * 1024)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    currentCoroutineContext().ensureActive()
                    extractedCount++
                    if (extractedCount > maxAllowedFiles) {
                        throw SecurityException("Zip extraction aborted: Archive exceeds maximum allowed file count ($maxAllowedFiles).")
                    }

                    val normalizedName = entry.name.replace('\\', '/')
                    val outFile = File(destinationDir, normalizedName)
                    // Zip Slip vulnerability check
                    val canonicalOut = outFile.canonicalFile.canonicalPath
                    if (!canonicalOut.startsWith(canonicalDest + File.separator) && canonicalOut != canonicalDest) {
                        throw SecurityException("Zip entry is outside destination: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        if (!outFile.exists()) {
                            outFile.mkdirs()
                            createdDuringRun += outFile
                        }
                    } else {
                        val parent = outFile.parentFile ?: destinationDir
                        val canonicalParent = parent.canonicalFile.canonicalPath
                        if (!canonicalParent.startsWith(canonicalDest + File.separator) && canonicalParent != canonicalDest) {
                            throw SecurityException("Zip entry parent is outside destination: ${entry.name}")
                        }
                        if (!parent.exists()) {
                            parent.mkdirs()
                            createdDuringRun += parent
                        }

                        val targetFile = if (outFile.exists()) {
                            when (collisionStrategy) {
                                ZipCollisionStrategy.SKIP -> {
                                    zis.closeEntry()
                                    entry = zis.nextEntry
                                    continue
                                }
                                ZipCollisionStrategy.REPLACE -> outFile
                                ZipCollisionStrategy.RENAME -> getNonConflictingFile(parent, outFile.name)
                            }
                        } else {
                            outFile
                        }
                        // Ownership is a property of the file actually written: under RENAME the
                        // original outFile exists but the renamed target does not, so checking
                        // outFile alone left that new file out of the rollback list.
                        val targetWasNew = !targetFile.exists()

                        val tempFile = File(parent, ".tmp_${stagingToken}_${targetFile.name}")
                        try {
                            FileOutputStream(tempFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    totalExtractedBytes += len
                                    if (totalExtractedBytes > maxAllowedBytes) {
                                        throw SecurityException("Zip extraction aborted: Total uncompressed content exceeds safe limit (1 GB).")
                                    }
                                    // Check compression ratio once uncompressed content exceeds 5 MB (defend against zip bombs)
                                    if (totalExtractedBytes > 5L * 1024 * 1024 && (totalExtractedBytes / zipFileSize) > 100L) {
                                        throw SecurityException("Zip extraction aborted: Potential zip bomb detected (compression ratio exceeds 100:1).")
                                    }
                                    fos.write(buffer, 0, len)
                                }
                            }
                            if (!tempFile.renameTo(targetFile)) {
                                tempFile.copyTo(targetFile, overwrite = true)
                                tempFile.delete()
                            }
                            // Only a file this run created is rolled back: under REPLACE the
                            // target already existed, so deleting it would take the user's own
                            // file as well as ours.
                            if (targetWasNew) createdDuringRun += targetFile
                        } catch (e: Exception) {
                            tempFile.delete()
                            throw e
                        }
                        onProgress(extractedCount)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            destinationDir
        }.onFailure { error ->
            // Roll back first, then rethrow. Cancellation used to return before the rollback ran,
            // leaving the entries already written behind with no result to say so.
            // An archive that failed halfway used to leave the earlier entries behind while the
            // UI reported failure; the folder looked half-extracted with nothing saying so.
            for (created in createdDuringRun.asReversed()) {
                runCatching { if (created.isDirectory) created.deleteRecursively() else created.delete() }
            }
            if (error is CancellationException) throw error
        }
    }

    override suspend fun queryCategory(category: FileCategory): List<StorageItem> = withContext(Dispatchers.IO) {
        if (category == FileCategory.DOWNLOADS) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            return@withContext if (downloadDir != null && downloadDir.exists() && downloadDir.isDirectory) {
                listDirectory(downloadDir, showHidden = false, sortOrder = FileSortOrder(FileSortField.DATE, false))
            } else {
                emptyList()
            }
        }

        val items = mutableListOf<StorageItem>()
        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val (selection, selectionArgs, sortOrder) = when (category) {
                FileCategory.DOCUMENTS -> {
                    val sel = "${MediaStore.Files.FileColumns.DATA} LIKE '%.pdf' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.doc' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.docx' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.xls' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.xlsx' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.ppt' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.pptx' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.txt' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.csv' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.md' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.rtf' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.odt' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.ods' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.odp' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.epub' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.fb2'"
                    Triple(sel, null, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
                }
                FileCategory.IMAGES -> {
                    val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.jpg' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.jpeg' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.png' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.webp' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.svg' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.bmp' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.gif'"
                    Triple(sel, null, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
                }
                FileCategory.VIDEOS -> {
                    val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO} OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.mp4' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.mkv' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.mov' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.avi' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.webm'"
                    Triple(sel, null, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
                }
                FileCategory.AUDIO -> {
                    val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO} OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.mp3' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.wav' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.m4a' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.flac' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.ogg' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.aac' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.opus' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.wma'"
                    Triple(sel, null, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
                }
                FileCategory.APKS -> {
                    val sel = "${MediaStore.Files.FileColumns.DATA} LIKE '%.apk' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.xapk' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.apks'"
                    Triple(sel, null, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
                }
                FileCategory.ARCHIVES -> {
                    val sel = "${MediaStore.Files.FileColumns.DATA} LIKE '%.zip' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.rar' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.7z' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.tar' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.gz' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.bz2' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.xz' OR " +
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.jar'"
                    Triple(sel, null, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
                }
                FileCategory.LARGE_FILES -> {
                    val sel = "${MediaStore.Files.FileColumns.SIZE} >= 52428800" // 50MB
                    Triple(sel, null, "${MediaStore.Files.FileColumns.SIZE} DESC")
                }
            }

            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                var count = 0
                while (cursor.moveToNext() && count < 1000) {
                    val path = if (dataIndex >= 0) cursor.getString(dataIndex) else null
                    if (path != null) {
                        val f = File(path)
                        if (f.exists() && !f.isDirectory) {
                            items.add(StorageItem(f))
                            count++
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.w("StorageRepository", "Permission denied discovering recent files", e)
        } catch (e: Exception) {
            android.util.Log.w("StorageRepository", "Error discovering recent files", e)
        }
        items
    }

    override suspend fun searchFiles(rootDirectory: File, query: String, recursive: Boolean): List<StorageItem> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !rootDirectory.exists()) return@withContext emptyList()
        val trimmed = query.trim()
        val results = mutableListOf<StorageItem>()

        if (!recursive) {
            val files = rootDirectory.listFiles() ?: return@withContext emptyList()
            for (f in files) {
                if (f.name.contains(trimmed, ignoreCase = true)) {
                    results.add(StorageItem(f))
                }
            }
        } else {
            var count = 0
            try {
                rootDirectory.walkTopDown()
                    .maxDepth(6)
                    .filter { it.name.contains(trimmed, ignoreCase = true) }
                    .forEach {
                        if (it.absolutePath != rootDirectory.absolutePath) {
                            results.add(StorageItem(it))
                            count++
                            if (count >= 200) return@forEach
                        }
                    }
            } catch (e: SecurityException) {
                android.util.Log.w("StorageRepository", "Permission denied walking directory for search", e)
            } catch (e: Exception) {
                android.util.Log.w("StorageRepository", "Error walking directory for search", e)
            }
        }
        results
    }

    private fun getTrashDir(): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val trash = File(baseDir, ".trash")
        if (!trash.exists()) trash.mkdirs()
        return trash
    }

    private suspend fun scanFilesAwait(context: Context, paths: List<String>) {
        if (paths.isEmpty()) return
        withTimeoutOrNull(2000L) {
            suspendCancellableCoroutine { continuation ->
                MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    override fun getTrashItems(): Flow<List<TrashItem>> = flow {
        // Purge runs in the collector's scope (cancellable, caller-managed) rather than an
        // unmanaged CoroutineScope that could fire destructive deletes racing move/restore.
        purgeExpiredTrash()
        val dao = trashDao
        if (dao == null) {
            emit(emptyList())
        } else {
            dao.getAllTrashItems().collect { list -> emit(list.map { it.toDomain() }) }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun purgeExpiredTrash(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
            val expiredItems = trashDao?.getItemsOlderThan(thirtyDaysAgo) ?: emptyList()
            val successfullyDeletedIds = mutableListOf<Long>()

            for (expired in expiredItems) {
                val file = File(expired.trashPath)
                val deleted = try {
                    if (!file.exists()) {
                        true
                    } else {
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    }
                } catch (_: Exception) {
                    false
                }
                if (deleted) {
                    successfullyDeletedIds.add(expired.id)
                }
            }

            if (successfullyDeletedIds.isNotEmpty()) {
                trashDao?.deleteByIds(successfullyDeletedIds)
            }

            // Orphan accounting. A file in .trash with no row used to be deleted after 24 hours,
            // which turned a crash between move and insert into permanent data loss. Rows are now
            // reserved before the move, so an orphan means something unexpected happened; keeping
            // it costs disk, deleting it can cost a document, so it is logged and left alone.
            try {
                val trashDir = getTrashDir()
                val knownTrashPaths = (trashDao?.getAllTrashItemsSync() ?: emptyList())
                    .mapNotNull { runCatching { File(it.trashPath).canonicalPath }.getOrNull() }
                    .toSet()
                val orphans = (trashDir.listFiles() ?: emptyArray()).filter { diskFile ->
                    val canonical = runCatching { diskFile.canonicalPath }.getOrNull()
                    canonical != null && canonical !in knownTrashPaths
                }
                if (orphans.isNotEmpty()) {
                    android.util.Log.w(
                        "TrashReconciliation",
                        "Keeping ${orphans.size} trash file(s) with no database record; " +
                            "empty the Recycle Bin to reclaim them"
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w("TrashReconciliation", "Orphan accounting failed", e)
            }

            successfullyDeletedIds.size
        }
    }

    override suspend fun moveToTrash(files: List<File>): Result<List<TrashItem>> = withContext(Dispatchers.IO) {
        try {
            val trashDir = getTrashDir()
            val trashed = mutableListOf<TrashItem>()
            val scannedPaths = mutableListOf<String>()
            val now = System.currentTimeMillis()

            var failed = 0
            var firstError: Throwable? = null
            val retained = mutableListOf<String>()
            for (file in files) {
                if (!file.exists()) {
                    failed++
                    continue
                }
                // now is computed once per batch, so same-basename files would collide and
                // overwrite each other (two DB rows sharing one trashPath). Make the name unique.
                var trashFileName = "${now}_${file.name}"
                var destTrashFile = File(trashDir, trashFileName)
                var collisionIndex = 1
                while (destTrashFile.exists()) {
                    trashFileName = "${now}_${collisionIndex}_${file.name}"
                    destTrashFile = File(trashDir, trashFileName)
                    collisionIndex++
                }

                val entity = TrashEntity(
                    originalPath = file.absolutePath,
                    trashPath = destTrashFile.absolutePath,
                    displayName = file.name,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    deletedTimestamp = now,
                    isDirectory = file.isDirectory
                )
                // The row is reserved before the file moves. Moving first meant a crash in
                // between left a file in .trash with no row: Restore could not see it and the
                // reconciliation in purgeExpiredTrash deleted it, so trashing a file could
                // destroy it outright.
                val rowId = trashDao?.insert(entity) ?: 0L

                var trashedNow = entryMutator.rename(file, destTrashFile)
                var retainedSource = false
                if (!trashedNow) {
                    val copyError = copyAndVerify(file, destTrashFile)
                    if (copyError != null) {
                        discardPartial(destTrashFile)
                        if (rowId != 0L) runCatching { trashDao?.deleteByIds(listOf(rowId)) }
                        failed++
                        firstError = firstError ?: copyError
                        continue
                    }
                    val before = snapshotSource(file)
                    val survivors = deleteSourceEntries(file)
                    trashedNow = survivors.isEmpty()
                    if (!trashedNow) {
                        // The copy in the Recycle Bin stays, and so does its row: some of the
                        // original has already been removed, so this is the only complete version
                        // of those parts. The user is told the item is in both places.
                        retainedSource = true
                        retained += file.name
                    }
                }

                if (trashedNow || retainedSource) {
                    destTrashFile.setLastModified(now)
                    scannedPaths += file.absolutePath
                    trashed.add(entity.copy(id = rowId).toDomain())
                } else {
                    failed++
                    if (rowId != 0L) runCatching { trashDao?.deleteByIds(listOf(rowId)) }
                    discardPartial(destTrashFile)
                }
            }

            // One awaited scan per file capped every delete at 2s; a batch costs one IPC.
            scanFilesAwait(context, scannedPaths)

            // Transactional purge of expired items and orphaned files
            purgeExpiredTrash()

            if (trashed.isEmpty() && failed > 0) {
                Result.failure(firstError ?: IllegalStateException("Nothing could be moved to the Recycle Bin"))
            } else if (retained.isNotEmpty()) {
                // Reporting the full list as "trashed" would claim the originals are gone when
                // they are not, so the partial result carries both counts.
                Result.failure(
                    PartialStorageOperationException(trashed.size, failed, firstError, retained)
                )
            } else {
                Result.success(trashed)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun restoreFromTrash(items: List<TrashItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var restoredCount = 0
            var failedCount = 0
            val unresolved = mutableListOf<String>()
            val restoredPaths = mutableListOf<String>()
            val trashDir = getTrashDir()
            // An unreadable bin may mean the volume is simply away. The records stay so the
            // restore can be retried; only a readable bin that lacks the item proves the content
            // is gone.
            if (!trashDir.isDirectory || !trashDir.canRead()) {
                return@withContext Result.failure(
                    java.io.IOException("The Recycle Bin storage is unavailable; nothing was restored.")
                )
            }
            for (item in items) {
                currentCoroutineContext().ensureActive()
                val trashFile = item.trashFile
                if (!trashFile.exists()) {
                    failedCount++
                    unresolved += item.displayName
                    trashDao?.deleteByIds(listOf(item.id))
                    continue
                }

                val originalParent = item.originalFile.parentFile
                if (originalParent == null || (!originalParent.exists() && !originalParent.mkdirs())) {
                    // Nowhere to put it back: keep the record so the item stays recoverable.
                    failedCount++
                    unresolved += item.displayName
                    continue
                }

                val destFile = getNonConflictingFile(originalParent, item.displayName)
                var restored = entryMutator.rename(trashFile, destFile)
                if (!restored) {
                    if (copyAndVerify(trashFile, destFile) != null) {
                        discardPartial(destFile)
                        failedCount++
                        unresolved += item.displayName
                        continue
                    }
                    val before = snapshotSource(trashFile)
                    val survivors = deleteSourceEntries(trashFile)
                    restored = survivors.isEmpty()
                    if (!restored && !resolvePartialSource(trashFile, destFile, before)) {
                        // Part of the bin copy is already gone, so the restored copy is the only
                        // complete version. Count it and leave the record for the survivor.
                        restoredCount++
                        restoredPaths += destFile.absolutePath
                        continue
                    }
                }

                if (restored) {
                    restoredPaths += destFile.absolutePath
                    trashDao?.deleteByIds(listOf(item.id))
                    restoredCount++
                } else {
                    discardPartial(destFile)
                    failedCount++
                    unresolved += item.displayName
                }
            }
            scanFilesAwait(context, restoredPaths)
            if (failedCount == 0) {
                Result.success(restoredCount)
            } else {
                Result.failure(
                    PartialStorageOperationException(restoredCount, failedCount, null, unresolved)
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun deletePermanently(items: List<TrashItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var deletedCount = 0
            val deletedIds = mutableListOf<Long>()
            for (item in items) {
                val trashFile = item.trashFile
                val deleted = try {
                    if (trashFile.exists()) {
                        if (trashFile.isDirectory) {
                            trashFile.deleteRecursively()
                        } else {
                            trashFile.delete()
                        }
                    } else {
                        true
                    }
                } catch (_: Exception) {
                    false
                }
                if (deleted) {
                    deletedCount++
                    deletedIds.add(item.id)
                }
            }
            if (deletedIds.isNotEmpty()) {
                trashDao?.deleteByIds(deletedIds)
            }
            // Items that could not be removed used to be folded into a success count, which the UI
            // then announced as "Permanently deleted N items". A partial batch is reported as one.
            val failed = items.size - deletedCount
            if (failed > 0) {
                throw PartialStorageOperationException(completed = deletedCount, failed = failed)
            }
            Result.success(deletedCount)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun emptyTrash(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val trashDir = getTrashDir()
            val files = trashDir.listFiles()
            if (files == null) {
                return@withContext Result.failure(
                    java.io.IOException("The Recycle Bin folder could not be read.")
                )
            }
            // Rows are removed only for content that is provably gone. Clearing the whole table
            // after a partial sweep left files sitting in .trash with no record: nothing in the UI
            // could see them, nothing could retry them, and the space was never reclaimed.
            val rows = trashDao?.getAllTrashItemsSync().orEmpty()
            val idsToRemove = mutableListOf<Long>()
            var deleted = 0
            var failed = 0
            val stuck = mutableListOf<String>()
            for (f in files) {
                currentCoroutineContext().ensureActive()
                val gone = try {
                    if (f.isDirectory) {
                        f.walkTopDown().sortedByDescending { it.absolutePath.length }
                            .forEach { node -> entryMutator.delete(node) }
                    } else {
                        entryMutator.delete(f)
                    }
                    !f.exists()
                } catch (_: Exception) {
                    false
                }
                if (gone) {
                    deleted++
                    rows.firstOrNull { it.trashPath == f.absolutePath }?.let { idsToRemove += it.id }
                } else {
                    failed++
                    stuck += f.name
                }
            }
            // A record whose file is already missing describes nothing recoverable, so it goes too.
            rows.filter { it.id !in idsToRemove && !File(it.trashPath).exists() }
                .forEach { idsToRemove += it.id }
            if (idsToRemove.isNotEmpty()) trashDao?.deleteByIds(idsToRemove)
            if (failed == 0) {
                Result.success(deleted)
            } else {
                Result.failure(PartialStorageOperationException(deleted, failed, null, stuck))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}

