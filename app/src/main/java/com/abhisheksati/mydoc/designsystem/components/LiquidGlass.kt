package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius

/**
 * Editorial Studio Surface System (formerly LiquidGlass).
 * Replaces fake specular reflections and rainbow gradients with clean,
 * tactile matte surfaces and crisp hairline borders.
 */
object LiquidGlassTokens {
    // Restrained, editorial monochromatic tints (brand ink-violet and calm neutrals)
    val AzureLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFF5B4FE8), Color(0xFF6E64EE))
    )

    val SunsetLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFFE25547), Color(0xFFEA6B5E))
    )

    val AuroraLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFF2E9461), Color(0xFF38A56E))
    )

    val VioletLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFF6C5CE7), Color(0xFF7E70ED))
    )

    val AmberLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFFD97706), Color(0xFFE5881E))
    )

    val IndigoLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFF5B4FE8), Color(0xFF7367F0))
    )
}

/**
 * Applies a clean, tactile editorial surface with a hairline border.
 * Eliminates artificial glossy glare and heavy drop shadows.
 */
fun Modifier.liquidGlass(
    isDarkTheme: Boolean = false,
    shape: Shape = AppRadius.cardShape,
    elevation: Dp = 0.dp,
    tintColor: Color? = null
): Modifier {
    val backgroundColor = if (tintColor != null) {
        tintColor.copy(alpha = if (isDarkTheme) 0.14f else 0.08f)
    } else if (isDarkTheme) {
        Color(0xFF18181A)
    } else {
        Color(0xFFFFFFFF)
    }

    val borderColor = if (tintColor != null) {
        tintColor.copy(alpha = if (isDarkTheme) 0.35f else 0.20f)
    } else if (isDarkTheme) {
        Color(0xFF26262A)
    } else {
        Color(0xFFE5E5DF)
    }

    return this
        .clip(shape)
        .background(color = backgroundColor, shape = shape)
        .border(width = 0.5.dp, color = borderColor, shape = shape)
}
