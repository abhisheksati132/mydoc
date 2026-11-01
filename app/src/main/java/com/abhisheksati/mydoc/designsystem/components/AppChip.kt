package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.VividGradients

/**
 * The only chip in the app.
 *
 * Vivid rule: the selected chip wears the brand gradient and white content;
 * unselected chips sit on a quiet container with a hairline border. Category
 * hue reaches the icon only — [accentTint] — so recognition survives.
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    accentTint: Color? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val gradientAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = AppMotion.FAST),
        label = "chipGradient"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else colors.textSecondary,
        animationSpec = tween(durationMillis = AppMotion.FAST),
        label = "chipContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else colors.line,
        animationSpec = tween(durationMillis = AppMotion.FAST),
        label = "chipBorder"
    )

    Box(
        modifier = modifier.defaultMinSize(minHeight = AppSpacing.minTouchTarget),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(AppRadius.pillShape)
                .background(colors.surfaceContainer)
                .border(BorderStroke(0.5.dp, borderColor), AppRadius.pillShape)
                .pressScaleEffect(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = gradientAlpha }
                    .background(VividGradients.brand)
            )
            Row(
                modifier = Modifier.padding(
                    horizontal = AppSpacing.medium,
                    vertical = AppSpacing.small
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (selected) contentColor else accentTint ?: contentColor,
                        modifier = Modifier.defaultMinSize(16.dp, 16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xSmall + 2.dp))
                }
                Text(
                    text = label,
                    style = if (selected) typography.captionStrong else typography.caption,
                    color = contentColor
                )
            }
        }
    }
}
