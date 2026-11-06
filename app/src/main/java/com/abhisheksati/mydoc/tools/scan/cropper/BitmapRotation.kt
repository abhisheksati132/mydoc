package com.abhisheksati.mydoc.tools.scan.cropper

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

/**
 * Rotation for the crop screen, with the geometry kept apart from the drawing so the arithmetic can
 * be tested without a device.
 *
 * `Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)` returns a bitmap of exactly
 * the requested size, so rotating 90 degrees inside the source's own box clips the image: the long
 * edge no longer fits. A quarter turn has to swap the axes.
 */
object BitmapRotation {

    fun swapsAxes(degrees: Int): Boolean {
        val normalised = ((degrees % 360) + 360) % 360
        return normalised == 90 || normalised == 270
    }

    fun outputSize(width: Int, height: Int, degrees: Int): Pair<Int, Int> =
        if (swapsAxes(degrees)) height to width else width to height

    /**
     * Rotates [src] and returns a new bitmap. The source is left untouched and is never recycled
     * here, because the caller still owns it.
     */
    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val (outWidth, outHeight) = outputSize(src.width, src.height, degrees)
        val config = src.config ?: Bitmap.Config.ARGB_8888
        val out = createBitmap(outWidth, outHeight, config)
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
            // A rotation about the origin leaves the image outside a positive-coordinate bitmap,
            // so each turn is followed by the translation that brings it back into the output box.
            // 180 needs this as much as a quarter turn: without it the whole image is drawn at
            // negative coordinates and the result is blank.
            when (((degrees % 360) + 360) % 360) {
                90 -> postTranslate(outHeight.toFloat(), 0f)
                180 -> postTranslate(outWidth.toFloat(), outHeight.toFloat())
                270 -> postTranslate(0f, outWidth.toFloat())
            }
        }
        Canvas(out).drawBitmap(src, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }
}
