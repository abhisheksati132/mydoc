package com.abhisheksati.mydoc.data.archive

import com.abhisheksati.mydoc.domain.archive.ArchiveFailure
import com.abhisheksati.mydoc.domain.archive.ArchiveProblem
import java.io.File
import java.nio.file.Files

/** Normalize opening any part to the first part and reject gaps before invoking codecs. */
internal object ArchiveVolumes {
    fun ordered(file: File, limit: Int): List<File> {
        require(file.isFile && !Files.isSymbolicLink(file.toPath())) { "Archive is unavailable or a link" }
        val numbered = Regex("^(.*)\\.(\\d{3,})$").matchEntire(file.name)
        val rar = Regex("^(.*)\\.part(\\d+)\\.rar$", RegexOption.IGNORE_CASE).matchEntire(file.name)
        val classic = Regex("^(.*)\\.(rar|[r-z]\\d{2})$", RegexOption.IGNORE_CASE).matchEntire(file.name)
        val pattern: Regex
        val first: Int
        val index: (MatchResult) -> Int?
        when {
            numbered != null -> {
                pattern = Regex(Regex.escape(numbered.groupValues[1]) + "\\.(\\d{3,})")
                first = 1; index = { it.groupValues[1].toIntOrNull() }
            }
            rar != null -> {
                pattern = Regex(Regex.escape(rar.groupValues[1]) + "\\.part(\\d+)\\.rar", RegexOption.IGNORE_CASE)
                first = 1; index = { it.groupValues[1].toIntOrNull() }
            }
            classic != null -> {
                pattern = Regex(Regex.escape(classic.groupValues[1]) + "\\.(rar|[r-z]\\d{2})", RegexOption.IGNORE_CASE)
                first = 0
                index = {
                    val extension = it.groupValues[1].lowercase()
                    if (extension == "rar") 0 else (extension[0] - 'r') * 100 + extension.substring(1).toInt() + 1
                }
            }
            else -> return listOf(file)
        }
        val parts = mutableListOf<Pair<Int, File>>()
        Files.newDirectoryStream(requireNotNull(file.parentFile).toPath()).use { directory ->
            for (path in directory) {
                val match = pattern.matchEntire(path.fileName.toString()) ?: continue
                val part = index(match) ?: throw ArchiveFailure(ArchiveProblem.MISSING_VOLUMES)
                require(parts.size < limit) { "Too many volumes" }
                parts += part to path.toFile()
            }
        }
        parts.sortBy { it.first }
        if (parts.isEmpty() || parts.withIndex().any { it.value.first != it.index + first }) throw ArchiveFailure(ArchiveProblem.MISSING_VOLUMES)
        return parts.map { it.second }
    }
}
