package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Standard symmetrical app header across all 4 root tabs (Files, Documents, Media, Tools).
 * Guarantees identical 56dp action row height, statusBarsPadding(), title styling, and balanced action buttons.
 */
@Composable
fun AppTabHeader(
    title: String,
    titleContent: (@Composable () -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    searchContent: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    moreMenuContent: (@Composable ColumnScope.(onDismiss: () -> Unit) -> Unit)? = null,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val isOled = colors.background == DarkAppColors.background

    var isMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // Top 56dp Action Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Left Title / Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                }
                if (titleContent != null) {
                    titleContent()
                } else {
                    Text(
                        text = title,
                        style = typography.title.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Balanced Action Buttons (Consistent trio: Search, Theme Toggle, Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                extraActions?.invoke(this)

                // 1. Search Action (if enabled)
                if (onSearchClick != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSearchClick()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) colors.accent else colors.textPrimary
                        )
                    }
                }
// 3. Action 3: Contextual Overflow Menu (with nested Settings) OR Standalone Settings Icon
                if (moreMenuContent != null) {
                    Box {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isMenuOpen = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More options",
                                tint = colors.textPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = if (isOled) Color(0xFF141720) else colors.surfaceContainer,
                            border = BorderStroke(
                                1.dp,
                                if (isOled) Color.White.copy(alpha = 0.10f) else colors.cardBorder
                            ),
                            shadowElevation = 8.dp,
                            tonalElevation = 4.dp
                        ) {
                            moreMenuContent { isMenuOpen = false }
                            if (onSettingsClick != null) {
                                HorizontalDivider(
                                    color = colors.divider.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Settings",
                                            style = typography.body,
                                            color = colors.textPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = null,
                                            tint = colors.accent
                                        )
                                    },
                                    onClick = {
                                        isMenuOpen = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSettingsClick()
                                    }
                                )
                            }
                        }
                    }
                } else if (onSettingsClick != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSettingsClick()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colors.textPrimary
                        )
                    }
                }
            }
        }

        // Search Field (animated slide down when active)
        if (searchContent != null) {
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    searchContent()
                }
            }
        }

        // Bottom content (e.g. category filter chips)
        if (bottomContent != null) {
            bottomContent()
        }
    }
}
