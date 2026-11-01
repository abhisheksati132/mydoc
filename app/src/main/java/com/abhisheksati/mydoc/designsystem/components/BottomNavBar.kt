package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.VividGradients
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.HomeTab

/**
 * Root navigation as a floating glass pill: three entries in one rounded bar,
 * the active one wearing the brand gradient with white content. No hairlines —
 * depth comes from the bar's soft shadow and the gradient's glow.
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
    val barShape = AppRadius.roundShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.navBarHeight)
                .shadow(14.dp, barShape, ambientColor = colors.accent.copy(alpha = 0.25f), spotColor = colors.accent.copy(alpha = 0.25f))
                .clip(barShape)
                .background(colors.surface)
                .border(1.dp, colors.line, barShape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val icon: ImageVector = if (isSelected) when (tab) {
                    HomeTab.HOME -> Icons.Filled.Home
                    HomeTab.FILES -> Icons.Filled.Folder
                    HomeTab.TOOLS -> Icons.Filled.Build
                } else when (tab) {
                    HomeTab.HOME -> Icons.Outlined.Home
                    HomeTab.FILES -> Icons.Outlined.Folder
                    HomeTab.TOOLS -> Icons.Outlined.Build
                }

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else colors.inkMuted,
                    animationSpec = tween(durationMillis = AppMotion.FAST),
                    label = "navContentColor"
                )
                val gradientAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = AppMotion.FAST),
                    label = "navGradient"
                )

                val interactionSource = remember { MutableInteractionSource() }
                val pillShape = AppRadius.pillShape

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(6.dp)
                        .clip(pillShape)
                        .pressScale(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptics.tick()
                                onTabSelected(tab)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = gradientAlpha }
                            .background(VividGradients.brand)
                    )
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xSmall + 2.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tab.label,
                            style = typography.captionStrong,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
