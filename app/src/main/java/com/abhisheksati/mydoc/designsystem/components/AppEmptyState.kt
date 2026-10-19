package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Clean, breathable, free-flowing Empty State.
 * Focuses on generous whitespace, clear typography, and tactile interaction without claustrophobic nested boxes.
 */
@Composable
fun AppEmptyState(
    message: String,
    title: String? = null,
    icon: ImageVector = Icons.Outlined.Description,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onAction: (() -> Unit)? = onActionClick,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = colors.background == DarkAppColors.background
    val effectiveAction = onAction ?: onActionClick

    val btnInteractionSource = remember { MutableInteractionSource() }
    val isBtnPressed by btnInteractionSource.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        targetValue = if (isBtnPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "emptyStateBtnScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            // Understated minimal line icon (Apple / Linear style)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = typography.title.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = message,
                style = typography.body.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                ),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && effectiveAction != null) {
                Spacer(modifier = Modifier.height(24.dp))

                // Clean tactile pill button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = btnScale
                            scaleY = btnScale
                        }
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accent)
                        .clickable(
                            interactionSource = btnInteractionSource,
                            indication = null,
                            onClick = effectiveAction
                        )
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = typography.label.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = if (isOled) Color(0xFF000000) else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = if (isOled) Color(0xFF000000) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
