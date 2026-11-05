package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import java.util.Locale
import kotlin.math.abs

/**
 * BIFF8 - the record format Excel 97-2003 wrote into the `Workbook` stream of a CFBF container.
 *
 * The engine used to run a UTF-16 byte scan over the whole container and drop whatever words it
 * found into column 0 of a sheet named "Extracted Data", which lost every number and every cell
 * address in the workbook. Records are walked instead here: the global substream yields the shared
 * string table and the sheet directory, and each sheet substream yields cells at their real row and
 * column.
 *
 * Only what a viewer needs is interpreted. Formats, styles, formulas and charts stay in the file:
 * formula cells show the result Excel cached, exactly as the OOXML reader shows a cached `<v>`.
 */
internal object Biff8Reader {

    private const val BOF = 0x0809
    private const val BOUNDSHEET = 0x0085
    private const val SST = 0x00FC
    private const val CONTINUE = 0x003C
    private const val EOF = 0x000A

    private const val LABELSST = 0x00FD
    private const val LABEL = 0x0204
    private const val NUMBER = 0x0203
    private const val RK = 0x027E
    private const val MULRK = 0x00BD
    private const val FORMULA = 0x0006
    private const val STRING = 0x0207
    private const val BOOLERR = 0x0205
    private const val MERGEDCELLS = 0x00E5

    private const val MaxRecords = 4_000_000
    private const val MaxSheets = 256
    private const val MaxCellsPerSheet = 200_000
    private const val MaxStrings = 500_000

    private class Record(val id: Int, val start: Int, val length: Int)

    fun read(workbook: ByteArray): SpreadsheetModel? {
        val records = index(workbook) ?: return null
        if (records.none { it.id == BOF } || records.none { it.id == BOUNDSHEET }) return null

        val strings = readSharedStrings(workbook, records)
        val sheets = records.filter { it.id == BOUNDSHEET }.take(MaxSheets).map { boundsheet ->
            readSheet(workbook, records, boundsheet, strings)
        }
        return if (sheets.isEmpty()) null else SpreadsheetModel(sheets)
    }

    /** Records are fixed 4-byte headers; a stream that stops making sense returns null. */
    private fun index(workbook: ByteArray): List<Record>? {
        val out = ArrayList<Record>()
        var pos = 0
        while (pos + 4 <= workbook.size) {
            val id = u16(workbook, pos)
            val length = u16(workbook, pos + 2)
            if (length > workbook.size - pos - 4) return null
            out.add(Record(id, pos + 4, length))
            if (out.size > MaxRecords) return null
            pos += 4 + length
        }
        return out
    }

    private fun readSharedStrings(workbook: ByteArray, records: List<Record>): List<String> {
        val sst = records.firstOrNull { it.id == SST } ?: return emptyList()
        val blocks = ArrayList<ByteArray>()
        blocks.add(slice(workbook, sst.start, sst.length))
        var next = records.indexOf(sst) + 1
        while (next < records.size && records[next].id == CONTINUE) {
            blocks.add(slice(workbook, records[next].start, records[next].length))
            next++
        }

        val reader = ByteReader(blocks)
        reader.u32() // cstTotal, the number of references rather than of strings
        val unique = reader.u32()
        if (unique !in 1..MaxStrings) return emptyList()

        val strings = ArrayList<String>(unique.toInt())
        for (i in 0 until unique.toInt()) {
            val count = reader.u16()
            val flags = reader.u8()
            if (count < 0 || flags < 0) break
            val compressed = flags and 0x01 == 0
            val hasRich = flags and 0x08 != 0
            val runs: Int
            val extended: Int
            if (hasRich) {
                runs = reader.u8()
                extended = reader.u8()
                reader.skip(2) // unused
            } else {
                runs = 0
                extended = 0
            }
            val text = reader.chars(count, compressed)
            // Rich-text and phonetic runs are formatting and language hints; a reader needs neither.
            reader.skip(runs * 4 + extended * 4)
            strings.add(text)
        }
        return strings
    }

