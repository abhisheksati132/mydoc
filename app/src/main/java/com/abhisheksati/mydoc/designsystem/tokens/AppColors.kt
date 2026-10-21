package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Strict Design System Color Tokens.
 *
 * Rules:
 * - Dark background is a soft near-black (#0F0F10)
 * - Text is off-white (#F0F0F2) in dark mode, near-black (#111114) in light mode
 * - Accent is slightly muted in dark mode (#7FA4DE)
 * - No pure black (#000000) or pure white (#FFFFFF)
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentVariant: Color = accent.copy(alpha = 0.16f),
    val cardBorder: Color = divider,
    val error: Color,
    // Material 3 / Material You tokens
    val primaryContainer: Color = accentVariant,
    val onPrimaryContainer: Color = accent,
    val secondaryContainer: Color = surfaceVariant,
    val onSecondaryContainer: Color = textPrimary,
    val surfaceContainer: Color = surface,
    val surfaceContainerHigh: Color = surfaceVariant,
    val surfaceContainerLow: Color = surface,
    val outline: Color = divider,
    val outlineVariant: Color = cardBorder
)

val DarkAppColors = AppColors(
    background = Color(0xFF000000),      // Pure OLED deep black
    surface = Color(0xFF101217),         // Refined obsidian surface
    surfaceVariant = Color(0xFF181B22),  // Elevated container
    divider = Color(0xFF222630),         // Whisper hairline 1px separation
    textPrimary = Color(0xFFF6F7F9),     // Pure legible off-white
    textSecondary = Color(0xFF8B92A0),   // Balanced cool secondary text
    accent = Color(0xFF3B82F6),          // Neo-Precision Electric Cobalt
    accentVariant = Color(0x283B82F6),   // 16% Electric Cobalt glow
    cardBorder = Color(0x26FFFFFF),      // 15% white frosted edge
    error = Color(0xFFEF4444)            // Modern vivid crimson
)

val LightAppColors = AppColors(
    background = Color(0xFFF8F9FA),      // Pristine alabaster canvas
    surface = Color(0xFFFFFFFF),         // Clean white elevated card
    surfaceVariant = Color(0xFFEDF0F5),  // Soft cool-tinted container
    divider = Color(0xFFE2E6ED),         // Crisp subtle divider
    textPrimary = Color(0xFF0F172A),     // Deep obsidian slate text
    textSecondary = Color(0xFF64748B),   // Balanced muted text
    accent = Color(0xFF2563EB),          // Royal Cobalt
    accentVariant = Color(0x1E2563EB),   // 12% Royal Cobalt tint
    cardBorder = Color(0xFFE2E6ED),      // 1px subtle border
    error = Color(0xFFDC2626)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * Recognizable format colors (PDF crimson, Word cobalt, Excel emerald, PPT amber).
 * Provides soft container backgrounds and high-contrast foreground tints for both Light and Dark modes.
 */
object DocumentFormatColors {
    val pdfTint = Color(0xFFEF4444)
    val pdfContainerLight = Color(0xFFFEE2E2)
    val pdfContainerDark = Color(0xFF2A1215)

    val wordTint = Color(0xFF3B82F6)
    val wordContainerLight = Color(0xFFDBEAFE)
    val wordContainerDark = Color(0xFF0F1D36)

    val excelTint = Color(0xFF10B981)
    val excelContainerLight = Color(0xFFD1FAE5)
    val excelContainerDark = Color(0xFF0D281E)

    val pptTint = Color(0xFFF59E0B)
    val pptContainerLight = Color(0xFFFEF3C7)
    val pptContainerDark = Color(0xFF2E1F0A)

    val textTint = Color(0xFF64748B)
    val textContainerLight = Color(0xFFF1F5F9)
    val textContainerDark = Color(0xFF181B22)

    val mediaTint = Color(0xFFEC4899)
    val mediaContainerLight = Color(0xFFFCE7F3)
    val mediaContainerDark = Color(0xFF2E1020)

    val zipTint = Color(0xFF8B5CF6)
    val zipContainerLight = Color(0xFFEDE9FE)
    val zipContainerDark = Color(0xFF201336)

    fun getTint(extension: String): Color {
        return when (extension.lowercase()) {
            "pdf" -> pdfTint
            "docx", "doc" -> wordTint
            "xlsx", "xls", "csv" -> excelTint
            "pptx", "ppt", "ppsx", "pps", "potx", "pot" -> pptTint
            "txt", "md", "log", "json", "xml" -> textTint
            "jpg", "jpeg", "png", "webp", "gif", "mp4", "mp3" -> mediaTint
            "zip", "rar", "7z", "tar", "gz" -> zipTint
            else -> textTint
        }
    }

    fun getContainer(extension: String, isDark: Boolean): Color {
        val ext = extension.lowercase()
        return if (isDark) {
            when (ext) {
                "pdf" -> pdfContainerDark
                "docx", "doc" -> wordContainerDark
                "xlsx", "xls", "csv" -> excelContainerDark
                "pptx", "ppt", "ppsx", "pps", "potx", "pot" -> pptContainerDark
                "txt", "md", "log", "json", "xml" -> textContainerDark
                "jpg", "jpeg", "png", "webp", "gif", "mp4", "mp3" -> mediaContainerDark
                "zip", "rar", "7z", "tar", "gz" -> zipContainerDark
                else -> textContainerDark
            }
        } else {
            when (ext) {
                "pdf" -> pdfContainerLight
                "docx", "doc" -> wordContainerLight
                "xlsx", "xls", "csv" -> excelContainerLight
                "pptx", "ppt", "ppsx", "pps", "potx", "pot" -> pptContainerLight
                "txt", "md", "log", "json", "xml" -> textContainerLight
                "jpg", "jpeg", "png", "webp", "gif", "mp4", "mp3" -> mediaContainerLight
                "zip", "rar", "7z", "tar", "gz" -> zipContainerLight
                else -> textContainerLight
            }
        }
    }
}
