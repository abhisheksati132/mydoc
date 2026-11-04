package com.abhisheksati.mydoc.rendering.engine.word

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.util.Xml
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min
import org.xmlpull.v1.XmlPullParser

/**
 * Lays [DocxBlock]s out into vector PDF pages using the framework's own text measurement, so a
 * Word document opens as paginated pages the same way a converted .pptx or .xlsx does.
 *
 * Typographic rather than faithful: headings, inline emphasis, lists and table grids are
 * reproduced; embedded images and character-level styles from styles.xml are not, because
 * [DocxParser] does not read them.
 */
internal object DocxPdfRenderer {

    private const val BodySize = 11f
    private const val TableSize = 9f
    private const val Leading = 1.3f
    private const val ParagraphGap = 7f
    private const val ListIndent = 16f
    private const val CellPad = 4f
    private const val MaxPages = 4000

    private fun headingSize(level: Int): Float = when (level) {
        1 -> 19f
        2 -> 15.5f
        3 -> 13f
        4 -> 12f
        else -> 11f
    }

    fun render(blocks: List<DocxBlock>, docxBytes: ByteArray): DocxRenderResult? {
        if (blocks.isEmpty()) return null
        val writer = PageWriter(readSection(docxBytes))
        val outlines = writer.write(blocks)
        return writer.finish()?.let { DocxRenderResult(it, outlines) }
    }

    private class PageWriter(private val section: Section) {

        private val doc = PdfDocument()
        private val outlines = mutableListOf<OutlineItem>()
        private val paints = HashMap<Long, TextPaint>()
        private var canvas: Canvas? = null
        private var currentPage: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = section.top

        fun write(blocks: List<DocxBlock>): List<OutlineItem> {
            for (block in blocks) {
                if (pageNumber >= MaxPages) break
                when (block) {
                    is DocxBlock.Heading -> heading(block)
                    is DocxBlock.Paragraph -> text(block.runs, BodySize, 0f)
                    is DocxBlock.Bullet -> bullet(block.runs)
                    is DocxBlock.Table -> table(block.rows)
                }
            }
            return outlines
        }

        fun finish(): PdfDocument? {
            if (pageNumber == 0) startPage()
            currentPage?.let { doc.finishPage(it) }
            currentPage = null
            canvas = null
            return if (pageNumber > 0) doc else null.also { doc.close() }
        }

        private fun heading(block: DocxBlock.Heading) {
            val size = headingSize(block.level)
            val step = size * Leading
            // Keep the heading on the page that carries at least one line under it.
            ensureRoom(step * 2f)
            y += step * 0.3f
            val lines = wrap(listOf(DocxRun(block.text)), size, section.contentWidth, forceBold = true)
            for (line in lines) {
                ensureRoom(step)
                drawLine(line, section.left)
                y += step
            }
            y += ParagraphGap * 0.6f
            outlines.add(OutlineItem(title = block.text, pageIndex = pageNumber - 1, level = block.level - 1))
        }

        private fun bullet(runs: List<DocxRun>) {
            val step = BodySize * Leading
            val marker = paint(BodySize, false, false, false)
            val lines = wrap(runs, BodySize, section.contentWidth - ListIndent, forceBold = false)
            for ((index, line) in lines.withIndex()) {
                ensureRoom(step)
                val baseline = y + line.maxOf { -it.paint.fontMetrics.ascent }
                if (index == 0) canvas?.drawText("•", section.left + 2f, baseline, marker)
                drawLine(line, section.left + ListIndent, baseline)
                y += step
            }
            y += ParagraphGap * 0.5f
        }

        private fun text(runs: List<DocxRun>, size: Float, indent: Float) {
            val step = size * Leading
            for (line in wrap(runs, size, section.contentWidth - indent, forceBold = false)) {
                ensureRoom(step)
                drawLine(line, section.left + indent)
                y += step
            }
            y += ParagraphGap
        }

