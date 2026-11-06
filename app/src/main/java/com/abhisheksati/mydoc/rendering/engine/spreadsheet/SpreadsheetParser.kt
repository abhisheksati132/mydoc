package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import android.util.Xml
import com.abhisheksati.mydoc.rendering.engine.Ole2File
import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import com.abhisheksati.mydoc.domain.model.LoadFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class SpreadsheetParser {

    private companion object {
        const val SAMPLE_BYTES = 4000
    }


    private val htmlRowPattern =
        Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val htmlCellPattern =
        Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val htmlTagPattern = Regex("<[^>]*>")
    private val htmlLineBreakPattern = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val whitespacePattern = Regex("\\s+")

    private fun readBoundedInputStream(inputStream: InputStream, maxBytes: Int = 50 * 1024 * 1024): ByteArray {
        val buffer = ByteArray(16384)
        val baos = java.io.ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        var totalRead = 0
        var n: Int
        while (inputStream.read(buffer).also { n = it } != -1) {
            totalRead += n
            if (totalRead > maxBytes) {
                throw java.io.IOException("File size exceeds safety limit of ${maxBytes / (1024 * 1024)} MB.")
            }
            baos.write(buffer, 0, n)
        }
        return baos.toByteArray()
    }

    /**
     * Reads a spreadsheet into the grid model.
     *
     * Failure is reported, never papered over: this used to return a blank "Sheet 1" whenever the
     * file was unreadable, oversized, or an XLSB whose binary worksheets it could not find, and the
     * viewer showed that empty grid as a loaded workbook.
     */
    suspend fun parse(inputStream: InputStream, extension: String): SpreadsheetModel = withContext(Dispatchers.IO) {
        val ext = extension.lowercase().removePrefix(".")
        if (ext == "xlsb") {
            throw DocumentLoadException(
                LoadFailure.UNSUPPORTED_FORMAT,
                "Excel Binary Workbook (.xlsb) stores its sheets in a binary format MyDoc cannot read. " +
                    "Save it as .xlsx to open it here."
            )
        }

        val bytes = try {
            readBoundedInputStream(inputStream, 50 * 1024 * 1024)
        } catch (e: java.io.IOException) {
            throw DocumentLoadException(LoadFailure.EXCEEDS_LIMITS, "This spreadsheet is larger than the 50 MB limit MyDoc reads.", e)
        } catch (e: SecurityException) {
            throw DocumentLoadException(LoadFailure.EXCEEDS_LIMITS, "This spreadsheet exceeds the safety limits MyDoc applies.", e)
        }
        ensureActive()

        if (bytes.isEmpty()) {
            throw DocumentLoadException(LoadFailure.EMPTY_DOCUMENT, "This file is empty (0 bytes).")
        }

        val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        val model = when {
            ext == "xlsx" || ext == "xlsm" -> readOoxmlWorkbook(bytes, isZip)

            ext == "ods" || ext == "ots" -> parseOds(bytes).ifEmpty {
                throw DocumentLoadException(LoadFailure.CORRUPT_DOCUMENT, "This OpenDocument spreadsheet has no readable sheets.")
            }

            ext == "fods" -> parseFods(bytes).ifEmpty {
                throw DocumentLoadException(LoadFailure.CORRUPT_DOCUMENT, "This flat ODF spreadsheet has no readable tables.")
            }

            ext == "csv" || ext == "tsv" || ext == "txt" -> parseCsv(bytes)

            // A real .xls is a CFBF container with a BIFF8 Workbook stream inside it, and that
            // stream says where every cell is. Only when it cannot be read does the file fall to
            // the byte scan below, which then reports itself as a failure rather than a grid.
            ext == "xls" -> readBiff8(bytes) ?: sniffAny(bytes, isZip).ifEmpty {
                throw DocumentLoadException(
                    LoadFailure.CORRUPT_DOCUMENT,
                    "This legacy Excel workbook has no readable cells."
                )
            }

            // Mislabelled files keep the existing content sniffing, but a completely unreadable
            // result is now an error rather than an empty grid.
            else -> sniffAny(bytes, isZip).ifEmpty {
                throw DocumentLoadException(
                    LoadFailure.CORRUPT_DOCUMENT,
                    if (isZip) "This file is a zip archive with no spreadsheet inside."
                    else "This file is not a spreadsheet MyDoc can read."
                )
            }
        }

        if (model.sheets.isEmpty()) {
            throw DocumentLoadException(LoadFailure.EMPTY_DOCUMENT, "This workbook contains no sheets.")
        }
        model
    }

    /** OOXML first, then ODF for archives mislabelled as .xlsx. */
    private fun readOoxmlWorkbook(bytes: ByteArray, isZip: Boolean): SpreadsheetModel {
        if (!isZip) {
            throw DocumentLoadException(
                LoadFailure.CORRUPT_DOCUMENT,
                "This .xlsx file is not a valid package — its contents are truncated or damaged."
            )
        }
        val workbook = try {
            XlsxReader.read(bytes)
        } catch (e: SecurityException) {
            throw DocumentLoadException(LoadFailure.EXCEEDS_LIMITS, "This spreadsheet exceeds the safety limits MyDoc applies.", e)
        } catch (e: Throwable) {
            throw DocumentLoadException(LoadFailure.CORRUPT_DOCUMENT, "The Excel package could not be parsed.", e)
        }
        if (workbook.sheets.isEmpty()) {
            val ods = parseOds(bytes)
            if (ods.sheets.isNotEmpty()) return ods
            throw DocumentLoadException(LoadFailure.EMPTY_DOCUMENT, "This workbook contains no sheets.")
        }
        return workbook.toSpreadsheetModel()
    }

    private fun SpreadsheetModel.ifEmpty(block: () -> Nothing): SpreadsheetModel =
        if (sheets.isNotEmpty() && sheets.any { it.cells.isNotEmpty() }) this else block()

    /** The Workbook stream of a CFBF container, decoded as BIFF8; null when it is not that. */
    private fun readBiff8(bytes: ByteArray): SpreadsheetModel? {
        if (!isOle2(bytes)) return null
        val ole = runCatching { Ole2File(bytes) }.getOrNull() ?: return null
        val workbook = runCatching { ole.getStream("Workbook") ?: ole.getStream("Book") }.getOrNull()
        if (workbook == null || workbook.size < 8) return null
        return runCatching { Biff8Reader.read(workbook) }.getOrNull()?.takeIf {
            it.sheets.any { sheet -> sheet.cells.isNotEmpty() }
        }
    }

    private fun sniffAny(bytes: ByteArray, isZip: Boolean): SpreadsheetModel {
        if (isZip) {
            val ods = parseOds(bytes)
            if (ods.sheets.isNotEmpty() && ods.sheets.any { it.cells.isNotEmpty() }) return ods
            val workbook = runCatching { XlsxReader.read(bytes).toSpreadsheetModel() }.getOrNull()
            if (workbook != null && workbook.sheets.any { it.cells.isNotEmpty() }) return workbook
        }
        if (!isOle2(bytes)) {
            // Web apps and CRMs export real spreadsheets as an HTML table or plain CSV with an
            // .xls extension; only a BIFF byte-scan renders those as an empty grid.
            val text = parseTextSheet(bytes)
            if (text.sheets.any { it.cells.isNotEmpty() }) return text
        }
        return parseXlsFallback(bytes)
    }

    private fun isOle2(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte()

    private fun parseTextSheet(bytes: ByteArray): SpreadsheetModel {
        val text = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        if (text.contains("<table", ignoreCase = true) || text.contains("<tr", ignoreCase = true)) {
            val htmlModel = parseHtmlTable(text)
            if (htmlModel.sheets.any { it.cells.isNotEmpty() }) return htmlModel
        }
        return parseCsv(bytes)
    }

    private fun parseHtmlTable(html: String): SpreadsheetModel {
        val cells = mutableMapOf<Pair<Int, Int>, String>()
        var maxRow = 0
        var maxCol = 0
        var rowIndex = -1

        for (rowMatch in htmlRowPattern.findAll(html)) {
            val rowHtml = rowMatch.groupValues[1]
            val rowCells = htmlCellPattern.findAll(rowHtml).map { it.groupValues[1] }.toList()
            if (rowCells.isEmpty()) continue
            rowIndex++
            rowCells.forEachIndexed { colIndex, rawCell ->
                val value = htmlCellText(rawCell)
                if (value.isNotEmpty()) {
                    cells[Pair(rowIndex, colIndex)] = value
                    if (rowIndex > maxRow) maxRow = rowIndex
                    if (colIndex > maxCol) maxCol = colIndex
                }
            }
        }

        return SpreadsheetModel(
            listOf(SheetData(name = "Sheet 1", maxRow = maxRow, maxCol = maxCol, cells = cells))
        )
    }

    private fun htmlCellText(rawCell: String): String =
        rawCell
            .replace(htmlLineBreakPattern, "\n")
            .let { htmlTagPattern.replace(it, " ") }
            .let { decodeHtmlEntities(it) }
            .lineSequence()
            .map { line -> line.trim().replace(whitespacePattern, " ") }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    private fun decodeHtmlEntities(text: String): String {
        if (!text.contains('&')) return text
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch != '&') {
                out.append(ch)
                i++
                continue
            }
            val semi = text.indexOf(';', i + 1)
            if (semi < 0 || semi - i > 10) {
                out.append(ch)
                i++
                continue
            }
            val body = text.substring(i + 1, semi)
            val decoded = when {
                body == "amp" -> "&"
                body == "lt" -> "<"
                body == "gt" -> ">"
                body == "quot" -> "\""
                body == "apos" -> "'"
                body == "nbsp" -> " "
                body.startsWith("#") -> {
                    val code = if (body.startsWith("#x") || body.startsWith("#X"))
                        body.drop(2).toIntOrNull(16)
                    else body.drop(1).toIntOrNull()
                    code?.takeIf { it in 0..0x10FFFF }?.let { String(Character.toChars(it)) }
                }
                else -> null
            }
            if (decoded != null) {
                out.append(decoded)
                i = semi + 1
            } else {
                out.append(ch)
                i++
            }
        }
        return out.toString()
    }

    private fun parseOds(bytes: ByteArray): SpreadsheetModel {
        val contentXml = extractOdfContent(bytes) ?: return SpreadsheetModel(emptyList())
        return parseOdfContent(contentXml)
    }

    /**
     * Flat OpenDocument Spreadsheet (.fods) is the ODF document XML with no zip around it.
     * It previously fell through to the CSV reader, which split every tag on commas and
     * produced a grid of markup fragments.
     */
    private fun parseFods(bytes: ByteArray): SpreadsheetModel = parseOdfContent(bytes)

    private fun extractOdfContent(bytes: ByteArray): ByteArray? {
        var contentXml: ByteArray? = null
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.equals("content.xml", ignoreCase = true)) {
                    val buf = ByteArray(16_384)
                    val baos = java.io.ByteArrayOutputStream()
                    var entryRead = 0L
                    val maxOdsEntry = 25L * 1024L * 1024L
                    var n: Int
                    while (zis.read(buf).also { n = it } != -1) {
                        if (entryRead + n > maxOdsEntry) {
                            android.util.Log.w("SpreadsheetParser", "ODS content.xml exceeds 25 MB limit")
                            contentXml = null
                            break
                        }
                        baos.write(buf, 0, n)
                        entryRead += n
                    }
                    if (entryRead <= maxOdsEntry) contentXml = baos.toByteArray()
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        } catch (_: Throwable) {
            return null
        }
        return contentXml
    }

    private fun parseOdfContent(contentXml: ByteArray): SpreadsheetModel {
        val sheets = mutableListOf<SheetData>()
        try {
            val parser = com.abhisheksati.mydoc.rendering.engine.EngineXml.newPullParser(ByteArrayInputStream(contentXml))
            var eventType = parser.eventType

            var currentSheetName = "Sheet 1"
            var sheetCounter = 1
            var inTable = false
            var inRow = false
            var inCell = false
            var inP = false

            var rowIndex = 0
            var colIndex = 0
            var maxRow = 0
            var maxCol = 0
            var curRepeatCols = 1
            var curRepeatRows = 1
            // The grid the reader can show, and a ceiling on the cells a repeated ODS range may
            // expand into.
            val maxOdsColumns = 1_024
            val maxOdsCells = 200_000
            // The values written by the current row, so a repeated row can be reproduced at the
            // following coordinates instead of being dropped.
            val rowValues = mutableListOf<Pair<Int, String>>()
            var cells = mutableMapOf<Pair<Int, Int>, String>()
            val cellTextBuilder = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name ?: ""
                val colonIdx = rawName.indexOf(':')
                val name = if (colonIdx >= 0) rawName.substring(colonIdx + 1) else rawName

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when {
                            name.equals("table", ignoreCase = true) -> {
                                inTable = true
                                val sheetNameAttr = parser.getAttributeValue(null, "table:name") ?: parser.getAttributeValue(null, "name")
                                currentSheetName = if (!sheetNameAttr.isNullOrBlank()) sheetNameAttr else "Sheet ${sheetCounter++}"
                                cells = mutableMapOf()
                                rowIndex = 0
                                maxRow = 0
                                maxCol = 0
                            }
                            name.equals("table-row", ignoreCase = true) -> {
                                inRow = true
                                colIndex = 0
                                rowValues.clear()
                                val repRows = parser.getAttributeValue(null, "table:number-rows-repeated")
                                    ?: parser.getAttributeValue(null, "number-rows-repeated")
                                curRepeatRows = (repRows?.toIntOrNull() ?: 1).coerceIn(1, 1_000)
                            }
                            name.equals("table-cell", ignoreCase = true) -> {
                                inCell = true
                                cellTextBuilder.clear()
                                val repVal = parser.getAttributeValue(null, "table:number-columns-repeated") ?: parser.getAttributeValue(null, "number-columns-repeated")
                                // A repeated cell is the same value in every column it spans. The
                                // old clamp to 100 silently moved every later cell in the row.
                                curRepeatCols = (repVal?.toIntOrNull() ?: 1).coerceIn(1, 1_024)
                            }
                            name.equals("p", ignoreCase = true) -> {
                                inP = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inCell && inP) {
                            val text = parser.text ?: ""
                            if (text.isNotEmpty()) {
                                cellTextBuilder.append(text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            name.equals("p", ignoreCase = true) -> {
                                inP = false
                            }
                            name.equals("table-cell", ignoreCase = true) -> {
                                val text = cellTextBuilder.toString().trim()
                                if (text.isNotEmpty()) {
                                    for (r in 0 until curRepeatCols) {
                                        val col = colIndex + r
                                        if (col >= maxOdsColumns || cells.size >= maxOdsCells) break
                                        cells[Pair(rowIndex, col)] = text
                                        rowValues.add(col to text)
                                    }
                                    if (colIndex + curRepeatCols - 1 > maxCol) {
                                        maxCol = minOf(colIndex + curRepeatCols - 1, maxOdsColumns - 1)
                                    }
                                    if (rowIndex > maxRow) maxRow = rowIndex
                                }
                                colIndex += curRepeatCols
                                inCell = false
                            }
                            name.equals("table-row", ignoreCase = true) -> {
                                // A repeated row is the same row N times; its values were dropped
                                // entirely before, so every following row moved up.
                                for (r in 1 until curRepeatRows) {
                                    if (cells.size >= maxOdsCells) break
                                    for ((col, value) in rowValues) {
                                        cells[Pair(rowIndex + r, col)] = value
                                    }
                                    if (rowIndex + r > maxRow) maxRow = rowIndex + r
                                }
                                rowIndex += curRepeatRows
                                curRepeatRows = 1
                                inRow = false
                            }
                            name.equals("table", ignoreCase = true) -> {
                                inTable = false
                                if (cells.isNotEmpty() || sheets.isEmpty()) {
                                    sheets.add(
                                        SheetData(
                                            name = currentSheetName,
                                            maxRow = maxRow,
                                            maxCol = maxCol,
                                            cells = cells.toMap()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Throwable) {}

        return SpreadsheetModel(sheets)
    }

    private fun parseXlsFallback(bytes: ByteArray): SpreadsheetModel {
        val strings = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        var i = 0
        val sbUtf16 = StringBuilder()
        while (i < bytes.size - 1) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            val ch = (b1 shl 8) or b0
            if (b1 == 0 && (b0 in 32..126)) {
                sbUtf16.append(b0.toChar())
                i += 2
            } else if (ch in 0x00A0..0x04FF || ch in 0x0600..0x097F) {
                sbUtf16.append(ch.toChar())
                i += 2
            } else {
                if (sbUtf16.length >= 2) {
                    val str = sbUtf16.toString().trim()
                    if (str.isNotBlank() && seen.add(str.lowercase())) strings.add(str)
                }
                sbUtf16.clear()
                i += 2
            }
        }

        val noise = setOf("root entry", "workbook", "book", "summaryinformation", "compobj")
        val valid = strings.filter { s ->
            val l = s.lowercase()
            noise.none { l.contains(it) } && !l.startsWith("<?xml")
        }

        val cells = mutableMapOf<Pair<Int, Int>, String>()
        if (valid.isEmpty()) {
            cells[Pair(0, 0)] = "Binary XLS format (.xls) requires conversion to XLSX for structured display."
            return SpreadsheetModel(listOf(SheetData(name = "Notice", maxRow = 0, maxCol = 0, cells = cells)))
        }

        cells[Pair(0, 0)] = "Extracted Text (Legacy XLS layout preview — convert to .xlsx for full grid formatting)"
        var row = 1
        for (token in valid.take(200)) {
            cells[Pair(row, 0)] = token
            row++
        }

        return SpreadsheetModel(
            listOf(
                SheetData(
                    name = "Extracted Data",
                    maxRow = row - 1,
                    maxCol = 0,
                    cells = cells,
                    // The scan is not a grid, and the viewer has to say so instead of the registry
                    // claiming structured reflow over a list of recovered words.
                    parseFailure = "The workbook's cell records could not be read; the text was recovered in order instead."
                )
            )
        )
    }

    /**
     * RFC 4180 reader with the practical extras: BOM and UTF-16 detection, a cp1252 fallback for
     * files that are not valid UTF-8, delimiter detection that ignores quoted content, and quoted
     * fields that keep their padding while bare fields are trimmed the way a spreadsheet would.
     */
    private fun parseCsv(bytes: ByteArray): SpreadsheetModel {
        val text = decodeText(bytes)
        if (text.isBlank()) return SpreadsheetModel(listOf(SheetData(name = "Data")))

        val delimiter = detectDelimiter(text)
        val cells = LinkedHashMap<Pair<Int, Int>, String>()
        val field = StringBuilder()
        var rowIndex = 0
        var colIndex = 0
        var maxRow = 0
        var maxCol = 0
        var quoted = false
        var fieldWasQuoted = false
        var i = 0
        val len = text.length

        fun endField() {
            // A quoted field keeps its characters exactly; a bare one is trimmed as a
            // spreadsheet would. `quoted` is already false by the time the closing quote is
            // consumed, so the decision needs its own flag.
            val value = if (fieldWasQuoted) field.toString() else field.toString().trim()
            if (value.isNotEmpty()) cells[rowIndex to colIndex] = value
            if (colIndex > maxCol) maxCol = colIndex
            field.setLength(0)
            quoted = false
            fieldWasQuoted = false
            colIndex++
        }

        fun endRow() {
            endField()
            if (rowIndex > maxRow) maxRow = rowIndex
            colIndex = 0
            rowIndex++
        }

        while (i < len) {
            val ch = text[i]
            if (quoted) {
                when {
                    ch == '"' && i + 1 < len && text[i + 1] == '"' -> { field.append('"'); i += 2 }
                    ch == '"' -> { quoted = false; i++ }
                    else -> { field.append(ch); i++ }
                }
                continue
            }
            when {
                // A quote only opens a field at its start; elsewhere it is literal content,
                // so one stray quote cannot swallow the remainder of the line.
                ch == '"' && field.isEmpty() -> { quoted = true; fieldWasQuoted = true; i++ }
                ch == delimiter -> { endField(); i++ }
                ch == '\r' -> {
                    if (i + 1 < len && text[i + 1] == '\n') i++
                    endRow(); i++
                }
                ch == '\n' -> { endRow(); i++ }
                else -> { field.append(ch); i++ }
            }
        }
        if (quoted) field.append('"')
        if (field.isNotEmpty() || colIndex > 0) endRow()

        return SpreadsheetModel(
            listOf(SheetData(name = "Data", maxRow = maxRow, maxCol = maxCol, cells = cells))
        )
    }

    /** Decodes a delimited file by its byte-order mark, falling back to cp1252 for legacy exports. */
    private fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        return try {
            java.nio.charset.Charset.forName("UTF-8").newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (_: java.nio.charset.CharacterCodingException) {
            // Western-European CSV exports from Excel are routinely cp1252, which UTF-8 cannot
            // decode; reading them as UTF-8 turned every accented character into U+FFFD.
            String(bytes, java.nio.charset.Charset.forName("windows-1252"))
        }
    }

    /** Counts candidate separators outside quoted fields; comma wins a tie. */
    private fun detectDelimiter(text: String): Char {
        val candidates = charArrayOf(',', ';', '\t', '|')
        val counts = HashMap<Char, Int>()
        var inQuotes = false
        var scanned = 0
        for (ch in text) {
            if (scanned++ > SAMPLE_BYTES) break
            when {
                ch == '"' -> inQuotes = !inQuotes
                inQuotes -> Unit
                ch in candidates -> counts[ch] = (counts[ch] ?: 0) + 1
            }
        }
        var best = ','
        var bestCount = 0
        for (c in candidates) {
            val n = counts[c] ?: 0
            if (n > bestCount) {
                best = c
                bestCount = n
            }
        }
        return best
    }

    private fun createXmlParser(): XmlPullParser {
        return com.abhisheksati.mydoc.rendering.engine.EngineXml.newPullParser()
    }

    private fun extractSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val parser = createXmlParser().apply {
            setInput(ByteArrayInputStream(bytes), "UTF-8")
        }
        var event = parser.eventType
        var inText = false
        val currentString = StringBuilder()
        var inSi = false

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.substringAfter(':')) {
                        "si" -> {
                            inSi = true
                            currentString.setLength(0)
                        }
                        // A <t> inside an <si> (plain or inside an <r> rich run) contributes text.
                        "t" -> if (inSi) inText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inText) currentString.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.substringAfter(':')) {
                        "t" -> inText = false
                        "si" -> {
                            strings.add(currentString.toString())
                            inSi = false
                        }
                    }
                }
            }
            event = parser.next()
        }
        return strings
    }

    private fun parseWorksheetXml(
        stream: InputStream,
        sheetName: String,
        sharedStrings: List<String>
    ): SheetData {
        val parser = createXmlParser().apply {
            setInput(stream, "UTF-8")
        }

        val cells = mutableMapOf<Pair<Int, Int>, String>()
        var maxRow = 0
        var maxCol = 0

        var event = parser.eventType
        var currentCellRef: String? = null
        var isSharedString = false
        var currentTag: String? = null
        val cellValue = StringBuilder()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "c") {
                        currentCellRef = parser.getAttributeValue(null, "r")
                        val type = parser.getAttributeValue(null, "t")
                        isSharedString = (type == "s")
                        cellValue.clear()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (currentTag == "v" || currentTag == "t") {
                        cellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "c") {
                        val ref = currentCellRef
                        if (ref != null) {
                            val (r, c) = parseCellRef(ref)
                            val raw = cellValue.toString().trim()
                            val finalVal = if (isSharedString) {
                                val index = raw.toIntOrNull() ?: -1
                                if (index in sharedStrings.indices) sharedStrings[index] else raw
                            } else {
                                raw
                            }

                            if (finalVal.isNotEmpty()) {
                                cells[Pair(r, c)] = finalVal
                                if (r > maxRow) maxRow = r
                                if (c > maxCol) maxCol = c
                            }
                        }
                        currentCellRef = null
                    }
                    currentTag = null
                }
            }
            event = parser.next()
        }

        return SheetData(
            name = sheetName,
            maxRow = maxRow,
            maxCol = maxCol,
            cells = cells
        )
    }

    private fun parseCellRef(ref: String): Pair<Int, Int> {
        var colIndex = 0
        var rowStr = ""

        for (ch in ref) {
            if (ch in 'A'..'Z') {
                colIndex = (colIndex * 26) + (ch - 'A' + 1)
            } else if (ch in '0'..'9') {
                rowStr += ch
            }
        }
        val row = (rowStr.toIntOrNull() ?: 1) - 1
        val col = if (colIndex > 0) colIndex - 1 else 0
        return Pair(row, col)
    }
}
