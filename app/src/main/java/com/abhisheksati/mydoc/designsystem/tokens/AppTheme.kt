package com.abhisheksati.mydoc.designsystem.tokens

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    dynamicColor: Boolean = true,
    amoledBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDynamicColor = dynamicColor && supportsDynamic

    val baseMaterialColorScheme: ColorScheme = when {
        useDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme(
            background = DarkAppColors.background,
            surface = DarkAppColors.surface,
            surfaceVariant = DarkAppColors.surfaceVariant,
            primary = DarkAppColors.accent,
            primaryContainer = DarkAppColors.accentVariant,
            onPrimaryContainer = DarkAppColors.accent,
            onBackground = DarkAppColors.textPrimary,
            onSurface = DarkAppColors.textPrimary,
            onSurfaceVariant = DarkAppColors.textSecondary,
            outline = DarkAppColors.divider,
            outlineVariant = DarkAppColors.cardBorder,
            error = DarkAppColors.error
        )
        else -> lightColorScheme(
            background = LightAppColors.background,
            surface = LightAppColors.surface,
            surfaceVariant = LightAppColors.surfaceVariant,
            primary = LightAppColors.accent,
            primaryContainer = LightAppColors.accentVariant,
            onPrimaryContainer = LightAppColors.accent,
            onBackground = LightAppColors.textPrimary,
            onSurface = LightAppColors.textPrimary,
            onSurfaceVariant = LightAppColors.textSecondary,
            outline = LightAppColors.divider,
            outlineVariant = LightAppColors.cardBorder,
            error = LightAppColors.error
        )
    }

    // Adapt Material Scheme for AMOLED Black if enabled in dark mode
    val materialColorScheme = if (darkTheme && amoledBlack) {
        baseMaterialColorScheme.copy(
            background = Color(0xFF000000),
            surface = Color(0xFF101217),
            surfaceContainer = Color(0xFF101217),
            surfaceContainerHigh = Color(0xFF181B22)
        )
    } else {
        baseMaterialColorScheme
    }

    // Construct unified AppColors mapped directly from the active Material Scheme
    val colors = if (darkTheme) {
        AppColors(
            background = materialColorScheme.background,
            surface = materialColorScheme.surface,
            surfaceVariant = materialColorScheme.surfaceVariant,
            divider = materialColorScheme.outlineVariant.copy(alpha = 0.5f),
            textPrimary = materialColorScheme.onSurface,
            textSecondary = materialColorScheme.onSurfaceVariant,
            accent = materialColorScheme.primary,
            accentVariant = materialColorScheme.primaryContainer,
            cardBorder = if (amoledBlack) Color(0x26FFFFFF) else materialColorScheme.outlineVariant.copy(alpha = 0.6f),
            error = materialColorScheme.error,
            primaryContainer = materialColorScheme.primaryContainer,
            onPrimaryContainer = materialColorScheme.onPrimaryContainer,
            secondaryContainer = materialColorScheme.secondaryContainer,
            onSecondaryContainer = materialColorScheme.onSecondaryContainer,
            surfaceContainer = materialColorScheme.surfaceContainer,
            surfaceContainerHigh = materialColorScheme.surfaceContainerHigh,
            surfaceContainerLow = materialColorScheme.surfaceContainerLow,
            outline = materialColorScheme.outline,
            outlineVariant = materialColorScheme.outlineVariant
        )
    } else {
        AppColors(
            background = materialColorScheme.background,
            surface = materialColorScheme.surface,
            surfaceVariant = materialColorScheme.surfaceVariant,
            divider = materialColorScheme.outlineVariant.copy(alpha = 0.6f),
            textPrimary = materialColorScheme.onSurface,
            textSecondary = materialColorScheme.onSurfaceVariant,
            accent = materialColorScheme.primary,
            accentVariant = materialColorScheme.primaryContainer,
            cardBorder = materialColorScheme.outlineVariant.copy(alpha = 0.6f),
            error = materialColorScheme.error,
            primaryContainer = materialColorScheme.primaryContainer,
            onPrimaryContainer = materialColorScheme.onPrimaryContainer,
            secondaryContainer = materialColorScheme.secondaryContainer,
            onSecondaryContainer = materialColorScheme.onSecondaryContainer,
            surfaceContainer = materialColorScheme.surfaceContainer,
            surfaceContainerHigh = materialColorScheme.surfaceContainerHigh,
            surfaceContainerLow = materialColorScheme.surfaceContainerLow,
            outline = materialColorScheme.outline,
            outlineVariant = materialColorScheme.outlineVariant
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
