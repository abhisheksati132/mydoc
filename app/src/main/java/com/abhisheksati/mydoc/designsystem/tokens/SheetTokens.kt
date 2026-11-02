package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
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

@Composable
fun rememberSheetPalette(): SheetPalette {
    val dark = AppTheme.isDark
    val oled = AppTheme.isOled
    return remember(dark, oled) {
        when {
            oled -> SheetPalette(
                sheet = Color(0xFF16130F),
                sheetEdge = Color(0xFF231F1A),
                ink = Color(0xFFF2EDE3),
                inkMuted = Color(0xFFA39B8D),
                rule = Color(0xFF373028),
                accent = Color(0xFF93A4FF),
                track = Color(0xFF201C18)
            )
            dark -> SheetPalette(
                sheet = Color(0xFF211D18),
                sheetEdge = Color(0xFF2E2923),
                ink = Color(0xFFF2EDE3),
                inkMuted = Color(0xFFA39B8D),
                rule = Color(0xFF3D362D),
                accent = Color(0xFF93A4FF),
                track = Color(0xFF2C2823)
            )
            else -> SheetPalette(
                sheet = Color(0xFFFCFAF4),
                sheetEdge = Color(0xFFEAE4D6),
                ink = Color(0xFF17150F),
                inkMuted = Color(0xFF6E6759),
                rule = Color(0xFFD9D2C3),
                accent = Color(0xFF2B4EFF),
                track = Color(0xFFE4DED0)
            )
        }
    }
}
