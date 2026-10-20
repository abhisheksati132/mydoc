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

    /** Maximum allowed pixel dimension on either axis to prevent OutOfMemoryError heap spikes */
    private const val MaxSafeDimension = 2560f

    /**
     * Warps the quadrilateral specified by [points] (Top-Left, Top-Right, Bottom-Right, Bottom-Left
     * in original bitmap pixel coordinates) into an upright rectangular bitmap with zero distortion.
     * Uses memory-bounded dimensions and software projective transformation with anti-aliasing.
     */
    fun crop(
        bitmap: Bitmap,
        points: List<Offset>
    ): Bitmap {
        if (points.size != 4 || bitmap.isRecycled) return bitmap

        val tl = PointF(points[0].x, points[0].y)
        val tr = PointF(points[1].x, points[1].y)
        val br = PointF(points[2].x, points[2].y)
        val bl = PointF(points[3].x, points[3].y)

        // Validate that points form a reasonable polygon (not all coincident)
        val topDist = tl.distance(tr)
        val bottomDist = bl.distance(br)
        val leftDist = tl.distance(bl)
        val rightDist = tr.distance(br)

        if (topDist <= 1f || bottomDist <= 1f || leftDist <= 1f || rightDist <= 1f) {
            return bitmap
        }

        var rawWidth = max(1f, maxOf(topDist, bottomDist))
        var rawHeight = max(1f, maxOf(leftDist, rightDist))

        // Bounded scaling to prevent OOM on 48MP/108MP camera sensors
        var scale = 1.0f
        val maxDim = maxOf(rawWidth, rawHeight)
        if (maxDim > MaxSafeDimension) {
            scale = MaxSafeDimension / maxDim
            rawWidth *= scale
            rawHeight *= scale
        }

        val width = rawWidth.roundToInt().coerceAtLeast(1)
        val height = rawHeight.roundToInt().coerceAtLeast(1)

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

        return createWarpedBitmapSafe(bitmap, width, height, matrix) ?: bitmap
    }

    /**
     * Safely creates the destination bitmap and draws the projective warp,
     * gracefully catching OutOfMemoryError and downscaling if system memory is low.
     */
    private fun createWarpedBitmapSafe(
        source: Bitmap,
        width: Int,
        height: Int,
        matrix: Matrix,
        retryCount: Int = 0
    ): Bitmap? {
        return try {
            val config = if (source.hasAlpha()) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            val out = Bitmap.createBitmap(width, height, config)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            canvas.drawBitmap(source, matrix, paint)
            out
        } catch (_: OutOfMemoryError) {
            if (retryCount < 2 && width > 400 && height > 400) {
                // Downscale by 50% and retry under extreme memory pressure
                val halfW = width / 2
                val halfH = height / 2
                val scaleDownMatrix = Matrix(matrix).apply {
                    postScale(0.5f, 0.5f)
                }
                createWarpedBitmapSafe(source, halfW, halfH, scaleDownMatrix, retryCount + 1)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
