package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Clean, uncluttered, and ergonomic top bar for the document viewer.
 * Keeps core actions (Back, Title, Search, Bookmark, Tools, More) instantly accessible
 * without horizontal crowding or overflowing on mobile screens.
 */
@Composable
fun ViewerTopBar(
    title: String,
    visible: Boolean,
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    shelfColor: Color? = null,
    onToolsClick: (() -> Unit)? = null,
    onOcrClick: (() -> Unit)? = null,
    onTtsClick: (() -> Unit)? = null,
    onSlideshowClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onThumbnailGridClick: (() -> Unit)? = null,
    onReadingModeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    var isMoreMenuOpen by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
        modifier = modifier
    ) {
        val isOled = colors.background == DarkAppColors.background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isOled) Color(0xFF000000) else colors.surfaceContainer)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }

                // Middle document title
                Text(
                    text = formatMiddleTruncated(title, maxChars = 28),
                    style = typography.title,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )

                // Search button
                if (onSearchClick != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSearchClick()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search text",
                            tint = colors.textSecondary
                        )
                    }
                }

                // Bookmark toggle
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleBookmark()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (isBookmarked) (shelfColor ?: colors.accent) else colors.textSecondary
                    )
                }

                // PDF Tools Button (Prominent)
                if (onToolsClick != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToolsClick()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Build,
                            contentDescription = "PDF Tools",
                            tint = colors.accent
                        )
                    }
                }

                // Overflow / More Menu
                Box {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isMoreMenuOpen = true
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More actions",
                            tint = colors.textSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = isMoreMenuOpen,
                        onDismissRequest = { isMoreMenuOpen = false }
                    ) {
                        // Share
                        DropdownMenuItem(
                            text = { Text("Share document", style = typography.body) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, contentDescription = null, tint = colors.textSecondary)
                            },
                            onClick = {
                                isMoreMenuOpen = false
                                onShareClick()
                            }
                        )

                        // Page Grid Overview
                        if (onThumbnailGridClick != null) {
                            DropdownMenuItem(
                                text = { Text("Page thumbnails grid", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.GridView, contentDescription = null, tint = colors.textSecondary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onThumbnailGridClick()
                                }
                            )
                        }

                        // Reading theme
                        if (onReadingModeClick != null) {
                            DropdownMenuItem(
                                text = { Text("Reading theme & settings", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Brightness4, contentDescription = null, tint = colors.textSecondary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onReadingModeClick()
                                }
                            )
                        }

                        // OCR text extraction
                        if (onOcrClick != null) {
                            DropdownMenuItem(
                                text = { Text("Extract text (OCR)", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.TextFields, contentDescription = null, tint = colors.textSecondary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onOcrClick()
                                }
                            )
                        }

                        // Text-to-speech Read Aloud
                        if (onTtsClick != null) {
                            DropdownMenuItem(
                                text = { Text("Read aloud (TTS)", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = colors.textSecondary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onTtsClick()
                                }
                            )
                        }

                        // Slideshow mode
                        if (onSlideshowClick != null) {
                            DropdownMenuItem(
                                text = { Text("Start presentation slideshow", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Slideshow, contentDescription = null, tint = colors.textSecondary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onSlideshowClick()
                                }
                            )
                        }
                    }
                }
            }

            AppDivider()
        }
    }
}
