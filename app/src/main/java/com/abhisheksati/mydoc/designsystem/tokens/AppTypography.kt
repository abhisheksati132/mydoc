package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.R

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation

/**
 * Typography System: Balanced Dual-Typeface Pairing.
 *
 * - Display / Titles / Headings: Bricolage Grotesque (Google Fonts, SIL OFL).
 *   Expressive modern grotesque display face, calibrated with FontVariation
 *   to avoid heavy 800-weight master blowouts.
 * - Body / Labels / Captions / UI: Inter (SIL OFL).
 *   Neutral, ultra-legible UI sans with open apertures and tall x-height,
 *   engineered specifically for screen reading at 12–16sp.
 *
 * Type scale (sp / line height / weight / letter-spacing):
 * - display: 36 / 42 / 600 / -0.6sp (hero titles, wordmark)
 * - title: 30 / 36 / 600 / -0.4sp (screen titles, sheet titles)
 * - heading: 22 / 28 / 600 / -0.2sp (card headers, dialog titles)
 * - bodyLarge: 16 / 24 / 400 / 0.0sp (intro text, prominent prompts)
 * - body: 14 / 20 / 400 / 0.0sp (primary body, file names, list items)
 * - label: 13 / 18 / 500 / +0.1sp (buttons, chips, tab labels)
 * - caption: 12 / 16 / 500 / +0.3sp (badges, timestamps, file sizes)
 *
 * Tabular figures (tnum) applied to numbers, sizes, and counters.
 */
@OptIn(ExperimentalTextApi::class)
val BricolageFontFamily = FontFamily(
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.Setting("opsz", 28f)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.Setting("opsz", 36f)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.Setting("opsz", 48f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val InterFontFamily = FontFamily(
    Font(
        resId = R.font.inter,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400)
        )
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500)
        )
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600)
        )
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700)
        )
    )
)

@Immutable
data class AppTypography(
    val display: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp
    ),
    val title: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp
    ),
    val heading: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    val label: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
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
