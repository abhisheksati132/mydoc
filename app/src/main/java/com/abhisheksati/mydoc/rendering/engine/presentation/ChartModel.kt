package com.abhisheksati.mydoc.rendering.engine.presentation

/**
 * The data an OOXML chart part carries.
 *
 * A chart part keeps its own cached values (`c:numCache` / `c:strCache`), so a viewer can draw the
 * chart without opening the workbook it was built from and without recalculating anything - the
 * numbers are the ones Excel wrote when it last calculated.
 *
 * Only what MyDoc can lay out honestly is modelled: vertical bars in the three groupings, lines that
 * are not stacked and pie. A combo plot area, a horizontal bar, a stacked line and any other kind
 * return null, and the frame stays empty rather than showing a picture of something else - a chart
 * is data, and a wrong chart is worse than no chart.
 */
internal object ChartModel {

    enum class Kind { BAR, LINE, PIE }

    /** How the series share the space of one category. */
    enum class Grouping { CLUSTERED, STACKED, PERCENT }

    data class Series(val name: String, val values: List<Double>)

    data class Chart(
        val kind: Kind,
        val title: String?,
        val categories: List<String>,
        val series: List<Series>,
        val grouping: Grouping = Grouping.CLUSTERED
    ) {
        val pointCount: Int get() = series.maxOfOrNull { it.values.size } ?: 0

        /**
         * What the value axis has to span. Standing beside each other, the tallest single value
         * decides; stacked, it is the tallest column of summed values; percentage-stacked, the
         * column always runs from 0 to 100 of its own total.
         */
        val maxValue: Double get() = when (grouping) {
            Grouping.PERCENT -> 100.0
            Grouping.CLUSTERED -> series.flatMap { it.values }.maxOrNull() ?: 0.0
            Grouping.STACKED -> (0 until pointCount).maxOfOrNull { i ->
                series.sumOf { it.values.getOrElse(i) { 0.0 }.coerceAtLeast(0.0) }
            } ?: 0.0
        }.coerceAtLeast(0.0)

        val minValue: Double get() = when (grouping) {
            Grouping.PERCENT -> 0.0
            Grouping.CLUSTERED -> (series.flatMap { it.values }.minOrNull() ?: 0.0).coerceAtMost(0.0)
            Grouping.STACKED -> (0 until pointCount).minOfOrNull { i ->
                series.sumOf { it.values.getOrElse(i) { 0.0 }.coerceAtMost(0.0) }
            }?.coerceAtMost(0.0) ?: 0.0
        }

        /** The value one segment is drawn for: its own, its stacked position, or its share of the column. */
        fun extents(seriesIndex: Int, point: Int): Pair<Double, Double> {
            val value = series[seriesIndex].values.getOrElse(point) { 0.0 }
            return when (grouping) {
                Grouping.CLUSTERED -> 0.0 to value
                Grouping.STACKED -> {
                    var below = 0.0
                    for (s in 0 until seriesIndex) below += series[s].values.getOrElse(point) { 0.0 }
                    below to below + value
                }
                Grouping.PERCENT -> {
                    val total = (0 until series.size).sumOf {
                        series[it].values.getOrElse(point) { 0.0 }.let { v -> if (v > 0) v else 0.0 }
                    }
                    if (total <= 0.0) return 0.0 to 0.0
                    var below = 0.0
                    for (s in 0 until seriesIndex) below +=
                        series[s].values.getOrElse(point) { 0.0 } / total * 100.0
                    below to below + value / total * 100.0
                }
            }
        }
    }

    private const val MaxPoints = 1024

