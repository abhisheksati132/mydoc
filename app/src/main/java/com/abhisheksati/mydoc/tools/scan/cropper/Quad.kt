package com.abhisheksati.mydoc.tools.scan.cropper

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

data class Quad(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
    fun toOffsetList(): List<Offset> = listOf(
        Offset(topLeft.x, topLeft.y),
        Offset(topRight.x, topRight.y),
        Offset(bottomRight.x, bottomRight.y),
        Offset(bottomLeft.x, bottomLeft.y)
    )

    companion object {
        fun fromOffsets(points: List<Offset>): Quad {
            require(points.size == 4) { "Quad requires exactly 4 points" }
            return Quad(
                topLeft = PointF(points[0].x, points[0].y),
                topRight = PointF(points[1].x, points[1].y),
                bottomRight = PointF(points[2].x, points[2].y),
                bottomLeft = PointF(points[3].x, points[3].y)
            )
        }
    }
}

fun PointF.distance(other: PointF): Float = hypot(other.x - x, other.y - y)
