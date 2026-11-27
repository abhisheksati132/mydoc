package com.abhisheksati.mydoc.domain.archive

import java.io.IOException

/** Expanded-byte budgets are explicit; metadata never authorizes unlimited writes. */
data class ArchivePolicy(
    val maxEntries: Int = 10_000,
    val maxExpandedBytes: Long = 512L * 1024 * 1024,
    val maxMetadataBytes: Int = 24 * 1024 * 1024,
    val maxDepth: Int = 64,
    val maxPathCharacters: Int = 4096
) {
    init {
        require(maxEntries in 1..100_000 && maxExpandedBytes in 1..(1024L * 1024 * 1024 * 1024) && maxMetadataBytes in 1..64 * 1024 * 1024)
        require(maxDepth in 1..128 && maxPathCharacters in 1..8192)
    }
}

data class ArchiveMember(
    val path: String,
    val bytes: Long,
    val directory: Boolean,
    val crc32: Long? = null,
    val encrypted: Boolean = false,
    val modified: String? = null,
    val selector: String = path
) {
    val name: String get() = path.substringAfterLast('/')
}

data class ArchiveListing(val type: String, val members: List<ArchiveMember>) {
    val totalBytes: Long get() = members.sumOf { it.bytes }
    val encrypted: Boolean get() = members.any { it.encrypted }
    val files: List<ArchiveMember> get() = members.filterNot { it.directory }
}

enum class ArchiveFormat(val type: String, val extension: String) {
    ZIP("zip", "zip"), SEVEN_ZIP("7z", "7z"), TAR("tar", "tar"),
    TAR_GZIP("gzip", "tar.gz"), TAR_BZIP2("bzip2", "tar.bz2"), TAR_XZ("xz", "tar.xz");

    val encryption: Boolean get() = this == ZIP || this == SEVEN_ZIP
    val solid: Boolean get() = this == SEVEN_ZIP
}

enum class ArchiveCompression(val level: Int, val dictionaryMiB: Int) {
    STORE(0, 8), FAST(1, 8), BALANCED(5, 32), MAXIMUM(9, 64)
}

enum class ArchiveConflictPolicy { NEW_FOLDER, SKIP_EXISTING, KEEP_BOTH }

class ArchiveMergeRecovery(val directory: java.io.File) : IOException("Verified extracted files remain available for recovery")

/** Credentials deliberately live outside this persistable/options value. */
data class ArchiveCreateOptions(
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val compression: ArchiveCompression = ArchiveCompression.BALANCED,
    val solid: Boolean = true,
    val encryptNames: Boolean = false,
    val splitBytes: Long? = null,
    val dictionaryMiB: Int = compression.dictionaryMiB,
    val threads: Int = 2
) {
    init {
        require(dictionaryMiB in 1..128 && threads in 1..4)
        require(splitBytes == null || splitBytes >= 1024 * 1024)
        require(!encryptNames || format == ArchiveFormat.SEVEN_ZIP)
    }
}

enum class ArchiveProblem { PASSWORD_REQUIRED, WRONG_PASSWORD, MISSING_VOLUMES, CORRUPT, UNSUPPORTED, NO_SPACE, MEMORY, CHANGED, ENGINE }

class ArchiveFailure(val problem: ArchiveProblem) : IOException(problem.name)

data class ArchiveProgress(val percent: Int? = null, val completedBytes: Long = 0, val totalBytes: Long = 0, val currentName: String = "")
