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
            val formulas = HashMap<Pair<Int, Int>, String>()
            val cellStyles = HashMap<Pair<Int, Int>, SpreadsheetCellStyle>()
            var widestRow = -1
            var widestCol = -1

            for ((rowIndex, row) in sheet.rows) {
                if (row.hidden) continue
                for ((colIndex, cell) in row.cells) {
                    val style = styles.resolve(cell.style)
                    if (cell.style != 0) {
                        cellStyles[rowIndex to colIndex] = SpreadsheetCellStyle(
                            fillArgb = style.fillArgb,
                            fontArgb = style.fontArgb,
                            bold = style.bold,
                            italic = style.italic,
                            underline = style.underline || style.doubleUnderline,
                            strike = style.strike,
                            horizontalAlignment = style.hAlign,
                            verticalAlignment = style.vAlign,
                            wrapText = style.wrap,
                            leftBorder = style.left?.toGridBorder(),
                            rightBorder = style.right?.toGridBorder(),
                            topBorder = style.top?.toGridBorder(),
                            bottomBorder = style.bottom?.toGridBorder()
                        )
                    }
                    cell.formula?.text?.takeIf { it.isNotBlank() }?.let {
                        formulas[rowIndex to colIndex] = it
                    }
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

            val rowHeights = HashMap<Int, Int>()
            for ((index, row) in sheet.rows) {
                rowHeights[index] = if (row.hidden) 0 else row.heightPt
                    ?.let { (it * POINTS_TO_DP).toInt().coerceIn(MIN_ROW_DP, MAX_ROW_DP) }
                    ?: (sheet.defaultRowHeightPt * POINTS_TO_DP).toInt().coerceIn(MIN_ROW_DP, MAX_ROW_DP)
            }

            SheetData(
                name = sheet.name,
                maxRow = widestRow.coerceAtLeast(0),
                maxCol = widestCol.coerceAtLeast(0),
                cells = cells,
                columnWidths = widths,
                defaultRowHeightDp = (sheet.defaultRowHeightPt * POINTS_TO_DP).toInt().coerceIn(MIN_ROW_DP, MAX_ROW_DP),
                rowHeightsDp = rowHeights,
                formulas = formulas,
                cellStyles = cellStyles,
                conditionalFills = sheet.conditionalFills.toMap(),
                mergedRegions = sheet.merges.toList(),
                truncated = sheet.truncated,
                parseFailure = sheet.parseFailure
            )
        }
    )
}

private fun EdgeStyle.toGridBorder() = SpreadsheetBorder(argb, weight.coerceIn(0.5f, 3f), dashed, double)

private const val CHARS_TO_DP = 8f
private const val MIN_COLUMN_DP = 40
private const val MAX_COLUMN_DP = 480
private const val POINTS_TO_DP = 4f / 3f
private const val MIN_ROW_DP = 1
private const val MAX_ROW_DP = 240
