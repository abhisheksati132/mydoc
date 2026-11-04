package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * H-8: `XlsxReader` wrapped every worksheet parse in `runCatching` and threw the result away, so a
 * workbook whose second sheet was malformed came back looking like a workbook with an empty second
 * sheet. That is a different claim about the user's file, and the grid had no way to disclose it.
 */
class XlsxWorksheetFailureTest {

    private val goodSheet = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
          <row r="1"><c r="A1" t="inlineStr"><is><t>Kept</t></is></c></row>
        </sheetData></worksheet>
    """.trimIndent()

    /** A worksheet part whose element names do not nest: the parser cannot read it at all. */
    private val brokenSheet = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
          <row r="1"><c r="A1" t="inlineStr"><is><t>Wrong</t></is></c>
        </wrongClose></sheetData></worksheet>
    """.trimIndent()

    private fun workbook(sheetNamesAndParts: List<Pair<String, String>>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun add(name: String, body: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
            add(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "</Types>"
            )
            add(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>"
            )
            add(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                    "<sheets>" +
                    sheetNamesAndParts.mapIndexed { index, (name, _) ->
                        "<sheet name=\"$name\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>"
                    }.joinToString("") +
                    "</sheets></workbook>"
            )
            add(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    sheetNamesAndParts.mapIndexed { index, (_, _) ->
                        "<Relationship Id=\"rId${index + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${index + 1}.xml\"/>"
                    }.joinToString("") +
                    "</Relationships>"
            )
            sheetNamesAndParts.forEachIndexed { index, (_, part) ->
                add("xl/worksheets/sheet${index + 1}.xml", part)
            }
        }
        return out.toByteArray()
    }

    @Test
    fun aWellFormedWorkbookReportsNothingFailed() {
        val bytes = workbook(listOf("Keep" to goodSheet, "Also" to goodSheet))
        val parsed = XlsxReader.read(bytes)

        assertEquals(listOf("Keep", "Also"), parsed.sheets.map { it.name })
        assertEquals(emptyList<String>(), parsed.failedSheets)
        assertTrue("a complete workbook must not be called partial", !parsed.isPartial)
        parsed.sheets.forEach { assertNull(it.parseFailure) }
    }

    @Test
    fun aMalformedWorksheetIsRecordedAsFailedAndItsNeighbourStillReads() {
        val bytes = workbook(listOf("Keep" to goodSheet, "Broken" to brokenSheet))
        val parsed = XlsxReader.read(bytes)
        val model = parsed.toSpreadsheetModel()

        assertNotNull("the damaged sheet was accepted as silently empty", parsed.sheetByName("Broken")?.parseFailure)
        assertEquals(listOf("Broken"), parsed.failedSheets)
        assertTrue(parsed.isPartial)

        assertEquals("Kept", model.sheets.first { it.name == "Keep" }.cells[0 to 0])
        assertEquals("the readable sheet must not be marked failed", null, model.sheets.first { it.name == "Keep" }.parseFailure)
        assertEquals(listOf("Broken"), model.failedSheets)
        assertTrue("the grid would present this as the whole workbook", model.isPartial)
    }

    @Test
    fun aSheetNamedByTheWorkbookWhosePartIsAbsentIsNotInventedAsEmpty() {
        // Two sheets are listed but only one part exists: the reader must not quietly hand back a
        // blank sheet for the missing one.
        val singlePartBytes = workbook(listOf("Keep" to goodSheet, "Ghost" to goodSheet))
        val withoutGhost = truncateToSingleSheet(singlePartBytes)
        val parsed = XlsxReader.read(withoutGhost)
        val ghost = parsed.sheetByName("Ghost")
        val reported = ghost?.parseFailure ?: if (ghost == null) "dropped entirely" else null
        assertTrue(
            "a missing part was presented as an ordinary sheet: ${parsed.sheets.map { it.name to it.parseFailure }}",
            parsed.failedSheets.isNotEmpty() || parsed.sheets.size == 1
        )
        assertNotNull(reported)
    }

    /** Drops the second worksheet part while leaving the workbook listing both. */
    private fun truncateToSingleSheet(bytes: ByteArray): ByteArray {
        val kept = java.util.zip.ZipInputStream(bytes.inputStream()).let { zip ->
            val entries = LinkedHashMap<String, ByteArray>()
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name != "xl/worksheets/sheet2.xml") entries[entry.name] = zip.readBytes()
                zip.closeEntry()
            }
            entries
        }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            kept.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
