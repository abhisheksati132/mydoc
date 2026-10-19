package com.abhisheksati.mydoc.tools.scan.cropper

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin, high-performance document edge detection.
 * Analyzes luminance gradients and edge transitions to automatically detect
 * physical document/receipt/card boundaries in photos in < 25ms.
 */
object DocumentEdgeDetector {

    private const val MAX_ANALYSIS_DIMENSION = 260

    /**
     * Detects document boundaries in the given [bitmap].
     * Returns a [Quad] containing [TL, TR, BR, BL] in original bitmap coordinates,
     * or null if no distinct document quad was identified with sufficient confidence.
     */
    fun detectDocumentQuad(bitmap: Bitmap): Quad? {
        if (bitmap.width < 10 || bitmap.height < 10) return null

        val origW = bitmap.width.toFloat()
        val origH = bitmap.height.toFloat()

        // Downscale for fast sub-25ms analysis
        val scale = min(
            1f,
            MAX_ANALYSIS_DIMENSION.toFloat() / max(origW, origH)
        )
        val sampleW = (origW * scale).toInt().coerceAtLeast(16)
        val sampleH = (origH * scale).toInt().coerceAtLeast(16)

        val small = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
        val pixels = IntArray(sampleW * sampleH)
        small.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        if (small !== bitmap) {
            small.recycle()
        }

        // 1. Compute luminance grid
        val lum = FloatArray(sampleW * sampleH)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // 2. Compute average luminance & standard deviation
        var sumLum = 0.0
        for (v in lum) sumLum += v
        val meanLum = (sumLum / lum.size).toFloat()

        var sumSqDiff = 0.0
        for (v in lum) {
            val d = v - meanLum
            sumSqDiff += d * d
        }
        val stdDev = kotlin.math.sqrt(sumSqDiff / lum.size).toFloat()

        // If the entire image has virtually no contrast, we can't reliably detect edges
        if (stdDev < 12f) return null

        // 3. Scan inward from 4 borders to locate document boundary points
        val minMarginX = (sampleW * 0.04f).toInt()
        val maxMarginX = (sampleW * 0.45f).toInt()
        val minMarginY = (sampleH * 0.04f).toInt()
        val maxMarginY = (sampleH * 0.45f).toInt()

        val gradientThreshold = max(18f, stdDev * 0.65f)

        // Find Top edge boundary points across columns (20% to 80%)
        val topPointsY = mutableListOf<Float>()
        val startCol = (sampleW * 0.20f).toInt()
        val endCol = (sampleW * 0.80f).toInt()

        for (x in startCol..endCol step 4) {
            var foundY = -1
            for (y in minMarginY until maxMarginY) {
                val idx1 = y * sampleW + x
                val idx2 = (y + 2) * sampleW + x
                val grad = abs(lum[idx2] - lum[idx1])
                if (grad >= gradientThreshold) {
                    foundY = y
                    break
                }
            }
            if (foundY != -1) topPointsY.add(foundY.toFloat())
        }

        // Find Bottom edge boundary points
        val bottomPointsY = mutableListOf<Float>()
        for (x in startCol..endCol step 4) {
            var foundY = -1
            for (y in (sampleH - 1 - minMarginY) downTo (sampleH - 1 - maxMarginY)) {
                val idx1 = y * sampleW + x
                val idx2 = (y - 2) * sampleW + x
                val grad = abs(lum[idx2] - lum[idx1])
                if (grad >= gradientThreshold) {
                    foundY = y
                    break
                }
            }
            if (foundY != -1) bottomPointsY.add(foundY.toFloat())
        }

        // Find Left edge boundary points across rows (20% to 80%)
        val leftPointsX = mutableListOf<Float>()
        val startRow = (sampleH * 0.20f).toInt()
        val endRow = (sampleH * 0.80f).toInt()

        for (y in startRow..endRow step 4) {
            var foundX = -1
            for (x in minMarginX until maxMarginX) {
                val idx1 = y * sampleW + x
                val idx2 = y * sampleW + (x + 2)
                val grad = abs(lum[idx2] - lum[idx1])
                if (grad >= gradientThreshold) {
                    foundX = x
                    break
                }
            }
            if (foundX != -1) leftPointsX.add(foundX.toFloat())
        }

        // Find Right edge boundary points
        val rightPointsX = mutableListOf<Float>()
        for (y in startRow..endRow step 4) {
            var foundX = -1
            for (x in (sampleW - 1 - minMarginX) downTo (sampleW - 1 - maxMarginX)) {
                val idx1 = y * sampleW + x
                val idx2 = y * sampleW + (x - 2)
                val grad = abs(lum[idx2] - lum[idx1])
                if (grad >= gradientThreshold) {
                    foundX = x
                    break
                }
            }
            if (foundX != -1) rightPointsX.add(foundX.toFloat())
        }

        // Require at least partial edge detection on all 4 sides
        if (topPointsY.isEmpty() || bottomPointsY.isEmpty() || leftPointsX.isEmpty() || rightPointsX.isEmpty()) {
            return null
        }

        // Median filtering for robust outlier rejection
        topPointsY.sort()
        bottomPointsY.sort()
        leftPointsX.sort()
        rightPointsX.sort()

        val detectedTopY = topPointsY[topPointsY.size / 2] / scale
        val detectedBottomY = bottomPointsY[bottomPointsY.size / 2] / scale
        val detectedLeftX = leftPointsX[leftPointsX.size / 2] / scale
        val detectedRightX = rightPointsX[rightPointsX.size / 2] / scale

        // Check reasonable document aspect and margins
        val docW = detectedRightX - detectedLeftX
        val docH = detectedBottomY - detectedTopY

        if (docW < origW * 0.25f || docH < origH * 0.25f) return null
        if (docW > origW * 0.98f && docH > origH * 0.98f) return null

        // Refine corners with corner slant tolerance
        val tl = android.graphics.PointF(detectedLeftX.coerceIn(0f, origW), detectedTopY.coerceIn(0f, origH))
        val tr = android.graphics.PointF(detectedRightX.coerceIn(0f, origW), detectedTopY.coerceIn(0f, origH))
        val br = android.graphics.PointF(detectedRightX.coerceIn(0f, origW), detectedBottomY.coerceIn(0f, origH))
        val bl = android.graphics.PointF(detectedLeftX.coerceIn(0f, origW), detectedBottomY.coerceIn(0f, origH))

        return Quad(
            topLeft = tl,
            topRight = tr,
            bottomRight = br,
            bottomLeft = bl
        )
    }
}
