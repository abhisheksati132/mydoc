package com.abhisheksati.mydoc.rendering.engine.spreadsheet

data class SheetData(
    val name: String,
    val maxRow: Int = 0,
    val maxCol: Int = 0,
    val cells: Map<Pair<Int, Int>, String> = emptyMap(),
    val columnWidths: Map<Int, Int> = emptyMap()
) {
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
}
