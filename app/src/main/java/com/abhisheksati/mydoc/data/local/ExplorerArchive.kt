package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.ArchiveIndex
import com.abhisheksati.mydoc.domain.model.ArchiveNode
import com.abhisheksati.mydoc.domain.model.ArchiveStamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class ZipArchiveChanged : java.io.IOException("Archive changed since browsing")

/** Reads central-directory metadata only; browsing never decompresses or writes entries. */
class ExplorerArchive(private val maxEntries: Int = 10_000) {
    suspend fun read(file: File): ArchiveIndex = withContext(Dispatchers.IO) {
        val stamp = ArchiveStamp(file.length(), file.lastModified())
        val index = ZipFile(file).use { read(it) }
        if (stamp != ArchiveStamp(file.length(), file.lastModified())) throw ZipArchiveChanged()
        index.copy(stamp = stamp)
    }

    internal suspend fun read(zip: ZipFile): ArchiveIndex {
        val nodes = linkedMapOf<String, ArchiveNode>()
        val explicit = mutableSetOf<String>()
        var files = 0
        var total = 0L
        require(zip.size() <= maxEntries) { "Archive entry limit exceeded" }
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            currentCoroutineContext().ensureActive()
            val entry = entries.nextElement()
            val rawName = entry.name
            val name = rawName.replace('\\', '/')
            require(name.length <= 4096 && !name.startsWith('/') && name.none { it.isISOControl() }) { "Unsafe archive path" }
            val parts = name.removeSuffix("/").split('/')
            require(parts.size <= 64 && parts.all { it.isNotEmpty() && it != "." && it != ".." }) { "Unsafe archive path" }
            val key = parts.joinToString("/")
            require(explicit.add(key)) { "Duplicate archive path" }
            var path = ""
            parts.forEachIndexed { index, part ->
                val directory = index < parts.lastIndex || entry.isDirectory
                path += part
                val existing = nodes[path]
                require(existing == null || existing.directory == directory) { "Ambiguous archive path" }
                if (existing == null) nodes[path] = ArchiveNode(if (directory) "$path/" else path, part, directory, if (directory) 0 else entry.size)
                path += "/"
            }
            require(nodes.size <= maxEntries) { "Archive folder limit exceeded" }
            if (!entry.isDirectory) {
                files++
                require(entry.size >= 0 && total <= Long.MAX_VALUE - entry.size) { "Invalid archive size" }
                total += entry.size
            }
        }
        return ArchiveIndex(nodes.values.toList(), files, total)
    }
}
