package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Apple Design System & Glassmorphism Color Tokens.
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
    val error: Color,
    val glassSurface: Color = Color(0x331E2436),
    val glassBorder: Color = Color(0x38FFFFFF),
    val glassHighlight: Color = Color(0x18FFFFFF),
    val cardBackground: Color = Color(0x55161B29),
    val dockBackground: Color = Color(0xDD121624)
)

val DarkAppColors = AppColors(
    background = Color(0xFF0C0E14),
    surface = Color(0xFF131722),
    surfaceVariant = Color(0xFF1B2030),
    divider = Color(0x22FFFFFF),
    textPrimary = Color(0xFFF7F8FA),
    textSecondary = Color(0xFF9EA3B0),
    accent = Color(0xFF0A84FF),
    error = Color(0xFFFF453A),
    glassSurface = Color(0x331E2436),
    glassBorder = Color(0x38FFFFFF),
    glassHighlight = Color(0x18FFFFFF),
    cardBackground = Color(0x55161B29),
    dockBackground = Color(0xDD121624)
)

val LightAppColors = AppColors(
    background = Color(0xFFF3F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6EBF2),
    divider = Color(0x18000000),
    textPrimary = Color(0xFF111217),
    textSecondary = Color(0xFF6B7280),
    accent = Color(0xFF007AFF),
    error = Color(0xFFFF3B30),
    glassSurface = Color(0xD8FFFFFF),
    glassBorder = Color(0x55FFFFFF),
    glassHighlight = Color(0x99FFFFFF),
    cardBackground = Color(0xEEFFFFFF),
    dockBackground = Color(0xF0FFFFFF)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * Apple-grade document format gradients and icon colors.
 */
object DocumentTypeGradients {
    val pdfStart = Color(0xFFFF3B30)
    val pdfEnd = Color(0xFFFF5A52)
    val pdfGradient = Brush.linearGradient(listOf(pdfStart, pdfEnd))

    val wordStart = Color(0xFF007AFF)
    val wordEnd = Color(0xFF0A84FF)
    val wordGradient = Brush.linearGradient(listOf(wordStart, wordEnd))

    val excelStart = Color(0xFF34C759)
    val excelEnd = Color(0xFF30D158)
    val excelGradient = Brush.linearGradient(listOf(excelStart, excelEnd))

    val pptStart = Color(0xFFFF9500)
    val pptEnd = Color(0xFFFFA726)
    val pptGradient = Brush.linearGradient(listOf(pptStart, pptEnd))

    val textStart = Color(0xFF5856D6)
    val textEnd = Color(0xFF7976FF)
    val textGradient = Brush.linearGradient(listOf(textStart, textEnd))

    val mediaStart = Color(0xFFFF2D55)
    val mediaEnd = Color(0xFFFF5478)
    val mediaGradient = Brush.linearGradient(listOf(mediaStart, mediaEnd))

    fun getGradientForExtension(extension: String): Brush {
        return when (extension.lowercase().removePrefix(".")) {
            "pdf" -> pdfGradient
            "doc", "docx" -> wordGradient
            "xls", "xlsx", "csv" -> excelGradient
            "ppt", "pptx" -> pptGradient
            "txt", "md", "json", "xml" -> textGradient
            "jpg", "jpeg", "png", "webp", "mp4", "mp3", "m4a", "wav" -> mediaGradient
            else -> textGradient
        }
    }

    fun getAccentColorForExtension(extension: String): Color {
        return when (extension.lowercase().removePrefix(".")) {
            "pdf" -> pdfStart
            "doc", "docx" -> wordStart
            "xls", "xlsx", "csv" -> excelStart
            "ppt", "pptx" -> pptStart
            "txt", "md", "json", "xml" -> textStart
            "jpg", "jpeg", "png", "webp", "mp4", "mp3", "m4a", "wav" -> mediaStart
            else -> textStart
        }
    }
}
