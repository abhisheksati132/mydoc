package com.abhisheksati.mydoc.rendering.engine.spreadsheet

/**
 * Projects the rich OOXML model onto the grid's flat sheet shape.
 *
 * The grid viewer used to be fed by a second, weaker parser that invented sheet names, sorted
 * sheets lexicographically (so "Sheet10" came before "Sheet2"), showed date serials as numbers,
 * dropped formulas without cached results, and ignored hidden sheets, column widths and merged
 * ranges. [XlsxReader] already gets all of that right but was only reachable through the PDF
 * converter, so the viewer now adapts its result instead of re-parsing badly.
 */
fun XlsxWorkbook.toSpreadsheetModel(): SpreadsheetModel {
    val visible = sheets.filterNot { it.hidden }
    // A workbook whose every sheet is hidden has nothing to show; say so rather than inventing one.
    val sheetsToShow = visible.ifEmpty { sheets }

    return SpreadsheetModel(
        sheets = sheetsToShow.map { sheet ->
            val cells = HashMap<Pair<Int, Int>, String>(sheet.rows.size * 4)
            var widestRow = -1
            var widestCol = -1

            for ((rowIndex, row) in sheet.rows) {
                if (row.hidden) continue
                for ((colIndex, cell) in row.cells) {
                    val text = display(cell)
                    if (text.isEmpty()) continue
                    cells[rowIndex to colIndex] = text
                    if (rowIndex > widestRow) widestRow = rowIndex
                    if (colIndex > widestCol) widestCol = colIndex
                }
            }

            // Column widths reach the grid in dp, derived from the character width Excel stored.
            val widths = HashMap<Int, Int>()
            for ((index, info) in sheet.cols) {
                if (info.hidden) continue
                val chars = info.widthChars ?: continue
                widths[index] = (chars * CHARS_TO_DP).toInt().coerceIn(MIN_COLUMN_DP, MAX_COLUMN_DP)
            }

            SheetData(
                name = sheet.name,
                maxRow = widestRow.coerceAtLeast(0),
                maxCol = widestCol.coerceAtLeast(0),
                cells = cells,
                columnWidths = widths,
                mergedRegions = sheet.merges.toList(),
                truncated = sheet.truncated,
                parseFailure = sheet.parseFailure
            )
        }
    )
}

private const val CHARS_TO_DP = 8f
private const val MIN_COLUMN_DP = 40
private const val MAX_COLUMN_DP = 480
