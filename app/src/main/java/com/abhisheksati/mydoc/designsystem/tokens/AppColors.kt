package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

import com.abhisheksati.mydoc.domain.model.MediaKind

/**
 * Single source of truth for MyDoc color. Vivid premium studio:
 * clean bright canvas, electric violet anchor, and a full spectrum of
 * secondary hues (pink, cyan, amber) used as gradients and glowing tiles.
 *
 * Rules:
 * - Every functional area owns a saturated hue; color is information AND joy.
 * - Gradients are the primary decoration language (see VividGradients).
 * - Soft colored glows replace hairlines for elevation.
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
    val accentPink: Color = Color(0xFFEC4899),
    val accentCyan: Color = Color(0xFF06B6D4),
    val accentAmber: Color = Color(0xFFF59E0B),
    val accentGreen: Color = Color(0xFF22C55E),

    // MediaKind Color Tokens
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

    // Semantic mappings
    val textPrimary: Color = ink,
    val textSecondary: Color = inkMuted,
    val divider: Color = line,
    val cardBorder: Color = line,
    // Unfilled part of a progress gauge or scrubber. A hairline's line token is too
    // close to the surface to read as a track, so this is ink at low alpha instead.
    val track: Color = ink.copy(alpha = 0.16f),
    val accentVariant: Color = accent.copy(alpha = 0.16f),
    val error: Color = Color(0xFFE5484D),
    val warning: Color = Color(0xFFFFB300),
    val scrim: Color = Color(0x88000000),

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
        /** Folds a Material 3 scheme (dynamic or fallback) into the app's token model. */
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
            accentPink = scheme.tertiary,
            accentCyan = scheme.secondary,
            accentAmber = Color(0xFFF59E0B),
            accentGreen = Color(0xFF22C55E),
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

val LightAppColors = AppColors(
    background = Color(0xFFF8F8FD),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF1F0FA),
    line = Color(0xFFE4E2F0),
    ink = Color(0xFF14121F),
    inkMuted = Color(0xFF5D5A6E),
    highlight = Color(0xFFE9E4FF),
    // MyDoc brand violet — electric, saturated, ours.
    accent = Color(0xFF6C3DF5),
    onAccent = Color(0xFFFFFFFF),
    accentPink = Color(0xFFEC4899),
    accentCyan = Color(0xFF06B6D4),
    accentAmber = Color(0xFFF59E0B),
    accentGreen = Color(0xFF22C55E),

    documentKind = Color(0xFF6C3DF5),
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFFEDE7FF),

    photoKind = Color(0xFFEC4899),
    onPhotoKind = Color(0xFFFFFFFF),
    photoContainer = Color(0xFFFFE3F1),

    videoKind = Color(0xFFF97316),
    onVideoKind = Color(0xFFFFFFFF),
    videoContainer = Color(0xFFFFE7D6),

    audioKind = Color(0xFF10B981),
    onAudioKind = Color(0xFFFFFFFF),
    audioContainer = Color(0xFFD6F8EC)
)

val DarkAppColors = AppColors(
    background = Color(0xFF0C0A14),
    surface = Color(0xFF171422),
    surfaceMuted = Color(0xFF221E33),
    line = Color(0xFF2E2945),
    ink = Color(0xFFF4F2FF),
    inkMuted = Color(0xFFA9A4C2),
    highlight = Color(0xFF322A63),
    // MyDoc brand violet — neon dark variant
    accent = Color(0xFFA78BFA),
    onAccent = Color(0xFF120C26),
    accentPink = Color(0xFFF472B6),
    accentCyan = Color(0xFF22D3EE),
    accentAmber = Color(0xFFFBBF24),
    accentGreen = Color(0xFF4ADE80),

    documentKind = Color(0xFFA78BFA),
    onDocumentKind = Color(0xFF120C26),
    documentContainer = Color(0xFF271E56),

    photoKind = Color(0xFFF472B6),
    onPhotoKind = Color(0xFF2A0C1B),
    photoContainer = Color(0xFF42172F),

    videoKind = Color(0xFFFB923C),
    onVideoKind = Color(0xFF2A1206),
    videoContainer = Color(0xFF42240F),

    audioKind = Color(0xFF34D399),
    onAudioKind = Color(0xFF06281C),
    audioContainer = Color(0xFF10382A)
)

