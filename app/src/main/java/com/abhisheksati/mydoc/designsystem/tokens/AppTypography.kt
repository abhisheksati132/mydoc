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
 * Editorial "Ink & Seal" type system:
 * - Fraunces Serif carries the editorial voice (display, titles, document names)
 * - Inter Grotesk carries the interface (body, labels, captions, buttons)
 * - JetBrains Mono carries the metadata (file size, page counts, timestamps, technical info)
 */

private val interWeights = listOf(
    400 to FontWeight.Normal,
    500 to FontWeight.Medium,
    600 to FontWeight.SemiBold,
    700 to FontWeight.Bold,
    800 to FontWeight.ExtraBold
)

private val frauncesWeights = listOf(
    400 to FontWeight.Normal,
    500 to FontWeight.Medium,
    600 to FontWeight.SemiBold,
    700 to FontWeight.Bold,
    800 to FontWeight.ExtraBold
)

private val monoWeights = listOf(
    400 to FontWeight.Normal,
    500 to FontWeight.Medium,
    600 to FontWeight.SemiBold,
    700 to FontWeight.Bold
)

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(
    resId: Int,
    weights: List<Pair<Int, FontWeight>>,
    fallback: FontFamily = FontFamily.SansSerif
): FontFamily = runCatching {
    FontFamily(
        weights.map { (value, weight) ->
            Font(
                resId = resId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(value))
            )
        }
    )
}.getOrElse { fallback }

val InterFontFamily: FontFamily = variableFamily(R.font.inter, interWeights, FontFamily.SansSerif)
val FrauncesFontFamily: FontFamily = variableFamily(R.font.fraunces, frauncesWeights, FontFamily.Serif)
val JetBrainsMonoFontFamily: FontFamily = variableFamily(R.font.jetbrains_mono, monoWeights, FontFamily.Monospace)

@Immutable
data class AppTypography(
    // --- Fraunces Serif Display Tier ---
    val display: TextStyle = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    val title: TextStyle = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    val heading: TextStyle = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    /** Compact title. */
    val titleSmall: TextStyle = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp
    ),
    /** Document item title. */
    val documentName: TextStyle = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    val wordmark: TextStyle = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp
    ),
    /** Root-tab header title. */
    val screenTitle: TextStyle = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),

    // --- Inter Clean Grotesk UI Tier ---
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    val bodyLargeStrong: TextStyle = TextStyle(
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
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    val bodyStrong: TextStyle = TextStyle(
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
    /** Small, wide-tracked section label. Use for "RECENT DOCUMENTS", group headers. */
    val overline: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    val captionStrong: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    /** Smallest legible tier: badges, dense counters. */
    val micro: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp
    ),

    // --- JetBrains Mono Technical Metadata Tier ---
    val metadata: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = (-0.1).sp,
        fontFeatureSettings = "tnum"
    ),
    val metadataStrong: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = (-0.1).sp,
        fontFeatureSettings = "tnum"
    ),
    val code: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),

    // Compatibility alias
    val headline: TextStyle = heading
)

val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

/**
 * Maps the Material 3 type scale onto the role names.
 */
@Composable
fun appTypographyFromMaterial(base: Typography): AppTypography = AppTypography(
    display = base.displaySmall,
    title = base.headlineSmall,
    heading = base.titleLarge,
    titleSmall = base.titleMedium,
    documentName = base.titleMedium,
    overline = base.labelSmall,
    wordmark = base.titleLarge,
    screenTitle = base.headlineSmall,
    bodyLarge = base.bodyLarge,
    bodyLargeStrong = base.bodyLarge.copy(fontWeight = FontWeight.Normal),
    body = base.bodyMedium,
    bodyMedium = base.bodyMedium.copy(fontWeight = FontWeight.Medium),
    bodyStrong = base.bodyMedium.copy(fontWeight = FontWeight.Normal),
    label = base.labelLarge,
    caption = base.bodySmall,
    captionStrong = base.bodySmall.copy(fontWeight = FontWeight.Normal),
    micro = base.labelSmall,
    headline = base.titleLarge
)

/**
 * The brand scale expressed in Material 3's own roles, so `MaterialTheme.typography.*` resolves
 * to Fraunces / Inter / JetBrains Mono instead of the framework default. Without this the whole
 * app renders Roboto and only components that opt into AppTypography get the editorial voice.
 */
fun brandMaterialTypography(): Typography {
    val brand = AppTypography()
    return Typography(
        displayLarge = brand.display,
        displayMedium = brand.display.copy(fontSize = 28.sp, lineHeight = 34.sp),
        displaySmall = brand.display.copy(fontSize = 24.sp, lineHeight = 30.sp),
        headlineLarge = brand.title,
        headlineMedium = brand.screenTitle,
        headlineSmall = brand.title,
        titleLarge = brand.heading,
        titleMedium = brand.documentName,
        titleSmall = brand.bodyLargeStrong,
        bodyLarge = brand.bodyLarge,
        bodyMedium = brand.body,
        bodySmall = brand.caption,
        labelLarge = brand.label,
        labelMedium = brand.captionStrong,
        labelSmall = brand.overline,
    )
}

/**
 * Extension helper to apply OpenType Tabular Figures (tnum) to any TextStyle.
 */
fun TextStyle.withTabularFigures(): TextStyle {
    return this.copy(fontFeatureSettings = "tnum")
}
