package com.abhisheksati.mydoc.data.archive

import com.abhisheksati.mydoc.domain.archive.ArchiveListing
import com.abhisheksati.mydoc.domain.archive.ArchiveMember
import com.abhisheksati.mydoc.domain.archive.ArchivePolicy

/** Strict reader for the pinned engine's technical listing. Never interprets shell text. */
class ArchiveListingParser(private val policy: ArchivePolicy = ArchivePolicy()) {
    fun parse(text: String): ArchiveListing {
        require(text.toByteArray(Charsets.UTF_8).size <= policy.maxMetadataBytes) { "Archive metadata limit exceeded" }
        var type = ""
        var entriesStarted = false
        var total = 0L
        var records = 0
        val explicit = hashSetOf<String>()
        val members = linkedMapOf<String, ArchiveMember>()
        val fields = linkedMapOf<String, String>()

        fun finish() {
            if (fields.isEmpty()) return
            require(++records <= policy.maxEntries) { "Excessive archive entries" }
            val rawPath = requireNotNull(fields["Path"]) { "Missing archive path" }
            require(fields["Symbolic Link"].isNullOrEmpty() && fields["Hard Link"].isNullOrEmpty() &&
                fields["Reparse Point"].isNullOrEmpty()) { "Archive links are not extracted" }
            val attributes = fields["Attributes"].orEmpty()
            require(!Regex("(?:^|\\s)l[rwxstST-]{9}(?:\\s|$)").containsMatchIn(attributes)) { "Archive link" }
            val directory = fields["Folder"] == "+" || attributes.startsWith('D') || fields["Path"]!!.endsWith('/')
            if (directory && rawPath.replace('\\', '/').trimEnd('/') == ".") { fields.clear(); return }
            val path = safePath(rawPath, policy)
            require(explicit.add(path)) { "Duplicate archive entries" }
            val bytes = if (directory) 0L else fields["Size"]?.toLongOrNull()
            require(bytes != null && bytes >= 0 && bytes <= policy.maxExpandedBytes - total) { "Archive expanded-byte limit exceeded" }
            val crc = fields["CRC"].orEmpty().let {
                require(it.isEmpty() || it.matches(Regex("[0-9A-Fa-f]{8}"))) { "Invalid archive checksum" }
                it.takeIf(String::isNotEmpty)?.toLong(16)
            }
            val parts = path.split('/')
            for (i in 1 until parts.size) {
                val parent = parts.take(i).joinToString("/")
                require(members[parent]?.directory != false) { "Archive file/folder collision" }
                members.putIfAbsent(parent, ArchiveMember(parent, 0, true))
            }
            val existing = members[path]
            require(existing == null || existing.directory && directory) { "Archive file/folder collision" }
            members[path] = ArchiveMember(path, bytes, directory, crc, fields["Encrypted"] == "+", fields["Modified"], rawPath)
            require(members.size <= policy.maxEntries) { "Archive directory limit exceeded" }
            total += bytes
            fields.clear()
        }

        for (raw in text.lineSequence()) {
            val line = raw.removeSuffix("\r")
            require(line.length <= policy.maxPathCharacters * 4 + 128) { "Archive metadata line too long" }
            if (!entriesStarted) {
                if (line.startsWith("Type = ")) type = line.substringAfter("Type = ").trim()
                if (line == "----------") entriesStarted = true
                continue
            }
            if (line.isBlank()) { finish(); continue }
            if (line == "Enter password:") continue
            val separator = line.indexOf(" = ")
            require(separator > 0) { "Unexpected archive metadata" }
            val key = line.substring(0, separator)
            require(fields.size < 128) { "Excessive archive metadata fields" }
            require(!fields.containsKey(key)) { "Repeated archive metadata" }
            fields[key] = line.substring(separator + 3)
        }
        finish()
        require(entriesStarted && type.isNotBlank()) { "Missing archive metadata" }
        return ArchiveListing(type, members.values.toList())
    }

    companion object {
        fun safePath(raw: String, policy: ArchivePolicy = ArchivePolicy()): String {
            require(raw.isNotEmpty() && raw.length <= policy.maxPathCharacters && raw.none { it.isISOControl() }) { "Unsafe archive path" }
            var path = raw.replace('\\', '/')
            while (path.startsWith("./")) path = path.removePrefix("./")
            path = path.removeSuffix("/")
            require(!path.startsWith('/') && ':' !in path) { "Unsafe archive path" }
            val parts = path.split('/')
            require(parts.size <= policy.maxDepth && parts.all { it.isNotEmpty() && it != "." && it != ".." }) { "Unsafe archive path" }
            return path
        }
    }
}
