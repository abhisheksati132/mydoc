package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.abhisheksati.mydoc.domain.model.MediaKind

/**
 * Single source of truth for MyDoc color. "Paper under glass" design identity:
 * - Deep ink-navy dark theme (hero identity)
 * - Warm ivory paper light theme
 * - ONE accent color (Stamp Vermilion) used strictly for progress, primary actions, and selection
 *
 * Chrome surfaces (toolbars, pills, sheets) use refractive glass tokens.
 * Documents, preview tiles, and reading sheets use physical paper tokens.
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val line: Color,
    val ink: Color,
    val inkMuted: Color,
    val highlight: Color,
    val accent: Color,
    val onAccent: Color,
    val accentPink: Color = Color(0xFFD94324), // Stamp Vermilion accent variant
    val accentCyan: Color = Color(0xFF2C4A7F), // Ink navy variant
    val accentAmber: Color = Color(0xFFD97706),
    val accentGreen: Color = Color(0xFF286B49),

    // MediaKind Color Tokens (Harmonious editorial tags)
    val documentKind: Color,
    val onDocumentKind: Color,
    val documentContainer: Color,

    val photoKind: Color,
    val onPhotoKind: Color,
    val photoContainer: Color,

    val videoKind: Color,
    val onVideoKind: Color,
    val videoContainer: Color,

    val audioKind: Color,
    val onAudioKind: Color,
    val audioContainer: Color,

    // Glass System Tokens
    val glassTint: Color,
    val glassRimTop: Color,
    val glassRimBottom: Color,
    val glassInnerShadow: Color,
    val paperGrainColor: Color,

    // Semantic mappings
    val textPrimary: Color = ink,
    val textSecondary: Color = inkMuted,
    val textSubtle: Color = inkMuted.copy(alpha = 0.72f),
    val inkSecondary: Color = inkMuted,
    val surfaceElevated: Color = surface,
    val divider: Color = line,
    val cardBorder: Color = line,
    val track: Color = ink.copy(alpha = 0.12f),
    val accentVariant: Color = accent.copy(alpha = 0.14f),
    val accentContainer: Color = accent.copy(alpha = 0.14f),
    val error: Color = Color(0xFFD94324),
    val warning: Color = Color(0xFFD97706),
    val scrim: Color = Color(0x99000000),

    // Container mappings
    val primaryContainer: Color = surfaceMuted,
    val onPrimaryContainer: Color = ink,
    val secondaryContainer: Color = surfaceMuted,
    val onSecondaryContainer: Color = ink,
    val surfaceContainer: Color = surface,
    val surfaceVariant: Color = surfaceMuted,
    val surfaceContainerHigh: Color = surfaceMuted,
    val surfaceContainerLow: Color = surface,
    val outline: Color = line,
    val outlineVariant: Color = line
) {
    fun kindColor(kind: MediaKind): Color = when (kind) {
        MediaKind.DOCUMENT -> documentKind
        MediaKind.PHOTO -> photoKind
        MediaKind.VIDEO -> videoKind
        MediaKind.AUDIO -> audioKind
    }

    fun kindOnColor(kind: MediaKind): Color = when (kind) {
        MediaKind.DOCUMENT -> onDocumentKind
        MediaKind.PHOTO -> onPhotoKind
        MediaKind.VIDEO -> onVideoKind
        MediaKind.AUDIO -> onAudioKind
    }

    fun kindContainer(kind: MediaKind): Color = when (kind) {
        MediaKind.DOCUMENT -> documentContainer
        MediaKind.PHOTO -> photoContainer
        MediaKind.VIDEO -> videoContainer
        MediaKind.AUDIO -> audioContainer
    }

    companion object {
        /** Folds a Material 3 scheme into the "Paper under glass" token model. */
        fun fromMaterialScheme(scheme: ColorScheme): AppColors = AppColors(
            background = scheme.background,
            surface = scheme.surface,
            surfaceMuted = scheme.surfaceContainerHigh,
            line = scheme.outlineVariant,
            ink = scheme.onBackground,
            inkMuted = scheme.onSurfaceVariant,
            highlight = scheme.primaryContainer,
            accent = scheme.primary,
            onAccent = scheme.onPrimary,
            accentPink = Color(0xFFD94324),
            accentCyan = Color(0xFF2C4A7F),
            accentAmber = Color(0xFFD97706),
            accentGreen = Color(0xFF286B49),
            documentKind = scheme.primary,
            onDocumentKind = scheme.onPrimary,
            documentContainer = scheme.primaryContainer,
            photoKind = scheme.tertiary,
            onPhotoKind = scheme.onTertiary,
            photoContainer = scheme.tertiaryContainer,
            videoKind = scheme.secondary,
            onVideoKind = scheme.onSecondary,
            videoContainer = scheme.secondaryContainer,
            audioKind = scheme.primary,
            onAudioKind = scheme.onPrimary,
            audioContainer = scheme.tertiaryContainer,
            glassTint = scheme.surface.copy(alpha = 0.78f),
            glassRimTop = Color(0x38FFFFFF),
            glassRimBottom = Color(0x40000000),
            glassInnerShadow = Color(0x20000000),
            paperGrainColor = scheme.onBackground.copy(alpha = 0.05f),
            textPrimary = scheme.onBackground,
            textSecondary = scheme.onSurfaceVariant,
            divider = scheme.outlineVariant,
            cardBorder = scheme.outlineVariant,
            track = scheme.surfaceVariant,
            accentVariant = scheme.primary.copy(alpha = 0.16f),
            error = scheme.error,
            primaryContainer = scheme.primaryContainer,
            onPrimaryContainer = scheme.onPrimaryContainer,
            secondaryContainer = scheme.secondaryContainer,
            onSecondaryContainer = scheme.onSecondaryContainer,
            surfaceContainer = scheme.surfaceContainer,
            surfaceVariant = scheme.surfaceVariant,
            surfaceContainerHigh = scheme.surfaceContainerHigh,
            surfaceContainerLow = scheme.surfaceContainerLow,
            outline = scheme.outline,
            outlineVariant = scheme.outlineVariant
        )
    }
}

