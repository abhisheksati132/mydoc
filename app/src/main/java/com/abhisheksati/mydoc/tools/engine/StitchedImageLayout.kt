package com.abhisheksati.mydoc.tools.engine

internal data class StitchedImageLayout(val width: Int, val height: Int, val scale: Float)

/** One geometry contract for allocation, rasterization and placement of every stitched page. */
internal fun stitchedImageLayout(widthPt: Float, heightPt: Float, initialScale: Float,
    minimumScale: Float, pixelBudget: Long): StitchedImageLayout {
    require(widthPt.isFinite() && heightPt.isFinite() && widthPt > 0 && heightPt > 0)
    require(initialScale.isFinite() && initialScale > 0 && minimumScale > 0 && pixelBudget > 0)
    var scale = initialScale
    while (scale > minimumScale &&
        (widthPt * scale).toLong() * (heightPt * scale).toLong() > pixelBudget) scale *= 0.75f
    var width = (widthPt * scale).toInt().coerceAtLeast(1)
    var height = (heightPt * scale).toInt().coerceAtLeast(1)
    val pixels = width.toLong() * height
    if (pixels > pixelBudget) {
        val factor = kotlin.math.sqrt(pixelBudget.toDouble() / pixels)
        width = (width * factor).toInt().coerceAtLeast(1)
        height = (height * factor).toInt().coerceAtLeast(1)
        scale *= factor.toFloat()
    }
    // Also handle extreme aspect ratios where clamping the narrow axis to one pixel uses budget.
    width = width.coerceAtMost(pixelBudget.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    height = height.coerceAtMost((pixelBudget / width).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    scale = minOf(scale, width / widthPt, height / heightPt)
    return StitchedImageLayout(width, height, scale)
}
