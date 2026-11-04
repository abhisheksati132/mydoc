package com.abhisheksati.mydoc.rendering.engine.spreadsheet

data class SheetData(
    val name: String,
    val maxRow: Int = 0,
    val maxCol: Int = 0,
    val cells: Map<Pair<Int, Int>, String> = emptyMap(),
    val columnWidths: Map<Int, Int> = emptyMap(),
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
