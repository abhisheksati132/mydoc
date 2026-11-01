package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for spacing, touch targets, and strokes.
 * Corner radii live in [AppRadius] — do not add radii here.
 *
 * Rules:
 * - 4dp grid: 4, 8, 16, 24, 32, 48. Nothing in between.
 * - Screen side padding: 16dp everywhere (root tabs, sheets, settings).
 * - Bottom nav height: 64dp + navigation bar inset.
 * - Two strokes only: [hairline] (0.5dp, internal dividers) and [rule] (2dp, section
 *   breaks and the masthead underline). No 1dp borders anywhere.
 * - Minimum touch target: 48dp.
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
    val screenPadding: Dp = 16.dp
    val navBarHeight: Dp = 64.dp
    val minTouchTarget: Dp = 48.dp
    val sectionGap: Dp = 32.dp
    val listRowHeight: Dp = 56.dp
    val objectRowHeight: Dp = 64.dp

    // Stroke & Elevation
    val hairline: Dp = 0.5.dp
    val rule: Dp = 2.dp
    val dividerThickness: Dp = 0.5.dp
    val elevation: Dp = 0.dp

    // Legacy aliases — prefer the named scale above.
    val paddingXSmall: Dp = xSmall
    val paddingSmall: Dp = small
    val paddingMedium: Dp = medium
    val paddingLarge: Dp = large
    val paddingXLarge: Dp = xLarge
    val extraSmall: Dp = xSmall
    val extraLarge: Dp = xLarge
    val listDividerThickness: Dp = 0.5.dp
}
