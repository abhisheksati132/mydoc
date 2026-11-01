package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsDark
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures

/**
 * The app header. One implementation for Home, Files, Tools and Settings:
 * 56dp action row, status-bar inset, Bricolage title on the left,
 * balanced 48dp action buttons on the right.
 */
@Composable
fun AppTabHeader(
    title: String,
    titleContent: (@Composable () -> Unit)? = null,
    kicker: String? = null,
    meta: String? = null,
    onSearchClick: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    searchContent: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    moreMenuContent: (@Composable ColumnScope.(onDismiss: () -> Unit) -> Unit)? = null,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isDark = LocalIsDark.current

    var isMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // Top action row: kicker, title, then the action rail
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    Column {
                        if (kicker != null) {
                            Text(
                                text = kicker,
                                style = typography.overline,
                                color = colors.inkMuted
                            )
                        }
                        Text(
                            text = title,
                            style = typography.title,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (meta != null) {
                    Text(
                        text = meta,
                        style = typography.overline.withTabularFigures(),
                        color = colors.inkMuted,
                        modifier = Modifier.padding(start = 8.dp)
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
                        onClick = onSearchClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) colors.accent else colors.textPrimary
                        )
                    }
                }
                if (onToggleTheme != null) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = colors.textPrimary
                        )
                    }
                }
// 3. Action 3: Contextual Overflow Menu (with nested Settings) OR Standalone Settings Icon
                if (moreMenuContent != null) {
                    Box {
                        IconButton(
                            onClick = { isMenuOpen = true },
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
                            shape = AppRadius.pillShape,
                            containerColor = colors.surface,
                            border = BorderStroke(
                                AppSpacing.hairline,
                                colors.line
                            ),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
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
                                        onSettingsClick()
                                    }
                                )
                            }
                        }
                    }
                } else if (onSettingsClick != null) {
                    IconButton(
                        onClick = onSettingsClick,
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

        // The masthead rule closes the page title, before any filters or content.
        EditorialRule()

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
