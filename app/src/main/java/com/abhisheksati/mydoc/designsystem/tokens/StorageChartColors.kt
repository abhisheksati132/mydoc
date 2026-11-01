package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Categorical palette for the storage breakdown bar and its legend.
 *
 * Five segments sit side by side in one 6dp bar, so each needs its own hue.
 * The values are pulled from the file-tint family rather than invented, so a
 * segment reads as the same family as the rows that fill it. Media types all
 * share [DocumentFormatColors.mediaTint] in iconography, which is unusable in
 * a chart, so images take the media pink and video/audio borrow the nearest
 * distinct tints from the same family.
 */
object StorageChartColors {
    val documents: Color = Color(0xFF5B4FE8) // light-mode accent
    val images: Color = DocumentFormatColors.mediaTint
    val videos: Color = DocumentFormatColors.pptTint
    val audio: Color = DocumentFormatColors.excelTint
    val apps: Color = DocumentFormatColors.zipTint
    val other: Color = DocumentFormatColors.textTint
}