    private fun readSheet(
        workbook: ByteArray,
        records: List<Record>,
        boundsheet: Record,
        strings: List<String>
    ): SheetData {
        val name = boundsheetName(workbook, boundsheet)
        val ply = u32(workbook, boundsheet.start).toInt()
        val cells = HashMap<Pair<Int, Int>, String>()
        val merges = ArrayList<CellRange>()
        var maxRow = 0
        var maxCol = 0
        var truncated = false

        var i = records.indexOfFirst { it.start - 4 >= ply }
        if (i < 0) return SheetData(name = name, parseFailure = "This sheet's data is not in the file.")
        var pendingFormula: Pair<Int, Int>? = null
        var entered = false
        while (i < records.size) {
            val record = records[i]
            if (record.id == EOF) break
            if (record.id == BOF) {
                if (entered) {
                    // A second BOF means the next sheet's substream started without an EOF between.
                    break
                }
                entered = true
            }
            when (record.id) {
                LABELSST -> {
                    val row = u16(workbook, record.start)
                    val col = u16(workbook, record.start + 2)
                    val index = u32(workbook, record.start + 6).toInt()
                    strings.getOrNull(index)?.takeIf { it.isNotEmpty() }?.let {
                        put(cells, row, col, it) { r, c ->
                            maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                        }
                    }
                }

                LABEL -> {
                    val row = u16(workbook, record.start)
                    val col = u16(workbook, record.start + 2)
                    val count = u16(workbook, record.start + 6)
                    val flags = workbook.getOrElse(record.start + 8) { 0 }.toInt() and 0xFF
                    val text = if (flags and 0x01 != 0) {
                        ascii(workbook, record.start + 9, count)
                    } else {
                        unicode(workbook, record.start + 9, count)
                    }
                    if (text.isNotEmpty()) put(cells, row, col, text) { r, c ->
                        maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                    }
                }

                NUMBER -> {
                    val row = u16(workbook, record.start)
                    val col = u16(workbook, record.start + 2)
                    val value = doubleAt(workbook, record.start + 6)
                    put(cells, row, col, formatNumber(value)) { r, c ->
                        maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                    }
                }

                RK -> {
                    val row = u16(workbook, record.start)
                    val col = u16(workbook, record.start + 2)
                    put(cells, row, col, formatNumber(decodeRk(u32(workbook, record.start + 6)))) { r, c ->
                        maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                    }
                }

                MULRK -> {
                    val row = u16(workbook, record.start)
                    val first = u16(workbook, record.start + 2)
                    // The trailing column index is read as "last + 1" by some writers and as "last"
                    // by Excel itself, so the number of (ixfe, rk) pairs the record length implies is
                    // what decides how many cells are here. A zero is a cell Excel wrote, not a blank.
                    val entries = (record.length - 6) / 6
                    var at = record.start + 4
                    for (k in 0 until entries) {
                        if (at + 6 > record.start + record.length) break
                        put(cells, row, first + k, formatNumber(decodeRk(u32(workbook, at + 2)))) { r, c ->
                            maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                        }
                        at += 6
                    }
                }

                FORMULA -> {
                    val row = u16(workbook, record.start)
                    val col = u16(workbook, record.start + 2)
                    val special = workbook.getOrElse(record.start + 6) { 0 }.toInt() and 0xFF
                    val byte0 = workbook.getOrElse(record.start + 7) { 0 }.toInt() and 0xFF
                    when {
                        special != 0xFF -> put(cells, row, col, formatNumber(doubleAt(workbook, record.start + 6))) { r, c ->
                            maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                        }

                        byte0 == 0x01 -> pendingFormula = row to col
                        byte0 == 0x02 -> put(cells, row, col, errorText(workbook[record.start + 3].toInt() and 0xFF)) { r, c ->
                            maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                        }

                        byte0 == 0x00 -> pendingFormula = row to col
                        else -> Unit
                    }
                }

                STRING -> {
                    val target = pendingFormula ?: break
                    val count = u16(workbook, record.start + 6)
                    val flags = workbook.getOrElse(record.start + 8) { 0 }.toInt() and 0xFF
                    val text = if (flags and 0x01 != 0) {
                        ascii(workbook, record.start + 9, count)
                    } else {
                        unicode(workbook, record.start + 9, count)
                    }
                    put(cells, target.first, target.second, text) { r, c ->
                        maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                    }
                    pendingFormula = null
                }

                BOOLERR -> {
                    val row = u16(workbook, record.start)
                    val col = u16(workbook, record.start + 2)
                    val value = workbook.getOrElse(record.start + 6) { 0 }.toInt() and 0xFF
                    val isError = workbook.getOrElse(record.start + 7) { 0 }.toInt() and 0xFF
                    put(cells, row, col, if (isError == 1) errorText(value) else if (value == 1) "TRUE" else "FALSE") { r, c ->
                        maxRow = maxOf(maxRow, r); maxCol = maxOf(maxCol, c)
                    }
                }

                MERGEDCELLS -> {
                    val count = u16(workbook, record.start)
                    var at = record.start + 2
                    for (n in 0 until count) {
                        if (at + 8 > record.start + record.length) break
                        val r1 = u16(workbook, at)
                        val r2 = u16(workbook, at + 2)
                        val c1 = u16(workbook, at + 4)
                        val c2 = u16(workbook, at + 6)
                        merges.add(CellRange.of(r1, c1, r2, c2))
                        maxRow = maxOf(maxRow, r2); maxCol = maxOf(maxCol, c2)
                        at += 8
                    }
                }

                else -> Unit
            }
            if (cells.size > MaxCellsPerSheet) {
                truncated = true
                break
            }
            i++
        }

        return SheetData(
            name = name,
            maxRow = maxRow,
            maxCol = maxCol,
            cells = cells,
            mergedRegions = merges,
            truncated = truncated
        )
    }

