package com.abhisheksati.mydoc.rendering.engine.word

import com.abhisheksati.mydoc.rendering.engine.Ole2File

/**
 * Word 97-2003 text: the FIB in the `WordDocument` stream points at a piece table in `0Table` or
 * `1Table`, and that table says where every run of characters lives and how it is encoded.
 *
 * The engine used to scan the whole container for printable byte pairs, which lost every non-ASCII
 * character, mixed text with the property streams, and put paragraphs in whatever order the scan
 * happened to reach them. This reads the text the way Word stores it.
 *
 * Formatting is a different structure - CHPX/PAPX fragments reached through the BTE tables - and is
 * deliberately not attempted here: the registry calls .doc TEXT_ONLY and this keeps that promise
 * rather than dressing up a scrape as a layout.
 */
internal object Word97Text {

    private const val WIDENT = 0xA5EC
    private const val FIB_FLAGS = 0x0A
    private const val WHICH_TABLE_BIT = 0x0200
    // ccpText lives 12 bytes into FibRgLw97, which starts after FibBase(32), csw and FibRgCswNew.
    // Word 97 puts it at 0x4C; the piece table's own last CP is the fallback when a writer moves it.
    private const val CCP_TEXT = 0x4C
    private const val FC_CLX = 0x1A2
    private const val MaxClxBytes = 8 * 1024 * 1024
    private const val MaxTextChars = 4_000_000

    /** The main document's characters, or null when the file is not a Word 97 binary document. */
    fun read(container: ByteArray): String? {
        val ole = runCatching { Ole2File(container) }.getOrNull() ?: return null
        val fib = runCatching { ole.getStream("WordDocument") }.getOrNull()
        if (fib == null || fib.size < 0x1A6) return null
        if (((fib[0].toInt() and 0xFF) or ((fib[1].toInt() and 0xFF) shl 8)) != WIDENT) return null

        val table = tableStream(ole, fib) ?: return null
        val fcClx = u32(fib, FC_CLX)
        val lcbClx = u32(fib, FC_CLX + 4)
        if (lcbClx <= 0 || lcbClx > MaxClxBytes || fcClx + lcbClx > table.size) return null

        val pieces = pieceTable(table, fcClx.toInt(), lcbClx.toInt()) ?: return null
        val lastCp = pieces.lastOrNull()?.let { it.start + it.chars } ?: 0
        val declared = u32(fib, CCP_TEXT)
        val ccpText = (if (declared > 0) declared else lastCp.toLong())
            .coerceAtMost(MaxTextChars.toLong())
            .toInt()
        if (ccpText <= 0) return null

        val text = StringBuilder(ccpText.coerceAtMost(1 shl 20))
        for (piece in pieces) {
            if (text.length >= ccpText) break
            val wanted = minOf(piece.chars, ccpText - text.length)
            val decoded = decode(fib, piece, wanted) ?: return null
            text.append(decoded)
        }
        return if (text.isEmpty()) null else normalise(text.toString())
    }

    private fun tableStream(ole: Ole2File, fib: ByteArray): ByteArray? {
        val preferOne = (u16(fib, FIB_FLAGS) and WHICH_TABLE_BIT) != 0
        val first = if (preferOne) "1Table" else "0Table"
        val second = if (preferOne) "0Table" else "1Table"
        return runCatching { ole.getStream(first) }.getOrNull()
            ?: runCatching { ole.getStream(second) }.getOrNull()
    }

    private class Piece(val start: Int, val chars: Int, val byteOffset: Int, val compressed: Boolean)

    /** Clx = zero or more Prc entries, then the Pcdt holding the PlcPcd. */
    private fun pieceTable(table: ByteArray, fcClx: Int, lcbClx: Int): List<Piece>? {
        var pos = fcClx
        val end = fcClx + lcbClx
        while (pos < end) {
            when (table[pos].toInt() and 0xFF) {
                0x01 -> {
                    val cb = u16(table, pos + 1)
                    pos += 3 + cb
                }

                0x02 -> {
                    val lcb = u32(table, pos + 1).toInt()
                    val pcdt = pos + 5
                    if (lcb <= 4 || pcdt + lcb > table.size) return null
                    val count = (lcb - 4) / 12
                    if (count <= 0 || count > 100_000) return null
                    val pieces = ArrayList<Piece>(count)
                    for (i in 0 until count) {
                        val cpStart = u32(table, pcdt + 4 * i).toInt()
                        val cpEnd = u32(table, pcdt + 4 * (i + 1)).toInt()
                        val pcd = pcdt + 4 * (count + 1) + 8 * i
                        if (pcd + 8 > table.size) return null
                        val fcField = u32(table, pcd + 2)
                        val compressed = (fcField and 0x4000_0000L) != 0L
                        var offset = (fcField and 0x3FFF_FFFFL)
                        if (compressed) offset /= 2
                        pieces.add(Piece(cpStart, cpEnd - cpStart, offset.toInt(), compressed))
                    }
                    return pieces
                }

                else -> return null
            }
        }
        return null
    }

