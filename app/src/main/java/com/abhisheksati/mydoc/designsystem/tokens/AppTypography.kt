package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.R

/**
 * Single Source of Truth for Typography ("Shelves" Design System).
 *
 * Font: Bricolage Grotesque (Google Fonts, SIL OFL).
 * Type scale (sp / line height / weight / letter-spacing):
 * - display: 38 / 42 / 600 / -1.4sp (screen hero titles)
 * - title: 34 / 40 / 600 / -1.2sp (screen titles)
 * - heading: 26 / 30 / 600 / -0.8sp
 * - bodyLarge: 16 / 24 / 400 / 0.0sp
 * - body: 15 / 20 / 500 / 0.0sp
 * - label: 13 / 18 / 500 / 0.0sp
 * - caption: 12 / 16 / 500 / +0.6sp (ALL CAPS section labels)
 *
 * Tabular figures (tnum) applied to numbers, sizes, and counters.
 */
val BricolageFontFamily = FontFamily(
    Font(R.font.bricolage_grotesque, FontWeight.Normal),
    Font(R.font.bricolage_grotesque, FontWeight.Medium),
    Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque, FontWeight.Bold)
)

val InterFontFamily = BricolageFontFamily

@Immutable
data class AppTypography(
    val display: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.4).sp
    ),
    val title: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1.2).sp
    ),
    val heading: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.8).sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    val label: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    ),
    // Compatibility alias
    val headline: TextStyle = heading
)

val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

/**
 * Extension helper to apply OpenType Tabular Figures (tnum) to any TextStyle.
 */
fun TextStyle.withTabularFigures(): TextStyle {
    return this.copy(fontFeatureSettings = "tnum")
}
