package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.animation.LocalMotionStyle
import com.abhisheksati.mydoc.designsystem.animation.MotionStyle
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

/** Anchor-aware menu placement, including edge, RTL and small-window fallbacks. */
internal fun menuPosition(
    anchor: IntRect, window: IntSize, menu: IntSize, direction: LayoutDirection,
    offset: IntOffset, margin: Int
): IntOffset {
    val preferredX = if (direction == LayoutDirection.Ltr) anchor.left + offset.x
        else anchor.right - menu.width - offset.x
    val below = anchor.bottom + offset.y
    val above = anchor.top - menu.height - offset.y
    val preferredY = when {
        below + menu.height <= window.height - margin -> below
        above >= margin -> above
        else -> below
    }
    return IntOffset(
        preferredX.coerceIn(margin, maxOf(margin, window.width - menu.width - margin)),
        preferredY.coerceIn(margin, maxOf(margin, window.height - menu.height - margin))
    )
}

/** Frosted menus unfold from their trigger; native popup focus and dismissal are preserved. */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    if (!visibility.currentState && !visibility.targetState) return
    val reduced = LocalReducedMotion.current
    val expressive = LocalMotionStyle.current == MotionStyle.EXPRESSIVE
    val transition = rememberTransition(visibility, label = "menuVisibility")
    val progress by transition.animateFloat(transitionSpec = {
        if (reduced) snap() else tween(if (targetState) AppMotion.BASE else AppMotion.FAST)
    }, label = "menuUnfold") { if (it) 1f else 0f }
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(TransformOrigin(0.5f, 0f)) }
    val positionProvider = remember(density, offset) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
                layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val position = menuPosition(anchorBounds, windowSize, popupContentSize, layoutDirection,
                    with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) },
                    with(density) { 8.dp.roundToPx() })
                // Keep the optical pivot on the trigger, including menus opening upwards.
                origin = TransformOrigin(
                    ((anchorBounds.center.x - position.x).toFloat() / popupContentSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                    ((anchorBounds.center.y - position.y).toFloat() / popupContentSize.height.coerceAtLeast(1)).coerceIn(0f, 1f))
                return position
            }
        }
    }
    Popup(popupPositionProvider = positionProvider, onDismissRequest = onDismissRequest, properties = properties) {
        GlassSurface(
            modifier = modifier.graphicsLayer {
                transformOrigin = origin
                scaleX = if (reduced) 1f else (if (expressive) 0.72f else 0.96f) + (if (expressive) 0.28f else 0.04f) * progress
                scaleY = if (reduced) 1f else (if (expressive) 0.25f else 0.96f) + (if (expressive) 0.75f else 0.04f) * progress
                alpha = progress
            },
            shape = AppRadius.xLargeShape,
            material = GlassMaterial.MENU,
            backgroundColor = AppTheme.colors.surface,
            elevation = 8.dp
        ) {
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.width(IntrinsicSize.Max).verticalScroll(scrollState).padding(vertical = 8.dp), content = content)
            }
        }
    }
}
