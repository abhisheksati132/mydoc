package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * One hue per tool group. Tool sheets sort and colour tools by what the tool does,
 * so the hue is a category label rather than decoration; it never appears outside
 * the tool picker.
 */
object ToolGroupColors {
    data class Tint(val tint: Color, val lightContainer: Color)

    val edit = Tint(Color(0xFFF59E0B), Color(0xFFFEF3C7))
    val organize = Tint(Color(0xFF3B82F6), Color(0xFFDBEAFE))
    val optimize = Tint(Color(0xFF10B981), Color(0xFFD1FAE5))
    val convertTo = Tint(Color(0xFF8B5CF6), Color(0xFFEDE9FE))
    val convertFrom = Tint(Color(0xFF6366F1), Color(0xFFE0E7FF))
    val security = Tint(Color(0xFFEF4444), Color(0xFFFEE2E2))
    val scan = Tint(Color(0xFF14B8A6), Color(0xFFCCFBF1))

    fun container(tint: Tint, isDark: Boolean): Color =
        if (isDark) tint.tint.copy(alpha = 0.16f) else tint.lightContainer
}
