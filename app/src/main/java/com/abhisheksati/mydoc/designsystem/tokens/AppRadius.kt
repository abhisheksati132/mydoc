package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for corner radii. No screen may hardcode a radius.
 *
 * - micro (6dp): format badges, tiny tags, icon tiles.
 * - pill  (8dp): chips, compact buttons, action pills.
 * - card (12dp): document cards, grouped lists, inputs, snackbars.
 * - sheet (20dp): bottom sheets and modals (top corners).
 * - round (999dp): circular buttons, progress tracks, avatars.
 */
object AppRadius {
    val micro: Dp = 6.dp
    val pill: Dp = 8.dp
    val card: Dp = 12.dp
    val sheet: Dp = 20.dp
    val round: Dp = 999.dp

    val microShape = RoundedCornerShape(micro)
    val pillShape = RoundedCornerShape(pill)
    val cardShape = RoundedCornerShape(card)
    val sheetShape = RoundedCornerShape(topStart = sheet, topEnd = sheet)
    val modalShape = RoundedCornerShape(sheet)
    val roundShape = RoundedCornerShape(round)
}
