package com.abhisheksati.mydoc.designsystem.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.ContinuousCornerShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.max

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
 * Scroll-driven glass response: 0f at rest, ramps to 1f over the first
 * [RAMP_PX] of scroll. Drives progressive blur radius and tint density so
 * chrome thickens like real glass under moving content.
 */
@Stable
class GlassMotionState(initial: Float = 0f) {
    var progress: Float by mutableFloatStateOf(initial)
    var interaction: Float by mutableFloatStateOf(0f)

    companion object {
        const val RAMP_PX = 240f
    }
}

@Composable
fun TrackGlassMotion(listState: LazyListState, motion: GlassMotionState) {
    LaunchedEffect(listState, motion) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { offset ->
                motion.progress = (offset / GlassMotionState.RAMP_PX).coerceIn(0f, 1f)
            }
    }
}

@Composable
fun TrackGlassMotion(gridState: LazyGridState, motion: GlassMotionState) {
    LaunchedEffect(gridState, motion) {
        snapshotFlow { gridState.firstVisibleItemScrollOffset }
            .collect { offset ->
                motion.progress = (offset / GlassMotionState.RAMP_PX).coerceIn(0f, 1f)
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

/**
 * The core "Paper under glass" chrome surface.
 *
 * Optics model (Apple Liquid Glass anatomy):
 * 1. Near-transparent base — the Haze style's backgroundColor is transparent so
 *    the blurred content itself supplies the color (vibrancy), instead of an
 *    opaque plate painted over the blur.
 * 2. Low-alpha tint that thickens with scroll via [motion].
 * 3. Fine noise (0.04) so large blurred fields don't band.
 * 4. Directional specular edge: a top-arc highlight fading out mid-face, not a
 *    uniform all-around stroke.
 * 5. Inner shadow drawn UNDER content — occlusion, not a film on top.
 * 6. Tinted drop shadows in light mode (accent-tinted spot).
 * 7. Continuous-curvature corners by default ([ContinuousCornerShape]).
 *
 * Falls back to a solid plate on API < 31, battery saver, low-RAM, or when no
 * [hazeState] is supplied (e.g. modal sheets, which live in their own window
 * and cannot sample the app window behind them).
 */
// Vibrancy: luminance-preserving saturation lift (s = 1.18) applied to the live
// blur so content color reads louder through the glass instead of muted.
private val Vibrancy = ColorMatrix(
    floatArrayOf(
        1.14166f, -0.12870f, -0.01296f, 0f, 0f,
        -0.03834f, 1.05130f, -0.01296f, 0f, 0f,
        -0.03834f, -0.12870f, 1.16704f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
)

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

    val progress = max(motion?.progress ?: 0f, (motion?.interaction ?: 0f) * 0.6f)

    // Directional specular: bright top arc fading to nothing by mid-face,
    // with only a whisper of occlusion along the bottom edge.
    val specularAndInnerShadow = Modifier.drawWithCache {
        val specular = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to actualRimTop,
                0.55f to Color.Transparent,
                1f to actualRimBottom.copy(alpha = actualRimBottom.alpha * 0.5f)
            )
        )
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
                drawPath(outlinePath, brush = specular, style = stroke)
            } else {
                drawRect(brush = specular, style = stroke)
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

    val styledModifier = if (glassActive && hazeState != null) {
        baseModifier
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color.Transparent,
                    tint = HazeTint(
                        actualTint.copy(
                            alpha = actualTint.alpha * (0.75f + 0.25f * progress)
                        )
                    ),
                    blurRadius = lerp(12.dp, 28.dp, progress),
                    noiseFactor = 0.04f
                )
            )
            .graphicsLayer { colorFilter = ColorFilter.colorMatrix(Vibrancy) }
            .then(specularAndInnerShadow)
    } else {
        // Solid fallback: plates the surface where live blur cannot run (old
        // APIs, battery saver, or modal windows that cannot sample the app
        // window behind them). Callers may pass a translucent
        // [backgroundColor] to keep some depth.
        baseModifier
            .background(color = actualBackground, shape = shape)
            .then(specularAndInnerShadow)
    }

    Box(
        modifier = styledModifier,
        content = content
    )
}
