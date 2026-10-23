package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.HomeTab

/**
 * Solid, premium 2-destination Bottom Bar: Documents and Tools.
 */
@Composable
fun HomeBottomBar(
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
            .background(colors.background)
            .navigationBarsPadding()
    ) {
        // Top hairline divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.hairline)
                .background(colors.line)
        )

        // 64dp bar content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                val icon: ImageVector = when (tab) {
                    HomeTab.DOCUMENTS -> Icons.AutoMirrored.Outlined.Article
                    HomeTab.TOOLS -> Icons.Outlined.Build
                }

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) colors.accent else colors.inkMuted,
                    label = "tabIconColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colors.ink else colors.inkMuted,
                    label = "tabTextColor"
                )
                val containerBg by animateColorAsState(
                    targetValue = if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.background,
                    label = "tabContainerBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(containerBg)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    haptics.select()
                                    onTabSelected(tab)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = tab.label,
                            style = typography.body.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility alias for ShelvesBottomBar.
 */
@Deprecated("Use HomeBottomBar instead", ReplaceWith("HomeBottomBar(currentTab, onTabSelected, modifier)"))
@Composable
fun ShelvesBottomBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    HomeBottomBar(currentTab, onTabSelected, modifier)
}
