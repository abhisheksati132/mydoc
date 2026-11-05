package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import com.abhisheksati.mydoc.rendering.engine.Ole2File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * BIFF8 read against the two things that matter: a workbook Excel itself saved as
 * "Excel 97-2003", and record streams assembled by hand for the cases that file does not contain.
 *
 * The fixture is not a courtesy - it is the test. A reader written only against records it invented
 * can agree with itself and still be wrong about what Excel writes.
 */
class Biff8ReaderTest {

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }

    private fun workbook(container: ByteArray): ByteArray {
        val ole = Ole2File(container)
        return ole.getStream("Workbook") ?: error("no Workbook stream in the fixture")
    }

    @Test
    fun `the workbook Excel saved reads as a grid, not as recovered words`() {
        val model = Biff8Reader.read(workbook(fixture("real-biff8.xls")))
        assertNotNull("BIFF8 did not parse the fixture Excel wrote", model)
        val sheets = model!!.sheets

        assertEquals(listOf("Quarterly", "Second"), sheets.map { it.name })
        assertTrue(
            "the reader fell back to the byte scan",
            sheets.none { it.name == "Extracted Data" || it.parseFailure != null }
        )

        val quarterly = sheets.first()
        assertEquals("Region", quarterly.getCellValue(0, 0))
        assertEquals("Units", quarterly.getCellValue(0, 1))
        assertEquals("North", quarterly.getCellValue(1, 0))
        assertEquals("1234", quarterly.getCellValue(1, 1))
        assertEquals("98765.43", quarterly.getCellValue(1, 2))
        assertEquals("77", quarterly.getCellValue(2, 1))
        assertEquals("1234.5", quarterly.getCellValue(2, 2))
        assertEquals("East", quarterly.getCellValue(3, 0))
        assertEquals("0", quarterly.getCellValue(3, 1))
        assertEquals("-42.25", quarterly.getCellValue(3, 2))
        assertEquals(
            "a string long enough to leave the compressed-string case behind",
            quarterly.getCellValue(4, 0)
        )
        assertEquals("7", quarterly.getCellValue(5, 1))
        assertEquals("merged note across three columns", quarterly.getCellValue(6, 0))
        assertEquals(6, quarterly.maxRow)
        assertEquals(2, quarterly.maxCol)
        assertTrue(
            "A7:C7 is merged in the file",
            quarterly.mergedRegions.any { it.r1 == 6 && it.c1 == 0 && it.c2 == 2 }
        )

        val second = sheets[1]
        assertEquals("hello", second.getCellValue(0, 0))
        assertEquals("42", second.getCellValue(1, 0))
    }

    @Test
    fun `the parser routes a real xls through BIFF8 rather than the byte scan`() = runBlocking {
        val model = SpreadsheetParser().parse(ByteArrayInputStream(fixture("real-biff8.xls")), "xls")
        assertEquals(listOf("Quarterly", "Second"), model.sheets.map { it.name })
        assertEquals("98765.43", model.sheets.first().getCellValue(1, 2))
        assertTrue(model.sheets.none { it.parseFailure != null })
    }

    @Test
    fun `an integer rk and a hundredth rk decode the way the spec says`() {
        // RK bit 0 says the value is a 30-bit integer, bit 1 says divide by 100. The fixture happens
        // to contain only the double form, so the other two are assembled here.
        val intRk = (42 shl 2) or 1
        val hundredthsRk = (1234 shl 2) or 3
        val stream = workbookOf(
            sheets = listOf("Numbers"),
            strings = emptyList(),
            cells = listOf(
                record(RK, rowCol(0, 0) + littleInt(intRk)),
                record(RK, rowCol(1, 0) + littleInt(hundredthsRk))
            )
        )
        val sheet = Biff8Reader.read(stream)!!.sheets.first()
        assertEquals("42", sheet.getCellValue(0, 0))
        assertEquals("12.34", sheet.getCellValue(1, 0))
    }

    @Test
    fun `a string split across a continue record keeps its characters`() {
        // Excel writes the one-byte flags again at the start of the continuation, and the reader has
        // to skip that repeat rather than treat it as text.
        val text = "ABCDEFGHIJKLMNOP"
        val sstBody = littleInt(1) + littleInt(1) + littleShort(text.length) + byteArrayOf(0) +
            text.substring(0, 8).toByteArray(Charsets.ISO_8859_1)
        val continuation = byteArrayOf(0) + text.substring(8).toByteArray(Charsets.ISO_8859_1)
        val stream = workbookOf(
            sheets = listOf("Split"),
            strings = listOf(text),
            cells = listOf(record(LABELSST, rowCol(0, 0) + littleInt(0))),
            sstOverride = sstBody,
            extraGlobals = listOf(record(CONTINUE, continuation))
        )
        assertEquals(text, Biff8Reader.read(stream)!!.sheets.first().getCellValue(0, 0))
    }

    @Test
    fun `a 16-bit string and its rich runs are both stepped over`() {
        val rich = "café"
        val body = littleInt(1) + littleInt(1) +
            littleShort(rich.length) + byteArrayOf(0x09, 1, 1, 0, 0) +
            ByteBuffer.allocate(rich.length * 2).order(ByteOrder.LITTLE_ENDIAN)
                .putChar(rich[0]).putChar(rich[1]).putChar(rich[2]).putChar(rich[3]).array() +
            ByteArray(4) + ByteArray(4) // one rich run and one phonetic run
        val stream = workbookOf(
            sheets = listOf("Accented"),
            strings = listOf(rich),
            cells = listOf(record(LABELSST, rowCol(0, 0) + littleInt(0))),
            sstOverride = body
        )
        assertEquals(rich, Biff8Reader.read(stream)!!.sheets.first().getCellValue(0, 0))
    }

    // ---- record assembly -------------------------------------------------

    private companion object {
        const val BOF = 0x0809
        const val EOF = 0x000A
        const val SST = 0x00FC
        const val CONTINUE = 0x003C
        const val BOUNDSHEET = 0x0085
        const val LABELSST = 0x00FD
        const val RK = 0x027E
    }

    private fun record(id: Int, body: ByteArray): ByteArray = littleShort(id) + littleShort(body.size) + body

    private fun rowCol(row: Int, col: Int): ByteArray =
        littleShort(row) + littleShort(col) + littleShort(15) // ixfe, which a reader ignores

    private fun littleShort(value: Int): ByteArray =
        byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())

    private fun littleInt(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun bofBody(dt: Int): ByteArray =
        littleShort(0x0600) + littleShort(15) + littleInt(dt) + ByteArray(8)

    private fun sstBody(strings: List<String>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(littleInt(strings.size) + littleInt(strings.size))
        strings.forEach { text ->
            out.write(littleShort(text.length) + byteArrayOf(0) + text.toByteArray(Charsets.ISO_8859_1))
        }
        return out.toByteArray()
    }

    private fun boundsheet(name: String, position: Int): ByteArray {
        val encoded = name.toByteArray(Charsets.ISO_8859_1)
        // ShortString flags: bit 0 selects 16-bit characters, so an ASCII name writes 0 here.
        return record(BOUNDSHEET, littleInt(position) + byteArrayOf(0, 0, encoded.size.toByte(), 0) + encoded)
    }

    /**
     * Globals (BOF, SST, directory, EOF) followed by one substream per sheet - the shape the reader
     * walks. Directory positions are computed from the sizes, because that is the part a hand-made
     * stream most easily gets wrong.
     */
    private fun workbookOf(
        sheets: List<String>,
        strings: List<String>,
        cells: List<ByteArray>,
        sstOverride: ByteArray? = null,
        extraGlobals: List<ByteArray> = emptyList()
    ): ByteArray {
        val head = record(BOF, bofBody(0x0700)) + record(SST, sstOverride ?: sstBody(strings))
        val headBytes = ByteArrayOutputStream().apply {
            write(head)
            extraGlobals.forEach { write(it) }
        }.toByteArray()
        val tail = record(EOF, ByteArray(0))
        val directoryLength = sheets.sumOf { 12 + it.toByteArray(Charsets.ISO_8859_1).size }

        val substreams = sheets.mapIndexed { index, _ ->
            ByteArrayOutputStream().apply {
                write(record(BOF, bofBody(0x0010)))
                if (index == 0) cells.forEach { write(it) }
                write(tail)
            }.toByteArray()
        }

        var position = headBytes.size + directoryLength + tail.size
        val directory = ByteArrayOutputStream()
        sheets.forEachIndexed { index, name ->
            directory.write(boundsheet(name, position))
            position += substreams[index].size
        }

        val out = ByteArrayOutputStream()
        out.write(headBytes)
        out.write(directory.toByteArray())
        out.write(tail)
        substreams.forEach { out.write(it) }
        return out.toByteArray()
    }
}