        private fun table(rows: List<List<String>>) {
            val columns = rows.maxOf { it.size }.coerceIn(1, 24)
            val cell = paint(TableSize, false, false, false)
            val step = TableSize * Leading
            val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 0.6f
                color = 0xFFBFC3C9.toInt()
            }
            val headerFill = Paint().apply { color = 0xFFF2F3F5.toInt() }

            // Columns holding longer words get more room, then the row is rescaled to the page.
            val wanted = (0 until columns).map { c ->
                var longest = 0f
                for (row in rows) {
                    val text = row.getOrNull(c) ?: continue
                    for (word in text.split(' ')) longest = max(longest, cell.measureText(word))
                }
                (longest + 2 * CellPad).coerceIn(34f, section.contentWidth * 0.6f)
            }
            val total = wanted.sum().coerceAtLeast(1f)
            val widths = wanted.map { it / total * section.contentWidth }

            y += ParagraphGap * 0.4f
            for ((rowIndex, row) in rows.withIndex()) {
                val lines = (0 until columns).map { c ->
                    wrap(listOf(DocxRun(row.getOrNull(c) ?: "")), TableSize, widths[c] - 2f * CellPad, forceBold = rowIndex == 0)
                }
                val rowHeight = max(step * lines.maxOf { it.size } + 2 * CellPad, step + 2 * CellPad)
                ensureRoom(rowHeight)
                val surface = canvas ?: return
                val top = y
                if (rowIndex == 0) {
                    surface.drawRect(section.left, top, section.left + section.contentWidth, top + rowHeight, headerFill)
                }
                var x = section.left
                for (c in 0 until columns) {
                    var lineY = top + CellPad
                    for (line in lines[c]) {
                        drawLine(line, x + CellPad, lineY + line.maxOf { -it.paint.fontMetrics.ascent })
                        lineY += step
                    }
                    surface.drawRect(x, top, x + widths[c], top + rowHeight, grid)
                    x += widths[c]
                }
                y = top + rowHeight
            }
            y += ParagraphGap
        }

        private fun drawLine(line: List<Piece>, startX: Float, baselineOverride: Float? = null) {
            val surface = canvas ?: return
            val baseline = baselineOverride ?: y + line.maxOf { -it.paint.fontMetrics.ascent }
            var x = startX
            for (piece in line) {
                surface.drawText(piece.text, x, baseline, piece.paint)
                x += piece.width
            }
        }

        private fun ensureRoom(height: Float) {
            if (canvas == null) {
                startPage()
                return
            }
            if (y + height > section.height - section.bottom && y > section.top) startPage()
        }

        private fun startPage() {
            if (pageNumber >= MaxPages) return
            currentPage?.let { doc.finishPage(it) }
            pageNumber++
            val page = doc.startPage(PdfDocument.PageInfo.Builder(section.width, section.height, pageNumber).create())
            canvas = page.canvas
            canvas?.drawColor(Color.WHITE)
            currentPage = page
            y = section.top
        }

        private fun wrap(runs: List<DocxRun>, size: Float, maxWidth: Float, forceBold: Boolean): List<List<Piece>> {
            val limit = max(maxWidth, 24f)
            val lines = mutableListOf<MutableList<Piece>>()
            var current = mutableListOf<Piece>()
            var width = 0f

            for (run in runs) {
                val bold = forceBold || run.isBold
                val piecePaint = paint(size, bold, run.isItalic, run.isUnderline)
                for (token in run.text.split(' ')) {
                    if (token.isEmpty()) continue
                    if (current.isNotEmpty() && width + piecePaint.measureText(" $token") > limit) {
                        lines.add(current)
                        current = mutableListOf()
                        width = 0f
                    }
                    val word = if (current.isEmpty()) token else " $token"
                    var measured = piecePaint.measureText(word)
                    var text = word
                    if (measured > limit && current.isEmpty()) {
                        text = trimTo(token, piecePaint, limit)
                        measured = piecePaint.measureText(text)
                    }
                    current.add(Piece(text, piecePaint, measured))
                    width += measured
                }
            }
            if (current.isNotEmpty()) lines.add(current)
            if (lines.isEmpty()) lines.add(mutableListOf(Piece("", paint(size, false, false, false), 0f)))
            return lines
        }

