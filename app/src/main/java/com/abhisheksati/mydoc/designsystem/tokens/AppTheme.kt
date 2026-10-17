package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Accessor object for App Design System Tokens:
 * AppTheme.colors, AppTheme.typography, AppTheme.spacing
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val spacing: AppSpacing
        get() = AppSpacing
}

@Composable
fun AppDesignSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    val typography = AppTypography()

    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceVariant,
            primary = colors.accent,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.divider,
            error = colors.error
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceVariant,
            primary = colors.accent,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.divider,
            error = colors.error
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}
