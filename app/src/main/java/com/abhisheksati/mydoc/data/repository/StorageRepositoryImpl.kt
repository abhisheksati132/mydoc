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
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.domain.repository.StorageRepository
import kotlinx.coroutines.CancellationException
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
    private val trashDao: TrashDao? = null
) : StorageRepository {

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
        try {
            if (!targetDir.exists() || !targetDir.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Target must be a directory"))
            }

            var copied = 0
            val total = sources.size
            val targetCanon = targetDir.canonicalPath
            for (src in sources) {
                if (!src.exists()) continue
                if (src.isDirectory) {
                    val srcCanon = src.canonicalPath
                    if (targetCanon == srcCanon || targetCanon.startsWith(srcCanon + File.separator)) {
                        continue // Prevent cyclic copy into self/child
                    }
                }
                val dest = getNonConflictingFile(targetDir, src.name)
                if (src.isDirectory) {
                    src.copyRecursively(dest, overwrite = true)
                } else {
                    src.copyTo(dest, overwrite = true)
                }
                copied++
                onProgress(copied, total)
            }
            Result.success(copied)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun move(
        sources: List<File>,
        targetDir: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists() || !targetDir.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Target must be a directory"))
            }

            var moved = 0
            val total = sources.size
            val targetCanon = targetDir.canonicalPath
            for (src in sources) {
                if (!src.exists()) continue
                if (src.isDirectory) {
                    val srcCanon = src.canonicalPath
                    if (targetCanon == srcCanon || targetCanon.startsWith(srcCanon + File.separator)) {
                        continue // Prevent cyclic move into self/child
                    }
                }
                val dest = getNonConflictingFile(targetDir, src.name)

                // Try quick filesystem atomic rename first
                val renamed = src.renameTo(dest)
                if (renamed) {
                    moved++
                } else {
                    // Fallback to copy + delete across mount boundaries
                    if (src.isDirectory) {
                        src.copyRecursively(dest, overwrite = true)
                        src.deleteRecursively()
                    } else {
                        src.copyTo(dest, overwrite = true)
                        src.delete()
                    }
                    moved++
                }
                onProgress(moved, total)
            }
            Result.success(moved)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
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
            android.util.Log.w("StorageRepository", "Checksum failed for " + file.name, e)
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

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destFile), 32 * 1024)).use { zos ->
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
                    onProgress(current, total)
                }
            }
            destFile
        }.onFailure { if (it is CancellationException) throw it }
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
                        }
                    } else {
                        val parent = outFile.parentFile ?: destinationDir
                        val canonicalParent = parent.canonicalFile.canonicalPath
                        if (!canonicalParent.startsWith(canonicalDest + File.separator) && canonicalParent != canonicalDest) {
                            throw SecurityException("Zip entry parent is outside destination: ${entry.name}")
                        }
                        if (!parent.exists()) {
                            parent.mkdirs()
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

                        val tempFile = File(parent, ".tmp_${System.currentTimeMillis()}_${targetFile.name}")
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
        }.onFailure { if (it is CancellationException) throw it }
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
                            "${MediaStore.Files.FileColumns.DATA} LIKE '%.epub'"
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

            // Safe reconciliation for orphaned trash files (files on disk without DB records)
            try {
                val trashDir = getTrashDir()
                val allDbItems = trashDao?.getAllTrashItemsSync() ?: emptyList()
                val knownTrashPaths = allDbItems.map { File(it.trashPath).canonicalPath }.toSet()
                val diskFiles = trashDir.listFiles() ?: emptyArray()
                for (diskFile in diskFiles) {
                    val canonicalDiskPath = try { diskFile.canonicalPath } catch (_: Exception) { null } ?: continue
                    if (canonicalDiskPath !in knownTrashPaths) {
                        // Orphaned file on disk. Check age: if older than 24h, purge safely
                        val fileAge = now - diskFile.lastModified()
                        if (fileAge > 24L * 60 * 60 * 1000) {
                            if (diskFile.isDirectory) diskFile.deleteRecursively() else diskFile.delete()
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore reconciliation errors
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

            for (file in files) {
                if (!file.exists()) continue
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

                val moved = file.renameTo(destTrashFile) || run {
                    if (file.isDirectory) {
                        file.copyRecursively(destTrashFile, overwrite = true)
                        file.deleteRecursively()
                    } else {
                        file.copyTo(destTrashFile, overwrite = true)
                        file.delete()
                    }
                    destTrashFile.exists()
                }

                if (moved) {
                    // Stamp the trash copy with the trash time so the orphan reconciliation in
                    // purgeExpiredTrash (which keys on mtime) never treats a just-trashed file,
                    // whose DB insert may still be in flight, as an old orphan and deletes it.
                    destTrashFile.setLastModified(now)
                    scannedPaths += file.absolutePath
                    val entity = TrashEntity(
                        originalPath = file.absolutePath,
                        trashPath = destTrashFile.absolutePath,
                        displayName = file.name,
                        sizeBytes = if (destTrashFile.isDirectory) 0L else destTrashFile.length(),
                        deletedTimestamp = now,
                        isDirectory = destTrashFile.isDirectory
                    )
                    val id = trashDao?.insert(entity) ?: 0L
                    trashed.add(entity.copy(id = id).toDomain())
                }
            }

            // One awaited scan per file capped every delete at 2s; a batch costs one IPC.
            scanFilesAwait(context, scannedPaths)

            // Transactional purge of expired items and orphaned files
            purgeExpiredTrash()

            Result.success(trashed)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun restoreFromTrash(items: List<TrashItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var restoredCount = 0
            val restoredPaths = mutableListOf<String>()
            for (item in items) {
                val trashFile = item.trashFile
                if (!trashFile.exists()) {
                    trashDao?.deleteByIds(listOf(item.id))
                    continue
                }

                val originalParent = item.originalFile.parentFile ?: continue
                if (!originalParent.exists()) originalParent.mkdirs()

                val destFile = getNonConflictingFile(originalParent, item.displayName)
                val restored = trashFile.renameTo(destFile) || run {
                    if (trashFile.isDirectory) {
                        trashFile.copyRecursively(destFile, overwrite = true)
                        trashFile.deleteRecursively()
                    } else {
                        trashFile.copyTo(destFile, overwrite = true)
                        trashFile.delete()
                    }
                    destFile.exists()
                }

                if (restored) {
                    restoredPaths += destFile.absolutePath
                    trashDao?.deleteByIds(listOf(item.id))
                    restoredCount++
                }
            }
            scanFilesAwait(context, restoredPaths)
            Result.success(restoredCount)
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
            Result.success(deletedCount)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun emptyTrash(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val trashDir = getTrashDir()
            val files = trashDir.listFiles() ?: emptyArray()
            var count = 0
            for (f in files) {
                if (f.isDirectory) {
                    if (f.deleteRecursively()) count++
                } else {
                    if (f.delete()) count++
                }
            }
            trashDao?.clearAll()
            Result.success(count)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}

