package com.abhisheksati.mydoc.designsystem.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.glassRimBrush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * User control for glass effects:
 * - AUTO: Enabled on Android 12+ (API 31+) if not in Battery Saver or Low-RAM device.
 * - ON: Enables live material on API 31+, while retaining the contrast fallback.
 * - OFF: Always uses high-performance solid tinted fallbacks.
 */
enum class GlassEffectMode {
    AUTO,
    ON,
    OFF
}

val LocalGlassEffectMode = staticCompositionLocalOf { GlassEffectMode.AUTO }

/**
 * Scroll-driven glass response: 0f at rest, ramps to 1f over the first
 * [GlassMotionState.RAMP_PX] of scroll. Drives tint density so
 * chrome thickens like real glass under moving content.
 */
@Stable
class GlassMotionState(initial: Float = 0f) {
    var progress: Float by mutableFloatStateOf(initial)

    companion object {
        const val RAMP_PX = 240f
    }
}

internal fun glassScrollProgress(index: Int, offset: Int): Float =
    if (index > 0) 1f else (offset / GlassMotionState.RAMP_PX).coerceIn(0f, 1f)

@Composable
fun TrackGlassMotion(listState: LazyListState, motion: GlassMotionState) {
    LaunchedEffect(listState, motion) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                motion.progress = glassScrollProgress(index, offset)
            }
    }
}

@Composable
fun TrackGlassMotion(gridState: LazyGridState, motion: GlassMotionState) {
    LaunchedEffect(gridState, motion) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                motion.progress = glassScrollProgress(index, offset)
            }
    }
}

@Composable
fun rememberGlassMotion(listState: LazyListState): GlassMotionState {
    val motion = remember { GlassMotionState() }
    TrackGlassMotion(listState, motion)
    return motion
}

@Composable
fun rememberGlassMotion(gridState: LazyGridState): GlassMotionState {
    val motion = remember { GlassMotionState() }
    TrackGlassMotion(gridState, motion)
    return motion
}

/** Shared navigation material. Optical refraction is limited to rounded chrome on API 33+.
 * Arbitrary shapes retain backdrop blur; unsupported devices and disabled effects use solid fill.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = AppRadius.glassShape,
    hazeState: HazeState? = null,
    elevation: Dp = 8.dp,
    motion: GlassMotionState? = null,
    tint: Color? = null,
    backgroundColor: Color? = null,
    rimTop: Color? = null,
    rimBottom: Color? = null,
    innerShadowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val mode = LocalGlassEffectMode.current
    val colors = AppTheme.colors

    val glassActive = shouldUseLiveGlass(mode, LocalGlassEnvironment.current)

    val actualBackground = backgroundColor ?: colors.surface
    val actualTint = tint ?: colors.glassTint
    val actualRimTop = rimTop ?: colors.glassRimTop
    val actualRimBottom = rimBottom ?: colors.glassRimBottom
    val actualInnerShadow = innerShadowColor ?: colors.glassInnerShadow

    // Specular rim. The rule lives in glassRimBrush so sheets, dialogs and menus cannot drift
    // from surfaces: even hairline on dark and AMOLED, directional glass on ivory.
    val rimBrush: Brush = glassRimBrush(colors, AppTheme.isDark, actualRimTop, actualRimBottom)

    val specularAndInnerShadow = Modifier.drawWithCache {
        val innerShadowHeight = 8.dp.toPx()
        val innerShadow = Brush.verticalGradient(
            colors = listOf(actualInnerShadow, Color.Transparent),
            startY = 0f,
            endY = innerShadowHeight
        )
        val outline = shape.createOutline(size, layoutDirection, this)
        val outlinePath: Path? = when (outline) {
            is Outline.Rounded -> Path().apply { addOutline(outline) }
            is Outline.Generic -> outline.path
            is Outline.Rectangle -> null
        }
        val stroke = Stroke(width = 1.dp.toPx())
        onDrawWithContent {
            // Inner shadow UNDER content: the glass edge occluding what lies below.
            drawRect(
                brush = innerShadow,
                size = Size(size.width, innerShadowHeight)
            )
            drawContent()
            if (outlinePath != null) {
                drawPath(outlinePath, brush = rimBrush, style = stroke)
            } else {
                drawRect(brush = rimBrush, style = stroke)
            }
        }
    }

    val baseModifier = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (AppTheme.isDark) {
                colors.scrim.copy(alpha = 0.35f)
            } else {
                colors.ink.copy(alpha = 0.06f)
            },
            spotColor = if (AppTheme.isDark) {
                colors.scrim.copy(alpha = 0.50f)
            } else {
                colors.accent.copy(alpha = 0.10f)
            }
        )
        .clip(shape)

    val isDark = AppTheme.isDark
    val progress = if (LocalReducedMotion.current) 0f else (motion?.progress ?: 0f).coerceIn(0f, 1f)
    val opticalTint = tint ?: colors.surface.copy(alpha = (if (isDark) 0.78f else 0.64f) + progress * 0.04f)
    val glassStyle = remember(shape, opticalTint, isDark) {
        GlassStyle.regular.then {
            shape(shape as? RoundedCornerShape ?: RoundedCornerShape(AppRadius.large))
            tint(opticalTint)
            optics { copy(refractionStrength = 0.5f, refractionDisplacement = 10.dp) }
            // Use the app theme even when it differs from Android's system theme.
            specularIntensity(if (isDark) 0.38f else 0.6f)
            edgeShadow(Color.Black.copy(alpha = if (isDark) 0.32f else 0.12f))
            ambientResponse(if (isDark) 0.08f else 0.12f)
            contrast(if (isDark) 0.08f else 0f)
            whitePoint(if (isDark) -0.22f else 0f)
            chromaMultiplier(1.1f)
            chromaticAberrationStrength(0f)
        }
    }
    val blurStyle = remember(actualTint) { HazeBlurStyle {
        backgroundColor(Color.Transparent)
        blurRadius(24.dp)
        colorEffects(listOf(HazeColorEffect.tint(actualTint)))
        noiseFactor(0.02f)
    } }
    val styledModifier = when {
        glassActive && hazeState != null && shape is RoundedCornerShape && Build.VERSION.SDK_INT >= 33 ->
            baseModifier.hazeGlass(input = HazeInput.Sources(hazeState), style = glassStyle)
        glassActive && hazeState != null ->
            baseModifier.hazeBlur(input = HazeInput.Sources(hazeState), style = blurStyle).then(specularAndInnerShadow)
        else -> baseModifier.background(color = actualBackground, shape = shape).then(specularAndInnerShadow)
    }

    Box(
        modifier = styledModifier,
        content = content
    )
}
