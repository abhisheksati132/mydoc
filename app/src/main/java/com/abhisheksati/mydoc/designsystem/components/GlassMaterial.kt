package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.runtime.staticCompositionLocalOf

/** A small set of tuned materials; hardware and contrast policy still take precedence. */
enum class GlassAppearance { BALANCED, FROSTED, CLEAR }
enum class GlassMaterial { CHROME, MENU, PANEL }
val LocalGlassAppearance = staticCompositionLocalOf { GlassAppearance.BALANCED }

internal fun glassTintAlpha(dark: Boolean, appearance: GlassAppearance, material: GlassMaterial): Float {
    val chrome = when (appearance) {
        GlassAppearance.BALANCED -> if (dark) 0.84f else 0.74f
        GlassAppearance.FROSTED -> if (dark) 0.94f else 0.92f
        GlassAppearance.CLEAR -> if (dark) 0.76f else 0.66f
    }
    return when (material) {
        GlassMaterial.CHROME -> chrome
        GlassMaterial.MENU -> maxOf(chrome, 0.94f)
        GlassMaterial.PANEL -> maxOf(chrome, 0.97f)
    }
}
