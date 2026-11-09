package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@Composable
internal fun PaperCompanion(icon: ImageVector) {
    val colors = AppTheme.colors
    Box(Modifier.size(112.dp, 94.dp), contentAlignment = Alignment.BottomEnd) {
        Canvas(Modifier.matchParentSize()) {
            val unit = size.width / 112f
            fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
            drawCircle(colors.accent.copy(alpha = 0.10f), 43 * unit, point(52f, 46f))
            drawRoundRect(colors.accent, point(28f, 13f), Size(49 * unit, 62 * unit), CornerRadius(12 * unit))
            drawPath(Path().apply {
                moveTo(61 * unit, 13 * unit)
                lineTo(76 * unit, 13 * unit)
                lineTo(76 * unit, 29 * unit)
                close()
            }, colors.accentVariant)
            for (x in listOf(43f, 62f)) {
                drawOval(colors.onAccent, point(x - 2.5f, 38f), Size(5 * unit, 8 * unit))
                drawCircle(colors.accentAmber, 2.5f * unit, point(x - 3f, 51f))
            }
            drawArc(colors.onAccent, 0f, 180f, false, point(48f, 47f),
                Size(9 * unit, 6 * unit), style = Stroke(1.7f * unit))
            for ((x, y) in listOf(17f to 24f, 91f to 24f, 17f to 65f)) {
                drawLine(colors.accentAmber, point(x - 4, y), point(x + 4, y), 2 * unit)
                drawLine(colors.accentAmber, point(x, y - 4), point(x, y + 4), 2 * unit)
            }
        }
        Icon(icon, contentDescription = null, tint = colors.inkMuted,
            modifier = Modifier.offset(x = (-10).dp, y = (-10).dp).size(22.dp))
    }
}
