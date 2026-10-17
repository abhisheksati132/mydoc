package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Strict Design System Spacing & Geometry Tokens.
 *
 * Rules:
 * - 8dp grid spacing (4dp, 8dp, 16dp, 24dp, 32dp, 48dp)
 * - Exactly one corner radius (8dp) + one smaller value for chips (4dp)
 * - Standard elevation = none (0dp, use dividers instead)
 * - Minimum touch target = 48dp (WCAG AA accessibility)
 */
object AppSpacing {
    val none: Dp = 0.dp
    val xSmall: Dp = 4.dp     // 0.5x grid (micro-gaps)
    val small: Dp = 8.dp      // 1x grid
    val medium: Dp = 16.dp    // 2x grid
    val large: Dp = 24.dp     // 3x grid
    val xLarge: Dp = 32.dp    // 4x grid
    val xxLarge: Dp = 48.dp   // 6x grid

    // Convenient semantic aliases
    val paddingXSmall: Dp = xSmall
    val paddingSmall: Dp = small
    val paddingMedium: Dp = medium
    val paddingLarge: Dp = large
    val paddingXLarge: Dp = xLarge

    // Minimum touch target size
    val minTouchTarget: Dp = 48.dp

    // Corner Radii
    val cornerRadius: Dp = 8.dp
    val chipCornerRadius: Dp = 4.dp

    // Elevation (none)
    val elevation: Dp = 0.dp

    // Hairline divider stroke
    val dividerThickness: Dp = 1.dp
}
