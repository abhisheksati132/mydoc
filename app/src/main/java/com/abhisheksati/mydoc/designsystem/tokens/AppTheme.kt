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
import androidx.compose.runtime.staticCompositionLocalOf
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

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDark.current

    val isOled: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsOled.current
}

val LocalIsDark = staticCompositionLocalOf { false }
val LocalIsOled = staticCompositionLocalOf { false }

private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020),
    surfaceVariant = Color(0xFF1A1A1A),
    outlineVariant = Color(0xFF2C2C2C),
    outline = Color(0xFF6F6F6F)
)

private fun fallbackLight() = lightColorScheme(
    primary = Color(0xFF007AFF),           // systemBlue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6EBFF),
    onPrimaryContainer = Color(0xFF004094),
    inversePrimary = Color(0xFF0A84FF),
    secondary = Color(0xFF8E8E93),         // systemGray
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = Color(0xFF3A3A3C),
    tertiary = Color(0xFF5856D6),          // systemIndigo
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3E2FA),
    onTertiaryContainer = Color(0xFF3634A0),
    error = Color(0xFFFF3B30),             // systemRed
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = Color(0xFFB3261E),
    background = Color(0xFFF2F2F7),        // systemGroupedBackground
    onBackground = Color(0xFF000000),      // label
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C7076),  // secondaryLabel
    surfaceTint = Color(0xFF007AFF),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFFFFFFF),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFC6C6C8),    // separator
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE5E5EA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFD1D1D6),
    scrim = Color(0xFF000000)
)

private fun fallbackDark() = darkColorScheme(
    primary = Color(0xFF0A84FF),           // systemBlue (dark)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0A3A6B),
    onPrimaryContainer = Color(0xFFB5D8FF),
    inversePrimary = Color(0xFF007AFF),
    secondary = Color(0xFF98989D),         // systemGray (dark)
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color(0xFFE5E5EA),
    tertiary = Color(0xFF5E5CE6),          // systemIndigo (dark)
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF1E1B4B),
    onTertiaryContainer = Color(0xFFB9B7F5),
    error = Color(0xFFFF453A),             // systemRed (dark)
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF3A1D1B),
    onErrorContainer = Color(0xFFFFB4AC),
    background = Color(0xFF000000),        // systemBackground (dark)
    onBackground = Color(0xFFFFFFFF),      // label
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989D),
    surfaceTint = Color(0xFF0A84FF),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF38383A),    // separator (dark)
    surfaceBright = Color(0xFF2C2C2E),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    scrim = Color(0xFF000000)
)

@Composable
fun AppDesignSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseScheme: ColorScheme = when {
        dynamicColor && supportsDynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && supportsDynamic -> dynamicLightColorScheme(context)
        darkTheme -> fallbackDark()
        else -> fallbackLight()
    }
    val materialColorScheme = if (darkTheme && amoledBlack) baseScheme.amoled() else baseScheme
    val colors = AppColors.fromMaterialScheme(materialColorScheme, isDark = darkTheme)

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
        LocalIsDark provides darkTheme,
        LocalIsOled provides (darkTheme && amoledBlack),
        dev.chrisbanes.haze.LocalHazeStyle provides dev.chrisbanes.haze.HazeDefaults.style(backgroundColor = Color.Transparent)
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = {
                CompositionLocalProvider(
                    LocalAppTypography provides appTypographyFromMaterial(MaterialTheme.typography)
                ) {
                    content()
                }
            }
        )
    }
}
