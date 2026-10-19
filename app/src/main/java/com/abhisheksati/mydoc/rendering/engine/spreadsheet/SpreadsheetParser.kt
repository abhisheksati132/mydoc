package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class SpreadsheetParser {

    suspend fun parse(inputStream: InputStream, extension: String): SpreadsheetModel = withContext(Dispatchers.IO) {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            return@withContext SpreadsheetModel(listOf(SheetData(name = "Sheet 1")))
        }

        val ext = extension.lowercase().removePrefix(".")
        if (ext in setOf("csv", "tsv", "txt")) {
            return@withContext parseCsv(bytes)
        }

        val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

        if (isZip) {
            val odsModel = parseOds(bytes)
            if (odsModel.sheets.isNotEmpty() && odsModel.sheets.any { it.cells.isNotEmpty() }) {
                return@withContext odsModel
            }

            val xlsxModel = parseXlsx(bytes)
            if (xlsxModel.sheets.isNotEmpty() && xlsxModel.sheets.any { it.cells.isNotEmpty() }) {
                return@withContext xlsxModel
            }
        }

        parseXlsFallback(bytes)
    }

    private fun parseOds(bytes: ByteArray): SpreadsheetModel {
        var contentXml: ByteArray? = null
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.equals("content.xml", ignoreCase = true)) {
                    contentXml = zis.readBytes()
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        } catch (_: Throwable) {
            return SpreadsheetModel(emptyList())
        }

        if (contentXml == null) return SpreadsheetModel(emptyList())

        val sheets = mutableListOf<SheetData>()
        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(contentXml), "UTF-8")
            }
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
                            }
                            name.equals("table-cell", ignoreCase = true) -> {
                                inCell = true
                                cellTextBuilder.clear()
                                val repVal = parser.getAttributeValue(null, "table:number-columns-repeated") ?: parser.getAttributeValue(null, "number-columns-repeated")
                                curRepeatCols = repVal?.toIntOrNull()?.coerceIn(1, 100) ?: 1
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
                                    cells[Pair(rowIndex, colIndex)] = text
                                    if (colIndex > maxCol) maxCol = colIndex
                                    if (rowIndex > maxRow) maxRow = rowIndex
                                }
                                colIndex += curRepeatCols
                                inCell = false
                            }
                            name.equals("table-row", ignoreCase = true) -> {
                                rowIndex++
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
        var row = 0
        var col = 0
        for (token in valid.take(200)) {
            cells[Pair(row, col)] = token
            col++
            if (col >= 4) {
                col = 0
                row++
            }
        }

        return SpreadsheetModel(listOf(SheetData(name = "Sheet 1", maxRow = row, maxCol = 3, cells = cells)))
    }

    private fun parseCsv(bytes: ByteArray): SpreadsheetModel {
        val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes)))
        val lines = reader.readLines()
        if (lines.isEmpty()) return SpreadsheetModel(listOf(SheetData(name = "Sheet1")))

        // Auto-detect delimiter
        val sample = lines.take(5).joinToString("\n")
        val delimiter = when {
            sample.count { it == '\t' } > sample.count { it == ',' } -> '\t'
            sample.count { it == ';' } > sample.count { it == ',' } -> ';'
            else -> ','
        }

        val cells = mutableMapOf<Pair<Int, Int>, String>()
        var maxRow = 0
        var maxCol = 0

        lines.forEachIndexed { rowIndex, line ->
            if (line.isNotBlank()) {
                val tokens = line.split(delimiter)
                tokens.forEachIndexed { colIndex, token ->
                    val cleanVal = token.trim().removeSurrounding("\"")
                    if (cleanVal.isNotEmpty()) {
                        cells[Pair(rowIndex, colIndex)] = cleanVal
                    }
                    if (colIndex > maxCol) maxCol = colIndex
                }
                if (rowIndex > maxRow) maxRow = rowIndex
            }
        }

        val sheet = SheetData(
            name = "Data",
            maxRow = maxRow,
            maxCol = maxCol,
            cells = cells
        )
        return SpreadsheetModel(listOf(sheet))
    }

    private fun parseXlsx(bytes: ByteArray): SpreadsheetModel {
        // Step 1: Extract shared strings
        val sharedStrings = extractSharedStrings(bytes)

        // Step 2: Extract sheets
        val sheets = mutableListOf<SheetData>()
        val zip = ZipInputStream(ByteArrayInputStream(bytes))
        var entry: ZipEntry? = zip.nextEntry

        var sheetCounter = 1
        while (entry != null) {
            val name = entry.name
            if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                val sheetData = parseWorksheetXml(zip, "Sheet $sheetCounter", sharedStrings)
                sheets.add(sheetData)
                sheetCounter++
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        if (sheets.isEmpty()) {
            sheets.add(SheetData(name = "Sheet 1"))
        }

        return SpreadsheetModel(sheets)
    }

    private fun extractSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val zip = ZipInputStream(ByteArrayInputStream(bytes))
        var entry: ZipEntry? = zip.nextEntry

        while (entry != null) {
            if (entry.name == "xl/sharedStrings.xml") {
                val parser = Xml.newPullParser().apply {
                    setInput(zip, "UTF-8")
                }
                var event = parser.eventType
                var inText = false
                val currentText = StringBuilder()

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "t") {
                                inText = true
                                currentText.clear()
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inText) {
                                currentText.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "t") {
                                inText = false
                                strings.add(currentText.toString())
                            }
                        }
                    }
                    event = parser.next()
                }
                break
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        return strings
    }

    private fun parseWorksheetXml(
        stream: InputStream,
        sheetName: String,
        sharedStrings: List<String>
    ): SheetData {
        val parser = Xml.newPullParser().apply {
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
