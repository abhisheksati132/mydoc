package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for corner radii. No screen may hardcode a radius.
 *
 * - micro (6dp): format badges, tiny tags, icon tiles, thumbnails.
 * - pill  (8dp): chips, compact buttons, action pills.
 * - card (12dp): document cards, grouped lists, inputs, snackbars.
 * - large (16dp): hero cards, dialogs, floating bars.
 * - sheet (20dp): bottom sheets and modals (top corners).
 * - xLarge (24dp): large media surfaces, player panels, full sheets.
 * - round (999dp): circular buttons, progress tracks, avatars, drag handles.
 */
object AppRadius {
    val micro: Dp = 6.dp
    val pill: Dp = 8.dp
    val card: Dp = 12.dp
    val large: Dp = 16.dp
    val sheet: Dp = 20.dp
    val xLarge: Dp = 24.dp
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
