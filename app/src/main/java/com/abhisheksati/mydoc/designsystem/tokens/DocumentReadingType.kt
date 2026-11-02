package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Type sizes for *rendered document content* — the reader's Word/PPT text views.
 *
 * These sit outside AppTypography on purpose: the reader mirrors the author's
 * heading hierarchy, and the user's reading scale multiplies every size here.
 * Mapping document text onto UI roles would let a Material You theme change
 * someone's reading layout, so the numbers live in one place instead.
 */
object DocumentReadingType {
    val heading1: TextUnit = 24.sp
    val heading2: TextUnit = 20.sp
    val heading3: TextUnit = 17.sp
    val heading4: TextUnit = 15.sp
    val body: TextUnit = 14.sp
    val bodyLineHeight: TextUnit = 20.sp
    val tableCell: TextUnit = 12.sp

    /** Heading levels below 1 and above 3 both land on the smallest tier. */
    fun heading(level: Int): TextUnit = when (level) {
        1 -> heading1
        2 -> heading2
        3 -> heading3
        else -> heading4
    }
}
