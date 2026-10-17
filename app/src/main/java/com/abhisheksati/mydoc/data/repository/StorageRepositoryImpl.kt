package com.abhisheksati.mydoc.data.repository

import android.content.Context
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


class StorageRepositoryImpl(
    private val context: Context
) : StorageRepository {

    override fun getStorageVolumes(): List<StorageVolume> {
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
        } catch (_: Exception) {}

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
        } catch (_: Exception) {}

        return volumes
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
                FileSortField.NAME -> a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT))
                FileSortField.DATE -> a.lastModified.compareTo(b.lastModified)
                FileSortField.SIZE -> {
                    if (a.isDirectory) a.itemCount.compareTo(b.itemCount)
                    else a.sizeBytes.compareTo(b.sizeBytes)
                }
                FileSortField.TYPE -> a.extension.compareTo(b.extension)
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

    override suspend fun calculateHash(file: File, algorithm: String): String = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canRead() || file.isDirectory) {
                return@withContext "Unavailable"
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
            "Error: ${e.message}"
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
        }
    }

    override suspend fun unzip(
        zipFile: File,
        destinationDir: File,
        onProgress: (extractedFiles: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!zipFile.exists() || !zipFile.isFile) throw IllegalArgumentException("Zip file does not exist")
            if (!destinationDir.exists()) destinationDir.mkdirs()

            val canonicalDest = destinationDir.canonicalFile.canonicalPath
            var extractedCount = 0
            val buffer = ByteArray(32 * 1024)

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile), 32 * 1024)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val normalizedName = entry.name.replace('\\', '/')
                    val outFile = File(destinationDir, normalizedName)
                    // Zip Slip vulnerability check
                    val canonicalOut = outFile.canonicalFile.canonicalPath
                    if (!canonicalOut.startsWith(canonicalDest + File.separator) && canonicalOut != canonicalDest) {
                        throw SecurityException("Zip entry is outside destination: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        extractedCount++
                        onProgress(extractedCount)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            destinationDir
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
                // DOWNLOADS is handled above by short-circuit (directory listing), never reaches here
                else -> return@withContext emptyList()
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
        } catch (_: Exception) {}
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
            } catch (_: Exception) {}
        }
        results
    }
}

