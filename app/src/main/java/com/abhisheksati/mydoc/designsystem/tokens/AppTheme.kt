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
    primary = Color(0xFF0066FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Color(0xFF1E40AF),
    inversePrimary = Color(0xFF93C5FD),
    secondary = Color(0xFF475569),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF6366F1),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEF2FF),
    onTertiaryContainer = Color(0xFF3730A3),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceTint = Color(0xFF0066FF),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE2E8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    scrim = Color(0xFF000000)
)

private fun fallbackDark() = darkColorScheme(
    primary = Color(0xFF2979FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFF93C5FD),
    inversePrimary = Color(0xFF0066FF),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E283D),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF1E1B4B),
    onTertiaryContainer = Color(0xFFC7D2FE),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFCA5A5),
    background = Color(0xFF090D16),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF131A29),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E283D),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = Color(0xFF2979FF),
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF090D16),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF2B374F),
    surfaceBright = Color(0xFF1E293B),
    surfaceDim = Color(0xFF090D16),
    surfaceContainerLowest = Color(0xFF060910),
    surfaceContainerLow = Color(0xFF0E1422),
    surfaceContainer = Color(0xFF131A29),
    surfaceContainerHigh = Color(0xFF1B2438),
    surfaceContainerHighest = Color(0xFF24304A),
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
        dev.chrisbanes.haze.LocalHazeStyle provides dev.chrisbanes.haze.HazeDefaults.style(backgroundColor = colors.surface)
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