    /**
     * BOUNDSHEET: 4-byte stream position, 1 status byte, 1 unused, then a short unicode string -
     * whose flag bit 0 means 16-bit characters, the same rule the shared string table uses.
     */
    private fun boundsheetName(workbook: ByteArray, record: Record): String {
        val count = workbook.getOrElse(record.start + 6) { 0 }.toInt() and 0xFF
        val compressed = workbook.getOrElse(record.start + 7) { 0 }.toInt() and 0x01 == 0
        val text = if (compressed) {
            ascii(workbook, record.start + 8, count)
        } else {
            unicode(workbook, record.start + 8, count)
        }
        return text.ifBlank { "Sheet" }
    }

    private fun put(
        cells: MutableMap<Pair<Int, Int>, String>,
        row: Int,
        col: Int,
        value: String,
        grow: (Int, Int) -> Unit
    ) {
        if (row < 0 || col < 0 || row > 1_048_575 || col > 16_383) return
        cells[Pair(row, col)] = value
        grow(row, col)
    }

    /**
     * An RK value is either a 30-bit integer or the top half of an IEEE-754 double, with two flag
     * bits stolen from its low end; bit 0 says which, bit 1 says the real value is hundredths.
     */
    private fun decodeRk(raw: Long): Double {
        val asInt = (raw and 1L) != 0L
        val divide = (raw and 2L) != 0L
        val value = if (asInt) {
            (raw.toInt() shr 2).toDouble()
        } else {
            java.lang.Double.longBitsToDouble((raw and 0xFFFF_FFFCL) shl 32)
        }
        return if (divide) value / 100.0 else value
    }

    private fun formatNumber(value: Double): String = when {
        value.isNaN() -> "#NUM!"
        value.isInfinite() -> if (value > 0) "#NUM!" else "#NUM!"
        abs(value) < 1e-12 -> "0"
        value == Math.floor(value) && abs(value) < 1e15 -> value.toLong().toString()
        else -> String.format(Locale.US, "%.15g", value).trimEnd('0').trimEnd('.')
    }

