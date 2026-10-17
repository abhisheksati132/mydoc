package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

/**
 * Modifier extension to apply Apple-style frosted glassmorphism:
 * - Translucent surface
 * - Specular dual-edge refraction border
 * - Smooth squircle clipping
 */
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(AppSpacing.cardCornerRadius),
    surfaceColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    ambientGlow: Color? = null
): Modifier = this
    .then(
        if (ambientGlow != null && ambientGlow != Color.Transparent) {
            Modifier.drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ambientGlow.copy(alpha = 0.28f), Color.Transparent),
                        center = center,
                        radius = size.maxDimension * 0.7f
                    )
                )
            }
        } else Modifier
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                (if (surfaceColor != Color.Unspecified) surfaceColor else Color(0x33FFFFFF)),
                (if (surfaceColor != Color.Unspecified) surfaceColor.copy(alpha = (surfaceColor.alpha * 0.8f).coerceIn(0.1f, 1f)) else Color(0x18FFFFFF))
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                (if (borderColor != Color.Unspecified) borderColor else Color.White.copy(alpha = 0.35f)),
                (if (borderColor != Color.Unspecified) borderColor.copy(alpha = (borderColor.alpha * 0.3f).coerceIn(0.05f, 1f)) else Color.White.copy(alpha = 0.08f))
            )
        ),
        shape = shape
    )

/**
 * Reusable Apple Glassmorphic Card Container.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppSpacing.cardCornerRadius),
    backgroundColor: Color = AppTheme.colors.glassSurface,
    borderColor: Color = AppTheme.colors.glassBorder,
    ambientGlow: Color? = null,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val clickModifier = if (onClick != null) {
        Modifier.combinedClickable(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            onLongClick = onLongClick?.let {
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    it()
                }
            }
        )
    } else Modifier

    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp) Modifier.shadow(elevation, shape, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                else Modifier
            )
            .glassmorphic(
                shape = shape,
                surfaceColor = backgroundColor,
                borderColor = borderColor,
                ambientGlow = ambientGlow
            )
            .then(clickModifier),
        content = content
    )
}
