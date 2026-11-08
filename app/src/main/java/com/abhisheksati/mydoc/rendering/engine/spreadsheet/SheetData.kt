package com.abhisheksati.mydoc.rendering.engine.spreadsheet

data class SheetData(
    val name: String,
    val maxRow: Int = 0,
    val maxCol: Int = 0,
    val cells: Map<Pair<Int, Int>, String> = emptyMap(),
    val columnWidths: Map<Int, Int> = emptyMap(),
    val defaultRowHeightDp: Int = 20,
    val rowHeightsDp: Map<Int, Int> = emptyMap(),
    /** Original formula text keyed by zero-based cell address, without the leading '='. */
    val formulas: Map<Pair<Int, Int>, String> = emptyMap(),
    /** Cell colors and emphasis used by the workbook, keyed by zero-based cell address. */
    val cellStyles: Map<Pair<Int, Int>, SpreadsheetCellStyle> = emptyMap(),
    /** Dynamic fill colors from supported numeric conditional-formatting rules. */
    val conditionalFills: Map<Pair<Int, Int>, Int> = emptyMap(),
    /** Ranges where one value visually spans several cells. */
    val mergedRegions: List<CellRange> = emptyList(),
    /** True when the reader stopped early at a safety cap, so the sheet shown is incomplete. */
    val truncated: Boolean = false,
    /** Why this sheet could not be read at all; null when it was read to the end. */
    val parseFailure: String? = null
) {
    /** Merged cells other than the anchor hold no value; the grid dims them instead of leaving blanks. */
    fun isMergedNonAnchor(row: Int, col: Int): Boolean =
        mergedRegions.any { it.contains(row, col) && (it.r1 != row || it.c1 != col) }
    fun getCellValue(row: Int, col: Int): String {
        return cells[Pair(row, col)] ?: ""
    }

    companion object {
        fun columnIndexToLetter(colIndex: Int): String {
            var temp = colIndex
            val sb = StringBuilder()
            while (temp >= 0) {
                sb.append(('A'.code + (temp % 26)).toChar())
                temp = (temp / 26) - 1
            }
            return sb.reverse().toString()
        }
    }
}

data class SpreadsheetCellStyle(
    val fillArgb: Int?,
    val fontArgb: Int?,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val strike: Boolean,
    val horizontalAlignment: String,
    val verticalAlignment: String,
    val wrapText: Boolean,
    val leftBorder: SpreadsheetBorder? = null,
    val rightBorder: SpreadsheetBorder? = null,
    val topBorder: SpreadsheetBorder? = null,
    val bottomBorder: SpreadsheetBorder? = null
)

data class SpreadsheetBorder(
    val colorArgb: Int?,
    val widthDp: Float,
    val dashed: Boolean,
    val double: Boolean
)

data class SpreadsheetModel(
    val sheets: List<SheetData> = emptyList()
) {
    val isEmpty: Boolean
        get() = sheets.isEmpty() || sheets.all { it.cells.isEmpty() }

    /** Sheets that failed to parse, in the order the reader met them. */
    val failedSheets: List<String>
        get() = sheets.mapNotNull { it.parseFailure?.let { _ -> it.name } }

    /** True when what the grid shows is not everything the workbook contains. */
    val isPartial: Boolean
        get() = sheets.any { it.parseFailure != null || it.truncated }
}
