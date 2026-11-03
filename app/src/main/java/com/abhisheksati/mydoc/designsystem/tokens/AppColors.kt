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
    val inkPrimary: Color = ink,
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
        /** Folds a Material 3 scheme into the Apple Liquid Glass token model. */
        fun fromMaterialScheme(scheme: ColorScheme, isDark: Boolean = false): AppColors = AppColors(
            background = scheme.background,
            surface = scheme.surface,
            surfaceMuted = scheme.surfaceContainerHigh,
            line = scheme.outlineVariant,
            ink = scheme.onBackground,
            inkMuted = scheme.onSurfaceVariant,
            highlight = scheme.primaryContainer,
            accent = scheme.primary,
            onAccent = scheme.onPrimary,
            accentPink = if (isDark) Color(0xFFF43F5E) else Color(0xFFEC4899),
            accentCyan = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
            accentAmber = if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B),
            accentGreen = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
            documentKind = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444),
            onDocumentKind = Color(0xFFFFFFFF),
            documentContainer = if (isDark) Color(0xFF3B1519) else Color(0xFFFEE2E2),
            photoKind = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
            onPhotoKind = Color(0xFFFFFFFF),
            photoContainer = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF),
            videoKind = if (isDark) Color(0xFFFB923C) else Color(0xFFF97316),
            onVideoKind = Color(0xFFFFFFFF),
            videoContainer = if (isDark) Color(0xFF431407) else Color(0xFFFFEDD5),
            audioKind = if (isDark) Color(0xFF2DD4BF) else Color(0xFF14B8A6),
            onAudioKind = Color(0xFFFFFFFF),
            audioContainer = if (isDark) Color(0xFF042F2E) else Color(0xFFCCFBF1),
            // Low-alpha tint over a transparent Haze base: content supplies the
            // color (vibrancy); the tint only densifies the glass slightly.
            glassTint = scheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.42f else 0.55f),
            // White specular top arc in light (reflects light, not a dark stroke);
            // bottom stop is halved again inside GlassSurface.
            glassRimTop = if (isDark) Color(0x38FFFFFF) else Color(0x59FFFFFF),
            glassRimBottom = if (isDark) Color(0x20FFFFFF) else Color(0x14000000),
            glassInnerShadow = if (isDark) Color(0x0F000000) else Color(0x08000000),
            paperGrainColor = Color(0x00000000),
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

/** Apple Liquid Glass Light Theme: crisp slate-white canvas with luminous surfaces. */
val LightAppColors = AppColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF1F5F9),
    line = Color(0xFFE2E8F0),
    ink = Color(0xFF0F172A),
    inkMuted = Color(0xFF64748B),
    highlight = Color(0xFFEFF6FF),
    accent = Color(0xFF0066FF),
    onAccent = Color(0xFFFFFFFF),
    accentPink = Color(0xFFEC4899),
    accentCyan = Color(0xFF0284C7),
    accentAmber = Color(0xFFF59E0B),
    accentGreen = Color(0xFF10B981),

    documentKind = Color(0xFFEF4444),
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFFFEE2E2),

    photoKind = Color(0xFF6366F1),
    onPhotoKind = Color(0xFFFFFFFF),
    photoContainer = Color(0xFFEEF2FF),

    videoKind = Color(0xFFF97316),
    onVideoKind = Color(0xFFFFFFFF),
    videoContainer = Color(0xFFFFEDD5),

    audioKind = Color(0xFF14B8A6),
    onAudioKind = Color(0xFFFFFFFF),
    audioContainer = Color(0xFFCCFBF1),

    glassTint = Color(0x8CFFFFFF),
    glassRimTop = Color(0x59FFFFFF),
    glassRimBottom = Color(0x14000000),
    glassInnerShadow = Color(0x08000000),
    paperGrainColor = Color(0x00000000)
)

