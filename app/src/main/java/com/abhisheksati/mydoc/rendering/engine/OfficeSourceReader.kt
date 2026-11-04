package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.domain.model.LoadFailure
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.InputStream

enum class OfficeSourceFailure { UNREADABLE, EMPTY, TOO_LARGE, OUT_OF_MEMORY }

sealed class OfficeSource {
    class Ready(val bytes: ByteArray) : OfficeSource()
    class Failed(val failure: OfficeSourceFailure) : OfficeSource()
}

/**
 * The bytes of an office document, read under an explicit budget.
 *
 * Both converters used to call `readBytes()` on the whole document before checking anything, so the
 * 200 MB limit the DOCX converter documented was applied *after* the allocation the limit existed to
 * prevent — a large enough file reached it only by dying. The declared size is consulted before
 * anything is allocated, and the budget is enforced again while reading, because a provider can
 * misreport the length it advertises.
 */
object OfficeSourceReader {

    private const val Tag = "OfficeSource"
    private const val DEFAULT_CHUNK = 64 * 1024

    /** The limit the DOCX converter already documented, now enforced before allocating. */
    const val MAX_SOURCE_BYTES = 200L * 1024 * 1024

    fun read(context: Context, uri: Uri, displayName: String): OfficeSource {
        val declaredSize = DocumentStreamResolver.sourceStamp(context, uri).first
        if (declaredSize > MAX_SOURCE_BYTES) {
            Log.w(Tag, "$displayName declares $declaredSize bytes, over the $MAX_SOURCE_BYTES budget")
            return OfficeSource.Failed(OfficeSourceFailure.TOO_LARGE)
        }
        val stream = try {
            DocumentStreamResolver.openStream(context, uri)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(Tag, "could not open $displayName", t)
            null
        } ?: return OfficeSource.Failed(OfficeSourceFailure.UNREADABLE)

        return stream.use { source ->
            try {
                readBounded(source, declaredSize)
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                Log.e(Tag, "out of memory reading $displayName", e)
                OfficeSource.Failed(OfficeSourceFailure.OUT_OF_MEMORY)
            } catch (t: Throwable) {
                Log.w(Tag, "could not read $displayName", t)
                OfficeSource.Failed(OfficeSourceFailure.UNREADABLE)
            }
        }
    }

    /** The same budget applied to a file already on disk, such as the staged copy the deck
     *  converter writes before reading it back. */
    fun readFile(file: File): OfficeSource {
        if (!file.isFile) return OfficeSource.Failed(OfficeSourceFailure.UNREADABLE)
        val size = file.length()
        if (size > MAX_SOURCE_BYTES) return OfficeSource.Failed(OfficeSourceFailure.TOO_LARGE)
        return try {
            file.inputStream().use { readBounded(it, size) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            Log.e(Tag, "out of memory reading ${file.name}", e)
            OfficeSource.Failed(OfficeSourceFailure.OUT_OF_MEMORY)
        } catch (t: Throwable) {
            Log.w(Tag, "could not read ${file.name}", t)
            OfficeSource.Failed(OfficeSourceFailure.UNREADABLE)
        }
    }

    /** One buffer, sized from [declaredSize]; growth is capped at [maxBytes] instead of doubling up. */
    internal fun readBounded(
        source: InputStream,
        declaredSize: Long,
        maxBytes: Long = MAX_SOURCE_BYTES
    ): OfficeSource {
        val initial = (if (declaredSize in 1..maxBytes) declaredSize else DEFAULT_CHUNK.toLong())
            .coerceAtMost(maxBytes)
            .toInt()
        var buffer = ByteArray(initial)
        var filled = 0

        while (true) {
            if (filled == buffer.size) {
                val next = source.read()
                if (next == -1) return OfficeSource.Ready(buffer)
                if (buffer.size >= maxBytes) return OfficeSource.Failed(OfficeSourceFailure.TOO_LARGE)
                val grown = (buffer.size.toLong() * 2).coerceAtMost(maxBytes).toInt()
                buffer = buffer.copyOf(maxOf(grown, filled + 1))
                buffer[filled] = next.toByte()
                filled++
            }
            val read = source.read(buffer, filled, buffer.size - filled)
            if (read == -1) {
                if (filled == 0) return OfficeSource.Failed(OfficeSourceFailure.EMPTY)
                return OfficeSource.Ready(if (filled == buffer.size) buffer else buffer.copyOf(filled))
            }
            filled += read
        }
    }
}

/** Reading-stage failures, named. Layout failures stay with the converters, which know the format. */
fun OfficeSource.Failed.asConversionFailure(): OfficeConversion.Failure = when (failure) {
    OfficeSourceFailure.TOO_LARGE -> OfficeConversion.Failure(
        LoadFailure.EXCEEDS_LIMITS,
        "This document is larger than MyDoc can convert " +
            "(${OfficeSourceReader.MAX_SOURCE_BYTES / (1024 * 1024)} MB budget)."
    )
    OfficeSourceFailure.OUT_OF_MEMORY -> OfficeConversion.Failure(
        LoadFailure.EXCEEDS_LIMITS,
        "This device ran out of memory while reading this document."
    )
    OfficeSourceFailure.EMPTY -> OfficeConversion.Failure(
        LoadFailure.EMPTY_DOCUMENT,
        "This document has no content to convert."
    )
    OfficeSourceFailure.UNREADABLE -> OfficeConversion.Failure(
        LoadFailure.CONVERSION_FAILED,
        "MyDoc could not read this document."
    )
}