val BlackAppColors = DarkAppColors.copy(
    background = Color(0xFF000000), // Pure AMOLED zero-nit background
    surface = Color(0xFF0B0912),     // Deep obsidian card surface
    surfaceMuted = Color(0xFF161322),
    line = Color(0xFF221E33),
    surfaceContainer = Color(0xFF0B0912),
    surfaceContainerHigh = Color(0xFF161322),
    surfaceContainerLow = Color(0xFF000000),
    primaryContainer = Color(0xFF161322),
    secondaryContainer = Color(0xFF161322)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

fun kindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFA78BFA)
            MediaKind.PHOTO -> Color(0xFFF472B6)
            MediaKind.VIDEO -> Color(0xFFFB923C)
            MediaKind.AUDIO -> Color(0xFF34D399)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF6C3DF5)
            MediaKind.PHOTO -> Color(0xFFEC4899)
            MediaKind.VIDEO -> Color(0xFFF97316)
            MediaKind.AUDIO -> Color(0xFF10B981)
        }
    }
}

fun kindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF271E56)
            MediaKind.PHOTO -> Color(0xFF42172F)
            MediaKind.VIDEO -> Color(0xFF42240F)
            MediaKind.AUDIO -> Color(0xFF10382A)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFEDE7FF)
            MediaKind.PHOTO -> Color(0xFFFFE3F1)
            MediaKind.VIDEO -> Color(0xFFFFE7D6)
            MediaKind.AUDIO -> Color(0xFFD6F8EC)
        }
    }
}

fun onKindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        Color(0xFF0C0A14)
    } else {
        Color(0xFFFFFFFF)
    }
}

fun onKindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFA78BFA)
            MediaKind.PHOTO -> Color(0xFFF472B6)
            MediaKind.VIDEO -> Color(0xFFFB923C)
            MediaKind.AUDIO -> Color(0xFF34D399)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF3B1E9E)
            MediaKind.PHOTO -> Color(0xFF96165B)
            MediaKind.VIDEO -> Color(0xFF9A3D07)
            MediaKind.AUDIO -> Color(0xFF0A6847)
        }
    }
}

/**
 * File extension badge helper.
 * Small 14dp rounded square extension tag using the file's format color tint,
 * or surfaceMuted for unrecognized formats.
 */
object DocumentFormatColors {
    val pdfTint = Color(0xFFE5484D)
    val wordTint = Color(0xFF3D4CFF)
    val excelTint = Color(0xFF2B9A66)
    val pptTint = Color(0xFFFF7A3D)
    val textTint = Color(0xFF55554F)
    val mediaTint = Color(0xFFFF8AC2)
    val zipTint = Color(0xFF8A94FF)

    val pdfContainerLight = Color(0xFFFEE2E2)
    val pdfContainerDark = Color(0xFF2A1215)
    val wordContainerLight = Color(0xFFDBEAFE)
    val wordContainerDark = Color(0xFF0F1D36)
    val excelContainerLight = Color(0xFFD1FAE5)
    val excelContainerDark = Color(0xFF0D281E)
    val pptContainerLight = Color(0xFFFEF3C7)
    val pptContainerDark = Color(0xFF2E1F0A)
    val textContainerLight = Color(0xFFF1F5F9)
    val textContainerDark = Color(0xFF181B22)
    val mediaContainerLight = Color(0xFFFCE7F3)
    val mediaContainerDark = Color(0xFF2E1020)
    val zipContainerLight = Color(0xFFEDE9FE)
    val zipContainerDark = Color(0xFF201336)

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
        return if (isDark) Color(0xFF242426) else Color(0xFFEFEFEA)
    }
}
