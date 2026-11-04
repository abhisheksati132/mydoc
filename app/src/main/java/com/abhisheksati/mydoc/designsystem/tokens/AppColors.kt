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
            accentPink = if (isDark) Color(0xFFFF375F) else Color(0xFFFF2D55), // systemPink
            accentCyan = if (isDark) Color(0xFF64D2FF) else Color(0xFF32ADE6), // systemCyan
            accentAmber = if (isDark) Color(0xFFFF9F0A) else Color(0xFFFF9500), // systemOrange
            accentGreen = if (isDark) Color(0xFF30D158) else Color(0xFF34C759), // systemGreen
            documentKind = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30), // systemRed
            onDocumentKind = Color(0xFFFFFFFF),
            documentContainer = if (isDark) Color(0xFF3A1D1B) else Color(0xFFFFE5E3),
            photoKind = if (isDark) Color(0xFF5E5CE6) else Color(0xFF5856D6), // systemIndigo
            onPhotoKind = Color(0xFFFFFFFF),
            photoContainer = if (isDark) Color(0xFF1E1B4B) else Color(0xFFE3E2FA),
            videoKind = if (isDark) Color(0xFFFF9F0A) else Color(0xFFFF9500), // systemOrange
            onVideoKind = Color(0xFFFFFFFF),
            videoContainer = if (isDark) Color(0xFF3A2A10) else Color(0xFFFFEAD1),
            audioKind = if (isDark) Color(0xFF40C8E0) else Color(0xFF30B0C7), // systemTeal
            onAudioKind = if (isDark) Color(0xFF00303A) else Color(0xFFFFFFFF),
            audioContainer = if (isDark) Color(0xFF10353D) else Color(0xFFD9F1F7),
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

/** Apple Liquid Glass Light Theme: neutral iOS chrome, systemBlue accent, color from content. */
val LightAppColors = AppColors(
    background = Color(0xFFF2F2F7),        // systemGroupedBackground
    surface = Color(0xFFFFFFFF),           // systemBackground
    surfaceMuted = Color(0xFFE5E5EA),      // systemGray6
    line = Color(0xFFC6C6C8),              // separator
    ink = Color(0xFF000000),               // label
    inkMuted = Color(0xFF6C7076),          // secondaryLabel
    highlight = Color(0xFFD6EBFF),         // systemBlue tint
    accent = Color(0xFF007AFF),            // systemBlue
    onAccent = Color(0xFFFFFFFF),
    accentPink = Color(0xFFFF2D55),        // systemPink
    accentCyan = Color(0xFF32ADE6),        // systemCyan
    accentAmber = Color(0xFFFF9500),       // systemOrange
    accentGreen = Color(0xFF34C759),       // systemGreen

    documentKind = Color(0xFFFF3B30),      // systemRed (PDF)
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFFFFE5E3),

    photoKind = Color(0xFF5856D6),         // systemIndigo (photo)
    onPhotoKind = Color(0xFFFFFFFF),
    photoContainer = Color(0xFFE3E2FA),

    videoKind = Color(0xFFFF9500),         // systemOrange (video)
    onVideoKind = Color(0xFFFFFFFF),
    videoContainer = Color(0xFFFFEAD1),

    audioKind = Color(0xFF30B0C7),         // systemTeal (audio)
    onAudioKind = Color(0xFFFFFFFF),
    audioContainer = Color(0xFFD9F1F7),

    glassTint = Color(0x8CFFFFFF),
    glassRimTop = Color(0x59FFFFFF),
    glassRimBottom = Color(0x14000000),
    glassInnerShadow = Color(0x08000000),
    paperGrainColor = Color(0x00000000)
)

