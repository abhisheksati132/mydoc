package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple-style Liquid Glass design system foundation.
 * Provides translucent layered glass surfaces, gradient specular rim borders,
 * playful vibrant liquid gradients, and iOS-style rounded squircles.
 */
object LiquidGlassTokens {
    // Vibrant Apple-inspired liquid gradients
    val AzureLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFF00C6FF))
    )

    val SunsetLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFFFF2D55), Color(0xFFFF9500))
    )

    val AuroraLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFF00C853), Color(0xFF69F0AE))
    )

    val VioletLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFFAF52DE), Color(0xFF5856D6))
    )

    val AmberLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFFFF9500), Color(0xFFFFCC00))
    )

    val IndigoLiquid = Brush.linearGradient(
        colors = listOf(Color(0xFF5856D6), Color(0xFF007AFF))
    )
}

/**
 * Applies a frosted translucent liquid glass effect with a specular gradient border.
 */
fun Modifier.liquidGlass(
    isDarkTheme: Boolean = false,
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 4.dp,
    tintColor: Color? = null
): Modifier {
    val backgroundBrush = if (tintColor != null) {
        val topAlpha = if (isDarkTheme) 0.35f else 0.22f
        val botAlpha = if (isDarkTheme) 0.18f else 0.10f
        Brush.verticalGradient(
            listOf(
                tintColor.copy(alpha = topAlpha),
                tintColor.copy(alpha = botAlpha)
            )
        )
    } else if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                Color(0xDD222530),
                Color(0xBB171922)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xF2FFFFFF),
                Color(0xD0F8F9FA)
            )
        )
    }

    val borderBrush = if (tintColor != null) {
        Brush.verticalGradient(
            listOf(
                tintColor.copy(alpha = 0.55f),
                tintColor.copy(alpha = 0.15f)
            )
        )
    } else if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.85f),
                Color(0x33B0B8C4)
            )
        )
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isDarkTheme) Color(0x66000000) else Color(0x18000000),
            spotColor = if (isDarkTheme) Color(0x88000000) else Color(0x22000000)
        )
        .clip(shape)
        .background(brush = backgroundBrush, shape = shape)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}
