package com.abhisheksati.mydoc.designsystem.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.HomeTab

/**
 * Shelves Solid Bottom Bar (Replacing Floating Pill Dock).
 *
 * Rules:
 * - Solid theme background (content never shows through).
 * - 1dp top line divider.
 * - 72dp bar height + system navigation bar inset.
 * - Three equal labelled items: Now, Library, Tools.
 * - Active item: ink-coloured semibold label with an 8dp dot in the accent to its left.
 * - Inactive item: inkMuted, medium weight.
 * - No pill, no floating dock, no coloured background.
 */
@Composable
fun ShelvesBottomBar(
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
        // 1dp top hairline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.hairline)
                .background(colors.line)
        )

        // 72dp bar content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.navBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
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
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = tab.label,
                            style = if (isSelected) {
                                typography.body.copy(fontWeight = FontWeight.SemiBold)
                            } else {
                                typography.body.copy(fontWeight = FontWeight.Medium)
                            },
                            color = if (isSelected) colors.ink else colors.inkMuted
                        )
                    }
                }
            }
        }
    }
}
