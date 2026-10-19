package com.abhisheksati.mydoc.tools.scan.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Filter presets for scanned documents, receipts, contracts, and IDs.
 */
enum class ScanFilter(val label: String, val description: String) {
    ORIGINAL("Original", "Camera raw"),
    AUTO("Doc Auto", "Auto contrast & sharpness"),
    BLACK_WHITE("B&W Clean", "Thresholded black & white"),
    GRAYSCALE("Grayscale", "Clean monochrome"),
    COLOR_ENHANCE("Vivid Color", "Vivid color documents"),
    SHADOW_REMOVAL("Shadow Free", "Removes ambient lighting shadows")
}

object ScanFilterEngine {

    /**
     * Applies the chosen [ScanFilter] to [src] and returns a new processed [Bitmap].
     */
    suspend fun applyFilter(src: Bitmap, filter: ScanFilter): Bitmap = withContext(Dispatchers.Default) {
        when (filter) {
            ScanFilter.ORIGINAL -> src
            ScanFilter.GRAYSCALE -> applyColorMatrix(src, grayscaleMatrix())
            ScanFilter.BLACK_WHITE -> applyDocumentThreshold(src)
            ScanFilter.AUTO -> applyColorMatrix(src, autoEnhanceMatrix())
            ScanFilter.COLOR_ENHANCE -> applyColorMatrix(src, colorEnhanceMatrix())
            ScanFilter.SHADOW_REMOVAL -> removeDocumentShadows(src)
        }
    }

    /**
     * Compose ColorFilter for real-time live preview rendering without reallocating bitmaps.
     */
    fun getComposeColorFilter(filter: ScanFilter): ColorFilter? {
        return when (filter) {
            ScanFilter.ORIGINAL -> null
            ScanFilter.GRAYSCALE -> ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
            )
            ScanFilter.BLACK_WHITE -> {
                val sat = androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
                val contrast = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                    2.2f, 0f, 0f, 0f, -190f,
                    0f, 2.2f, 0f, 0f, -190f,
                    0f, 0f, 2.2f, 0f, -190f,
                    0f, 0f, 0f, 1f, 0f
                ))
                contrast.timesAssign(sat)
                ColorFilter.colorMatrix(contrast)
            }
            ScanFilter.AUTO -> ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                    1.22f, 0f, 0f, 0f, 8f,
                    0f, 1.22f, 0f, 0f, 8f,
                    0f, 0f, 1.22f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            ScanFilter.COLOR_ENHANCE -> {
                val sat = androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(1.4f) }
                val bright = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                    1.12f, 0f, 0f, 0f, 6f,
                    0f, 1.12f, 0f, 0f, 6f,
                    0f, 0f, 1.12f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                ))
                bright.timesAssign(sat)
                ColorFilter.colorMatrix(bright)
            }
            ScanFilter.SHADOW_REMOVAL -> ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                    1.35f, 0f, 0f, 0f, 15f,
                    0f, 1.35f, 0f, 0f, 15f,
                    0f, 0f, 1.35f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
        }
    }

    private fun grayscaleMatrix(): ColorMatrix {
        return ColorMatrix().apply { setSaturation(0f) }
    }

    private fun autoEnhanceMatrix(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            1.22f, 0f, 0f, 0f, 8f,
            0f, 1.22f, 0f, 0f, 8f,
            0f, 0f, 1.22f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    private fun colorEnhanceMatrix(): ColorMatrix {
        val sat = ColorMatrix().apply { setSaturation(1.4f) }
        val bright = ColorMatrix(floatArrayOf(
            1.12f, 0f, 0f, 0f, 6f,
            0f, 1.12f, 0f, 0f, 6f,
            0f, 0f, 1.12f, 0f, 6f,
            0f, 0f, 0f, 1f, 0f
        ))
        bright.preConcat(sat)
        return bright
    }

    /**
     * High-contrast document thresholding for legal, tax, or receipt documents.
     * Keeps ink crisp black and turns paper into pure white.
     */
    private fun applyDocumentThreshold(src: Bitmap): Bitmap {
        val gray = ColorMatrix().apply { setSaturation(0f) }
        val contrast = ColorMatrix(floatArrayOf(
            2.5f, 0f, 0f, 0f, -210f,
            0f, 2.5f, 0f, 0f, -210f,
            0f, 0f, 2.5f, 0f, -210f,
            0f, 0f, 0f, 1f, 0f
        ))
        contrast.preConcat(gray)
        return applyColorMatrix(src, contrast)
    }

    /**
     * Shadow removal & background flattening for documents captured under uneven lighting.
     * Uses fast integral-block background estimation and contrast normalization.
     */
    private fun removeDocumentShadows(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Downsample grid for background illumination estimation
        val step = maxOf(16, minOf(width, height) / 30)
        val gridW = (width + step - 1) / step
        val gridH = (height + step - 1) / step
        val bgGrid = FloatArray(gridW * gridH)

        for (gy in 0 until gridH) {
            val yCenter = (gy * step).coerceAtMost(height - 1)
            for (gx in 0 until gridW) {
                val xCenter = (gx * step).coerceAtMost(width - 1)
                // Sample local neighborhood max brightness (paper background)
                var maxLuma = 0f
                var count = 0
                for (dy in -step / 2..step / 2 step 4) {
                    val py = (yCenter + dy).coerceIn(0, height - 1)
                    for (dx in -step / 2..step / 2 step 4) {
                        val px = (xCenter + dx).coerceIn(0, width - 1)
                        val color = pixels[py * width + px]
                        val r = (color shr 16) and 0xFF
                        val g = (color shr 8) and 0xFF
                        val b = color and 0xFF
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        if (luma > maxLuma) maxLuma = luma
                        count++
                    }
                }
                bgGrid[gy * gridW + gx] = if (maxLuma > 20f) maxLuma else 180f
            }
        }

        // Apply illumination normalization to each pixel
        val outPixels = IntArray(width * height)
        for (y in 0 until height) {
            val gy = (y / step).coerceIn(0, gridH - 1)
            val yOffset = y * width
            for (x in 0 until width) {
                val gx = (x / step).coerceIn(0, gridW - 1)
                val bg = bgGrid[gy * gridW + gx]
                val color = pixels[yOffset + x]
                val a = color and -0x1000000
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                val scale = if (bg > 30f) 255f / bg else 1f
                val nr = (r * scale).toInt().coerceIn(0, 255)
                val ng = (g * scale).toInt().coerceIn(0, 255)
                val nb = (b * scale).toInt().coerceIn(0, 255)

                outPixels[yOffset + x] = a or (nr shl 16) or (ng shl 8) or nb
            }
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, width, 0, 0, width, height)
        return out
    }

    private fun applyColorMatrix(src: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
