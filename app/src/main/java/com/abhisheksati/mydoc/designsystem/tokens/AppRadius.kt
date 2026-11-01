package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Editorial Document Studio Corner Radius Contract.
 *
 * Rules:
 * - Micro (6dp): Small format badges, tags, and item indicators.
 * - Pill (8dp): Segmented filter chips, search chips, and compact buttons.
 * - Card (12dp): Document cards, grouped list containers, and dialog surfaces.
 * - Sheet (20dp): Bottom sheets, modals, and major floating drawers.
 * - Round (999dp): Circular action buttons and status indicators.
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
