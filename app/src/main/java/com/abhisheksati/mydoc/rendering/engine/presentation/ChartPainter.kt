package com.abhisheksati.mydoc.rendering.engine.presentation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws a [ChartModel.Chart] into the rectangle the author gave the chart, on the platform canvas.
 *
 * The renderer already draws shapes, text and tables onto a canvas that is a PDF page just as
 * readily as it is a screen, so this one painter serves the on-screen slides and the exported PDF.
 *
 * Colours come from a fixed sequence rather than the chart's own theme: the accent chain is not
 * resolved here, and a chart whose bars are the wrong blue still says the thing it has to say,
 * while one with the wrong numbers does not.
 */
internal object ChartPainter {

    private val Palette = intArrayOf(
        Color.rgb(0x4E, 0x79, 0xC7),
        Color.rgb(0xED, 0x7D, 0x31),
        Color.rgb(0xA5, 0xC6, 0x54),
        Color.rgb(0xFF, 0xC0, 0x00),
        Color.rgb(0x5B, 0x9B, 0xD5),
        Color.rgb(0x70, 0xAD, 0x47)
    )

    fun paint(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, chart: ChartModel.Chart) {
        val width = right - left
        val height = bottom - top
        if (chart.series.isEmpty() || width < 24f || height < 24f) return

        val labelSize = min(height / 14f, 13f).coerceAtLeast(7f)
        val titleSize = labelSize * 1.25f
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40); textSize = labelSize
        }
        val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(150, 150, 150); strokeWidth = 1f; style = Paint.Style.STROKE
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        var plotTop = top + labelSize * 1.4f
        chart.title?.takeIf { it.isNotEmpty() }?.let { title ->
            text.textSize = titleSize
            canvas.drawText(title, left, top + titleSize, text)
            text.textSize = labelSize
            plotTop += titleSize * 1.3f
        }

        // A pie's slices mean nothing without their category names, and a multi-series cartesian
        // chart needs its series colours, so both get the same right-hand key.
        val legendNames = when {
            chart.kind == ChartModel.Kind.PIE -> chart.categories
            chart.series.size > 1 -> chart.series.map { it.name }
            else -> emptyList()
        }
        val legendWidth = if (legendNames.isNotEmpty()) width * 0.28f else 0f
        val plotLeft = left + labelSize * 3.4f
        val plotRight = right - legendWidth
        val plotBottom = bottom - labelSize * (if (chart.kind == ChartModel.Kind.PIE) 1.2f else 2.2f)
        if (plotRight - plotLeft < 12f || plotBottom - plotTop < 12f) return

        if (chart.kind == ChartModel.Kind.PIE) {
            paintPie(canvas, plotLeft, plotTop, plotRight, plotBottom, chart, text, fill)
        } else {
            paintCartesian(canvas, plotLeft, plotTop, plotRight, plotBottom, chart, text, fill, axis)
        }
        if (legendNames.isNotEmpty()) {
            paintLegend(canvas, legendNames, plotRight + labelSize * 0.4f, plotTop + labelSize, bottom, labelSize, text, fill)
        }
    }

    /** One colour swatch and its name per entry, top-down; the sequence is the painter's own. */
    private fun paintLegend(
        canvas: Canvas, names: List<String>, left: Float, top: Float, limit: Float,
        labelSize: Float, text: Paint, fill: Paint
    ) {
        var y = top
        text.color = Color.rgb(40, 40, 40)
        text.textSize = labelSize * 0.85f
        names.forEachIndexed { index, name ->
            if (name.isEmpty() || y > limit) return@forEachIndexed
            fill.color = Palette[index % Palette.size]
            canvas.drawRect(left, y - labelSize * 0.75f, left + labelSize * 0.8f, y, fill)
            canvas.drawText(name, left + labelSize * 1.1f, y, text)
            y += labelSize * 1.55f
        }
        text.textSize = labelSize
    }

    private fun paintCartesian(
        canvas: Canvas, plotLeft: Float, plotTop: Float, plotRight: Float, plotBottom: Float,
        chart: ChartModel.Chart, text: Paint, fill: Paint, axis: Paint
    ) {
        val hi = chart.maxValue
        val lo = chart.minValue
        val span = (hi - lo).takeIf { it != 0.0 } ?: 1.0
        fun yOf(value: Double): Float =
            plotBottom - ((value - lo) / span * (plotBottom - plotTop)).toFloat()

        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axis)
        canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, axis)
        if (lo < 0.0) canvas.drawLine(plotLeft, yOf(0.0), plotRight, yOf(0.0), axis)

        // Five gridlines with their values: enough for a reader to judge magnitude without turning
        // the picture into a spreadsheet.
        text.color = Color.rgb(90, 90, 90)
        for (step in 0..4) {
            val value = lo + span * step / 4.0
            val y = yOf(value)
            canvas.drawLine(plotLeft - 3f, y, plotLeft, y, axis)
            val label = shortNumber(value)
            canvas.drawText(label, plotLeft - 6f - text.measureText(label), y + text.textSize / 3f, text)
        }

        val points = chart.pointCount
        val groupWidth = (plotRight - plotLeft) / points.toFloat()
        if (chart.kind == ChartModel.Kind.BAR) {
            val clustered = chart.grouping == ChartModel.Grouping.CLUSTERED
            val slot = if (clustered) groupWidth * 0.72f / chart.series.size else groupWidth * 0.6f
            for (i in 0 until points) {
                for (s in chart.series.indices) {
                    val (from, to) = chart.extents(s, i)
                    if (from == to) continue   // a zero segment is no ink, in any grouping
                    fill.color = Palette[s % Palette.size]
                    val barLeft = if (clustered) {
                        plotLeft + groupWidth * i + groupWidth * 0.14f + slot * s
                    } else {
                        plotLeft + groupWidth * (i + 0.2f)
                    }
                    val y1 = yOf(from)
                    val y2 = yOf(to)
                    canvas.drawRect(barLeft, min(y1, y2), barLeft + slot * 0.94f, max(y1, y2), fill)
                }
            }
        } else {
            chart.series.forEachIndexed { s, series ->
                fill.color = Palette[s % Palette.size]
                fill.style = Paint.Style.STROKE
                fill.strokeWidth = min(groupWidth * 0.06f, 4f).coerceAtLeast(1.5f)
                var previousX = 0f
                var previousY = 0f
                for (i in 0 until points) {
                    val x = plotLeft + groupWidth * (i + 0.5f)
                    val y = yOf(series.values.getOrElse(i) { 0.0 })
                    if (i > 0) canvas.drawLine(previousX, previousY, x, y, fill)
                    previousX = x; previousY = y
                }
                fill.style = Paint.Style.FILL
                for (i in 0 until points) {
                    val x = plotLeft + groupWidth * (i + 0.5f)
                    val y = yOf(series.values.getOrElse(i) { 0.0 })
                    canvas.drawCircle(x, y, fill.strokeWidth, fill)
                }
            }
        }

        text.color = Color.rgb(90, 90, 90)
        for (i in 0 until points) {
            val label = chart.categories.getOrElse(i) { "" }
            if (label.isEmpty()) continue
            val x = plotLeft + groupWidth * (i + 0.5f)
            canvas.drawText(label, x - text.measureText(label) / 2f, plotBottom + text.textSize * 1.4f, text)
        }
    }

    private fun paintPie(
        canvas: Canvas, plotLeft: Float, plotTop: Float, plotRight: Float, plotBottom: Float,
        chart: ChartModel.Chart, text: Paint, fill: Paint
    ) {
        val values = chart.series.first().values
        val total = values.sum()
        if (total <= 0.0) return
        val radius = min(plotRight - plotLeft, plotBottom - plotTop) / 2f
        val cx = plotLeft + (plotRight - plotLeft) / 2f
        val cy = plotTop + (plotBottom - plotTop) / 2f
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        var start = -90f
        text.color = Color.rgb(40, 40, 40)
        values.forEachIndexed { index, value ->
            val sweep = (value / total * 360.0).toFloat()
            fill.color = Palette[index % Palette.size]
            canvas.drawArc(oval, start, sweep, true, fill)
            val mid = Math.toRadians(start + sweep / 2.0)
            val label = "%d%%".format((value / total * 100).roundToInt())
            canvas.drawText(
                label,
                (cx + cos(mid) * radius * 0.62f - text.measureText(label) / 2f).toFloat(),
                (cy + sin(mid) * radius * 0.62f).toFloat(),
                text
            )
            start += sweep
        }
    }

    private fun shortNumber(value: Double): String = when {
        value == value.roundToInt().toDouble() && abs(value) < 1e9 -> value.roundToInt().toString()
        abs(value) >= 1e6 -> "%.1fM".format(value / 1e6)
        abs(value) >= 1e3 -> "%.1fk".format(value / 1e3)
        else -> "%.1f".format(value)
    }
}
