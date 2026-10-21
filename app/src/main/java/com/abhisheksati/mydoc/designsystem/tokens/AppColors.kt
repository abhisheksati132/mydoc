package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Shelves Color Definition.
 * Stored as an index (0..5) in the database so Light and Dark themes dynamically render the matching shade.
 */
@Immutable
data class ShelfColor(
    val id: Int,
    val name: String,
    val color: Color,
    val onColor: Color
)

/**
 * Single Source of Truth for Design System Colors ("Shelves" Design System).
 *
 * Rules:
 * - Neutral canvas and surfaces (no random cobalt or rainbow backgrounds).
 * - Colour is reserved exclusively for SHELVES.
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
    val shelves: List<ShelfColor>,

    // Semantic mappings
    val textPrimary: Color = ink,
    val textSecondary: Color = inkMuted,
    val divider: Color = line,
    val cardBorder: Color = line,
    val accent: Color = shelves[0].color,
    val accentVariant: Color = shelves[0].color.copy(alpha = 0.16f),
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
    /**
     * Retrieves the shelf color by its persistent index (0..5), defaulting to Blue if invalid.
     */
    fun getShelfColor(index: Int): ShelfColor {
        return shelves.getOrElse(index) { shelves[0] }
    }
}

val LightShelfColors = listOf(
    ShelfColor(0, "Blue", Color(0xFF3D4CFF), Color(0xFFFFFFFF)),
    ShelfColor(1, "Orange", Color(0xFFFF7A3D), Color(0xFF1A0E05)),
    ShelfColor(2, "Mint", Color(0xFF86E7B8), Color(0xFF06210F)),
    ShelfColor(3, "Pink", Color(0xFFFF8AC2), Color(0xFF2A0A18)),
    ShelfColor(4, "Yellow", Color(0xFFFFD84D), Color(0xFF2A2000)),
    ShelfColor(5, "Ink", Color(0xFF121212), Color(0xFFFFFFFF))
)

val DarkShelfColors = listOf(
    ShelfColor(0, "Blue", Color(0xFF8A94FF), Color(0xFF0E0E0F)),
    ShelfColor(1, "Orange", Color(0xFFFF8F5C), Color(0xFF0E0E0F)),
    ShelfColor(2, "Mint", Color(0xFF86E7B8), Color(0xFF0E0E0F)),
    ShelfColor(3, "Pink", Color(0xFFFF9BCB), Color(0xFF0E0E0F)),
    ShelfColor(4, "Yellow", Color(0xFFFFDF6B), Color(0xFF0E0E0F)),
    ShelfColor(5, "Paper", Color(0xFFF2F1EC), Color(0xFF0E0E0F))
)

val LightAppColors = AppColors(
    background = Color(0xFFF7F7F4),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEFEFEA),
    line = Color(0xFFE2E2DD),
    ink = Color(0xFF121212),
    inkMuted = Color(0xFF55554F),
    highlight = Color(0xFFD9DCFF),
    shelves = LightShelfColors
)

val DarkAppColors = AppColors(
    background = Color(0xFF0E0E0F),
    surface = Color(0xFF18181A),
    surfaceMuted = Color(0xFF242426),
    line = Color(0xFF2A2A2D),
    ink = Color(0xFFF2F1EC),
    inkMuted = Color(0xFFA3A29B),
    highlight = Color(0xFF2A2F6B),
    shelves = DarkShelfColors
)

val BlackAppColors = AppColors(
    background = Color(0xFF000000), // Pure AMOLED
    surface = Color(0xFF121214),
    surfaceMuted = Color(0xFF1E1E21),
    line = Color(0xFF252528),
    ink = Color(0xFFF2F1EC),
    inkMuted = Color(0xFFA3A29B),
    highlight = Color(0xFF2A2F6B),
    shelves = DarkShelfColors
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/**
 * File extension badge helper.
 * Small 14dp rounded square extension tag using the file's shelf color tint,
 * or surfaceMuted for unfiled documents.
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

    fun getContainer(extension: String, isDark: Boolean): Color {
        return if (isDark) Color(0xFF242426) else Color(0xFFEFEFEA)
    }
}
