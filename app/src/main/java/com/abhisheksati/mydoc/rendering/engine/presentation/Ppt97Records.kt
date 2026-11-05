package com.abhisheksati.mydoc.rendering.engine.presentation

import java.nio.charset.Charset

/**
 * The record tree of a PowerPoint 97-2003 `.ppt`.
 *
 * A `.ppt` is one stream of records, each with an eight-byte header: the low nibble of the first
 * byte is the version and `0xF` means the record is a container whose body is more records, the next
 * twelve bits are the instance, bytes two and three are the record type and bytes four to seven the
 * length of the body. Reading that tree rather than scanning for plausible-looking headers is what
 * tells a slide apart from the master it was made from: the master's own text - "Click to edit
 * Master title style" and the bullet levels - is written into the same stream, and a scanner that
 * cannot see the containers reads it as slide content.
 *
 * The record numbers and the two text encodings are taken from files PowerPoint itself wrote (see
 * `make-ppt97-unicode.ps1` beside these fixtures): a text block is `RT_TextBytesAtom` in
 * Windows-1252 when every character fits there, and the wide-character atom in UTF-16 when it does
 * not.
 */
internal object Ppt97Records {

    private const val ContainerVersion = 0xF
    private const val RtSlide = 1006
    private const val RtTextBytesAtom = 4008
    private const val RtTextCharsAtom = 4000

    /** Longest nesting we are willing to follow; a real deck is a handful deep. */
    private const val MaxDepth = 64

    /** One slide's worth of text, in the order the file writes it. */
    data class Slide(val blocks: List<String>)

    private val windows1252: Charset? = runCatching { Charset.forName("windows-1252") }.getOrNull()

    fun slides(doc: ByteArray): List<Slide> {
        val out = ArrayList<Slide>()
        forEachRecord(doc, 0, doc.size) { off, version, type, length ->
            if (type == RtSlide && version == ContainerVersion) {
                val blocks = textBlocks(doc, off + 8, off + 8 + length, 0)
                if (blocks.isNotEmpty()) out.add(Slide(blocks))
            }
        }
        return out
    }

    /**
     * The text inside one slide container, deepest records first. A block is one text shape, so the
     * lines of a bullet list arrive together rather than interleaved with the next shape's.
     */
    private fun textBlocks(doc: ByteArray, start: Int, end: Int, depth: Int): List<String> {
        if (depth > MaxDepth) return emptyList()
        val blocks = ArrayList<String>()
        forEachRecord(doc, start, end) { off, version, type, length ->
            if (version == ContainerVersion) {
                blocks.addAll(textBlocks(doc, off + 8, off + 8 + length, depth + 1))
            } else {
                val text = when (type) {
                    RtTextBytesAtom -> decode(doc, off + 8, length, windows1252 ?: Charsets.ISO_8859_1)
                    RtTextCharsAtom -> decode(doc, off + 8, length, Charsets.UTF_16LE)
                    else -> null
                }
                if (!text.isNullOrBlank()) blocks.add(text)
            }
        }
        return blocks
    }

    private fun decode(doc: ByteArray, offset: Int, length: Int, charset: Charset): String? {
        if (length <= 0 || offset + length > doc.size) return null
        return String(doc, offset, length, charset)
    }

    /**
     * Walks the records laid out at [start] up to [end]. A body that would run past the range stops
     * the walk: the bytes after it belong to whatever encloses this container, and guessing the next
     * header by shifting one byte at a time is how a scanner invents records that were never written.
     */
    private inline fun forEachRecord(
        doc: ByteArray,
        start: Int,
        end: Int,
        action: (offset: Int, version: Int, type: Int, length: Int) -> Unit
    ) {
        var i = start
        while (i + 8 <= end) {
            val version = doc[i].toInt() and 0xF
            val type = (doc[i + 2].toInt() and 0xFF) or ((doc[i + 3].toInt() and 0xFF) shl 8)
            val length = (doc[i + 4].toLong() and 0xFF) or
                ((doc[i + 5].toLong() and 0xFF) shl 8) or
                ((doc[i + 6].toLong() and 0xFF) shl 16) or
                ((doc[i + 7].toLong() and 0xFF) shl 24)
            if (type == 0 || length < 0 || length > end - i - 8) return
            action(i, version, type, length.toInt())
            i += 8 + length.toInt()
        }
    }
}
