package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Shape
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Continuous-curvature ("squircle") corner shape: flat edges joined by corners
 * whose curvature ramps in instead of switching on at a circular arc boundary.
 * This is the corner language iOS glass and icons use; circular RoundedCornerShape
 * reads as a sticker by comparison.
 */
class ContinuousCornerShape(
    private val radius: Dp,
    private val exponent: Float = 4.5f
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { min(radius.toPx(), min(size.width, size.height) / 2f) }
        if (r <= 0.5f) return Outline.Rectangle(Rect(Offset.Zero, size))
        val k = 2f / exponent
        val steps = 16
        val half = (Math.PI / 2).toFloat() / (steps * 2f)
        val path = Path()
        path.moveTo(r, 0f)
        path.lineTo(size.width - r, 0f)
        // Corners are sampled half a step inside each tangent junction so no
        // vertex is duplicate or exactly collinear: Skia rejects such paths as
        // non-convex, which empties the outline and clips the whole surface.
        // Top-right
        for (i in 1..steps) {
            val t = (2f * i - 1f) * half
            path.lineTo(
                size.width - r + r * sin(t).pow(k),
                r - r * cos(t).pow(k)
            )
        }
        path.lineTo(size.width, size.height - r)
        // Bottom-right
        for (i in 1..steps) {
            val t = (2f * i - 1f) * half
            path.lineTo(
                size.width - r + r * cos(t).pow(k),
                size.height - r + r * sin(t).pow(k)
            )
        }
        path.lineTo(r, size.height)
        // Bottom-left
        for (i in 1..steps) {
            val t = (2f * i - 1f) * half
            path.lineTo(
                r - r * sin(t).pow(k),
                size.height - r + r * cos(t).pow(k)
            )
        }
        path.lineTo(0f, r)
        // Top-left
        for (i in 1..steps) {
            val t = (2f * i - 1f) * half
            path.lineTo(
                r - r * cos(t).pow(k),
                r - r * sin(t).pow(k)
            )
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is ContinuousCornerShape && other.radius == radius && other.exponent == exponent

    override fun hashCode(): Int = 31 * radius.hashCode() + exponent.hashCode()
}