    private fun errorText(code: Int): String = when (code) {
        0x00 -> "#NULL!"
        0x07 -> "#DIV/0!"
        0x08 -> "#VALUE!"
        0x09 -> "#REF!"
        0x0A -> "#NAME?"
        0x0B -> "#NUM!"
        0x0C -> "#N/A"
        else -> "#ERROR!"
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

    private fun doubleAt(bytes: ByteArray, at: Int): Double {
        if (at < 0 || at + 7 >= bytes.size) return 0.0
        return java.lang.Double.longBitsToDouble(
            (u32(bytes, at) or (u32(bytes, at + 4) shl 32))
        )
    }

    private fun slice(bytes: ByteArray, at: Int, length: Int): ByteArray {
        if (at < 0 || length <= 0 || at + length > bytes.size) return ByteArray(0)
        return bytes.copyOfRange(at, at + length)
    }

    private fun ascii(bytes: ByteArray, at: Int, count: Int): String {
        if (at < 0 || at >= bytes.size) return ""
        val end = minOf(at + count, bytes.size)
        val sb = StringBuilder(end - at)
        for (i in at until end) sb.append((bytes[i].toInt() and 0xFF).toChar())
        return sb.toString()
    }

    private fun unicode(bytes: ByteArray, at: Int, count: Int): String {
        if (at < 0 || at >= bytes.size) return ""
        val sb = StringBuilder(count)
        var i = at
        var produced = 0
        while (i + 1 < bytes.size && produced < count) {
            sb.append(u16(bytes, i).toChar())
            i += 2
            produced++
        }
        return sb.toString()
    }

    /**
     * Reads across CONTINUE records. When a string's character array is split, the continuation
     * repeats the one-byte flag before the rest of the text; header and run bytes cross without
     * that repetition, which is why the character reader is separate from the plain byte reader.
     */
    private class ByteReader(private val blocks: List<ByteArray>) {
        private var block = 0
        private var pos = 0

        private fun advance(): Boolean {
            if (block + 1 >= blocks.size) return false
            block++
            pos = 0
            return true
        }

        fun u8(): Int {
            if (pos >= blocks[block].size && !advance()) return -1
            return blocks[block][pos++].toInt() and 0xFF
        }

        fun u16(): Int {
            val low = u8()
            val high = u8()
            if (low < 0 || high < 0) return -1
            return low or (high shl 8)
        }

        fun u32(): Long {
            val a = u8(); val b = u8(); val c = u8(); val d = u8()
            if (d < 0) return -1L
            return (a or (b shl 8) or (c shl 16) or (d shl 24)).toLong() and 0xFFFF_FFFFL
        }

        fun skip(count: Int) {
            var left = count
            while (left > 0) {
                val available = blocks[block].size - pos
                val take = minOf(available, left)
                pos += take
                left -= take
                if (left > 0 && !advance()) return
            }
        }

        fun chars(count: Int, compressed: Boolean): String {
            val sb = StringBuilder(count.coerceAtMost(65_535))
            var produced = 0
            while (produced < count) {
                if (pos >= blocks[block].size) {
                    if (!advance()) break
                    pos++ // the repeated flag byte
                }
                val available = blocks[block].size - pos
                if (compressed) {
                    val take = minOf(available, count - produced)
                    for (i in 0 until take) sb.append((blocks[block][pos + i].toInt() and 0xFF).toChar())
                    pos += take
                } else {
                    val take = minOf(available / 2, count - produced)
                    if (take == 0) {
                        // A single stray byte cannot form a character; it belongs to the next block.
                        if (!advance()) break
                        pos++
                        continue
                    }
                    for (i in 0 until take) {
                        val at = pos + i * 2
                        sb.append(((blocks[block][at].toInt() and 0xFF) or
                            ((blocks[block][at + 1].toInt() and 0xFF) shl 8)).toChar())
                    }
                    pos += take * 2
                }
                produced = sb.length
            }
            return sb.toString()
        }
    }
}
