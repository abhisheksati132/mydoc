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
        if (extension.equals("csv", ignoreCase = true) || extension.equals("tsv", ignoreCase = true) || extension.equals("txt", ignoreCase = true)) {
            parseCsv(bytes)
        } else {
            parseXlsx(bytes)
        }
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