/** Apple Liquid Glass Dark Theme: graphite chrome, brighter system accents. */
val DarkAppColors = AppColors(
    background = Color(0xFF000000),        // systemBackground (dark)
    surface = Color(0xFF1C1C1E),           // secondarySystemBackground
    surfaceMuted = Color(0xFF2C2C2E),      // tertiarySystemBackground / elevated
    line = Color(0xFF38383A),              // separator (dark)
    ink = Color(0xFFFFFFFF),               // label
    inkMuted = Color(0xFF98989D),          // secondaryLabel
    highlight = Color(0xFF0A3A6B),         // systemBlue tint (dark)
    accent = Color(0xFF0A84FF),            // systemBlue (dark)
    onAccent = Color(0xFFFFFFFF),
    accentPink = Color(0xFFFF375F),        // systemPink (dark)
    accentCyan = Color(0xFF64D2FF),        // systemCyan (dark)
    accentAmber = Color(0xFFFF9F0A),       // systemOrange (dark)
    accentGreen = Color(0xFF30D158),       // systemGreen (dark)

    documentKind = Color(0xFFFF453A),      // systemRed (dark)
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFF3A1D1B),

    photoKind = Color(0xFF5E5CE6),         // systemIndigo (dark)
    onPhotoKind = Color(0xFFFFFFFF),
    photoContainer = Color(0xFF1E1B4B),

    videoKind = Color(0xFFFF9F0A),         // systemOrange (dark)
    onVideoKind = Color(0xFFFFFFFF),
    videoContainer = Color(0xFF3A2A10),

    audioKind = Color(0xFF40C8E0),         // systemTeal (dark)
    onAudioKind = Color(0xFF00303A),
    audioContainer = Color(0xFF10353D),

    glassTint = Color(0x6B2C2C2E),
    glassRimTop = Color(0x38FFFFFF),
    glassRimBottom = Color(0x20FFFFFF),
    glassInnerShadow = Color(0x0F000000),
    paperGrainColor = Color(0x00000000)
)

/** Pure AMOLED zero-nit obsidian variant. */
val BlackAppColors = DarkAppColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0B0D),
    surfaceMuted = Color(0xFF1C1C1E),
    line = Color(0xFF2A2A2C),
    ink = Color(0xFFFFFFFF),
    inkMuted = Color(0xFF98989D),
    highlight = Color(0xFF0A3A6B),
    glassTint = Color(0x6B1C1C1E),
    glassRimTop = Color(0x40FFFFFF),
    glassRimBottom = Color(0x26FFFFFF),
    glassInnerShadow = Color(0x0F000000),
    surfaceContainer = Color(0xFF0B0B0D),
    surfaceContainerHigh = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF000000),
    primaryContainer = Color(0xFF1C1C1E),
    secondaryContainer = Color(0xFF1C1C1E)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

fun kindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFF453A)  // systemRed
            MediaKind.PHOTO -> Color(0xFF5E5CE6)     // systemIndigo
            MediaKind.VIDEO -> Color(0xFFFF9F0A)     // systemOrange
            MediaKind.AUDIO -> Color(0xFF40C8E0)     // systemTeal
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFF3B30)  // systemRed
            MediaKind.PHOTO -> Color(0xFF5856D6)     // systemIndigo
            MediaKind.VIDEO -> Color(0xFFFF9500)     // systemOrange
            MediaKind.AUDIO -> Color(0xFF30B0C7)     // systemTeal
        }
    }
}

fun kindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF3A1D1B)
            MediaKind.PHOTO -> Color(0xFF1E1B4B)
            MediaKind.VIDEO -> Color(0xFF3A2A10)
            MediaKind.AUDIO -> Color(0xFF10353D)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFFE5E3)
            MediaKind.PHOTO -> Color(0xFFE3E2FA)
            MediaKind.VIDEO -> Color(0xFFFFEAD1)
            MediaKind.AUDIO -> Color(0xFFD9F1F7)
        }
    }
}

fun onKindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return Color(0xFFFFFFFF)
}

fun onKindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFF9A93)
            MediaKind.PHOTO -> Color(0xFFB9B7F5)
            MediaKind.VIDEO -> Color(0xFFFFC173)
            MediaKind.AUDIO -> Color(0xFF8FE0F0)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFB3261E)
            MediaKind.PHOTO -> Color(0xFF3634A0)
            MediaKind.VIDEO -> Color(0xFF8A5200)
            MediaKind.AUDIO -> Color(0xFF00667A)
        }
    }
}

/**
 * Editorial Document Format colors.
 * Refined, vibrant format badges designed for modern Liquid Glass contrast.
 */
object DocumentFormatColors {
    val pdfTint = Color(0xFFFF3B30)     // systemRed
    val wordTint = Color(0xFF007AFF)    // systemBlue
    val excelTint = Color(0xFF34C759)   // systemGreen
    val pptTint = Color(0xFFFF9500)     // systemOrange
    val textTint = Color(0xFF8E8E93)    // systemGray
    val mediaTint = Color(0xFFAF52DE)   // systemPurple
    val zipTint = Color(0xFFA2845E)     // systemBrown

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
