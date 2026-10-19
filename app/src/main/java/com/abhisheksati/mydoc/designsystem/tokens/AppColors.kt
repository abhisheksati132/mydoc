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
    val error: Color
)

val DarkAppColors = AppColors(
    background = Color(0xFF000000), // Pure OLED background
    surface = Color(0xFF111216),    // Deep refined zinc surface
    surfaceVariant = Color(0xFF181A20), // Subtle container
    divider = Color(0xFF22252C),    // Whisper hairline separation
    textPrimary = Color(0xFFF4F4F6), // Off-white for comfortable contrast
    textSecondary = Color(0xFF8E8E96), // Calm secondary text
    accent = Color(0xFF3B82F6),     // Sophisticated, calm system blue (no neon)
    error = Color(0xFFEF5350)
)

val LightAppColors = AppColors(
    background = Color(0xFFF9F9FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEFF2),
    divider = Color(0xFFE5E5EB),
    textPrimary = Color(0xFF111114),
    textSecondary = Color(0xFF6E6E78),
    accent = Color(0xFF1A73E8),     // Clean, trustworthy system blue
    error = Color(0xFFD32F2F)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * Recognizable Google-standard format colors (PDF red, Word blue, Excel green, PPT amber).
 * Provides soft container backgrounds and high-contrast foreground tints for both Light and Dark modes.
 */
object DocumentFormatColors {
    val pdfTint = Color(0xFFEA4335)
    val pdfContainerLight = Color(0xFFFCE8E6)
    val pdfContainerDark = Color(0xFF261010)

    val wordTint = Color(0xFF1A73E8)
    val wordContainerLight = Color(0xFFE8F0FE)
    val wordContainerDark = Color(0xFF0F1B30)

    val excelTint = Color(0xFF188038)
    val excelContainerLight = Color(0xFFE6F4EA)
    val excelContainerDark = Color(0xFF102616)

    val pptTint = Color(0xFFF29900)
    val pptContainerLight = Color(0xFFFEF7E0)
    val pptContainerDark = Color(0xFF2B200D)

    val textTint = Color(0xFF5F6368)
    val textContainerLight = Color(0xFFF1F3F4)
    val textContainerDark = Color(0xFF1A1A1E)

    val mediaTint = Color(0xFFE91E63)
    val mediaContainerLight = Color(0xFFFCE4EC)
    val mediaContainerDark = Color(0xFF26101C)

    fun getTint(extension: String): Color {
        return when (extension.lowercase()) {
            "pdf" -> pdfTint
            "docx", "doc" -> wordTint
            "xlsx", "xls", "csv" -> excelTint
            "pptx", "ppt", "ppsx", "pps", "potx", "pot" -> pptTint
            "txt", "md", "log", "json", "xml" -> textTint
            "jpg", "jpeg", "png", "webp", "gif", "mp4", "mp3" -> mediaTint
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
                else -> textContainerLight
            }
        }
    }
}
