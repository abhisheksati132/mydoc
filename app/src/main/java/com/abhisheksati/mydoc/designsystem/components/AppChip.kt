package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * The only chip in the app.
 *
 * One surface rule everywhere: selected chips are stamped in ink, unselected
 * chips sit on a quiet container with a hairline. Category hue is allowed to
 * reach the icon only — [accentTint] — so recognition survives without turning
 * the chip row into a colour chart.
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

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.ink else colors.surfaceContainer,
        animationSpec = tween(durationMillis = AppMotion.FAST),
        label = "chipContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.background else colors.textSecondary,
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
        Surface(
            shape = AppRadius.pillShape,
            color = containerColor,
            border = BorderStroke(0.5.dp, borderColor),
            modifier = Modifier
                .clip(AppRadius.pillShape)
                .pressScaleEffect(onClick = onClick)
        ) {
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
