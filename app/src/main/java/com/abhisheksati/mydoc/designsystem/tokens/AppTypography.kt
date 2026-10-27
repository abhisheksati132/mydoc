package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.R

/**
 * Clean, native Android system typography system.
 * Uses standard, highly-legible Inter / system sans-serif across all headings, titles, and body text.
 */

@OptIn(ExperimentalTextApi::class)
val InterFontFamily: FontFamily = runCatching {
    FontFamily(
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
        ),
        Font(
            resId = R.font.inter,
            weight = FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800)
            )
        )
    )
}.getOrElse { FontFamily.SansSerif }

// Native alias for backwards compatibility
val BricolageFontFamily: FontFamily = InterFontFamily

@Immutable
data class AppTypography(
    val display: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1.0).sp
    ),
    val title: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    val heading: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
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
        letterSpacing = 0.2.sp
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
