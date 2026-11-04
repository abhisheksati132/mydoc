package com.abhisheksati.mydoc.ui.viewer.text

import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.util.UserMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction

/** How much of a text file the viewer is willing to hold, and what it says when it stops. */
data class TextBudget(val maxBytes: Long, val maxLines: Int, val maxLineChars: Int) {

    companion object {
        /**
         * 8 MB of characters, 50,000 lines, 10,000 characters in a line.
         *
         * The viewer keeps every line in memory as a list of strings and a single LazyColumn; a
         * log file of any size would otherwise be copied whole into the heap and then copied again
         * for read-aloud. The reader is told when it is looking at a prefix rather than a document.
         */
        val DEFAULT = TextBudget(maxBytes = 8L * 1024 * 1024, maxLines = 50_000, maxLineChars = 10_000)

        fun noteFor(budget: TextBudget): String =
            "Showing the first ${budget.maxBytes / (1024 * 1024)} MB and ${budget.maxLines} lines of a larger file."
    }
}

sealed interface TextLoadOutcome {
    data class Loaded(
        val lines: List<String>,
        val bytesRead: Long,
        val truncated: Boolean
    ) : TextLoadOutcome

    data class Failed(val failure: LoadFailure, val error: Throwable?) : TextLoadOutcome
}

/**
 * Reading and searching for the plain-text viewer, outside the composable so both can be tested
 * against real bytes. `readLines()` on an unbounded stream used to be the whole loading strategy.
 */
object TextViewerModel {

    /** Truncation marker appended to a line that was cut at the per-line cap. */
    const val CUT = " …"

    suspend fun load(
        openStream: () -> InputStream?,
        budget: TextBudget = TextBudget.DEFAULT
    ): TextLoadOutcome = try {
        val stream = openStream()
        if (stream == null) {
            TextLoadOutcome.Failed(LoadFailure.CORRUPT_DOCUMENT, IllegalStateException(COULD_NOT_OPEN))
        } else {
            stream.use { readBounded(it, budget) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: DocumentLoadException) {
        TextLoadOutcome.Failed(e.failure, e)
    } catch (e: Throwable) {
        TextLoadOutcome.Failed(
            if (UserMessages.isPermissionFailure(e)) LoadFailure.PERMISSION_EXPIRED else LoadFailure.CORRUPT_DOCUMENT,
            e
        )
    }

    private suspend fun readBounded(stream: InputStream, budget: TextBudget): TextLoadOutcome.Loaded {
        val counting = CountingInputStream(stream)
        val reader: Reader = InputStreamReader(counting, utf8ReplacingErrors()).buffered()
        val lines = ArrayList<String>()
        val chunk = CharArray(8192)
        val pending = StringBuilder()
        var pastLineCap = false
        var truncated = false
        var stoppedForBudget = false

        while (!stoppedForBudget) {
            currentCoroutineContext().ensureActive()
            val read = reader.read(chunk)
            if (read < 0) break
            var i = 0
            while (i < read) {
                val c = chunk[i]
                i++
                if (lines.size >= budget.maxLines) {
                    // The pending line is not shown at all, so say so rather than cutting a
                    // different line and implying it was the one that overflowed.
                    truncated = true
                    stoppedForBudget = true
                    break
                }
                when {
                    c == '\n' -> {
                        lines += if (pastLineCap) pending.toString() + CUT else pending.toString()
                        pending.setLength(0)
                        pastLineCap = false
                    }

                    pastLineCap -> truncated = true

                    pending.length < budget.maxLineChars -> pending.append(c)

                    else -> {
                        pastLineCap = true
                        truncated = true
                    }
                }
            }
            if (!stoppedForBudget && counting.bytesRead >= budget.maxBytes) {
                truncated = true
                stoppedForBudget = true
            }
        }

        if (!stoppedForBudget && pastLineCap) {
            lines += pending.toString() + CUT
        } else if (!stoppedForBudget && pending.isNotEmpty() && lines.size < budget.maxLines) {
            lines += pending.toString()
        }
        return TextLoadOutcome.Loaded(lines, counting.bytesRead, truncated)
    }

    /**
     * UTF-8 with replacement rather than an exception: a file with one bad byte in it is still a
     * document the reader wants the rest of, and `InputStreamReader` over a plain Charset is not
     * specified to tolerate the bad byte.
     */
    private fun utf8ReplacingErrors(): CharsetDecoder =
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)

    private class CountingInputStream(private val delegate: InputStream) : InputStream() {
        var bytesRead: Long = 0
            private set

        override fun read(): Int = delegate.read().also { if (it >= 0) bytesRead++ }
        override fun read(b: ByteArray, off: Int, len: Int): Int =
            delegate.read(b, off, len).also { if (it > 0) bytesRead += it }

        override fun available(): Int = delegate.available()
        override fun close() = delegate.close()
    }

    const val COULD_NOT_OPEN = "Could not open input stream"

    /** Occurrence-level line matches, in reading order, for the search overlay. */
    fun matchingLines(lines: List<String>, query: String): List<Int> {
        if (query.isBlank()) return emptyList()
        return lines.mapIndexedNotNull { index, line ->
            if (line.contains(query, ignoreCase = true)) index else null
        }
    }
}
