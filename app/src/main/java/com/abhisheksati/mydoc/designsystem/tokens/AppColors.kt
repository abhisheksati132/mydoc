package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

import com.abhisheksati.mydoc.domain.model.MediaKind

/**
 * Single Source of Truth for Design System Colors (Stacks Design System).
 *
 * Rules:
 * - Neutral canvas and surfaces.
 * - Meaningful color by MediaKind: Document (Blue), Photo (Pink), Video (Orange), Audio (Mint).
 * - Flat 1dp hairline lines, no shadows or gradients.
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
    val accentVariant: Color = accent.copy(alpha = 0.16f),
    val error: Color = Color(0xFFE5484D),

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
}

val LightAppColors = AppColors(
    background = Color(0xFFF7F7F4),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEFEFEA),
    line = Color(0xFFE2E2DD),
    ink = Color(0xFF121212),
    inkMuted = Color(0xFF55554F),
    highlight = Color(0xFFE5E3FF),
    // MyDoc brand violet — not Apple blue. Ours.
    accent = Color(0xFF5B4FE8),
    onAccent = Color(0xFFFFFFFF),

    documentKind = Color(0xFF5B4FE8),
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFFEDEBFF),

    photoKind = Color(0xFFFF8AC2),
    onPhotoKind = Color(0xFF2A0A18),
    photoContainer = Color(0xFFFFE0EF),

    videoKind = Color(0xFFFF7A3D),
    onVideoKind = Color(0xFF1A0E05),
    videoContainer = Color(0xFFFFE3D4),

    audioKind = Color(0xFF86E7B8),
    onAudioKind = Color(0xFF06210F),
    audioContainer = Color(0xFFD4F6E5)
)

val DarkAppColors = AppColors(
    background = Color(0xFF0E0E0F),
    surface = Color(0xFF18181A),
    surfaceMuted = Color(0xFF242426),
    line = Color(0xFF2A2A2D),
    ink = Color(0xFFF2F1EC),
    inkMuted = Color(0xFFA3A29B),
    highlight = Color(0xFF2A2660),
    // MyDoc brand violet dark variant
    accent = Color(0xFF9B93FF),
    onAccent = Color(0xFF0E0E0F),

    documentKind = Color(0xFF9B93FF),
    onDocumentKind = Color(0xFF0E0E0F),
    documentContainer = Color(0xFF1D1A42),

    photoKind = Color(0xFFFF9BCB),
    onPhotoKind = Color(0xFF0E0E0F),
    photoContainer = Color(0xFF3B1A2B),

    videoKind = Color(0xFFFF8F5C),
    onVideoKind = Color(0xFF0E0E0F),
    videoContainer = Color(0xFF3A2315),

    audioKind = Color(0xFF86E7B8),
    onAudioKind = Color(0xFF0E0E0F),
    audioContainer = Color(0xFF12301F)
)

val BlackAppColors = DarkAppColors.copy(
    background = Color(0xFF000000), // Pure AMOLED zero-nit background
    surface = Color(0xFF0A0A0C),     // Deep obsidian card surface
    surfaceMuted = Color(0xFF141416),
    line = Color(0xFF1E1E22),
    surfaceContainer = Color(0xFF0A0A0C),
    surfaceContainerHigh = Color(0xFF141416),
    surfaceContainerLow = Color(0xFF000000),
    primaryContainer = Color(0xFF141416),
    secondaryContainer = Color(0xFF141416)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

fun kindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF9B93FF)
            MediaKind.PHOTO -> Color(0xFFFF9BCB)
            MediaKind.VIDEO -> Color(0xFFFF8F5C)
            MediaKind.AUDIO -> Color(0xFF86E7B8)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF5B4FE8)
            MediaKind.PHOTO -> Color(0xFFFF8AC2)
            MediaKind.VIDEO -> Color(0xFFFF7A3D)
            MediaKind.AUDIO -> Color(0xFF86E7B8)
        }
    }
}

fun kindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF1D1A42)
            MediaKind.PHOTO -> Color(0xFF3B1A2B)
            MediaKind.VIDEO -> Color(0xFF3A2315)
            MediaKind.AUDIO -> Color(0xFF12301F)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFE3E6FF)
            MediaKind.PHOTO -> Color(0xFFFFE0EF)
            MediaKind.VIDEO -> Color(0xFFFFE3D4)
            MediaKind.AUDIO -> Color(0xFFD4F6E5)
        }
    }
}

fun onKindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        Color(0xFF0E0E0F)
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFFFFFFF)
            MediaKind.PHOTO -> Color(0xFF2A0A18)
            MediaKind.VIDEO -> Color(0xFF1A0E05)
            MediaKind.AUDIO -> Color(0xFF06210F)
        }
    }
}

fun onKindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF9B93FF)
            MediaKind.PHOTO -> Color(0xFFFF9BCB)
            MediaKind.VIDEO -> Color(0xFFFF8F5C)
            MediaKind.AUDIO -> Color(0xFF86E7B8)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF2B2590)
            MediaKind.PHOTO -> Color(0xFF5A1538)
            MediaKind.VIDEO -> Color(0xFF5A2508)
            MediaKind.AUDIO -> Color(0xFF0D4222)
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