/** Apple Liquid Glass Dark Theme: obsidian glass canvas with luminous accents. */
val DarkAppColors = AppColors(
    background = Color(0xFF090D16),
    surface = Color(0xFF131A29),
    surfaceMuted = Color(0xFF1E283D),
    line = Color(0xFF2B374F),
    ink = Color(0xFFF8FAFC),
    inkMuted = Color(0xFF94A3B8),
    highlight = Color(0xFF1E293B),
    accent = Color(0xFF2979FF),
    onAccent = Color(0xFFFFFFFF),
    accentPink = Color(0xFFF43F5E),
    accentCyan = Color(0xFF38BDF8),
    accentAmber = Color(0xFFFBBF24),
    accentGreen = Color(0xFF34D399),

    documentKind = Color(0xFFF87171),
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFF3B1519),

    photoKind = Color(0xFF818CF8),
    onPhotoKind = Color(0xFFFFFFFF),
    photoContainer = Color(0xFF1E1B4B),

    videoKind = Color(0xFFFB923C),
    onVideoKind = Color(0xFFFFFFFF),
    videoContainer = Color(0xFF431407),

    audioKind = Color(0xFF2DD4BF),
    onAudioKind = Color(0xFFFFFFFF),
    audioContainer = Color(0xFF042F2E),

    glassTint = Color(0x6B1E283D),
    glassRimTop = Color(0x38FFFFFF),
    glassRimBottom = Color(0x20FFFFFF),
    glassInnerShadow = Color(0x0F000000),
    paperGrainColor = Color(0x00000000)
)

/** Pure AMOLED zero-nit obsidian variant. */
val BlackAppColors = DarkAppColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0D1117),
    surfaceMuted = Color(0xFF161B22),
    line = Color(0xFF30363D),
    ink = Color(0xFFF0F6FC),
    inkMuted = Color(0xFF8B949E),
    highlight = Color(0xFF1F242C),
    glassTint = Color(0x6B161B22),
    glassRimTop = Color(0x40FFFFFF),
    glassRimBottom = Color(0x26FFFFFF),
    glassInnerShadow = Color(0x0F000000),
    surfaceContainer = Color(0xFF0D1117),
    surfaceContainerHigh = Color(0xFF161B22),
    surfaceContainerLow = Color(0xFF000000),
    primaryContainer = Color(0xFF161B22),
    secondaryContainer = Color(0xFF161B22)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

fun kindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFF87171)
            MediaKind.PHOTO -> Color(0xFF818CF8)
            MediaKind.VIDEO -> Color(0xFFFB923C)
            MediaKind.AUDIO -> Color(0xFF2DD4BF)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFEF4444)
            MediaKind.PHOTO -> Color(0xFF6366F1)
            MediaKind.VIDEO -> Color(0xFFF97316)
            MediaKind.AUDIO -> Color(0xFF14B8A6)
        }
    }
}

fun kindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF3B1519)
            MediaKind.PHOTO -> Color(0xFF1E1B4B)
            MediaKind.VIDEO -> Color(0xFF431407)
            MediaKind.AUDIO -> Color(0xFF042F2E)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFEE2E2)
            MediaKind.PHOTO -> Color(0xFFEEF2FF)
            MediaKind.VIDEO -> Color(0xFFFFEDD5)
            MediaKind.AUDIO -> Color(0xFFCCFBF1)
        }
    }
}

fun onKindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return Color(0xFFFFFFFF)
}

fun onKindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFCA5A5)
            MediaKind.PHOTO -> Color(0xFFC7D2FE)
            MediaKind.VIDEO -> Color(0xFFFDBA74)
            MediaKind.AUDIO -> Color(0xFF99F6E4)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF991B1B)
            MediaKind.PHOTO -> Color(0xFF3730A3)
            MediaKind.VIDEO -> Color(0xFF9A3412)
            MediaKind.AUDIO -> Color(0xFF115E59)
        }
    }
}

/**
 * Editorial Document Format colors.
 * Refined, vibrant format badges designed for modern Liquid Glass contrast.
 */
object DocumentFormatColors {
    val pdfTint = Color(0xFFEF4444)     // Crimson
    val wordTint = Color(0xFF2563EB)    // Royal Blue
    val excelTint = Color(0xFF10B981)   // Emerald Green
    val pptTint = Color(0xFFF97316)     // Vibrant Orange
    val textTint = Color(0xFF64748B)    // Slate
    val mediaTint = Color(0xFF8B5CF6)   // Radiant Violet
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
        return if (isDark) Color(0xFF1E283D) else Color(0xFFF1F5F9)
    }
}
