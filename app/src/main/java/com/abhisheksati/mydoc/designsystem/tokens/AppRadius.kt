package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for corner radii. No screen may hardcode a radius.
 * Vivid premium: everything is soft and generous — big radii, pill chips.
 *
 * The scale steps by 4dp: 8 / 12 / 16 / 20 / 24 / 28, plus `round` for anything
 * that must clamp to a stadium. Off-scale values a screen used to reach for
 * (10, 14, 18, 22) are folded onto the nearest step below.
 *
 * - micro (8dp): format badges, inline chips, grid cells.
 * - inset (12dp): elements nested inside a card — icon wells, thumbnails,
 *   row-leading artwork. Always smaller than the container holding it.
 * - card (16dp): document cards, grouped lists, inputs, snackbars.
 * - large (20dp): hero cards, dialogs, floating bars.
 * - sheet (24dp): bottom sheets and modals (top corners); `modalShape` is the
 *   same value on all four corners.
 * - xLarge (28dp): large media surfaces, player panels, full sheets.
 * - pill (28dp): chips, compact buttons, action pills — clamps to a stadium at
 *   chip heights, and stays a rounded rect for larger shapes (slide elements).
 * - round (999dp): circular buttons, progress tracks, avatars, drag handles,
 *   and 2dp indicators (clamping makes it identical to a 1dp radius there).
 */
object AppRadius {
    val micro: Dp = 8.dp
    val inset: Dp = 12.dp
    val pill: Dp = 28.dp
    val card: Dp = 16.dp
    val large: Dp = 20.dp
    val sheet: Dp = 24.dp
    val xLarge: Dp = 28.dp
    val round: Dp = 999.dp

    val microShape = RoundedCornerShape(micro)
    val smallShape = RoundedCornerShape(micro)
    val insetShape = RoundedCornerShape(inset)
    val pillShape = RoundedCornerShape(pill)
    val cardShape = RoundedCornerShape(card)
    val largeShape = RoundedCornerShape(large)
    val sheetShape = RoundedCornerShape(topStart = sheet, topEnd = sheet)
    val modalShape = RoundedCornerShape(sheet)
    val xLargeShape = RoundedCornerShape(xLarge)
    val roundShape = RoundedCornerShape(round)

    // Full-width chrome that clings to a screen edge. Both reader bars, the search
    // drop-down and the thumbnail strip share these so the chrome reads as one family
    // instead of 16dp here and 20dp there. Top-rounded = anchored to the bottom edge.
    val topRoundedShape = RoundedCornerShape(topStart = large, topEnd = large)
    val bottomRoundedShape = RoundedCornerShape(bottomStart = large, bottomEnd = large)

    // Continuous-curvature (squircle) variants for glass and hero surfaces:
    // curvature ramps in instead of snapping at a circular arc boundary.
    val glassShape = ContinuousCornerShape(large)
    val glassSheetShape = ContinuousCornerShape(sheet)
    val glassNavShape = ContinuousCornerShape(xLarge)
}
