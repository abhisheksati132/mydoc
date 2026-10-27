package com.abhisheksati.mydoc.rendering.engine.spreadsheet

class TextRun(val start: Int, val end: Int, val props: XmlEl?)

class RichText(val text: String, val runs: List<TextRun>?) {
    val isRich get() = runs != null && runs.any { it.props != null }
}

data class FormulaSpec(val text: String, val type: String? = null, val si: String? = null, val ref: String? = null)

data class CellData(
    val type: String,
    val raw: String,
    val style: Int,
    val formula: FormulaSpec? = null,
    val rich: RichText? = null
) {
    val isBlank get() = raw.isEmpty() && formula == null
}

class RowData(
    var heightPt: Float? = null,
    var hidden: Boolean = false,
    var style: Int = -1
) {
    val cells = java.util.TreeMap<Int, CellData>()
}

data class ColumnInfo(val widthChars: Float?, val hidden: Boolean, val style: Int)

enum class ValidationKind { LIST, OTHER }

class DataValidation(
    var ranges: List<CellRange>,
    val kind: ValidationKind,
    val formula1: String?,
    val allowBlank: Boolean,
    val showDropDown: Boolean,
    val strict: Boolean,
    val prompt: String?
)

class XlsxSheet(
    val name: String,
    val partPath: String
) {
    val rows = java.util.TreeMap<Int, RowData>()
    val cols = java.util.TreeMap<Int, ColumnInfo>()
    val merges = mutableListOf<CellRange>()
    val validations = mutableListOf<DataValidation>()
    var defaultRowHeightPt = 15f
    var defaultColWidthChars = 8.43f
    var frozenRows = 0
    var frozenCols = 0
    var showGridLines = true
    var tabColor: ColorSpec? = null
    var hidden = false
    var truncated = false

    fun cell(r: Int, c: Int): CellData? = rows[r]?.cells?.get(c)

    val maxRow: Int get() = rows.lastEntry()?.let { e -> if (e.value.cells.isEmpty() && e.value.heightPt == null) lastRealRow() else e.key } ?: -1
    private fun lastRealRow(): Int = rows.descendingMap().entries.firstOrNull { it.value.cells.isNotEmpty() }?.key ?: -1
    val maxCol: Int get() {
        var m = -1
        for (row in rows.values) row.cells.lastEntry()?.let { if (it.key > m) m = it.key }
        for (mr in merges) if (mr.c2 > m) m = mr.c2
        return m
    }

    fun isRowHidden(r: Int) = rows[r]?.hidden == true
    fun isColHidden(c: Int) = cols[c]?.hidden == true

    fun rowHeightPt(r: Int): Float {
        val row = rows[r]
        if (row?.hidden == true) return 0f
        return row?.heightPt ?: defaultRowHeightPt
    }

    fun colWidthChars(c: Int): Float {
        val ci = cols[c]
        if (ci?.hidden == true) return 0f
        return ci?.widthChars ?: defaultColWidthChars
    }

    fun mergeAt(r: Int, c: Int): CellRange? {
        for (m in merges) if (m.contains(r, c)) return m
        return null
    }

    fun validationAt(r: Int, c: Int): DataValidation? =
        validations.firstOrNull { v -> v.ranges.any { it.contains(r, c) } }
}

