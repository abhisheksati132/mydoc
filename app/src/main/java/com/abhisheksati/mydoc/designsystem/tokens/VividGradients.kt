package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The vivid premium decoration language: saturated two-stop gradients and the
 * colored glows they cast. Stops are fixed so every family stays punchy on
 * both light and dark canvases; only the wordmark follows the theme.
 */
object VividGradients {
    /** Brand hero: electric violet into hot pink. Scan, primary CTAs, active nav. */
    val brand: Brush = Brush.linearGradient(
        listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
    )

    /** Sunset: pink into orange. Signatures, edits. */
    val sunset: Brush = Brush.linearGradient(
        listOf(Color(0xFFEC4899), Color(0xFFF97316))
    )

    /** Ocean: cyan into deep blue. Merge, organize, file ops. */
    val ocean: Brush = Brush.linearGradient(
        listOf(Color(0xFF06B6D4), Color(0xFF3B5BFD))
    )

    /** Forest: green into teal. Success, audio, convert-from. */
    val forest: Brush = Brush.linearGradient(
        listOf(Color(0xFF22C55E), Color(0xFF14B8A6))
    )

    /** Gold: amber into orange. Compress, optimize, warnings-as-delight. */
    val gold: Brush = Brush.linearGradient(
        listOf(Color(0xFFF59E0B), Color(0xFFF97316))
    )

    /** Grape: violet into indigo. Convert-to, security. */
    val grape: Brush = Brush.linearGradient(
        listOf(Color(0xFF8B5CF6), Color(0xFF5B4FE8))
    )

    /** Fire: red into rose. Destructive emphasis, PDF format. */
    val fire: Brush = Brush.linearGradient(
        listOf(Color(0xFFEF4444), Color(0xFFE11D74))
    )

    /** The brand wordmark gradient, angled for a two-word lockup. */
    fun wordmark(colors: AppColors): Brush = Brush.linearGradient(
        listOf(colors.accent, colors.accentPink, Color(0xFFF97316))
    )

    /** Solid end-color of a gradient family, for glows and tints. */
    fun glowColor(family: Family): Color = when (family) {
        Family.BRAND -> Color(0xFFEC4899)
        Family.SUNSET -> Color(0xFFF97316)
        Family.OCEAN -> Color(0xFF06B6D4)
        Family.FOREST -> Color(0xFF22C55E)
        Family.GOLD -> Color(0xFFF59E0B)
        Family.GRAPE -> Color(0xFF8B5CF6)
        Family.FIRE -> Color(0xFFEF4444)
    }

    fun brush(family: Family): Brush = when (family) {
        Family.BRAND -> brand
        Family.SUNSET -> sunset
        Family.OCEAN -> ocean
        Family.FOREST -> forest
        Family.GOLD -> gold
        Family.GRAPE -> grape
        Family.FIRE -> fire
    }

    enum class Family { BRAND, SUNSET, OCEAN, FOREST, GOLD, GRAPE, FIRE }
}

/**
 * A soft colored halo behind an element — the vivid replacement for hairline
 * borders. Below API 28 the platform draws neutral shadow instead; acceptable.
 */
fun Modifier.glow(
    color: Color,
    shape: Shape,
    elevation: Dp = 12.dp
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = color.copy(alpha = 0.55f),
    spotColor = color.copy(alpha = 0.55f)
)
