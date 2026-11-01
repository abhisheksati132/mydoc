package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.HomeTab

/**
 * Root navigation as a printed footer: three entries on one line, the active one set
 * in ink and underscored by a 2dp rule. No pill, no tint — the rule is the indicator
 * and matches the rules that open every section above it.
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .navigationBarsPadding()
    ) {
        EditorialRule(thickness = AppSpacing.hairline, color = colors.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.navBarHeight),
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
                    targetValue = if (isSelected) colors.ink else colors.inkMuted,
                    animationSpec = tween(durationMillis = AppMotion.FAST),
                    label = "navContentColor"
                )
                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 0.dp,
                    animationSpec = tween(durationMillis = AppMotion.FAST),
                    label = "navIndicator"
                )
                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = AppMotion.FAST),
                    label = "navIndicatorAlpha"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressScale(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptics.tick()
                                onTabSelected(tab)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
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
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                    Box(
                        modifier = Modifier
                            .width(indicatorWidth)
                            .height(AppSpacing.rule)
                            .background(colors.ink.copy(alpha = indicatorAlpha))
                    )
                }
            }
        }
    }
}
