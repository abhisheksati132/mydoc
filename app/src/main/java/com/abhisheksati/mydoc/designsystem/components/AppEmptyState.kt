package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Double-Bezel Architecture & Button-in-Button CTA Empty State (Taste Design System).
 */
@Composable
fun AppEmptyState(
    message: String,
    title: String? = null,
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
        targetValue = if (isBtnPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "emptyStateBtnScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Shell (Doppelrand / Double-Bezel)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isOled) Color(0x1AFFFFFF) else colors.surfaceVariant.copy(alpha = 0.4f)
                )
                .border(
                    BorderStroke(
                        0.8.dp,
                        if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(6.dp)
        ) {
            // Inner Core
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isOled) Color(0xFF0C0D11) else colors.surface
                    )
                    .border(
                        BorderStroke(
                            0.5.dp,
                            if (isOled) Color.White.copy(alpha = 0.05f) else Color.Transparent
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Eyebrow tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.accent.copy(alpha = 0.12f))
                            .border(
                                BorderStroke(0.6.dp, colors.accent.copy(alpha = 0.3f)),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "STATUS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (title != null) {
                        Text(
                            text = title,
                            style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = message,
                        style = typography.body.copy(lineHeight = 20.sp),
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (actionLabel != null && effectiveAction != null) {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Button-in-Button CTA
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
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
                                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            Text(
                                text = actionLabel,
                                style = typography.label.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = if (isOled) Color(0xFF000000) else Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isOled) Color(0x33000000) else Color(0x33FFFFFF)
                                    )
                            ) {
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
        }
    }
}
