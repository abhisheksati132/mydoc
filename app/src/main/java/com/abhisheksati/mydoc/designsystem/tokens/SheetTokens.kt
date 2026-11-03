package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The sheet palette: paper stock, ink and one signal accent. It is deliberately
 * separate from [AppColors] so the paper language can be prototyped on a single
 * surface without dragging the rest of the app with it.
 */
@Immutable
data class SheetPalette(
    val sheet: Color,
    val sheetEdge: Color,
    val ink: Color,
    val inkMuted: Color,
    val rule: Color,
    val accent: Color,
    val track: Color
)

/** Paper corners are cut, not rounded like a card. */
val PaperShape = RoundedCornerShape(3.dp)

/**
 * Present below a composable subtree that has opted into the sheet language.
 * Shared components (chips, section headers) read it to drop their vivid
 * decoration without changing Tools, which is still vivid.
 */
val LocalPaperPalette = staticCompositionLocalOf<SheetPalette?> { null }

/**
 * Remaps the app color roles onto paper. The canvas stays the app background so
 * Files and Settings sit on the same desk as the Home sheet.
 */
fun AppColors.papered(paper: SheetPalette): AppColors {
    val onAccent = if (paper.accent.luminance() > 0.5f) Color(0xFF121018) else Color.White
    return copy(
        surface = paper.sheet,
        surfaceMuted = paper.sheetEdge,
        line = paper.rule,
        ink = paper.ink,
        inkMuted = paper.inkMuted,
        highlight = paper.sheetEdge,
        accent = paper.accent,
        onAccent = onAccent,
        track = paper.track,
        textPrimary = paper.ink,
        textSecondary = paper.inkMuted,
        divider = paper.rule,
        cardBorder = paper.rule,
        accentVariant = paper.accent.copy(alpha = 0.14f),
        primaryContainer = paper.sheetEdge,
        onPrimaryContainer = paper.ink,
        secondaryContainer = paper.sheetEdge,
        onSecondaryContainer = paper.ink,
        surfaceContainer = paper.sheet,
        surfaceVariant = paper.sheetEdge,
        surfaceContainerHigh = paper.sheetEdge,
        surfaceContainerLow = paper.sheet,
        outline = paper.rule,
        outlineVariant = paper.rule
    )
}