/** Warm ivory paper light theme: authentic physical paper palette. */
val LightAppColors = AppColors(
    background = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFDF9),
    surfaceMuted = Color(0xFFF0ECE1),
    line = Color(0xFFE5DFD3),
    ink = Color(0xFF191614),
    inkMuted = Color(0xFF6B645C),
    highlight = Color(0xFFF5EFE4),
    // Stamp Vermilion accent
    accent = Color(0xFFD94324),
    onAccent = Color(0xFFFFFFFF),
    accentPink = Color(0xFFD94324),
    accentCyan = Color(0xFF2C4A7F),
    accentAmber = Color(0xFFD97706),
    accentGreen = Color(0xFF286B49),

    documentKind = Color(0xFFD94324),
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFFFCEBE7),

    photoKind = Color(0xFF2C4A7F),
    onPhotoKind = Color(0xFFFFFFFF),
    photoContainer = Color(0xFFE8EEF8),

    videoKind = Color(0xFF8B5A2B),
    onVideoKind = Color(0xFFFFFFFF),
    videoContainer = Color(0xFFF7EFE6),

    audioKind = Color(0xFF286B49),
    onAudioKind = Color(0xFFFFFFFF),
    audioContainer = Color(0xFFE7F3EC),

    glassTint = Color(0xD8FAF7F2),
    glassRimTop = Color(0x99FFFFFF),
    glassRimBottom = Color(0x18000000),
    glassInnerShadow = Color(0x0A000000),
    paperGrainColor = Color(0x0A191614)
)

/** Deep ink-navy dark theme (Hero Identity): dark studio canvas with warm paper text. */
val DarkAppColors = AppColors(
    background = Color(0xFF0B0E14),
    surface = Color(0xFF141923),
    surfaceMuted = Color(0xFF1D2433),
    line = Color(0xFF263043),
    ink = Color(0xFFF2F5F8),
    inkMuted = Color(0xFF8C9AB0),
    highlight = Color(0xFF20293A),
    // Warm Vermilion accent
    accent = Color(0xFFF05435),
    onAccent = Color(0xFFFFFFFF),
    accentPink = Color(0xFFF05435),
    accentCyan = Color(0xFF5B82C2),
    accentAmber = Color(0xFFF59E0B),
    accentGreen = Color(0xFF3EA874),

    documentKind = Color(0xFFF05435),
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFF331914),

    photoKind = Color(0xFF6B8DC7),
    onPhotoKind = Color(0xFFFFFFFF),
    photoContainer = Color(0xFF152238),

    videoKind = Color(0xFFD49B60),
    onVideoKind = Color(0xFFFFFFFF),
    videoContainer = Color(0xFF2E1F12),

    audioKind = Color(0xFF4DBB84),
    onAudioKind = Color(0xFFFFFFFF),
    audioContainer = Color(0xFF102B1D),

    glassTint = Color(0xD0121722),
    glassRimTop = Color(0x38FFFFFF),
    glassRimBottom = Color(0x66000000),
    glassInnerShadow = Color(0x33000000),
    paperGrainColor = Color(0x08F2F5F8)
)

