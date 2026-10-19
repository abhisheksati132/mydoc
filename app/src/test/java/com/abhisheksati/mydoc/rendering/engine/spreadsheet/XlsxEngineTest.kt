package com.abhisheksati.mydoc.rendering.engine.spreadsheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxEngineTest {

    @Test
    fun testCellRefParsing() {
        assertEquals(0 to 0, XlsxRefs.parseCell("A1"))
        assertEquals(6 to 1, XlsxRefs.parseCell("B7"))
        assertEquals(6 to 1, XlsxRefs.parseCell("\$B\$7"))
        assertEquals("AA10", XlsxRefs.cellRef(9, 26))
    }

    @Test
    fun testRangeParsing() {
        assertEquals(CellRange(0, 0, 2, 2), XlsxRefs.parseRange("A1:C3"))
        assertEquals(CellRange(0, 2, XlsxRefs.MaxRow, 3), XlsxRefs.parseRange("C:D"))
    }

    @Test
    fun testRangeShift() {
        assertEquals(
            CellRange(5, 0, 6, 1),
            XlsxRefs.shiftRange(CellRange(3, 0, 4, 1), Axis.ROW, 2, 2)
        )
        assertEquals(
            CellRange(1, 0, 6, 1),
            XlsxRefs.shiftRange(CellRange(1, 0, 4, 1), Axis.ROW, 2, 2)
        )
    }

    @Test
    fun testColorTintAndLuminance() {
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        assertEquals(1.0, XlsxColors.luminance(white), 0.01)
        assertEquals(0.0, XlsxColors.luminance(black), 0.01)

        val tinted = XlsxColors.applyTint(black, 0.5)
        assertTrue(XlsxColors.luminance(tinted) > 0.0)
    }

    @Test
    fun testNumberAndDateFormatting() {
        assertEquals("123.45", ExcelCellFormat.apply("123.45", "0.00"))
        assertEquals("1,000", ExcelCellFormat.apply("1000", "#,##0"))
        assertEquals("50%", ExcelCellFormat.apply("0.5", "0%"))
        assertTrue(ExcelCellFormat.isDateCode("dd-mm-yyyy"))
    }
}
