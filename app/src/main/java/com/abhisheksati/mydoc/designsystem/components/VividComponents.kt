package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.VividGradients
import com.abhisheksati.mydoc.designsystem.tokens.glow

/**
 * Ambient aurora wash: three slow-breathing radial blooms in the brand spectrum,
 * painted behind screen content. Subtle enough to read as light, not pattern.
 */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )

    val violet = colors.accent.copy(alpha = if (AppTheme.isDark) 0.14f else 0.13f)
    val pink = colors.accentPink.copy(alpha = if (AppTheme.isDark) 0.11f else 0.10f)
    val cyan = colors.accentCyan.copy(alpha = if (AppTheme.isDark) 0.08f else 0.09f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val d = drift

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(violet, Color.Transparent),
                center = Offset(w * (0.1f + 0.08f * d), h * 0.02f),
                radius = w * (0.75f + 0.08f * d)
            ),
            center = Offset(w * (0.1f + 0.08f * d), h * 0.02f),
            radius = w * (0.75f + 0.08f * d)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(pink, Color.Transparent),
                center = Offset(w * (0.95f - 0.06f * d), h * 0.1f),
                radius = w * (0.6f + 0.06f * d)
            ),
            center = Offset(w * (0.95f - 0.06f * d), h * 0.1f),
            radius = w * (0.6f + 0.06f * d)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cyan, Color.Transparent),
                center = Offset(w * 0.5f, h * (0.42f + 0.03f * d)),
                radius = w * 0.65f
            ),
            center = Offset(w * 0.5f, h * (0.42f + 0.03f * d)),
            radius = w * 0.65f
        )
    }
}

/**
 * The signature gradient rule: a 3dp spectrum line that closes headers and
 * replaces the old ink rule. One consistent stroke of brand color everywhere.
 */
@Composable
fun VividRule(modifier: Modifier = Modifier, thickness: Dp = 1.dp) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * A gradient icon plaque with a matching colored glow — the atom of the vivid
 * system. Sits inside cards, hero panels and tool rows.
 */
@Composable
fun VividIconTile(
    icon: ImageVector,
    family: VividGradients.Family,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    shape: Shape = AppRadius.largeShape,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .glow(VividGradients.glowColor(family), shape, elevation = size / 5)
            .clip(shape)
            .background(VividGradients.brush(family)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}