    fun parse(chartSpace: OoxmlNode?): Chart? {
        val chart = chartSpace?.child("c:chart") ?: return null
        val plotArea = chart.child("c:plotArea") ?: return null
        // A plot area holds one `c:*Chart` group per thing it draws, so a combo chart has two or
        // more. Drawing only the bars of a bar-and-line chart would leave out half of the data, and
        // half a chart is the wrong chart, so anything but a single group is refused.
        val groups = plotArea.children.filter { it.name.endsWith("Chart") }
        if (groups.size != 1) return null
        val group = groups.first()
        val kind = when (group.name) {
            "c:barChart" -> Kind.BAR
            "c:lineChart" -> Kind.LINE
            // A 3-D pie is the same data with a tilt; the tilt is not reproduced, the slices are.
            "c:pieChart", "c:pie3DChart" -> Kind.PIE
            else -> return null
        }
        if (kind == Kind.LINE && group.child("c:grouping")?.attr("val") !in lineGroupings) return null
        // A horizontal bar chart needs its own axis layout; drawing it vertically would show the
        // author a different chart than they made.
        if (kind == Kind.BAR && group.child("c:barDir")?.attr("val") == "bar") return null
        val grouping = when (group.child("c:grouping")?.attr("val")) {
            null, "clustered", "standard" -> Grouping.CLUSTERED
            "stacked" -> Grouping.STACKED
            "percentStacked" -> Grouping.PERCENT
            else -> return null
        }

        val seriesList = group.childrenNamed("c:ser").mapNotNull { ser ->
            val values = numbers(ser.child("c:val")) ?: return@mapNotNull null
            if (values.isEmpty()) return@mapNotNull null
            Series(name = seriesName(ser), values = values)
        }
        if (seriesList.isEmpty()) return null

        val categories = strings(group.childrenNamed("c:ser").first().child("c:cat"))
            ?: List(seriesList.maxOf { it.values.size }) { "" }

        return Chart(kind, title(chart), categories, seriesList, grouping)
    }

    /**
     * A line chart drawn one line per series is the author's chart only while the series are not
     * stacked into each other; stacking lines is a different picture from stacking bars, so a
     * stacked line group is refused rather than flattened into something else.
     */
    private val lineGroupings = setOf(null, "standard", "clustered")

    private fun title(chart: OoxmlNode): String? =
        chart.child("c:title")?.textContent()?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * A cache can be sparse: each `c:pt` carries its own index and a hole is an empty cell, not a
     * zero, so the points are placed by index against `c:ptCount`.
     *
     * The cache is looked for by descent rather than as a direct child, because the reference wraps
     * it: `c:val` holds `c:numRef`, which holds `c:numCache` alongside the `c:f` formula string.
     */
    private fun numbers(ref: OoxmlNode?): List<Double>? {
        val cache = ref?.find("c:numCache") ?: ref?.find("c:strCache") ?: return null
        val points = cache.childrenNamed("c:pt")
        if (points.isEmpty()) return null
        val out = MutableList(minOf(size(cache, points), MaxPoints)) { 0.0 }
        points.forEachIndexed { fallback, pt ->
            val index = pt.attr("idx")?.toIntOrNull() ?: fallback
            if (index in out.indices) out[index] = pt.child("c:v")?.textContent()?.trim()?.toDoubleOrNull() ?: 0.0
        }
        return out
    }

    private fun strings(ref: OoxmlNode?): List<String>? {
        val cache = ref?.find("c:strCache") ?: ref?.find("c:multiLvlStrCache") ?: ref?.find("c:numCache")
            ?: return null
        val points = cache.childrenNamed("c:pt")
        if (points.isEmpty()) return null
        val out = MutableList(minOf(size(cache, points), MaxPoints)) { "" }
        points.forEachIndexed { fallback, pt ->
            val index = pt.attr("idx")?.toIntOrNull() ?: fallback
            if (index in out.indices) out[index] = value(pt)
        }
        return out
    }

    /** A multi-level category cache writes the string into `c:lvl` instead of `c:v`. */
    private fun value(pt: OoxmlNode): String =
        pt.child("c:v")?.textContent()?.trim().orEmpty()
            .ifEmpty { pt.childrenNamed("c:lvl").joinToString(" / ") { it.textContent().trim() } }

    private fun size(cache: OoxmlNode, points: List<OoxmlNode>): Int =
        maxOf(cache.child("c:ptCount")?.attr("val")?.toIntOrNull() ?: 0, points.size)

    private fun seriesName(ser: OoxmlNode): String =
        strings(ser.child("c:tx"))?.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "Series"
}