        private fun trimTo(token: String, paint: TextPaint, limit: Float): String {
            var end = token.length
            while (end > 1 && paint.measureText(token.substring(0, end)) > limit) end--
            return token.substring(0, end)
        }

        private fun paint(size: Float, bold: Boolean, italic: Boolean, underline: Boolean): TextPaint {
            val key = (size.toLong() and 0xFFFFL) or
                ((if (bold) 1L else 0L) shl 16) or
                ((if (italic) 1L else 0L) shl 17) or
                ((if (underline) 1L else 0L) shl 18)
            return paints.getOrPut(key) {
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = size
                    isUnderlineText = underline
                    typeface = Typeface.create(
                        Typeface.SANS_SERIF,
                        when {
                            bold && italic -> Typeface.BOLD_ITALIC
                            bold -> Typeface.BOLD
                            italic -> Typeface.ITALIC
                            else -> Typeface.NORMAL
                        }
                    )
                }
            }
        }
    }

    private class Piece(val text: String, val paint: TextPaint, val width: Float)

    private data class Section(
        val width: Int,
        val height: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val contentWidth: Float get() = (width - left - right).coerceAtLeast(60f)
    }

    private fun readSection(docxBytes: ByteArray): Section {
        val xml = runCatching {
            ZipInputStream(ByteArrayInputStream(docxBytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name.removePrefix("/").lowercase() == "word/document.xml") {
                        return@use readBounded(zip, 8 * 1024 * 1024)
                    }
                }
                ByteArray(0)
            }
        }.getOrDefault(ByteArray(0))
        if (xml.isEmpty()) return defaultSection()

        return runCatching {
            val parser = com.abhisheksati.mydoc.rendering.engine.EngineXml.newPullParser(ByteArrayInputStream(xml))
            var width = 0
            var height = 0
            var top = 0f
            var bottom = 0f
            var left = 0f
            var right = 0f
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "w:pgSz" -> if (width == 0) {
                            width = parser.getAttributeValue(null, "w:w")?.toIntOrNull() ?: 0
                            height = parser.getAttributeValue(null, "w:h")?.toIntOrNull() ?: 0
                        }
                        "w:pgMar" -> {
                            top = parser.getAttributeValue(null, "w:top")?.toFloatOrNull() ?: 0f
                            bottom = parser.getAttributeValue(null, "w:bottom")?.toFloatOrNull() ?: 0f
                            left = parser.getAttributeValue(null, "w:left")?.toFloatOrNull() ?: 0f
                            right = parser.getAttributeValue(null, "w:right")?.toFloatOrNull() ?: 0f
                        }
                    }
                }
                event = parser.next()
            }
            if (width <= 0 || height <= 0) return@runCatching defaultSection()
            Section(
                width = (width / 20f).toInt().coerceIn(200, 2000),
                height = (height / 20f).toInt().coerceIn(200, 3000),
                left = twipsToPoints(left, 72f),
                top = twipsToPoints(top, 72f),
                right = twipsToPoints(right, 72f),
                bottom = twipsToPoints(bottom, 72f)
            )
        }.getOrDefault(defaultSection())
    }

    private fun twipsToPoints(twips: Float, fallback: Float): Float =
        if (twips <= 0f) fallback else (twips / 20f).coerceIn(18f, 200f)

    private fun defaultSection(): Section =
        Section(width = 595, height = 842, left = 72f, top = 72f, right = 72f, bottom = 72f)

    private fun readBounded(input: InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArrayOutputStream(64 * 1024)
        val chunk = ByteArray(16 * 1024)
        var total = 0
        while (total < maxBytes) {
            val read = input.read(chunk)
            if (read == -1) break
            val toWrite = min(read, maxBytes - total)
            buffer.write(chunk, 0, toWrite)
            total += toWrite
        }
        return buffer.toByteArray()
    }
}

data class DocxRenderResult(
    val doc: PdfDocument,
    val outlines: List<OutlineItem>
)
