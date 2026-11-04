package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

/**
 * The Seal: a rounded-square emblem whose negative space forms an M with a folded-paper corner.
 * Drawn rather than shipped as a drawable so it follows the in-app theme (Seal teal on paper,
 * lifted teal on graphite) instead of the system night mode.
 */
@Composable
fun SealMark(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Canvas(modifier = modifier) {
        val corner = size.minDimension * 0.19f
        drawRoundRect(
            color = colors.accent,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )
        // Folded corner: a darker wedge of the same plate.
        val fold = size.minDimension * 0.21f
        val foldPath = Path().apply {
            moveTo(size.width - fold, 0f)
            lineTo(size.width, fold)
            lineTo(size.width - fold, fold)
            close()
        }
        drawPath(foldPath, color = Color.Black.copy(alpha = 0.14f))

        // The M, struck in the paper tone.
        val stroke = size.minDimension * 0.085f
        val left = size.width * 0.27f
        val right = size.width * 0.73f
        val top = size.height * 0.31f
        val bottom = size.height * 0.69f
        val mid = size.height * 0.54f
        val mPath = Path().apply {
            moveTo(left, bottom)
            lineTo(left, top)
            lineTo(size.width / 2f, mid)
            lineTo(right, top)
            lineTo(right, bottom)
        }
        drawPath(
            path = mPath,
            // The M reads as negative space: it shows the surface behind the seal.
            color = colors.background,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
