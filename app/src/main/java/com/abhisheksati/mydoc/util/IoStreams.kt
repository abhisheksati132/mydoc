package com.abhisheksati.mydoc.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Safe I/O stream utilities that enforce bounded transfers to prevent disk exhaustion,
 * zip-bombs, or infinite streams from crashing the application.
 */
object IoStreams {
    const val DEFAULT_MAX_STREAM_BYTES: Long = 500L * 1024 * 1024 // 500MB safety ceiling

    @Throws(IOException::class)
    fun copyBounded(
        input: InputStream,
        output: OutputStream,
        maxBytes: Long = DEFAULT_MAX_STREAM_BYTES,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): Long {
        var bytesCopied: Long = 0
        val buffer = ByteArray(bufferSize)
        var bytes = input.read(buffer)
        while (bytes >= 0) {
            bytesCopied += bytes
            if (bytesCopied > maxBytes) {
                throw IOException("Stream size exceeded safety limit of $maxBytes bytes")
            }
            output.write(buffer, 0, bytes)
            bytes = input.read(buffer)
        }
        return bytesCopied
    }
}

@Throws(IOException::class)
fun InputStream.copyBounded(
    out: OutputStream,
    maxBytes: Long = IoStreams.DEFAULT_MAX_STREAM_BYTES,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long = IoStreams.copyBounded(this, out, maxBytes, bufferSize)
