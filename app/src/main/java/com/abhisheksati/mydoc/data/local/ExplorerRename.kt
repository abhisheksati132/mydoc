package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.UUID
import java.util.Properties
import java.io.FileOutputStream

/** All writes are same-directory, no-replacement moves. A failed batch rolls back in two phases. */
class ExplorerRename(
    private val journalDirectory: File? = null,
    private val move: (File, File) -> Unit = { from, to -> Files.move(from.toPath(), to.toPath()); Unit }
) {
    /** Read-only recovery details survive process death. Never guess which colliding file to replace. */
    suspend fun pendingRecovery(): List<String> = withContext(Dispatchers.IO) {
        journalDirectory?.listFiles()?.filter { it.extension == "properties" }?.flatMap { journal ->
            val props = Properties().apply { journal.inputStream().use { load(it) } }
            (0 until (props.getProperty("count")?.toIntOrNull() ?: 0).coerceIn(0, 10_000)).map { i ->
                "${props.getProperty("source$i")} → ${props.getProperty("target$i")}\n${props.getProperty("staging$i")}"
            }
        }.orEmpty()
    }

    suspend fun clearRecoveryJournals(): Unit = withContext(Dispatchers.IO) {
        val dir = journalDirectory ?: return@withContext
        val archive = File(dir, "archive").apply { if (!exists()) mkdirs() }
        dir.listFiles()?.filter { it.extension == "properties" }?.forEach {
            runCatching {
                val dest = File(archive, it.name)
                if (!it.renameTo(dest)) {
                    it.copyTo(dest, overwrite = true)
                    it.delete()
                }
            }
        }
    }
    suspend fun preview(files: List<File>, options: ExplorerRenameOptions): ExplorerRenamePlan = withContext(Dispatchers.IO) {
        val sources = files.distinctBy { it.absolutePath }.sortedBy { it.absolutePath }
        val sourcePaths = sources.map { it.absolutePath }.toSet()
        val entries = sources.mapIndexed { index, source ->
            val extension = if (options.preserveExtension && source.isFile && source.nameWithoutExtension.isNotEmpty() && !source.name.startsWith(".")) source.extension else ""
            val stem = if (extension.isNotEmpty()) source.name.removeSuffix(".$extension") else source.name
            val replaced = if (options.find.isEmpty()) stem else stem.replace(options.find, options.replacement)
            val number = if (options.numbered) "_" + (options.start.toLong() + index).toString().padStart(3, '0') else ""
            val name = options.prefix + replaced + options.suffix + number + if (extension.isEmpty()) "" else ".$extension"
            val target = File(source.parentFile, name)
            val problem = when {
                !source.exists() || Files.isSymbolicLink(source.toPath()) -> "source"
                name.isBlank() || name in setOf(".", "..") || name.any { it.isISOControl() || it in "/\\:*?\"<>|" } || name.toByteArray(Charsets.UTF_8).size > 255 -> "name"
                options.start < 0 -> "number"
                // On case-insensitive storage (emulated /sdcard) a case-only rename targets the
                // source itself; exists() alone must not condemn it as a collision.
                target.exists() && target.absolutePath !in sourcePaths && !Files.isSameFile(source.toPath(), target.toPath()) -> "conflict"
                sources.any { other -> other != source && source.toPath().startsWith(other.toPath()) } -> "nested"
                else -> null
            }
            ExplorerRenameEntry(source, target, source.length(), source.lastModified(), source.isDirectory, problem)
        }
        val duplicates = entries.groupBy { it.target.absolutePath.lowercase(Locale.ROOT) }.filterValues { it.size > 1 }.keys
        ExplorerRenamePlan(entries.map { if (it.target.absolutePath.lowercase(Locale.ROOT) in duplicates) it.copy(problem = "conflict") else it })
    }

    suspend fun apply(plan: ExplorerRenamePlan): ExplorerRenameResult = withContext(Dispatchers.IO) {
        if (!plan.canApply) return@withContext ExplorerRenameResult(false)
        val changed = plan.entries.filter { it.source != it.target }
        // Once staging begins cancellation must not strand originals under private names.
        withContext(NonCancellable) {
            val locations = changed.associateWith { it.source }.toMutableMap()
            val stagingNames = changed.associateWith { File(it.source.parentFile, ".mydoc-rename-${UUID.randomUUID()}") }
            var journal: File? = null
            try {
                check(plan.entries.all { it.source.exists() && !Files.isSymbolicLink(it.source.toPath()) && it.source.length() == it.size && it.source.lastModified() == it.modified && it.source.isDirectory == it.directory })
                journalDirectory?.let { directory ->
                    check(directory.isDirectory || directory.mkdirs())
                    val props = Properties().apply {
                        setProperty("count", changed.size.toString())
                        changed.forEachIndexed { i, entry ->
                            setProperty("source$i", entry.source.absolutePath)
                            setProperty("target$i", entry.target.absolutePath)
                            setProperty("staging$i", stagingNames.getValue(entry).absolutePath)
                        }
                    }
                    journal = File.createTempFile("rename-", ".properties", directory)
                    FileOutputStream(journal).use { props.store(it, "MyDoc interrupted rename recovery"); it.fd.sync() }
                }
                for (entry in changed) {
                    val staging = stagingNames.getValue(entry)
                    move(entry.source, staging)
                    locations[entry] = staging
                }
                for (entry in changed) {
                    move(locations.getValue(entry), entry.target)
                    locations[entry] = entry.target
                }
                journal?.delete()
                ExplorerRenameResult(true)
            } catch (_: Exception) {
                // Evacuate published names first so swaps/chains can be restored without collisions.
                for (entry in changed) {
                    val current = locations.getValue(entry)
                    if (current == entry.target) {
                        val recovery = stagingNames.getValue(entry)
                        runCatching { move(current, recovery); locations[entry] = recovery }
                    }
                }
                for (entry in changed) {
                    val current = locations.getValue(entry)
                    if (current != entry.source) runCatching { move(current, entry.source); locations[entry] = entry.source }
                }
                val unresolved = locations.filter { (entry, file) -> file != entry.source }.values.map { it.absolutePath }
                if (unresolved.isEmpty()) journal?.delete()
                ExplorerRenameResult(false, unresolved)
            }
        }
    }
}
