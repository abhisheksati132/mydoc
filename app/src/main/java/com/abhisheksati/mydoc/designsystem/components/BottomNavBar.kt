package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.HomeTab

/**
 * Editorial Studio Navigation Bar: Home | Files | Tools.
 * Features:
 * - Crisp 0.5dp hairline top divider.
 * - Restrained brand ink-violet active indicator pill.
 * - Filled vs outlined iconography for instant visual distinction.
 * - Subtle spring animations with tactile haptic feedback.
 */
@Composable
fun BottomNavBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    val isDark = colors.background != Color(0xFFF7F7F4)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(
                width = 0.5.dp,
                color = colors.line,
                shape = RectangleShape
            )
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                val icon: ImageVector = if (isSelected) when (tab) {
                    HomeTab.HOME -> Icons.Filled.Home
                    HomeTab.FILES -> Icons.Filled.Folder
                    HomeTab.TOOLS -> Icons.Filled.Build
                } else when (tab) {
                    HomeTab.HOME -> Icons.Outlined.Home
                    HomeTab.FILES -> Icons.Outlined.Folder
                    HomeTab.TOOLS -> Icons.Outlined.Build
                }

                val pillWidth by animateDpAsState(
                    targetValue = if (isSelected) 60.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = 0.76f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "pillWidth"
                )

                val pillBg = if (isSelected) {
                    colors.accent.copy(alpha = if (isDark) 0.16f else 0.10f)
                } else Color.Transparent

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) colors.accent else colors.inkMuted,
                    animationSpec = tween(durationMillis = 180),
                    label = "tabIconColor"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colors.accent else colors.inkMuted,
                    animationSpec = tween(durationMillis = 180),
                    label = "tabTextColor"
                )

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "iconScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = false, radius = 32.dp),
                            onClick = {
                                if (!isSelected) {
                                    haptics.tick()
                                    onTabSelected(tab)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Editorial Active Pill
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) pillWidth else 44.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(pillBg)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 0.5.dp,
                                            color = colors.accent.copy(alpha = if (isDark) 0.35f else 0.20f),
                                            shape = RoundedCornerShape(15.dp)
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(iconScale)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = typography.caption.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
