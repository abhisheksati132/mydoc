package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions
import com.abhisheksati.mydoc.domain.model.ExplorerSearchResult
import com.abhisheksati.mydoc.domain.model.StorageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.ArrayDeque

class ExplorerSearch(private val maxResults: Int = 1_000, private val maxEntries: Int = 50_000) {
    suspend fun search(root: File, options: ExplorerSearchOptions, showHidden: Boolean): ExplorerSearchResult = withContext(Dispatchers.IO) {
        require(options.isValid)
        val boundary = root.canonicalFile.toPath()
        val pending = ArrayDeque<File>().apply { add(root) }
        val results = mutableListOf<StorageItem>()
        var visited = 0
        var incomplete = false
        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val directory = pending.removeFirst()
            val children = directory.listFiles()
            if (children == null) { incomplete = true; continue }
            for (file in children) {
                currentCoroutineContext().ensureActive()
                if (++visited > maxEntries || results.size >= maxResults) {
                    return@withContext ExplorerSearchResult(results, true)
                }
                if (!showHidden && file.name.startsWith(".")) continue
                if (Files.isSymbolicLink(file.toPath()) || !file.canonicalFile.toPath().startsWith(boundary)) continue
                val item = StorageItem(file)
                if (options.matches(item)) results += item
                if (options.recursive && item.isDirectory) pending.add(file)
            }
        }
        ExplorerSearchResult(results, incomplete)
    }
}
