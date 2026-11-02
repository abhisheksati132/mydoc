package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
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
 * Editorial type system: Bricolage Grotesque carries the brand (display, titles, headings),
 * Inter carries the work (body, labels, captions, numbers).
 *
 * Sizes are fixed here on purpose. Screens must not override fontSize/weight with
 * `.copy(...)` — pick the closest role instead, so the app keeps one voice.
 */

private val interWeights = listOf(400 to FontWeight.Normal, 500 to FontWeight.Medium, 600 to FontWeight.SemiBold, 700 to FontWeight.Bold, 800 to FontWeight.ExtraBold)
private val bricolageWeights = listOf(400 to FontWeight.Normal, 600 to FontWeight.SemiBold, 700 to FontWeight.Bold, 800 to FontWeight.ExtraBold)

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int, weights: List<Pair<Int, FontWeight>>): FontFamily = runCatching {
    FontFamily(
        weights.map { (value, weight) ->
            Font(
                resId = resId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(value))
            )
        }
    )
}.getOrElse { FontFamily.SansSerif }

val InterFontFamily: FontFamily = variableFamily(R.font.inter, interWeights)

val BricolageFontFamily: FontFamily = variableFamily(R.font.bricolage_grotesque, bricolageWeights)

@Immutable
data class AppTypography(
    val display: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.0).sp
    ),
    val title: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    val heading: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    /** Compact Bricolage title. Below this size, titles are Inter body-strong. */
    val titleSmall: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    /** Small, wide-tracked section label. Use for "RECENT DOCUMENTS", group headers. */
    val overline: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp
    ),
    val wordmark: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.6).sp
    ),
    /** Root-tab header title. */
    val screenTitle: TextStyle = TextStyle(
        fontFamily = BricolageFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    val bodyLargeStrong: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
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
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    val bodyStrong: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
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
    val captionStrong: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    /** Smallest legible tier: metadata, badges, dense counters. */
    val micro: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    ),
    // Compatibility alias
    val headline: TextStyle = heading
)

val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

/**
 * Maps the Material 3 type scale onto the legacy role names so screens that
 * still read LocalAppTypography render the same Roboto scale as the rest of
 * the app.
 */
@Composable
fun appTypographyFromMaterial(base: Typography): AppTypography = AppTypography(
    display = base.displaySmall,
    title = base.headlineSmall,
    heading = base.titleLarge,
    titleSmall = base.titleMedium,
    overline = base.labelSmall,
    wordmark = base.titleLarge,
    screenTitle = base.headlineSmall,
    bodyLarge = base.bodyLarge,
    bodyLargeStrong = base.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    body = base.bodyMedium,
    bodyMedium = base.bodyMedium.copy(fontWeight = FontWeight.Medium),
    bodyStrong = base.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
    label = base.labelLarge,
    caption = base.bodySmall,
    captionStrong = base.bodySmall.copy(fontWeight = FontWeight.SemiBold),
    micro = base.labelSmall,
    headline = base.titleLarge
)

/**
 * Extension helper to apply OpenType Tabular Figures (tnum) to any TextStyle.
 */
fun TextStyle.withTabularFigures(): TextStyle {
    return this.copy(fontFeatureSettings = "tnum")
}
