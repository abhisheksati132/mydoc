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
    primary = Color(0xFFD94324),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFCEBE7),
    onPrimaryContainer = Color(0xFF8A210B),
    inversePrimary = Color(0xFFFFA694),
    secondary = Color(0xFF57534E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0ECE1),
    onSecondaryContainer = Color(0xFF191614),
    tertiary = Color(0xFF2C4A7F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8EEF8),
    onTertiaryContainer = Color(0xFF18315B),
    error = Color(0xFFD94324),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCEBE7),
    onErrorContainer = Color(0xFF8A210B),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF191614),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF191614),
    surfaceVariant = Color(0xFFF0ECE1),
    onSurfaceVariant = Color(0xFF6B645C),
    surfaceTint = Color(0xFFD94324),
    inverseSurface = Color(0xFF191614),
    inverseOnSurface = Color(0xFFFAF7F2),
    outline = Color(0xFF9E968D),
    outlineVariant = Color(0xFFE5DFD3),
    surfaceBright = Color(0xFFFFFDF9),
    surfaceDim = Color(0xFFEBE6DC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF7F2),
    surfaceContainer = Color(0xFFF5F1E8),
    surfaceContainerHigh = Color(0xFFF0ECE1),
    surfaceContainerHighest = Color(0xFFEBE6DC),
    scrim = Color(0xFF000000)
)

private fun fallbackDark() = darkColorScheme(
    primary = Color(0xFFF05435),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF331914),
    onPrimaryContainer = Color(0xFFFFA694),
    inversePrimary = Color(0xFFD94324),
    secondary = Color(0xFF8C9AB0),
    onSecondary = Color(0xFF141923),
    secondaryContainer = Color(0xFF1D2433),
    onSecondaryContainer = Color(0xFFF2F5F8),
    tertiary = Color(0xFF6B8DC7),
    onTertiary = Color(0xFF0B1424),
    tertiaryContainer = Color(0xFF152238),
    onTertiaryContainer = Color(0xFFB5CEF7),
    error = Color(0xFFF05435),
    onError = Color(0xFF331914),
    errorContainer = Color(0xFF5A1C12),
    onErrorContainer = Color(0xFFFFA694),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFF2F5F8),
    surface = Color(0xFF141923),
    onSurface = Color(0xFFF2F5F8),
    surfaceVariant = Color(0xFF1D2433),
    onSurfaceVariant = Color(0xFF8C9AB0),
    surfaceTint = Color(0xFFF05435),
    inverseSurface = Color(0xFFF2F5F8),
    inverseOnSurface = Color(0xFF0B0E14),
    outline = Color(0xFF5A677D),
    outlineVariant = Color(0xFF263043),
    surfaceBright = Color(0xFF222B3D),
    surfaceDim = Color(0xFF0B0E14),
    surfaceContainerLowest = Color(0xFF07090E),
    surfaceContainerLow = Color(0xFF0E131C),
    surfaceContainer = Color(0xFF141923),
    surfaceContainerHigh = Color(0xFF1D2433),
    surfaceContainerHighest = Color(0xFF263043),
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
    val colors = AppColors.fromMaterialScheme(materialColorScheme)

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
        LocalIsOled provides (darkTheme && amoledBlack)
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
