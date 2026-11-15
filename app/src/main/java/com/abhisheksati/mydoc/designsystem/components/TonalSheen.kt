package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Directional light for tonal cards: a soft highlight in the top corner and a whisper of
 * falloff toward the bottom edge, drawn beneath the content. GlassSurface already carries
 * a specular rim; this is the same lighting idea for the flat, tint-washed paper cards so
 * they read as lit material instead of a colour fill.
 */
fun Modifier.tonalSheen(
    tint: Color = Color.White,
    intensity: Float = 0.08f
): Modifier = drawWithCache {
    val highlight = Brush.radialGradient(
        colors = listOf(tint.copy(alpha = intensity), Color.Transparent),
        center = Offset(size.width * 0.82f, -size.width * 0.08f),
        radius = size.width.coerceAtLeast(1f)
    )
    val falloff = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.02f)),
        endY = size.height.coerceAtLeast(1f)
    )
    onDrawWithContent {
        drawRect(highlight)
        drawRect(falloff)
        drawContent()
    }
}