/** Pure AMOLED zero-nit obsidian variant. */
val BlackAppColors = DarkAppColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0E14),
    surfaceMuted = Color(0xFF141923),
    line = Color(0xFF1E2636),
    ink = Color(0xFFF2F5F8),
    inkMuted = Color(0xFF8C9AB0),
    highlight = Color(0xFF161D2B),
    glassTint = Color(0xE007090E),
    glassRimTop = Color(0x30FFFFFF),
    glassRimBottom = Color(0x80000000),
    glassInnerShadow = Color(0x40000000),
    surfaceContainer = Color(0xFF0B0E14),
    surfaceContainerHigh = Color(0xFF141923),
    surfaceContainerLow = Color(0xFF000000),
    primaryContainer = Color(0xFF141923),
    secondaryContainer = Color(0xFF141923)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

fun kindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFF05435)
            MediaKind.PHOTO -> Color(0xFF6B8DC7)
            MediaKind.VIDEO -> Color(0xFFD49B60)
            MediaKind.AUDIO -> Color(0xFF4DBB84)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFD94324)
            MediaKind.PHOTO -> Color(0xFF2C4A7F)
            MediaKind.VIDEO -> Color(0xFF8B5A2B)
            MediaKind.AUDIO -> Color(0xFF286B49)
        }
    }
}

fun kindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF331914)
            MediaKind.PHOTO -> Color(0xFF152238)
            MediaKind.VIDEO -> Color(0xFF2E1F12)
            MediaKind.AUDIO -> Color(0xFF102B1D)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFCEBE7)
            MediaKind.PHOTO -> Color(0xFFE8EEF8)
            MediaKind.VIDEO -> Color(0xFFF7EFE6)
            MediaKind.AUDIO -> Color(0xFFE7F3EC)
        }
    }
}

fun onKindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return Color(0xFFFFFFFF)
}

fun onKindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFFA694)
            MediaKind.PHOTO -> Color(0xFFB5CEF7)
            MediaKind.VIDEO -> Color(0xFFF3CE9F)
            MediaKind.AUDIO -> Color(0xFFA2E8C4)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF8A210B)
            MediaKind.PHOTO -> Color(0xFF18315B)
            MediaKind.VIDEO -> Color(0xFF5E3917)
            MediaKind.AUDIO -> Color(0xFF14472F)
        }
    }
}

/**
 * Editorial Document Format colors.
 * Refined, tactile format badges designed for readability on paper.
 */
object DocumentFormatColors {
    val pdfTint = Color(0xFFD94324)     // Stamp Vermilion
    val wordTint = Color(0xFF2C4A7F)    // Deep Indigo
    val excelTint = Color(0xFF286B49)   // Forest Green
    val pptTint = Color(0xFFC26116)     // Warm Amber
    val textTint = Color(0xFF57534E)    // Slate Gray
    val mediaTint = Color(0xFF7C3AED)   // Plum
    val zipTint = Color(0xFF475569)     // Charcoal

    fun getTint(extension: String): Color {
        return when (extension.lowercase()) {
            "pdf" -> pdfTint
            "docx", "doc" -> wordTint
            "xlsx", "xls", "csv" -> excelTint
            "pptx", "ppt" -> pptTint
            "txt", "md", "log", "json", "xml" -> textTint
            "jpg", "jpeg", "png", "webp", "gif", "mp4", "mp3" -> mediaTint
            "zip", "rar", "7z", "tar", "gz" -> zipTint
            else -> textTint
        }
    }

    fun getContainer(extension: String, isDark: Boolean = false): Color {
        return if (isDark) Color(0xFF1D2433) else Color(0xFFF0ECE1)
    }
}
