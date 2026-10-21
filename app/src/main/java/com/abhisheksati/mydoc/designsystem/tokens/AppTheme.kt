package com.abhisheksati.mydoc.designsystem.tokens

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Accessor object for App Design System Tokens ("Shelves"):
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
    dynamicColor: Boolean = false, // Strictly OFF to preserve Shelves brand identity
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors: AppColors = when {
        darkTheme && amoledBlack -> BlackAppColors
        darkTheme -> DarkAppColors
        else -> LightAppColors
    }

    val materialColorScheme: ColorScheme = if (darkTheme) {
        darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceMuted,
            primary = colors.accent,
            primaryContainer = colors.surfaceMuted,
            onPrimary = colors.shelves[0].onColor,
            onPrimaryContainer = colors.ink,
            onBackground = colors.ink,
            onSurface = colors.ink,
            onSurfaceVariant = colors.inkMuted,
            outline = colors.line,
            outlineVariant = colors.line,
            error = colors.error
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceMuted,
            primary = colors.accent,
            primaryContainer = colors.surfaceMuted,
            onPrimary = colors.shelves[0].onColor,
            onPrimaryContainer = colors.ink,
            onBackground = colors.ink,
            onSurface = colors.ink,
            onSurfaceVariant = colors.inkMuted,
            outline = colors.line,
            outlineVariant = colors.line,
            error = colors.error
        )
    }

    val typography = AppTypography()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
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
