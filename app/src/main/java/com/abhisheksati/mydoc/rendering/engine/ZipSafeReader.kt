package com.abhisheksati.mydoc.rendering.engine

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Shared bounded-read helpers that protect every ZIP-based parser from:
 *  - Oversized individual entries (> MAX_ENTRY_BYTES)
 *  - Zip bombs (decompressed/compressed ratio > MAX_COMPRESSION_RATIO)
 *  - Entry-count explosions (> MAX_ENTRY_COUNT)
 *  - Total uncompressed size exceeding MAX_TOTAL_BYTES
 *
 * Any violation throws [SecurityException] so callers can catch it, log it, and
 * surface a "file too large or unsupported" message to the user.
 */
object ZipSafeReader {

    private const val TAG = "ZipSafeReader"

    const val MAX_ENTRY_BYTES = 25L * 1024L * 1024L        // 25 MB per entry
    const val MAX_TOTAL_BYTES = 200L * 1024L * 1024L       // 200 MB total uncompressed
    const val MAX_ENTRY_COUNT = 10_000                      // max zip entries
    const val MAX_COMPRESSION_RATIO = 100L                  // decompressed / compressed

    private val BUFFER = ByteArray(16_384)

    /**
     * Reads the current entry from [zip] into a [ByteArray], enforcing all caps.
     *
     * @param entry  The [ZipEntry] that is currently open (used for metadata checks).
     * @param totalRead  Running total of bytes already decompressed this session;
     *                   updated by the returned delta.
     * @param entryIndex  1-based index for logging.
     * @return The entry bytes, guaranteed ≤ [MAX_ENTRY_BYTES].
     * @throws SecurityException if any cap is exceeded.
     */
    fun readEntryBounded(
        zip: ZipInputStream,
        entry: ZipEntry,
        totalRead: LongArray,       // single-element array used as an in-out counter
        entryIndex: Int = 0
    ): ByteArray {
        // Compression-ratio check when the compressed size is known.
        val compressedSize = entry.compressedSize
        if (compressedSize > 0 && entry.size > 0) {
            val ratio = entry.size / compressedSize
            if (ratio > MAX_COMPRESSION_RATIO) {
                throw SecurityException(
                    "Zip bomb suspected: entry '${entry.name}' ratio=$ratio > $MAX_COMPRESSION_RATIO"
                )
            }
        }

        val baos = ByteArrayOutputStream(minOf(MAX_ENTRY_BYTES, 64 * 1024).toInt())
        var entryRead = 0L
        val buf = BUFFER.copyOf()   // thread-local copy avoids BUFFER race on concurrent calls
        var n: Int
        while (zip.read(buf).also { n = it } != -1) {
            if (entryRead + n > MAX_ENTRY_BYTES) {
                throw SecurityException(
                    "Entry '${entry.name}' exceeds ${MAX_ENTRY_BYTES / (1024 * 1024)} MB limit"
                )
            }
            baos.write(buf, 0, n)
            entryRead += n
            if (totalRead[0] + entryRead > MAX_TOTAL_BYTES) {
                throw SecurityException(
                    "Total uncompressed size exceeds ${MAX_TOTAL_BYTES / (1024 * 1024)} MB limit"
                )
            }
        }
        totalRead[0] += entryRead
        return baos.toByteArray()
    }

    /**
     * Iterates [zip], validates entry count, and collects entries into a map.
     * Only entries for which [shouldInclude] returns true are read and stored.
     *
     * @return Map of normalised entry name → bytes.
     * @throws SecurityException if entry count or size caps are exceeded.
     */
    fun collectEntries(
        zip: ZipInputStream,
        shouldInclude: (name: String) -> Boolean
    ): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        val totalRead = longArrayOf(0L)
        var count = 0
        var entry = zip.nextEntry
        while (entry != null) {
            count++
            if (count > MAX_ENTRY_COUNT) {
                throw SecurityException("Entry count exceeds $MAX_ENTRY_COUNT limit")
            }
            if (!entry.isDirectory) {
                val norm = entry.name.replace('\\', '/').trimStart('/')
                if (shouldInclude(norm)) {
                    result[norm] = readEntryBounded(zip, entry, totalRead, count)
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        return result
    }

    /**
     * Convenience: read a single named entry from a ZipInputStream that is
     * already positioned at that entry (i.e. you just called nextEntry()).
     */
    fun readCurrentEntry(zip: ZipInputStream, entry: ZipEntry, maxBytes: Long = MAX_ENTRY_BYTES): ByteArray {
        val total = longArrayOf(0L)
        return readEntryBounded(zip, entry, total)
    }
}
