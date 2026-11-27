package com.abhisheksati.mydoc.data.archive

import com.abhisheksati.mydoc.domain.archive.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS

/** Publish only fully verified content. Every final move refuses replacement. */
internal object ArchiveMerge {
    suspend fun publish(stage: File, parent: File, conflict: ArchiveConflictPolicy, limit: Int): File {
        require(conflict != ArchiveConflictPolicy.NEW_FOLDER)
        val destination = parent.canonicalFile
        val directories = mutableMapOf<String, File?>("" to destination)
        try {
            val paths = Files.walk(stage.toPath()).use { stream ->
                stream.limit(limit.toLong() + 2).toArray().map { it as java.nio.file.Path }
                    .sortedBy { it.nameCount }
            }
            require(paths.size <= limit + 1)
            val directoryFlags = paths.associateWith { Files.isDirectory(it, NOFOLLOW_LINKS) }
            for (path in paths.drop(1)) {
                currentCoroutineContext().ensureActive()
                require(!Files.isSymbolicLink(path))
                val relative = ArchiveListingParser.safePath(stage.toPath().relativize(path).toString().replace('\\', '/'))
                val folder = directories[relative.substringBeforeLast('/', "")] ?: continue
                var target = File(folder, path.fileName.toString())
                val exists = Files.exists(target.toPath(), NOFOLLOW_LINKS)
                if (directoryFlags[path] == true) {
                    if (exists && Files.isDirectory(target.toPath(), NOFOLLOW_LINKS)) directories[relative] = target
                    else if (exists && conflict == ArchiveConflictPolicy.SKIP_EXISTING) directories[relative] = null
                    else {
                        if (exists) target = unique(folder, target.name)
                        ArchiveNativeFiles.directory(target)
                        directories[relative] = target
                    }
                } else {
                    require(Files.isRegularFile(path, NOFOLLOW_LINKS))
                    if (exists && conflict == ArchiveConflictPolicy.SKIP_EXISTING) continue
                    if (exists) target = unique(folder, target.name)
                    ArchiveNativeFiles.move(path.toFile(), target)
                }
            }
            for (path in paths.asReversed()) {
                currentCoroutineContext().ensureActive()
                ArchiveNativeFiles.delete(path.toFile(), directoryFlags[path] == true)
            }
            return destination
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { throw ArchiveMergeRecovery(stage) }
    }

    private fun unique(parent: File, requested: String): File {
        val base = requested.substringBeforeLast('.', requested)
        val suffix = requested.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        for (index in 1..10_000) {
            val file = File(parent, "$base ($index)$suffix")
            if (!Files.exists(file.toPath(), NOFOLLOW_LINKS)) return file
        }
        error("Too many conflicting names")
    }
}
