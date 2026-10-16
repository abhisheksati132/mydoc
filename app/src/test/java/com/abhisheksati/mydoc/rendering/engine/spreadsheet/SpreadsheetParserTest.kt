package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class SpreadsheetParserTest {

    @Test
    fun columnIndexToLetter_isCorrect() {
        assertEquals("A", SheetData.columnIndexToLetter(0))
        assertEquals("B", SheetData.columnIndexToLetter(1))
        assertEquals("Z", SheetData.columnIndexToLetter(25))
        assertEquals("AA", SheetData.columnIndexToLetter(26))
        assertEquals("AB", SheetData.columnIndexToLetter(27))
        assertEquals("AZ", SheetData.columnIndexToLetter(51))
        assertEquals("BA", SheetData.columnIndexToLetter(52))
    }

    @Test
    fun parseCsv_withCommaDelimiter_extractsCellsCorrectly() = runBlocking {
        val parser = SpreadsheetParser()
        val csvData = """
            Name,Age,Role
            Alice,29,Engineer
            Bob,34,Designer
        """.trimIndent()

        val stream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
        val model = parser.parse(stream, "csv")

        assertEquals(1, model.sheets.size)
        val sheet = model.sheets[0]

        assertEquals("Name", sheet.getCellValue(0, 0))
        assertEquals("Age", sheet.getCellValue(0, 1))
        assertEquals("Role", sheet.getCellValue(0, 2))

        assertEquals("Alice", sheet.getCellValue(1, 0))
        assertEquals("29", sheet.getCellValue(1, 1))
        assertEquals("Engineer", sheet.getCellValue(1, 2))

        assertEquals("Bob", sheet.getCellValue(2, 0))
        assertEquals("34", sheet.getCellValue(2, 1))
        assertEquals("Designer", sheet.getCellValue(2, 2))
    }

    @Test
    fun parseCsv_withTabDelimiter_extractsCellsCorrectly() = runBlocking {
        val parser = SpreadsheetParser()
        val tsvData = "ColA\tColB\tColC\n100\t200\t300"

        val stream = ByteArrayInputStream(tsvData.toByteArray(Charsets.UTF_8))
        val model = parser.parse(stream, "tsv")

        val sheet = model.sheets[0]
        assertEquals("ColA", sheet.getCellValue(0, 0))
        assertEquals("ColB", sheet.getCellValue(0, 1))
        assertEquals("ColC", sheet.getCellValue(0, 2))
        assertEquals("100", sheet.getCellValue(1, 0))
        assertEquals("200", sheet.getCellValue(1, 1))
        assertEquals("300", sheet.getCellValue(1, 2))
    }
}
