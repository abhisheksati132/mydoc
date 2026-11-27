package com.abhisheksati.mydoc.data.archive

import android.content.Context
import com.abhisheksati.mydoc.domain.archive.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.zip.CRC32

/** Multi-format operations with app-controlled output paths and transactional publication. */
class NativeArchiveEngine(
    private val context: Context,
    private val policy: ArchivePolicy = ArchivePolicy(),
    onControl: (ArchiveProcessControl?) -> Unit = {},
    private val onProgress: (ArchiveProgress) -> Unit = {}
) {
    private val runner = ArchiveProcessRunner(context, onControl) { onProgress(ArchiveProgress(it)) }

    suspend fun list(file: File, password: CharArray? = null): ArchiveListing = readable(file, password) { listContainer(it, password) }

    suspend fun extract(file: File, parent: File, selected: Set<String>? = null, password: CharArray? = null,
                        conflict: ArchiveConflictPolicy = ArchiveConflictPolicy.NEW_FOLDER): File =
        readable(file, password) {
            val output = extractContainer(it, parent, selected, password)
            if (conflict == ArchiveConflictPolicy.NEW_FOLDER) output else ArchiveMerge.publish(output, parent, conflict, policy.maxEntries)
        }

    private suspend fun <T> readable(file: File, password: CharArray?, block: suspend (File) -> T): T = withContext(Dispatchers.IO) {
        val listing = listContainer(file, password)
        if (listing.type.lowercase() !in setOf("gzip", "bzip2", "xz") || listing.files.size != 1 ||
            !listing.files.single().name.endsWith(".tar", true)) return@withContext block(file)
        val before = snapshot(file)
        val work = workspace()
        try {
            val folder = extractContainer(file, work, null, password)
            val tar = File(folder, listing.files.single().path)
            val result = block(tar)
            checkUnchanged(before)
            result
        } finally { work.deleteRecursively() }
    }

    private suspend fun listContainer(file: File, password: CharArray?): ArchiveListing = withContext(Dispatchers.IO) {
        val before = snapshot(file)
        var text = runner.text(listOf("l", "-smemx256m", "-slt", "-sccUTF-8", "-bsp0", "--", before.first().file.absolutePath), password, policy.maxMetadataBytes)
        val type = text.lineSequence().lastOrNull { it.startsWith("Type = ") }?.substringAfter("Type = ")
        val storedName = text.substringAfter("----------", "").lineSequence().firstOrNull { it.startsWith("Path = ") }?.substringAfter("Path = ")?.takeIf { it.isNotEmpty() }
        if (type in setOf("bzip2", "xz") || type == "gzip" && storedName == null) {
            // These streams have no member name; bzip2 also omits expanded size.
            // Measure bounded output rather than trusting an absent size field.
            var size = 0L
            val execution = currentCoroutineContext()
            runner.execute(listOf("x", "-so", "-smemx256m", "-bso0", "-bsp0", "--", before.first().file.absolutePath), password) { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    execution.ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(count <= policy.maxExpandedBytes - size) { "Archive expanded-byte limit exceeded" }
                    size += count
                }
            }
            val inferred = file.name.substringBeforeLast('.') + if (file.extension.lowercase() in setOf("txz", "tbz", "tbz2", "tgz")) ".tar" else ""
            val path = ArchiveListingParser.safePath(storedName ?: inferred, policy)
            text = text.substringBefore("----------") + "----------\nPath = $path\nSize = $size\n\n"
        }
        checkUnchanged(before)
        ArchiveListingParser(policy).parse(text)
    }

    suspend fun test(file: File, password: CharArray? = null) = withContext(Dispatchers.IO) {
        val listing = list(file, password)
        if (listing.encrypted && password == null) throw ArchiveFailure(ArchiveProblem.PASSWORD_REQUIRED)
        val before = snapshot(file)
        runner.text(listOf("t", "-smemx256m", "-bd", "-bsp2", "-sccUTF-8", "--", before.first().file.absolutePath), password, policy.maxMetadataBytes)
        checkUnchanged(before)
    }

    private suspend fun extractContainer(file: File, parent: File, selected: Set<String>?, password: CharArray?): File = withContext(Dispatchers.IO) {
        require(parent.isDirectory && !Files.isSymbolicLink(parent.toPath()))
        val before = snapshot(file)
        val listing = listContainer(file, password)
        val choices = selected?.map { ArchiveListingParser.safePath(it, policy) }?.toSet()
        require(choices == null || choices.isNotEmpty() && choices.all { path -> listing.members.any { it.path == path } }) { "Unknown archive selection" }
        val members = listing.members.filter { member -> choices == null || choices.any { member.path == it || member.path.startsWith("$it/") } }
        val files = members.filterNot { it.directory }
        if (password == null && files.any { it.encrypted }) throw ArchiveFailure(ArchiveProblem.PASSWORD_REQUIRED)
        val bytes = files.sumOf { it.bytes }
        require(bytes <= policy.maxExpandedBytes)
        if (parent.usableSpace < bytes + 16L * 1024 * 1024) throw ArchiveFailure(ArchiveProblem.NO_SPACE)
        val work = workspace()
        val stage = stageDirectory(parent.canonicalFile, ".mydoc-extract-")
        val made = mutableListOf(stage to true)
        var published = false
        try {
            fun directory(relative: String): File {
                var current = stage
                for (part in relative.split('/').filter { it.isNotEmpty() }) {
                    current = File(current, part)
                    require(current.canonicalFile.toPath().startsWith(stage.toPath()) && !Files.isSymbolicLink(current.toPath()))
                    if (!current.exists()) { ArchiveNativeFiles.directory(current); made += current to true }
                    require(current.isDirectory)
                }
                return current
            }
            members.filter { it.directory }.forEach { directory(it.path) }
            if (files.isNotEmpty()) {
                val selectors = File(work, "members.txt").apply { writeText(files.joinToString("\n") { it.selector } + "\n", Charsets.UTF_8) }
                val execution = currentCoroutineContext()
                var completed = 0L
                val include = if (listing.type.lowercase() in setOf("gzip", "bzip2", "xz")) emptyList() else listOf("-i@${selectors.absolutePath}")
                runner.execute(listOf("x", "-smemx256m", "-so", "-bd", "-bso0", "-bsp0", "-bse2", "-spd", "-scsUTF-8") + include + listOf("--",
                    before.first().file.absolutePath), password) { input ->
                    val buffer = ByteArray(64 * 1024)
                    for (member in files) {
                        execution.ensureActive()
                        val folder = member.path.substringBeforeLast('/', "").let { if (it.isEmpty()) stage else directory(it) }
                        val output = File(folder, member.name)
                        require(output.canonicalFile.toPath().startsWith(stage.toPath()) && !Files.isSymbolicLink(output.toPath()))
                        val crc = CRC32()
                        ArchiveNativeFiles.output(output).use { stream ->
                            made += output to false
                            var remaining = member.bytes
                            while (remaining > 0) {
                                execution.ensureActive()
                                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                                if (read < 0) throw EOFException("Incomplete archive member")
                                stream.write(buffer, 0, read)
                                crc.update(buffer, 0, read)
                                remaining -= read
                                completed += read
                                onProgress(ArchiveProgress(if (bytes == 0L) 0 else ((completed.toDouble() / bytes) * 100).toInt().coerceIn(0, 100), completed, bytes, member.path))
                            }
                        }
                        if (member.crc32 != null && member.crc32 != crc.value) throw ArchiveFailure(ArchiveProblem.CORRUPT)
                    }
                    if (input.read() != -1) throw ArchiveFailure(ArchiveProblem.CORRUPT)
                }
            }
            checkUnchanged(before)
            val destination = unique(parent.canonicalFile, file.name.substringBeforeLast('.').take(60).ifBlank { "Archive" } + " extracted")
            ArchiveNativeFiles.move(stage, destination)
            published = true
            destination
        } finally {
            work.deleteRecursively()
            if (!published) made.asReversed().forEach { (candidate, directory) ->
                runCatching {
                    // Never follow a replaced parent while cleaning user storage.
                    if (candidate == stage || candidate.parentFile?.canonicalFile?.toPath()?.startsWith(stage.toPath()) == true) {
                        ArchiveNativeFiles.delete(candidate, directory)
                    }
                }
            }
        }
    }

    suspend fun create(sources: List<File>, parent: File, name: String, options: ArchiveCreateOptions,
                       password: CharArray? = null): List<File> = withContext(Dispatchers.IO) {
        require(parent.isDirectory)
        val targetParent = parent.canonicalFile
        require(name.isNotBlank() && name !in listOf(".", "..") && name.none { it.isISOControl() || it in "/\\:" } && name.toByteArray().size <= 200) { "Invalid archive name" }
        require(password == null || options.format.encryption) { "This format does not support passwords" }
        require(!options.encryptNames || password != null) { "Filename encryption requires a password" }
        val work = workspace()
        try {
            val input = File(work, "input").apply { check(mkdir()) }
            val execution = currentCoroutineContext()
            var entries = 0
            var expanded = 0L
            val names = hashSetOf<String>()
            for (original in sources) {
                require(original.exists() && !Files.isSymbolicLink(original.toPath())) { "Source is unavailable or a link" }
                require(names.add(original.name)) { "Selected files have duplicate names" }
                val source = original.canonicalFile
                Files.walk(source.toPath()).use { paths ->
                    val iterator = paths.iterator()
                    while (iterator.hasNext()) {
                        execution.ensureActive()
                        val path = iterator.next()
                        require(++entries <= policy.maxEntries && !Files.isSymbolicLink(path)) { "Source contains links or too many entries" }
                        val relative = source.toPath().relativize(path).toString().replace('\\', '/')
                        val nameInArchive = ArchiveListingParser.safePath(source.name + if (relative.isEmpty()) "" else "/$relative", policy)
                        val output = File(input, nameInArchive)
                        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                            check(output.isDirectory || output.mkdirs())
                        } else {
                            require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) { "Unsupported source file" }
                            val length = Files.size(path)
                            require(length <= policy.maxExpandedBytes - expanded) { "Archive input size limit exceeded" }
                            if (context.cacheDir.usableSpace < length + 32L * 1024 * 1024) throw ArchiveFailure(ArchiveProblem.NO_SPACE)
                            expanded += length
                            val modified = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS)
                            output.parentFile?.mkdirs()
                            Files.newInputStream(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS).use { stream ->
                                output.outputStream().use { target ->
                                    val buffer = ByteArray(64 * 1024)
                                    var copied = 0L
                                    while (true) {
                                        execution.ensureActive()
                                        val count = stream.read(buffer)
                                        if (count < 0) break
                                        require(count <= length - copied) { "Source changed during staging" }
                                        target.write(buffer, 0, count)
                                        copied += count
                                    }
                                    require(copied == length && Files.size(path) == length && Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS) == modified) { "Source changed during staging" }
                                }
                            }
                            output.setLastModified(modified.toMillis())
                        }
                    }
                }
            }
            val output = File(work, name)
            val compressedTar = options.format in setOf(ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_BZIP2, ArchiveFormat.TAR_XZ)
            val createSource = if (compressedTar) {
                val tarDirectory = File(work, "tar-source").apply { check(mkdir()) }
                val suffix = ".${options.format.extension}"
                val stem = if (name.endsWith(suffix, true)) name.dropLast(suffix.length) else name.substringBeforeLast('.', name)
                val tar = File(tarDirectory, "${stem.ifBlank { "Archive" }}.tar")
                runCreate(input.listFiles()!!.map { it.name }, input, tar, ArchiveCreateOptions(format = ArchiveFormat.TAR), null, work)
                listOf(tar.absolutePath)
            } else input.listFiles()!!.map { it.name }
            runCreate(createSource, input, output, options, password, work)
            val generated = if (options.splitBytes == null) listOf(output) else
                work.listFiles().orEmpty().filter { it.name.matches(Regex(Regex.escape(name) + "\\.\\d{3,}")) }.sortedBy { it.name.substringAfterLast('.').toInt() }
            require(generated.isNotEmpty()) { "Archive engine produced no output" }
            test(generated.first(), password)
            val publishBytes = generated.sumOf { it.length() }
            if (parent.usableSpace < publishBytes + 16L * 1024 * 1024) throw ArchiveFailure(ArchiveProblem.NO_SPACE)
            if (generated.size == 1 && options.splitBytes == null) {
                val destination = unique(targetParent, name)
                publishFile(generated.first(), destination)
                listOf(destination)
            } else {
                val stage = stageDirectory(targetParent, ".mydoc-volumes-")
                val made = mutableListOf<File>()
                var published = false
                try {
                    generated.forEach { part -> File(stage, part.name).also { publishFile(part, it); made += it } }
                    val destination = unique(targetParent, "$name parts")
                    ArchiveNativeFiles.move(stage, destination)
                    published = true
                    made.map { File(destination, it.name) }
                } finally {
                    if (!published) { made.asReversed().forEach { ArchiveNativeFiles.delete(it) }; ArchiveNativeFiles.delete(stage, true) }
                }
            }
        } finally { work.deleteRecursively() }
    }

    private suspend fun runCreate(sources: List<String>, directory: File, output: File, options: ArchiveCreateOptions,
                                  password: CharArray?, work: File) {
        val selectors = File(work, "sources-${UUID.randomUUID()}.txt").apply { writeText(sources.joinToString("\n") + "\n", Charsets.UTF_8) }
        val args = mutableListOf("a", "-t${options.format.type}", "-mx=${options.compression.level}", "-mmt=${options.threads}",
            "-bsp2", "-bso0", "-bse2", "-scsUTF-8", "-sccUTF-8", "-spd", "-snl")
        if (options.format == ArchiveFormat.SEVEN_ZIP) {
            args += "-ms=${if (options.solid) "on" else "off"}"
            if (options.compression != ArchiveCompression.STORE) args += "-md=${options.dictionaryMiB}m"
            if (options.encryptNames) args += "-mhe=on"
        }
        if (password != null) {
            args += "-p"
            if (options.format == ArchiveFormat.ZIP) args += "-mem=AES256"
        }
        options.splitBytes?.let { args += "-v${it}b" }
        // -- disables @list-file expansion as well as switch parsing. Use the
        // explicit include switch before it, keeping archive paths literal.
        args += listOf("-i@${selectors.absolutePath}", "--", output.absolutePath)
        runner.text(args, password, policy.maxMetadataBytes, directory)
    }

    /** Rebuild a verified copy. The original archive is never modified. */
    suspend fun editCopy(file: File, parent: File, remove: Set<String> = emptySet(),
                         renames: Map<String, String> = emptyMap(), additions: List<File> = emptyList(),
                         password: CharArray? = null): List<File> = withContext(Dispatchers.IO) {
        val original = snapshot(file)
        require(original.size == 1 && !Regex(".*\\.\\d{3,}").matches(file.name)) { "Split archives are read only" }
        val listing = list(file, password)
        val format = when (listing.type.lowercase()) {
            "zip" -> ArchiveFormat.ZIP
            "7z" -> ArchiveFormat.SEVEN_ZIP
            else -> throw ArchiveFailure(ArchiveProblem.UNSUPPORTED)
        }
        require(!listing.encrypted || password != null) { "A password is required to save an encrypted copy" }
        require(remove.isNotEmpty() || renames.isNotEmpty() || additions.isNotEmpty())
        val paths = listing.members.map { it.path }.toSet()
        require(remove.all { it in paths } && renames.keys.all { it in paths })
        val normalizedRenames = renames.mapValues { ArchiveListingParser.safePath(it.value, policy) }
        val work = workspace()
        try {
            val contents = extract(file, work, password = password)
            remove.sortedByDescending { it.length }.forEach { File(contents, it).deleteRecursively() }
            for ((from, to) in normalizedRenames) {
                val source = File(contents, from)
                require(source.exists()) { "Renamed entry was removed" }
                val target = File(contents, to)
                require(!target.exists() && !to.startsWith("$from/")) { "The new path conflicts with an entry" }
                target.parentFile?.let { check(it.isDirectory || it.mkdirs()) }
                ArchiveNativeFiles.move(source, target)
            }
            val retained = contents.listFiles().orEmpty().toList()
            require(additions.none { incoming -> retained.any { it.name == incoming.name } }) { "An added name already exists; remove the old entry first" }
            checkUnchanged(original)
            val result = create(retained + additions, parent, file.nameWithoutExtension + " edited." + format.extension,
                ArchiveCreateOptions(format = format, encryptNames = listing.encrypted && format == ArchiveFormat.SEVEN_ZIP), password)
            result
        } finally { work.deleteRecursively() }
    }

    private data class Stamp(val file: File, val canonical: File, val length: Long, val modified: Long)

    private fun snapshot(file: File): List<Stamp> {
        val files = ArchiveVolumes.ordered(file, policy.maxEntries)
        return files.map {
            require(it.isFile && !Files.isSymbolicLink(it.toPath())) { "Archive is unavailable or a link" }
            Stamp(it, it.canonicalFile, it.length(), it.lastModified())
        }
    }

    private fun checkUnchanged(stamps: List<Stamp>) {
        if (stamps.any { !it.file.isFile || Files.isSymbolicLink(it.file.toPath()) || it.file.canonicalFile != it.canonical ||
                it.file.length() != it.length || it.file.lastModified() != it.modified }) throw ArchiveFailure(ArchiveProblem.CHANGED)
    }

    private fun workspace(): File {
        val root = File(context.cacheDir, "archive-work")
        check(root.isDirectory || root.mkdirs())
        require(!Files.isSymbolicLink(root.toPath()))
        return Files.createTempDirectory(root.canonicalFile.toPath(), "operation-").toFile()
    }

    private fun unique(parent: File, requested: String): File {
        val name = requested.substringBeforeLast('.', requested)
        val extension = requested.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        var file = File(parent, requested)
        var index = 1
        while (file.exists()) {
            require(index < 10_000) { "Too many conflicting names" }
            file = File(parent, "$name (${index++})$extension")
        }
        return file
    }

    private fun publishFile(source: File, destination: File) {
        val stage = File(destination.parentFile, ".mydoc-archive-${UUID.randomUUID()}.tmp")
        try {
            source.inputStream().use { input -> ArchiveNativeFiles.output(stage).use { input.copyTo(it, 64 * 1024) } }
            ArchiveNativeFiles.move(stage, destination)
        } finally { ArchiveNativeFiles.delete(stage) }
    }

    private fun stageDirectory(parent: File, prefix: String): File = File(parent, prefix + UUID.randomUUID()).also { ArchiveNativeFiles.directory(it) }
}
