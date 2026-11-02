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
    primary = Color(0xFF5B47E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF17065B),
    inversePrimary = Color(0xFFC8C2FF),
    secondary = Color(0xFF5F5A72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5DFF1),
    onSecondaryContainer = Color(0xFF1B1926),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF31101D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE5E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    surfaceTint = Color(0xFF5B47E0),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF3EFF7),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC9C5D0),
    surfaceBright = Color(0xFFFDFBFF),
    surfaceDim = Color(0xFFDDD9E3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F4FA),
    surfaceContainer = Color(0xFFF1EEF6),
    surfaceContainerHigh = Color(0xFFEBE8F0),
    surfaceContainerHighest = Color(0xFFE5E2EA),
    scrim = Color(0xFF000000)
)

private fun fallbackDark() = darkColorScheme(
    primary = Color(0xFFC4BCFF),
    onPrimary = Color(0xFF2B1E72),
    primaryContainer = Color(0xFF43358F),
    onPrimaryContainer = Color(0xFFE5DEFF),
    inversePrimary = Color(0xFF5B47E0),
    secondary = Color(0xFFC9C2DC),
    onSecondary = Color(0xFF302D3F),
    secondaryContainer = Color(0xFF464356),
    onSecondaryContainer = Color(0xFFE5DFF1),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF4A2532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD9E2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF131218),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC9C5D0),
    surfaceTint = Color(0xFFC4BCFF),
    inverseSurface = Color(0xFFE6E1E9),
    inverseOnSurface = Color(0xFF303036),
    outline = Color(0xFF928F9A),
    outlineVariant = Color(0xFF47464F),
    surfaceBright = Color(0xFF39383F),
    surfaceDim = Color(0xFF131218),
    surfaceContainerLowest = Color(0xFF0D0C12),
    surfaceContainerLow = Color(0xFF1B1A20),
    surfaceContainer = Color(0xFF1F1E24),
    surfaceContainerHigh = Color(0xFF29282F),
    surfaceContainerHighest = Color(0xFF34323A),
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
