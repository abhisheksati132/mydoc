package com.abhisheksati.mydoc.designsystem.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * User control for glass effects:
 * - AUTO: Enabled on Android 12+ (API 31+) if not in Battery Saver or Low-RAM device.
 * - ON: Forces live blur on API 31+.
 * - OFF: Always uses high-performance solid tinted fallbacks.
 */
enum class GlassEffectMode {
    AUTO,
    ON,
    OFF
}

val LocalGlassEffectMode = staticCompositionLocalOf { GlassEffectMode.AUTO }

/**
 * Checks whether live backdrop blur should be active.
 */
fun isGlassEffectActive(context: Context, mode: GlassEffectMode): Boolean {
    if (mode == GlassEffectMode.OFF) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

    if (mode == GlassEffectMode.ON) return true

    // AUTO mode checks system power and memory health
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    if (powerManager?.isPowerSaveMode == true) return false

    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    if (activityManager?.isLowRamDevice == true) return false

    return true
}

/**
 * The core "Paper under glass" chrome surface.
 * Features:
 * 1. Live backdrop blur via Haze (when active and [hazeState] is supplied).
 * 2. Optical refraction tint ([tint] or [AppColors.glassTint]).
 * 3. 1dp specular gradient rim: bright reflection at top edge, soft occlusion at bottom.
 * 4. Subtle inner caustic glow to create tactile optical depth without visual clutter.
 * 5. Automatic solid fallback on API < 31, battery saver, or low-RAM environments.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = AppRadius.largeShape,
    hazeState: HazeState? = null,
    elevation: Dp = 8.dp,
    tint: Color? = null,
    backgroundColor: Color? = null,
    rimTop: Color? = null,
    rimBottom: Color? = null,
    innerShadowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val mode = LocalGlassEffectMode.current
    val colors = AppTheme.colors

    val glassActive = remember(mode, context) {
        isGlassEffectActive(context, mode)
    }

    val actualBackground = backgroundColor ?: colors.surface
    val actualTint = tint ?: colors.glassTint
    val actualRimTop = rimTop ?: colors.glassRimTop
    val actualRimBottom = rimBottom ?: colors.glassRimBottom
    val actualInnerShadow = innerShadowColor ?: colors.glassInnerShadow

    val rimBrush = remember(actualRimTop, actualRimBottom) {
        Brush.verticalGradient(
            colors = listOf(actualRimTop, actualRimBottom)
        )
    }

    val baseModifier = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (AppTheme.isDark) 0.35f else 0.08f),
            spotColor = Color.Black.copy(alpha = if (AppTheme.isDark) 0.50f else 0.12f)
        )
        .clip(shape)

    val styledModifier = if (glassActive && hazeState != null) {
        baseModifier
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = actualBackground,
                    tint = HazeTint(actualTint),
                    blurRadius = 24.dp,
                    noiseFactor = 0f
                )
            )
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    // Soft inner caustic shadow along top edge
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(actualInnerShadow, Color.Transparent),
                            startY = 0f,
                            endY = 16.dp.toPx()
                        ),
                        size = Size(size.width, 16.dp.toPx())
                    )
                }
            }
            .border(width = 1.dp, brush = rimBrush, shape = shape)
    } else {
        // High-performance fallback: semi-opaque solid tint with the specular rim
        val fallbackBackground = if (AppTheme.isDark) {
            colors.surface.copy(alpha = 0.94f)
        } else {
            colors.surface.copy(alpha = 0.96f)
        }

        baseModifier
            .background(color = fallbackBackground, shape = shape)
            .border(width = 1.dp, brush = rimBrush, shape = shape)
    }

    Box(
        modifier = styledModifier,
        content = content
    )
}
