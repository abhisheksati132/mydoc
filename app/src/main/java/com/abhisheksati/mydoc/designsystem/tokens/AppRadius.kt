package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for corner radii. No screen may hardcode a radius.
 * Vivid premium: everything is soft and generous — big radii, pill chips.
 *
 * - micro (8dp): format badges, tiny tags, thumbnails.
 * - card (16dp): document cards, grouped lists, inputs, snackbars.
 * - large (20dp): hero cards, dialogs, floating bars.
 * - sheet (24dp): bottom sheets and modals (top corners).
 * - xLarge (28dp): large media surfaces, player panels, full sheets.
 * - pill (28dp): chips, compact buttons, action pills — clamps to a stadium at
 *   chip heights, and stays a rounded rect for larger shapes (slide elements).
 * - round (999dp): circular buttons, progress tracks, avatars, drag handles.
 */
object AppRadius {
    val micro: Dp = 8.dp
    val pill: Dp = 28.dp
    val card: Dp = 16.dp
    val large: Dp = 20.dp
    val sheet: Dp = 24.dp
    val xLarge: Dp = 28.dp
    val round: Dp = 999.dp

    val microShape = RoundedCornerShape(micro)
    val pillShape = RoundedCornerShape(pill)
    val cardShape = RoundedCornerShape(card)
    val largeShape = RoundedCornerShape(large)
    val sheetShape = RoundedCornerShape(topStart = sheet, topEnd = sheet)
    val modalShape = RoundedCornerShape(sheet)
    val xLargeShape = RoundedCornerShape(xLarge)
    val roundShape = RoundedCornerShape(round)
}
