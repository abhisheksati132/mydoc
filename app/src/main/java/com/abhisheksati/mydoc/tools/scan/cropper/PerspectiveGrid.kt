package com.abhisheksati.mydoc.tools.scan.cropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs

fun DrawScope.drawPerspectiveGrid(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size != 4) return

    val transform = PerspectiveQuadTransform.from(points)

    fun pointAt(hFraction: Float, vFraction: Float): Offset {
        return transform?.map(hFraction, vFraction)
            ?: points.bilinearPoint(hFraction, vFraction)
    }

    for (index in 1 until 3) {
        val fraction = index.toFloat() / 3f

        drawLine(
            color = color,
            start = pointAt(fraction, 0f),
            end = pointAt(fraction, 1f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = pointAt(0f, fraction),
            end = pointAt(1f, fraction),
            strokeWidth = strokeWidth
        )
    }
}

private data class PerspectiveQuadTransform(
    val horizontalX: Float,
    val verticalX: Float,
    val originX: Float,
    val horizontalY: Float,
    val verticalY: Float,
    val originY: Float,
    val horizontalPerspective: Float,
    val verticalPerspective: Float,
    val fallbackPoints: List<Offset>
) {
    fun map(horizontalFraction: Float, verticalFraction: Float): Offset {
        val denominator = horizontalPerspective * horizontalFraction +
            verticalPerspective * verticalFraction + 1f
        if (abs(denominator) <= 0.0001f) {
            return fallbackPoints.bilinearPoint(horizontalFraction, verticalFraction)
        }

        val point = Offset(
            x = (horizontalX * horizontalFraction + verticalX * verticalFraction + originX) / denominator,
            y = (horizontalY * horizontalFraction + verticalY * verticalFraction + originY) / denominator
        )
        return if (point.x.isFinite() && point.y.isFinite()) point else fallbackPoints.bilinearPoint(horizontalFraction, verticalFraction)
    }

    companion object {
        fun from(points: List<Offset>): PerspectiveQuadTransform? {
            val topLeft = points[0]
            val topRight = points[1]
            val bottomRight = points[2]
            val bottomLeft = points[3]

            val rightEdgeX = topRight.x - bottomRight.x
            val rightEdgeY = topRight.y - bottomRight.y
            val bottomEdgeX = bottomLeft.x - bottomRight.x
            val bottomEdgeY = bottomLeft.y - bottomRight.y
            val perspectiveX = topLeft.x - topRight.x + bottomRight.x - bottomLeft.x
            val perspectiveY = topLeft.y - topRight.y + bottomRight.y - bottomLeft.y
            val determinant = rightEdgeX * bottomEdgeY - bottomEdgeX * rightEdgeY

            if (abs(determinant) <= 0.0001f) return null

            val horizontalPerspective = (perspectiveX * bottomEdgeY - bottomEdgeX * perspectiveY) / determinant
            val verticalPerspective = (rightEdgeX * perspectiveY - perspectiveX * rightEdgeY) / determinant
            val topRightDenominator = 1f + horizontalPerspective
            val bottomLeftDenominator = 1f + verticalPerspective
            val bottomRightDenominator = 1f + horizontalPerspective + verticalPerspective
            val minDenominator = minOf(topRightDenominator, bottomLeftDenominator, bottomRightDenominator)

            if (minDenominator <= 0.0001f) return null

            return PerspectiveQuadTransform(
                horizontalX = topRight.x - topLeft.x + horizontalPerspective * topRight.x,
                verticalX = bottomLeft.x - topLeft.x + verticalPerspective * bottomLeft.x,
                originX = topLeft.x,
                horizontalY = topRight.y - topLeft.y + horizontalPerspective * topRight.y,
                verticalY = bottomLeft.y - topLeft.y + verticalPerspective * bottomLeft.y,
                originY = topLeft.y,
                horizontalPerspective = horizontalPerspective,
                verticalPerspective = verticalPerspective,
                fallbackPoints = points
            )
        }
    }
}

private fun List<Offset>.bilinearPoint(
    horizontalFraction: Float,
    verticalFraction: Float
): Offset {
    val top = this[0] + (this[1] - this[0]) * horizontalFraction
    val bottom = this[3] + (this[2] - this[3]) * horizontalFraction
    return top + (bottom - top) * verticalFraction
}