class XlsxWorkbook(
    val sheets: List<XlsxSheet>,
    val styles: StyleTable,
    val sharedStrings: List<RichText>,
    val editable: Boolean
) {
    fun sheetByName(name: String): XlsxSheet? = sheets.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun textOf(c: CellData): RichText? = when (c.type) {
        "s" -> sharedStrings.getOrNull(c.raw.trim().toIntOrNull() ?: -1)
        "inlineStr" -> c.rich ?: RichText(c.raw, null)
        else -> null
    }

    fun display(c: CellData): String = if (c.formula != null && c.raw.isEmpty() && c.formula.text.isNotEmpty()) {
        "=" + c.formula.text
    } else when (c.type) {
        "s", "inlineStr" -> textOf(c)?.text ?: c.raw
        "b" -> if (c.raw.trim() == "1") "TRUE" else "FALSE"
        "str", "e" -> c.raw
        else -> if (c.raw.isEmpty()) "" else ExcelCellFormat.apply(c.raw, styles.resolve(c.style).numFmtCode)
    }

    fun editText(c: CellData): String = when {
        c.formula != null && c.formula.text.isNotEmpty() -> "=" + c.formula.text
        c.type == "s" || c.type == "inlineStr" -> textOf(c)?.text ?: c.raw
        c.type == "b" -> if (c.raw.trim() == "1") "TRUE" else "FALSE"
        c.type == "" && ExcelCellFormat.isDateCode(styles.resolve(c.style).numFmtCode) -> display(c)
        else -> c.raw
    }

    fun listItems(v: DataValidation, host: XlsxSheet): List<String> {
        val f = v.formula1?.trim() ?: return emptyList()
        if (f.startsWith("\"")) {
            return f.removeSurrounding("\"").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        }
        val bang = f.lastIndexOf('!')
        val sheet = if (bang > 0) sheetByName(f.substring(0, bang).removePrefix("=").removeSurrounding("'").replace("''", "'")) else host
        val range = XlsxRefs.parseRange(f.removePrefix("=")) ?: return emptyList()
        sheet ?: return emptyList()
        val out = ArrayList<String>()
        val rLast = minOf(range.r2, sheet.rows.lastKey() ?: range.r1, range.r1 + 500)
        for (r in range.r1..rLast) for (c in range.c1..minOf(range.c2, range.c1 + 50)) {
            sheet.cell(r, c)?.let { cd -> display(cd).takeIf { it.isNotBlank() }?.let(out::add) }
        }
        return out.distinct()
    }

    fun parseInput(text: String, style: Int): CellData? {
        if (text.isEmpty()) return null
        if (text.startsWith("=") && text.length > 1) return CellData("", "", style, FormulaSpec(text.substring(1)))
        val trimmed = text.trim()
        val upper = trimmed.uppercase()
        if (upper == "TRUE" || upper == "FALSE") return CellData("b", if (upper == "TRUE") "1" else "0", style)
        val code = styles.resolve(style).numFmtCode
        if (code != "@") {
            val num = parseNumber(trimmed)
            if (num != null) return CellData("", num, style)
            if (ExcelCellFormat.isDateCode(code)) parseDateSerial(trimmed)?.let { return CellData("", it, style) }
        }
        return CellData("inlineStr", text, style)
    }

    private fun parseNumber(s: String): String? {
        var t = s.replace(",", "")
        var pct = false
        if (t.endsWith("%")) { pct = true; t = t.dropLast(1) }
        val d = t.toDoubleOrNull() ?: return null
        if (d.isNaN() || d.isInfinite()) return null
        val v = if (pct) d / 100 else d
        return if (v == Math.floor(v) && kotlin.math.abs(v) < 1e15) v.toLong().toString() else v.toString()
    }

    private fun parseDateSerial(s: String): String? {
        val m = Regex("^(\\d{1,4})[-/.](\\d{1,2})[-/.](\\d{1,4})$").find(s) ?: return null
        val (a, b, c) = m.destructured
        val (y, mo, d) = if (a.length == 4) Triple(a.toInt(), b.toInt(), c.toInt()) else Triple(c.toInt().let { if (it < 100) it + 2000 else it }, b.toInt(), a.toInt())
        if (mo !in 1..12 || d !in 1..31) return null
        val cal = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC")).apply { clear(); set(y, mo - 1, d) }
        val base = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC")).apply { clear(); set(1899, 11, 30) }
        val days = (cal.timeInMillis - base.timeInMillis) / 86_400_000L
        return days.toString()
    }
}
