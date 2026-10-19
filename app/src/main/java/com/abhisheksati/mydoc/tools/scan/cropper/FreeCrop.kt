package com.abhisheksati.mydoc.tools.scan.cropper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.roundToInt

object FreeCrop {

    /**
     * Warps the quadrilateral specified by [points] (Top-Left, Top-Right, Bottom-Right, Bottom-Left
     * in original bitmap pixel coordinates) into an upright rectangular bitmap with zero distortion.
     * Uses Android's native Matrix poly-to-poly projective transformation with anti-aliasing.
     */
    fun crop(
        bitmap: Bitmap,
        points: List<Offset>
    ): Bitmap {
        if (points.size != 4) return bitmap

        val tl = PointF(points[0].x, points[0].y)
        val tr = PointF(points[1].x, points[1].y)
        val br = PointF(points[2].x, points[2].y)
        val bl = PointF(points[3].x, points[3].y)

        val topDist = tl.distance(tr)
        val bottomDist = bl.distance(br)
        val leftDist = tl.distance(bl)
        val rightDist = tr.distance(br)

        val width = max(1, maxOf(topDist, bottomDist).roundToInt())
        val height = max(1, maxOf(leftDist, rightDist).roundToInt())

        val src = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )
        val dst = floatArrayOf(
            0f, 0f,
            width.toFloat(), 0f,
            width.toFloat(), height.toFloat(),
            0f, height.toFloat()
        )

        val matrix = Matrix()
        if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
            return bitmap
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
        return out
    }
}
