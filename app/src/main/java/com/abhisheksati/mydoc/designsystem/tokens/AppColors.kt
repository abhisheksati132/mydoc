package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Strict Design System Color Tokens.
 *
 * Rules:
 * - Dark background is a soft near-black (#0F0F10)
 * - Text is off-white (#F0F0F2) in dark mode, near-black (#111114) in light mode
 * - Accent is slightly muted in dark mode (#7FA4DE)
 * - No pure black (#000000) or pure white (#FFFFFF)
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val error: Color
)

val DarkAppColors = AppColors(
    background = Color(0xFF0F0F10),
    surface = Color(0xFF18181A),
    surfaceVariant = Color(0xFF222226),
    divider = Color(0xFF2B2B30),
    textPrimary = Color(0xFFF0F0F2),
    textSecondary = Color(0xFFA0A0A6),
    accent = Color(0xFF7FA4DE),
    error = Color(0xFFE57373)
)

val LightAppColors = AppColors(
    background = Color(0xFFF9F9FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEFF2),
    divider = Color(0xFFE0E0E5),
    textPrimary = Color(0xFF111114),
    textSecondary = Color(0xFF666670),
    accent = Color(0xFF2B5BB8),
    error = Color(0xFFD32F2F)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
