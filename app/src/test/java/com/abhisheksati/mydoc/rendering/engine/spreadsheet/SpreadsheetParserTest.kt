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

    @Test
    fun parseHtmlTableNamedXls_extractsCellsCorrectly() = runBlocking {
        val parser = SpreadsheetParser()
        val html = "<html><body><table><tr><td>Col A</td><td>Col B</td></tr>" +
            "<tr><td>1</td><td>2</td></tr></table></body></html>"

        val model = parser.parse(ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)), "xls")

        val sheet = model.sheets[0]
        assertEquals("Col A", sheet.getCellValue(0, 0))
        assertEquals("Col B", sheet.getCellValue(0, 1))
        assertEquals("1", sheet.getCellValue(1, 0))
        assertEquals("2", sheet.getCellValue(1, 1))
    }

    @Test
    fun parseHtmlTable_stripsMarkupAndDecodesEntities() = runBlocking {
        val parser = SpreadsheetParser()
        val html = "<TABLE><TR><TH><b>Total</b></TH><TD>A &amp; B&nbsp;2 &lt;3 &#65;</TD></TR></TABLE>"

        val sheet = parser.parse(ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)), "xls").sheets[0]

        assertEquals("Total", sheet.getCellValue(0, 0))
        assertEquals("A & B 2 <3 A", sheet.getCellValue(0, 1))
    }

    @Test
    fun parseCsvNamedXls_extractsCellsCorrectly() = runBlocking {
        val parser = SpreadsheetParser()
        val csv = "Region,Spend\nEMEA,1200\nAPAC,900"

        val sheet = parser.parse(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)), "xls").sheets[0]

        assertEquals("Region", sheet.getCellValue(0, 0))
        assertEquals("Spend", sheet.getCellValue(0, 1))
        assertEquals("1200", sheet.getCellValue(1, 1))
    }
}
