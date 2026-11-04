package com.abhisheksati.mydoc.ui.viewer.word

import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxDocument
import com.abhisheksati.mydoc.rendering.engine.word.DocxRun
import kotlinx.coroutines.CancellationException
import java.io.InputStream

/**
 * Search and load decisions for the Word viewer, kept out of the composable so they can be
 * tested against the document model rather than against a screenshot.
 */
object WordViewerModel {

    /** Plain text of a block, in the same order the viewer renders it. */
    fun plainText(block: DocxBlock): String = when (block) {
        is DocxBlock.Heading -> block.text
        is DocxBlock.Paragraph -> block.runs.joinToString("") { it.text }
        is DocxBlock.Bullet -> block.runs.joinToString("") { it.text }
        is DocxBlock.Table -> block.rows.joinToString("\n") { it.joinToString(" | ") }
    }

    /** One occurrence of the query, addressed by block and by character range inside that block. */
    data class Match(val blockIndex: Int, val start: Int, val end: Int)

    /**
     * Every occurrence of [query], in reading order. Counting blocks rather than occurrences made
     * "3 of 7" disagree with what the reader could see, and filtering the block list to just the
     * matches renumbered the whole document under the reader's feet.
     */
    fun matches(blocks: List<DocxBlock>, query: String): List<Match> {
        if (query.isBlank()) return emptyList()
        val found = mutableListOf<Match>()
        blocks.forEachIndexed { index, block ->
            val text = plainText(block)
            var from = 0
            while (from <= text.length - query.length) {
                val hit = text.indexOf(query, from, ignoreCase = true)
                if (hit < 0) break
                found += Match(index, hit, hit + query.length)
                from = hit + query.length
            }
        }
        return found
    }

    /** Character ranges covered by matches inside one block, for highlighting. */
    fun rangesIn(blocks: List<DocxBlock>, blockIndex: Int, matches: List<Match>): List<IntRange> =
        matches.filter { it.blockIndex == blockIndex }.map { it.start..it.end - 1 }

    /**
     * Character range each run occupies in the block's plain text, so a hit that lands in one run
     * highlights that run rather than the paragraph around it.
     */
    fun runOffsets(runs: List<DocxRun>): List<IntRange> {
        var cursor = 0
        return runs.map { run ->
            val range = cursor until cursor + run.text.length
            cursor += run.text.length
            range
        }
    }
}

/** Terminal state of a Word document load. Nothing here is allowed to leave the reader spinning. */
sealed interface WordLoadOutcome {
    data class Loaded(val document: DocxDocument) : WordLoadOutcome

    data class Failed(val failure: LoadFailure, val error: Throwable?) : WordLoadOutcome
}

object WordDocumentLoader {

    /**
     * Opens and parses, and always ends with something the viewer can display. A typed
     * [DocumentLoadException] keeps the classification the parser chose; anything else is decided
     * the same way the viewer decided it before, so fixing the spinner does not quietly change
     * which problem screen a reader sees. Cancellation is never turned into a failure — the reader
     * has simply moved on, and the next load starts from a clean state.
     */
    suspend fun load(
        openStream: () -> InputStream?,
        parse: suspend (InputStream) -> DocxDocument
    ): WordLoadOutcome = try {
        val stream = openStream()
        if (stream == null) {
            WordLoadOutcome.Failed(LoadFailure.CORRUPT_DOCUMENT, IllegalStateException(COULD_NOT_OPEN))
        } else {
            stream.use { WordLoadOutcome.Loaded(parse(it)) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: DocumentLoadException) {
        WordLoadOutcome.Failed(e.failure, e)
    } catch (e: Throwable) {
        WordLoadOutcome.Failed(
            if (com.abhisheksati.mydoc.util.UserMessages.isPermissionFailure(e)) {
                LoadFailure.PERMISSION_EXPIRED
            } else {
                LoadFailure.CORRUPT_DOCUMENT
            },
            e
        )
    }

    const val COULD_NOT_OPEN = "Could not open input stream"
}