    private fun decode(fib: ByteArray, piece: Piece, chars: Int): String? {
        return if (piece.compressed) {
            // A compressed piece is one byte per character in the Windows-1252 range; the high bits
            // are the ASCII half, so the bytes map straight onto the first 256 code points.
            val end = piece.byteOffset + chars
            if (piece.byteOffset < 0 || end > fib.size) return null
            val sb = StringBuilder(chars)
            for (i in piece.byteOffset until end) {
                val b = fib[i].toInt() and 0xFF
                sb.append(if (b < 0x80) b.toChar() else cp1252(b))
            }
            sb.toString()
        } else {
            val end = piece.byteOffset + chars * 2
            if (piece.byteOffset < 0 || end > fib.size) return null
            val sb = StringBuilder(chars)
            for (i in 0 until chars) sb.append(u16(fib, piece.byteOffset + i * 2).toChar())
            sb.toString()
        }
    }

    /** The 0x80-0x9F block of Windows-1252, which is not Latin-1 and not a control range. */
    private fun cp1252(byte: Int): Char = when (byte) {
        0x80 -> '€'
        0x81 -> ' '
        0x82 -> '‚'
        0x83 -> 'ƒ'
        0x84 -> '„'
        0x85 -> '…'
        0x86 -> '†'
        0x87 -> '‡'
        0x88 -> 'ˆ'
        0x89 -> '‰'
        0x8A -> 'Š'
        0x8B -> '‹'
        0x8C -> 'Œ'
        0x8D -> ' '
        0x8E -> 'Ž'
        0x8F -> ' '
        0x90 -> ' '
        0x91 -> '‘'
        0x92 -> '’'
        0x93 -> '“'
        0x94 -> '”'
        0x95 -> '•'
        0x96 -> '–'
        0x97 -> '—'
        0x98 -> '˜'
        0x99 -> '™'
        0x9A -> 'š'
        0x9B -> '›'
        0x9C -> 'œ'
        0x9D -> ' '
        0x9E -> 'ž'
        0x9F -> 'Ÿ'
        else -> byte.toChar()
    }

    /**
     * Word's text uses marks rather than the characters a reader expects: 0x0D ends a paragraph,
     * 0x07 ends a table cell, 0x0B is a line break, 0x0C a page break, and 0x13/0x14/0x15 delimit
     * field results. The marks that mean structure become structure; the ones that mean "an object
     * we cannot draw" disappear rather than printing a box.
     */
    private fun normalise(raw: String): String {
        val out = StringBuilder(raw.length)
        var fieldDepth = 0
        var instruction = false
        for (ch in raw) {
            when (ch) {
                '\r' -> out.append('\n')
                '\n' -> out.append('\n')
                0x0B.toChar() -> out.append('\n')
                0x0C.toChar() -> out.append('\n')
                0x07.toChar() -> out.append('\n')
                // A field is instruction, separator, result. Word shows the result; printing
                // HYPERLINK "https://…" in the middle of a sentence is not what the page says.
                0x13.toChar() -> {
                    fieldDepth++
                    instruction = true
                }

                0x14.toChar() -> instruction = false
                0x15.toChar() -> {
                    instruction = false
                    if (fieldDepth > 0) fieldDepth--
                }
                0x01.toChar(), 0x02.toChar(), 0x05.toChar(), 0x08.toChar(), 0x1F.toChar() -> Unit
                0x1E.toChar() -> out.append('-')
                else -> if (!instruction) out.append(ch)
            }
        }
        return out.toString()
    }

    private fun u16(bytes: ByteArray, at: Int): Int {
        if (at < 0 || at + 1 >= bytes.size) return 0
        return (bytes[at].toInt() and 0xFF) or ((bytes[at + 1].toInt() and 0xFF) shl 8)
    }

    private fun u32(bytes: ByteArray, at: Int): Long {
        if (at < 0 || at + 3 >= bytes.size) return 0L
        return (bytes[at].toInt() and 0xFF).toLong() or
            ((bytes[at + 1].toInt() and 0xFF).toLong() shl 8) or
            ((bytes[at + 2].toInt() and 0xFF).toLong() shl 16) or
            ((bytes[at + 3].toInt() and 0xFF).toLong() shl 24)
    }
}
