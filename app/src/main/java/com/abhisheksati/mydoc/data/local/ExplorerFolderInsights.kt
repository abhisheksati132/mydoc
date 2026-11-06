package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.FolderInsights
import com.abhisheksati.mydoc.domain.model.FolderSizeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.ArrayDeque
import java.util.PriorityQueue

class ExplorerFolderInsights(
    private val maxEntries: Int = 100_000,
    private val isSymbolicLink: (File) -> Boolean = { Files.isSymbolicLink(it.toPath()) }
) {
    suspend fun scan(root: File): FolderInsights = withContext(Dispatchers.IO) {
        require(root.isDirectory)
        val boundary = root.canonicalFile.toPath()
        val pending = ArrayDeque<Pair<File, File?>>()
        pending.add(root to null)
        val folders = linkedMapOf<File, Long>()
        val largest = PriorityQueue<FolderSizeEntry>(compareBy { it.bytes })
        var total = 0L
        var files = 0
        var directories = 0
        var visited = 0
        var incomplete = false
        scan@ while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (directory, owner) = pending.removeFirst()
            val children = try { directory.listFiles() } catch (_: SecurityException) { null }
            if (children == null) { incomplete = true; continue }
            for (file in children) {
                currentCoroutineContext().ensureActive()
                if (++visited > maxEntries) { incomplete = true; break@scan }
                try {
                    if (isSymbolicLink(file) || !file.canonicalFile.toPath().startsWith(boundary)) { incomplete = true; continue }
                    if (file.isDirectory) {
                        directories++
                        val childOwner = owner ?: file.also { folders[it] = 0 }
                        pending.add(file to childOwner)
                    } else if (file.isFile && file.canRead()) {
                        val bytes = file.length()
                        total = Math.addExact(total, bytes)
                        files++
                        if (owner != null) folders[owner] = Math.addExact(folders.getValue(owner), bytes)
                        largest.add(FolderSizeEntry(file, bytes))
                        if (largest.size > 20) largest.remove()
                    } else incomplete = true
                } catch (_: java.io.IOException) { incomplete = true }
                catch (_: SecurityException) { incomplete = true }
            }
        }
        FolderInsights(total, files, directories, folders.map { FolderSizeEntry(it.key, it.value) }.sortedByDescending { it.bytes }, largest.toList().sortedByDescending { it.bytes }, incomplete)
    }
}
