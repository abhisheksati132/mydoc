package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Delimited and OpenDocument behaviour the grid depends on.
 *
 * Each case exists because it used to be wrong in a specific way: quoted fields lost their
 * padding, a UTF-16 export decoded to NUL-interleaved text, a stray quote swallowed the rest of
 * the line, and .fods was handed to the CSV reader.
 */
class SpreadsheetFormatsTest {

    private val parser = SpreadsheetParser()

    private fun parseText(text: String, ext: String) =
        runBlocking { parser.parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)), ext) }

    private fun parseBytes(bytes: ByteArray, ext: String) =
        runBlocking { parser.parse(ByteArrayInputStream(bytes), ext) }

    @Test
    fun csv_keepsCommaAndNewlineInsideQuotedField() {
        val sheet = parseText("name,notes\r\n\"Widget, large\",\"line one\r\nline two\"\r\n", "csv").sheets.single()
        assertEquals("Widget, large", sheet.getCellValue(1, 0))
        assertEquals("line one\r\nline two", sheet.getCellValue(1, 1))
    }

    @Test
    fun csv_unescapesDoubledQuotes() {
        val sheet = parseText("a\r\n\"He said \"\"hi\"\"\"\r\n", "csv").sheets.single()
        assertEquals("He said \"hi\"", sheet.getCellValue(1, 0))
    }

    @Test
    fun csv_preservesPaddingOnlyInsideQuotes() {
        val sheet = parseText("a,b\r\n\" kept \",raw\r\n", "csv").sheets.single()
        assertEquals(" kept ", sheet.getCellValue(1, 0))
        assertEquals("raw", sheet.getCellValue(1, 1))
    }

    @Test
    fun csv_strayQuoteDoesNotSwallowRestOfLine() {
        val sheet = parseText("a,b,c\r\nx\"y,2,3\r\n", "csv").sheets.single()
        assertEquals("x\"y", sheet.getCellValue(1, 0))
        assertEquals("2", sheet.getCellValue(1, 1))
        assertEquals("3", sheet.getCellValue(1, 2))
    }

    @Test
    fun csv_detectsSemicolonWithoutBeingFooledByQuotedCommas() {
        val sheet = parseText("a;b\r\n\"1,000\";2\r\n", "csv").sheets.single()
        assertEquals("1,000", sheet.getCellValue(1, 0))
        assertEquals("2", sheet.getCellValue(1, 1))
    }

    @Test
    fun csv_readsUtf16LeWithByteOrderMark() {
        val payload = "city,country\nBelo Horizonte,Brazil\n".toByteArray(Charsets.UTF_16LE)
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + payload
        val value = parseBytes(bytes, "csv").sheets.single().getCellValue(1, 0)
        assertEquals("Belo Horizonte", value)
        assertTrue("NUL characters mean the byte-order mark was ignored", '\u0000' !in value)
    }

    @Test
    fun csv_fallsBackToCp1252WhenBytesAreNotValidUtf8() {
        val bytes = "name\r\ncaf\u00E9\r\n".toByteArray(java.nio.charset.Charset.forName("windows-1252"))
        assertEquals("caf\u00E9", parseBytes(bytes, "csv").sheets.single().getCellValue(1, 0))
    }

    @Test
    fun csv_reportsEmptyFileInsteadOfBlankSheet() {
        assertThrows(DocumentLoadException::class.java) {
            runBlocking { parser.parse(ByteArrayInputStream(ByteArray(0)), "csv") }
        }
    }

    private val flatOdf = """
        <?xml version="1.0" encoding="UTF-8"?>
        <office:document xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                         xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                         xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
          <office:body><office:spreadsheet>
            <table:table table:name="Q3 Report">
              <table:table-row>
                <table:table-cell office:value-type="string"><text:p>Revenue</text:p></table:table-cell>
                <table:table-cell office:value-type="float" office:value="1000"><text:p>1000</text:p></table:table-cell>
              </table:table-row>
              <table:table-row>
                <table:table-cell table:number-columns-repeated="2"/>
              </table:table-row>
              <table:table-row>
                <table:table-cell office:value-type="string"><text:p>Net</text:p></table:table-cell>
                <table:table-cell office:value-type="float" office:value="600"><text:p>600</text:p></table:table-cell>
              </table:table-row>
            </table:table>
            <table:table table:name="Sheet10">
              <table:table-row><table:table-cell><text:p>later</text:p></table:table-cell></table:table-row>
            </table:table>
          </office:spreadsheet></office:body>
        </office:document>
    """.trimIndent()

    @Test
    fun fods_isParsedAsOpenDocumentNotAsCsv() {
        val model = parseText(flatOdf, "fods")
        assertEquals(listOf("Q3 Report", "Sheet10"), model.sheets.map { it.name })

        val q3 = model.sheets[0]
        assertEquals("Revenue", q3.getCellValue(0, 0))
        assertEquals("1000", q3.getCellValue(0, 1))
        // The repeated-empty row must advance the row cursor, not collapse onto row 1.
        assertEquals("Net", q3.getCellValue(2, 0))
        assertEquals("600", q3.getCellValue(2, 1))
    }

    @Test
    fun fods_neverLeaksMarkupIntoCells() {
        val model = parseText(flatOdf, "fods")
        val all = model.sheets.flatMap { it.cells.values }
        assertTrue("raw XML leaked into cells: " + all.firstOrNull { it.contains("<table") },
            all.none { it.contains("<table") || it.contains("office:value") })
    }

    @Test
    fun xlsb_isReportedUnsupportedRatherThanAnEmptySheet() {
        val zipHeader = "PK".toByteArray() + ByteArray(20)
        assertThrows(DocumentLoadException::class.java) {
            runBlocking { parser.parse(ByteArrayInputStream(zipHeader), "xlsb") }
        }
    }
}
