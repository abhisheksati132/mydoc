package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single Source of Truth for Spacing, Shapes, and Touch Targets ("Shelves" Design System).
 *
 * Rules:
 * - 8dp grid spacing (4dp, 8dp, 16dp, 24dp, 32dp, 48dp)
 * - Strict Radii:
 *   hero card: 28dp
 *   shelf tile: 24dp
 *   cards / sheets: 24dp (sheet top corners: 32dp)
 *   inputs and pills: 24dp
 *   file icon square: 14dp
 *   chips: 999dp (fully rounded)
 * - Screen side padding: 20dp
 * - Bottom Bar height: 72dp
 * - Elevation: none (0dp, flat colour and 1dp hairline lines only)
 * - Minimum touch target: 48dp
 */
object AppSpacing {
    val none: Dp = 0.dp
    val xSmall: Dp = 4.dp     // 0.5x grid
    val small: Dp = 8.dp      // 1x grid
    val medium: Dp = 16.dp    // 2x grid
    val large: Dp = 24.dp     // 3x grid
    val xLarge: Dp = 32.dp    // 4x grid
    val xxLarge: Dp = 48.dp   // 6x grid

    // Layout Dimensions
    val screenPadding: Dp = 20.dp
    val navBarHeight: Dp = 72.dp
    val minTouchTarget: Dp = 48.dp

    // Strict Radii Hierarchy
    val heroCardRadius: Dp = 28.dp
    val shelfTileRadius: Dp = 24.dp
    val cardRadius: Dp = 24.dp
    val sheetTopRadius: Dp = 32.dp
    val inputRadius: Dp = 24.dp
    val buttonRadius: Dp = 24.dp
    val pillRadius: Dp = 999.dp
    val chipRadius: Dp = 999.dp
    val fileIconSquareRadius: Dp = 14.dp

    // Stroke & Elevation
    val hairline: Dp = 1.dp
    val dividerThickness: Dp = 1.dp
    val elevation: Dp = 0.dp

    // Aliases for backward compatibility
    val paddingXSmall: Dp = xSmall
    val paddingSmall: Dp = small
    val paddingMedium: Dp = medium
    val paddingLarge: Dp = large
    val paddingXLarge: Dp = xLarge
    val extraSmall: Dp = xSmall
    val extraLarge: Dp = xLarge
    val cornerRadius: Dp = cardRadius
    val chipCornerRadius: Dp = chipRadius
    val cardCornerRadius: Dp = cardRadius
    val pillCornerRadius: Dp = pillRadius
    val listRowHeight: Dp = 64.dp
    val listDividerThickness: Dp = 1.dp
}
