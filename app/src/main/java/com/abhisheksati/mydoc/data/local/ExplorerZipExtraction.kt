package com.abhisheksati.mydoc.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipFile
import com.abhisheksati.mydoc.domain.model.ArchiveStamp

class ZipPreviewCacheFull : IOException("Archive preview cache is full")
class ZipExtractionRecovery(val directory: File, cause: Throwable) : IOException("Extraction cleanup incomplete", cause)

/** Selected entries are staged in a fresh directory, never merged into user folders. */
class ExplorerZipExtraction(
    private val maxBytes: Long = 512L * 1024 * 1024,
    private val previewCacheBytes: Long = 256L * 1024 * 1024
) {
    suspend fun extract(archive: File, selected: Set<String>, parent: File,
                        expected: ArchiveStamp? = null,
                        onProgress: (Int, Int) -> Unit = { _, _ -> }): File = withContext(Dispatchers.IO) {
        perform(archive, selected, parent, maxBytes, null, expected, onProgress)
    }

    suspend fun preview(archive: File, member: String, cacheRoot: File, expected: ArchiveStamp? = null): File = withContext(Dispatchers.IO) {
        previewLock.withLock {
            check(cacheRoot.isDirectory || cacheRoot.mkdirs())
            require(!Files.isSymbolicLink(cacheRoot.toPath()))
            perform(archive, setOf(member), cacheRoot, minOf(maxBytes, 128L * 1024 * 1024), previewCacheBytes, expected) { _, _ -> }
                .resolve(member)
        }
    }

    private suspend fun perform(archive: File, selected: Set<String>, parent: File, limit: Long,
                                cacheLimit: Long?, expected: ArchiveStamp?, onProgress: (Int, Int) -> Unit): File {
        require(selected.isNotEmpty() && parent.isDirectory)
        require(!Files.isSymbolicLink(archive.toPath()))
        val length = archive.length()
        val modified = archive.lastModified()
        if (expected != null && expected != ArchiveStamp(length, modified)) throw ZipArchiveChanged()
        fun unchanged() = archive.isFile && !Files.isSymbolicLink(archive.toPath()) && archive.length() == length && archive.lastModified() == modified
        val destination = parent.canonicalFile
        var staging: File? = null
        val createdFiles = mutableListOf<File>()
        val createdDirectories = mutableListOf<File>()
        try {
            ZipFile(archive).use { zip ->
                val index = ExplorerArchive().read(zip)
                require(unchanged()) { "Archive changed" }
                require(selected.all { path -> index.entries.any { it.path == path } }) { "Unknown selection" }
                val entries = index.entries.filter { node -> selected.any { node.path == it || it.endsWith('/') && node.path.startsWith(it) } }
                val files = entries.filterNot { it.directory }
                if (cacheLimit != null) require(selected.size == 1 && files.size == 1 && entries.none { it.directory }) { "Choose a file to open" }
                var expected = 0L
                for (file in files) {
                    if (file.bytes > limit - expected) throw IOException("Selected entries exceed size limit")
                    expected += file.bytes
                }
                val cachedBytes = if (cacheLimit == null) 0L else cachedBytes(destination)
                if (cacheLimit != null && expected > cacheLimit - cachedBytes) throw ZipPreviewCacheFull()
                if (expected > 0 && destination.usableSpace - 16L * 1024 * 1024 < expected) throw IOException("Not enough free space for extraction")
                currentCoroutineContext().ensureActive()
                val stage = Files.createTempDirectory(destination.toPath(), ".mydoc-extract-").toFile().also { staging = it }
                createdDirectories += stage
                fun safe(file: File) = file.canonicalFile.toPath().startsWith(stage.toPath()) &&
                    file.canonicalFile.toPath() == file.toPath().toAbsolutePath().normalize() && destination.canonicalFile == destination
                fun directory(file: File) {
                    val relative = file.relativeTo(stage)
                    var current = stage
                    for (part in relative.invariantSeparatorsPath.split('/').filter { it.isNotEmpty() && it != "." }) {
                        current = File(current, part)
                        check(safe(current))
                        if (!current.exists()) { Files.createDirectory(current.toPath()); createdDirectories += current }
                        check(current.isDirectory && !Files.isSymbolicLink(current.toPath()))
                    }
                }
                var bytes = 0L
                var completed = 0
                for (node in entries) {
                    currentCoroutineContext().ensureActive()
                    check(unchanged()) { "Archive changed" }
                    val target = File(stage, node.path.removeSuffix("/"))
                    check(safe(target))
                    if (node.directory) { directory(target); continue }
                    directory(requireNotNull(target.parentFile))
                    val entry = requireNotNull(zip.getEntry(node.path))
                    val crc = CRC32()
                    var count = 0L
                    zip.getInputStream(entry).use { input ->
                        Files.newOutputStream(target.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { output ->
                            createdFiles += target
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                check(safe(target))
                                val read = input.read(buffer)
                                if (read == -1) break
                                if (read > limit - bytes || cacheLimit != null && read > cacheLimit - cachedBytes - bytes) throw IOException("Selected entries exceed size limit")
                                bytes += read; count += read
                                if (bytes > 5L * 1024 * 1024 && bytes / length.coerceAtLeast(1) > 100) throw IOException("Unsafe archive expansion ratio")
                                crc.update(buffer, 0, read)
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    check(count == node.bytes && entry.crc >= 0 && crc.value == entry.crc) { "Archive checksum or size mismatch" }
                    onProgress(++completed, files.size)
                }
                currentCoroutineContext().ensureActive()
                check(unchanged() && safe(stage))
                val base = archive.nameWithoutExtension.filter { !it.isISOControl() && it !in "/\\:*?\"<>|" }.take(40).ifBlank { "Archive" }
                val published = File(destination, "$base extracted-${UUID.randomUUID().toString().take(8)}")
                Files.move(stage.toPath(), published.toPath())
                staging = null
                return published
            }
        } catch (error: Throwable) {
            val stage = staging
            if (stage != null) {
                // Delete recorded entries only. Foreign additions to staging are never traversed.
                (createdFiles.asReversed() + createdDirectories.asReversed()).forEach { file ->
                    runCatching { if (file.canonicalFile.toPath().startsWith(stage.toPath())) Files.deleteIfExists(file.toPath()) }
                }
                if (stage.exists()) throw ZipExtractionRecovery(stage, error)
            }
            throw error
        }
    }

    private suspend fun cachedBytes(root: File): Long {
        var bytes = 0L
        var visited = 0
        Files.walk(root.toPath()).use { paths ->
            val iterator = paths.iterator()
            while (iterator.hasNext()) {
                currentCoroutineContext().ensureActive()
                val path = iterator.next()
                if (++visited > 10_000 || Files.isSymbolicLink(path)) throw ZipPreviewCacheFull()
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    val size = Files.size(path)
                    if (size > previewCacheBytes - bytes) throw ZipPreviewCacheFull()
                    bytes += size
                }
            }
        }
        return bytes
    }

    private companion object { val previewLock = Mutex() }
}
