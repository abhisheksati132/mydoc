package com.abhisheksati.mydoc.rendering.engine.word

import android.graphics.RectF
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
 * reproduced, along with embedded raster pictures at their text anchors. Floating wrapping and
 * character-level styles from styles.xml are not reproduced.
 */
internal object DocxPdfRenderer {

    private const val BodySize = 11f
    private const val TableSize = 9f
    private const val Leading = 1.3f
    private const val ParagraphGap = 7f
    private const val ListIndent = 16f
    private const val CellPad = 4f
    private const val MaxPages = 4000

    /** Links are drawn in a fixed blue because the converted page is always white paper. */
    private val LinkColor = 0xFF1A5FB4.toInt()

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
        return writer.finish()?.let { DocxRenderResult(it, outlines, writer.droppedTableColumns) }
    }

    private class PageWriter(private val section: Section) {

        private val doc = PdfDocument()
        private val outlines = mutableListOf<OutlineItem>()

        /** Columns beyond the readable grid, counted so the viewer can say the table was cut. */
        var droppedTableColumns = 0
        private val paints = HashMap<Long, TextPaint>()
        private var canvas: Canvas? = null
        private var currentPage: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = section.top
        private var pendingAfter = 0f

        fun write(blocks: List<DocxBlock>): List<OutlineItem> {
            for (block in blocks) {
                if (pageNumber >= MaxPages) break
                val layout = when (block) {
                    is DocxBlock.Image -> block.layout
                    is DocxBlock.Paragraph -> block.layout
                    is DocxBlock.Heading -> block.layout
                    is DocxBlock.ListItem -> block.layout
                    else -> null
                }
                if (layout != null) {
                    if (layout.pageBreakBefore == true && canvas != null && y > section.top) {
                        startPage()
                        pendingAfter = 0f
                    }
                    if (canvas == null) startPage()
                    y += max(pendingAfter, layout.before ?: 0f)
                    pendingAfter = 0f
                } else if (block != DocxBlock.PageBreak) {
                    y += pendingAfter
                    pendingAfter = 0f
                }
                when (block) {
                    DocxBlock.PageBreak -> {
                        if (canvas == null) startPage()
                        startPage()
                        pendingAfter = 0f
                    }
                    is DocxBlock.Image -> image(block)
                    is DocxBlock.Heading -> heading(block)
                    is DocxBlock.Paragraph -> text(block.runs, BodySize, 0f, block.layout)
                    is DocxBlock.ListItem -> list(block)
                    is DocxBlock.Table -> table(block.rows)
                }
                if (layout != null) pendingAfter = layout.after ?: when (block) {
                    is DocxBlock.Heading -> ParagraphGap * 0.6f
                    is DocxBlock.ListItem -> ParagraphGap * 0.5f
                    else -> ParagraphGap
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

        private fun image(block: DocxBlock.Image) {
            val bitmap = DocxImageDecoder.decode(block.bytes)
            if (bitmap == null) {
                text(listOf(DocxRun("[Image unavailable] " + block.description)), BodySize, 0f, block.layout)
                return
            }
            try {
                val nativeWidth = block.widthPoints.takeIf { it > 0 } ?: (bitmap.width * 0.75f)
                val nativeHeight = block.heightPoints.takeIf { it > 0 } ?: (nativeWidth * bitmap.height / bitmap.width)
                val (left, availableWidth) = paragraphRegion(block.layout)
                val scale = min(1f, min(availableWidth / nativeWidth,
                    (section.height - section.top - section.bottom - ParagraphGap) / nativeHeight))
                val width = nativeWidth * scale
                val height = nativeHeight * scale
                ensureRoom(height + ParagraphGap)
                val x = left + when (block.layout.alignment) {
                    DocxAlignment.CENTER -> (availableWidth - width) / 2f
                    DocxAlignment.RIGHT, DocxAlignment.END -> availableWidth - width
                    else -> 0f
                }
                canvas?.drawBitmap(bitmap, null, RectF(x, y, x + width, y + height),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                y += height
            } finally { bitmap.recycle() }
        }

        private fun heading(block: DocxBlock.Heading) {
            val size = headingSize(block.level)
            val step = size * Leading
            // Keep the heading on the page that carries at least one line under it.
            ensureRoom(step * if (block.isContinuation) 1f else 2f)
            val (left, width) = paragraphRegion(block.layout)
            val lines = wrap(listOf(DocxRun(block.text)), size, width, forceBold = true)
            for (line in lines) {
                ensureRoom(step)
                drawAlignedLine(line, left, width, block.layout.alignment)
                y += step
            }
            if (!block.isContinuation)
                outlines.add(OutlineItem(title = block.text, pageIndex = pageNumber - 1, level = block.level - 1))
        }

        /**
         * The marker comes from the document's own numbering definition, so an ordered list is drawn
         * "1." rather than as a bullet, and a nested level is indented by that level.
         */
        private fun list(block: DocxBlock.ListItem) {
            val step = BodySize * Leading
            val markerPaint = paint(BodySize, false, false, false)
            val (left, width) = paragraphRegion(block.layout)
            val indent = min(ListIndent * (1 + block.level), (width - 24f).coerceAtLeast(0f))
            val lines = wrap(block.runs, BodySize, (width - indent).coerceAtLeast(24f), forceBold = false)
            for ((index, line) in lines.withIndex()) {
                ensureRoom(step)
                val baseline = y + line.maxOf { -it.paint.fontMetrics.ascent }
                if (index == 0) canvas?.drawText(block.marker, left + 2f, baseline, markerPaint)
                drawAlignedLine(line, left + indent, (width - indent).coerceAtLeast(24f), block.layout.alignment, baseline)
                y += step
            }
        }

        private fun text(runs: List<DocxRun>, size: Float, indent: Float, layout: DocxParagraphLayout = DocxParagraphLayout()) {
            val step = size * Leading
            val (left, width) = paragraphRegion(layout)
            for (line in wrap(runs, size, (width - indent).coerceAtLeast(24f), forceBold = false)) {
                ensureRoom(step)
                drawAlignedLine(line, left + indent, (width - indent).coerceAtLeast(24f), layout.alignment)
                y += step
            }
        }

        private fun paragraphRegion(layout: DocxParagraphLayout): Pair<Float, Float> {
            val left = (layout.left ?: 0f).coerceIn(-section.left, section.contentWidth - 24f)
            val right = (layout.right ?: 0f).coerceIn(-section.right, section.contentWidth - left - 24f)
            return section.left + left to (section.contentWidth - left - right).coerceAtLeast(24f)
        }

        private fun drawAlignedLine(line: List<Piece>, left: Float, width: Float,
            alignment: DocxAlignment?, baseline: Float? = null) {
            val spare = (width - line.sumOf { it.width.toDouble() }.toFloat()).coerceAtLeast(0f)
            val offset = when (alignment) {
                DocxAlignment.CENTER -> spare / 2f
                DocxAlignment.RIGHT, DocxAlignment.END -> spare
                else -> 0f
            }
            if (baseline == null) drawLine(line, left + offset) else drawLine(line, left + offset, baseline)
        }

        private fun table(rows: List<List<DocxCell>>) {
            val widest = rows.maxOf { it.size }
            if (widest > 24) droppedTableColumns += widest - 24
            val columns = widest.coerceIn(1, 24)
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
                    val text = row.getOrNull(c)?.text ?: continue
                    for (word in text.split(' ')) longest = max(longest, cell.measureText(word))
                }
                (longest + 2 * CellPad).coerceIn(34f, section.contentWidth * 0.6f)
            }
            val total = wanted.sum().coerceAtLeast(1f)
            val widths = wanted.map { it / total * section.contentWidth }

            y += ParagraphGap * 0.4f
            for ((rowIndex, row) in rows.withIndex()) {
                val lines = (0 until columns).map { c ->
                    wrap(listOf(DocxRun(row.getOrNull(c)?.text ?: "")), TableSize, widths[c] - 2f * CellPad, forceBold = rowIndex == 0)
                }
                val rowHeight = max(step * lines.maxOf { it.size } + 2 * CellPad, step + 2 * CellPad)
                val pageHeight = section.height - section.bottom - section.top
                if (rowHeight <= pageHeight) {
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
                } else {
                    // Taller than a page: draw the cell text in slices, continuing the same row on
                    // the next page instead of letting it run off the bottom edge unread.
                    val tallest = lines.maxOf { it.size }
                    var lineOffset = 0
                    // A wide table starting near the foot of a page would otherwise be sliced into
                    // slivers; begin it on a page with room for at least half a page of it.
                    if (canvas == null || (section.height - section.bottom - y) < pageHeight * 0.5f) startPage()
                    while (lineOffset < tallest) {
                        if (canvas == null) startPage()
                        val available = (section.height - section.bottom - y).coerceAtLeast(step + 2 * CellPad)
                        val fitting = ((available - 2 * CellPad) / step).toInt().coerceAtLeast(1)
                        val count = minOf(fitting, tallest - lineOffset)
                        val sliceHeight = step * count + 2 * CellPad
                        val surface = canvas ?: return
                        val top = y
                        if (rowIndex == 0 && lineOffset == 0) {
                            surface.drawRect(section.left, top, section.left + section.contentWidth, top + sliceHeight, headerFill)
                        }
                        var x = section.left
                        for (c in 0 until columns) {
                            var lineY = top + CellPad
                            val end = minOf(lineOffset + count, lines[c].size)
                            for (i in lineOffset until end) {
                                val line = lines[c][i]
                                drawLine(line, x + CellPad, lineY + line.maxOf { -it.paint.fontMetrics.ascent })
                                lineY += step
                            }
                            surface.drawRect(x, top, x + widths[c], top + sliceHeight, grid)
                            x += widths[c]
                        }
                        y = top + sliceHeight
                        lineOffset += count
                        if (lineOffset < tallest) startPage()
                    }
                }
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
            if (runs.any { '\n' in it.text }) {
                val paragraphs = mutableListOf<MutableList<DocxRun>>(mutableListOf())
                for (run in runs) {
                    val parts = run.text.split('\n')
                    parts.forEachIndexed { index, part ->
                        if (index > 0) paragraphs.add(mutableListOf())
                        paragraphs.last().add(run.copy(text = part))
                    }
                }
                return paragraphs.flatMap { wrap(it, size, maxWidth, forceBold) }
            }
            val limit = max(maxWidth, 24f)
            val lines = mutableListOf<MutableList<Piece>>()
            var current = mutableListOf<Piece>()
            var width = 0f
            var spaceBefore = false

            for (run in runs) {
                val bold = forceBold || run.isBold
                val isLink = run.linkTarget != null
                val piecePaint = paint(size, bold, run.isItalic, run.isUnderline || isLink, isLink)
                // A space is only inserted where the document wrote one. Runs are adjacent fragments
                // of a sentence - "endnote", "[1]", "." - and joining them with a space of our own
                // puts a gap in front of the punctuation.
                val tokens = run.text.split(' ')
                for ((index, token) in tokens.withIndex()) {
                    if (index > 0) spaceBefore = true
                    if (token.isEmpty()) continue
                    if (current.isNotEmpty() && spaceBefore && width + piecePaint.measureText(" $token") > limit) {
                        lines.add(current)
                        current = mutableListOf()
                        width = 0f
                    }
                    val word = if (current.isEmpty() || !spaceBefore) token else " $token"
                    val measured = piecePaint.measureText(word)
                    if (measured > limit && current.isEmpty()) {
                        // A token wider than the line - a URL, an identifier, or a CJK run with no
                        // spaces - used to be trimmed to a prefix with the remainder discarded while
                        // the conversion still reported success. It is broken across lines instead.
                        var rest = word
                        while (rest.isNotEmpty()) {
                            val fits = prefixThatFits(rest, piecePaint, limit)
                            val take = if (fits.isEmpty()) rest.substring(0, 1) else fits
                            val taken = piecePaint.measureText(take)
                            if (take.length == rest.length) {
                                current.add(Piece(take, piecePaint, taken))
                                width = taken
                            } else {
                                lines.add(mutableListOf(Piece(take, piecePaint, taken)))
                                rest = rest.substring(take.length)
                                continue
                            }
                            rest = ""
                        }
                        spaceBefore = false
                        continue
                    }
                    current.add(Piece(word, piecePaint, measured))
                    width += measured
                    spaceBefore = false
                }
            }
            if (current.isNotEmpty()) lines.add(current)
            if (lines.isEmpty()) lines.add(mutableListOf(Piece("", paint(size, false, false, false), 0f)))
            return lines
        }

        /** Longest prefix of [text] that fits in [limit], never splitting a surrogate pair. */
        private fun prefixThatFits(text: String, paint: TextPaint, limit: Float): String {
            val count = paint.breakText(text, true, limit, null)
            if (count <= 0) return ""
            var end = count.coerceAtMost(text.length)
            if (end < text.length &&
                Character.isHighSurrogate(text[end - 1]) &&
                Character.isLowSurrogate(text[end])
            ) {
                end -= 1
            }
            return if (end <= 0) "" else text.substring(0, end)
        }

        private fun paint(
            size: Float,
            bold: Boolean,
            italic: Boolean,
            underline: Boolean,
            link: Boolean = false
        ): TextPaint {
            val key = (size.toLong() and 0xFFFFL) or
                ((if (bold) 1L else 0L) shl 16) or
                ((if (italic) 1L else 0L) shl 17) or
                ((if (underline) 1L else 0L) shl 18) or
                ((if (link) 1L else 0L) shl 19)
            return paints.getOrPut(key) {
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    // The page is always white paper here, so the link colour is fixed rather than
                    // themed; the reflow viewer is the surface that follows the app's palette.
                    color = if (link) LinkColor else Color.BLACK
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
    val outlines: List<OutlineItem>,
    /** Table columns past the readable limit, so a cut table is not presented as complete. */
    val droppedTableColumns: Int = 0
)
