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
    surfaceContainerLow = Color(0xFF0A0908),
    surfaceContainer = Color(0xFF12100D),
    surfaceContainerHigh = Color(0xFF1A1815),
    surfaceContainerHighest = Color(0xFF24211C),
    surfaceVariant = Color(0xFF1A1815),
    outlineVariant = Color(0xFF2A2721),
    outline = Color(0xFF6B6558)
)

private fun fallbackLight() = lightColorScheme(
    primary = Color(0xFF0E5C57),           // Seal teal
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEAE7),
    onPrimaryContainer = Color(0xFF06231F),
    inversePrimary = Color(0xFF4FB0A6),
    secondary = Color(0xFF6B6558),         // warm taupe
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEAE2),
    onSecondaryContainer = Color(0xFF1A1712),
    tertiary = Color(0xFFA67C3D),          // brass (micro-accent)
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0E6D8),
    onTertiaryContainer = Color(0xFF3A2A10),
    error = Color(0xFFB4231C),             // refined brick
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF0E1DE),
    onErrorContainer = Color(0xFF5E241F),
    background = Color(0xFFF5F3EE),        // warm bone canvas
    onBackground = Color(0xFF1A1712),      // warm near-black label
    surface = Color(0xFFFCFBF8),
    onSurface = Color(0xFF1A1712),
    surfaceVariant = Color(0xFFEDEAE2),
    onSurfaceVariant = Color(0xFF6B6558),
    surfaceTint = Color(0xFF0E5C57),
    inverseSurface = Color(0xFF24211C),
    inverseOnSurface = Color(0xFFF5F3EE),
    outline = Color(0xFF8A8378),
    outlineVariant = Color(0xFFDEDACF),    // warm hairline
    surfaceBright = Color(0xFFFCFBF8),
    surfaceDim = Color(0xFFE3DFD5),
    surfaceContainerLowest = Color(0xFFFCFBF8),
    surfaceContainerLow = Color(0xFFF7F5EF),
    surfaceContainer = Color(0xFFF5F3EE),
    surfaceContainerHigh = Color(0xFFEDEAE2),
    surfaceContainerHighest = Color(0xFFE3DFD5),
    scrim = Color(0xFF000000)
)

private fun fallbackDark() = darkColorScheme(
    primary = Color(0xFF4FB0A6),           // Seal teal (lifted)
    onPrimary = Color(0xFF06231F),
    primaryContainer = Color(0xFF123A36),
    onPrimaryContainer = Color(0xFFB4E4DC),
    inversePrimary = Color(0xFF0E5C57),
    secondary = Color(0xFFA8A090),         // warm taupe (dark)
    onSecondary = Color(0xFF241F1A),
    secondaryContainer = Color(0xFF332F28),
    onSecondaryContainer = Color(0xFFEDEAE2),
    tertiary = Color(0xFFC79A55),          // brass (dark)
    onTertiary = Color(0xFF2A1B0A),
    tertiaryContainer = Color(0xFF3A2A10),
    onTertiaryContainer = Color(0xFFE4C39A),
    error = Color(0xFFF0685E),
    onError = Color(0xFF2A0F0C),
    errorContainer = Color(0xFF3A1D1B),
    onErrorContainer = Color(0xFFF0B4AC),
    background = Color(0xFF0F0E0C),        // warm near-black
    onBackground = Color(0xFFF2EFE8),      // warm off-white label
    surface = Color(0xFF1A1815),
    onSurface = Color(0xFFF2EFE8),
    surfaceVariant = Color(0xFF24211C),
    onSurfaceVariant = Color(0xFFA8A090),
    surfaceTint = Color(0xFF4FB0A6),
    inverseSurface = Color(0xFFF2EFE8),
    inverseOnSurface = Color(0xFF1A1815),
    outline = Color(0xFFA8A090),
    outlineVariant = Color(0xFF332F28),    // warm hairline (dark)
    surfaceBright = Color(0xFF24211C),
    surfaceDim = Color(0xFF0F0E0C),
    surfaceContainerLowest = Color(0xFF0A0908),
    surfaceContainerLow = Color(0xFF12100D),
    surfaceContainer = Color(0xFF1A1815),
    surfaceContainerHigh = Color(0xFF24211C),
    surfaceContainerHighest = Color(0xFF2E2A23),
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
