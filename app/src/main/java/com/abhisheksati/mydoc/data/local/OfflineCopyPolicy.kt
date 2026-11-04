package com.abhisheksati.mydoc.data.local

/**
 * What an offline copy was taken from, and whether the copy may still be served.
 *
 * The copy used to be judged current on file size alone, so an edit that kept the length — replacing
 * a word in place, a re-export with the same padding, a database-backed document whose export size
 * happens to match — left the app holding an outdated copy of the user's document indefinitely.
 * Modification time is the second signal; when neither signal is available the copy is kept,
 * because refusing to serve it would break offline reading, which is the copy's whole purpose.
 */
data class OfflineCopyStamp(val sizeBytes: Long, val lastModified: Long) {

    fun isComparableTo(other: OfflineCopyStamp): Boolean =
        sizeBytes > 0L && other.sizeBytes > 0L
}

object OfflineCopyPolicy {

    /** Suffix of the sidecar that records where a copy came from. */
    const val META_SUFFIX = ".meta"

    fun metaFileFor(copyName: String): String = copyName + META_SUFFIX

    fun format(stamp: OfflineCopyStamp): String =
        "size=${stamp.sizeBytes};modified=${stamp.lastModified}"

    fun parse(text: String): OfflineCopyStamp? {
        val parts = text.trim().split(';').mapNotNull { entry ->
            val key = entry.substringBefore('=', "").trim()
            val value = entry.substringAfter('=', "").trim().toLongOrNull()
            if (key.isEmpty() || value == null) null else key to value
        }.toMap()
        val size = parts["size"] ?: return null
        val modified = parts["modified"] ?: return null
        return OfflineCopyStamp(size, modified)
    }

    /**
     * True when the offline copy must be rewritten. A missing or empty copy is obvious; anything
     * written before the sidecar existed is refreshed once so it starts carrying a stamp; a length
     * mismatch catches both a stale copy and a copy that was truncated while being written.
     */
    fun needsRefresh(
        recorded: OfflineCopyStamp?,
        copyLength: Long,
        source: OfflineCopyStamp
    ): Boolean {
        if (copyLength <= 0L) return true
        if (recorded == null) return true
        if (source.sizeBytes > 0L && copyLength != source.sizeBytes) return true
        if (source.sizeBytes > 0L && recorded.sizeBytes != source.sizeBytes) return true
        if (source.lastModified > 0L && recorded.lastModified != source.lastModified) return true
        return false
    }

    fun isMetaFile(fileName: String): Boolean = fileName.endsWith(META_SUFFIX)
}
