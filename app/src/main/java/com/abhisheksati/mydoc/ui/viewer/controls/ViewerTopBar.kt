package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures

/**
 * Clean, uncluttered, and ergonomic top bar for document viewers.
 * Strictly presents: Back, Title with Page Subtitle, Search, Bookmark, and Overflow Menu.
 * Complies with 48dp minimum touch targets and eliminates horizontal crowding.
 */
@Composable
fun ViewerTopBar(
    title: String,
    currentPage: Int,
    pageCount: Int,
    visible: Boolean,
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShareClick: () -> Unit,
    onToolsClick: (() -> Unit)? = null,
    onOcrClick: (() -> Unit)? = null,
    onTtsClick: (() -> Unit)? = null,
    onSlideshowClick: (() -> Unit)? = null,
    onDocumentDetailsClick: (() -> Unit)? = null,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainer)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (48dp touch target)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = colors.textPrimary
                    )
                }

                // Title and Page Subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = formatMiddleTruncated(title, maxChars = 28),
                        style = typography.title,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (pageCount > 0) {
                        Text(
                            text = "Page ${currentPage + 1} of $pageCount",
                            style = typography.caption.withTabularFigures(),
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }

                // In-Viewer Search
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchClick()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search text in document",
                        tint = colors.textSecondary
                    )
                }

                // Bookmark Toggle
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleBookmark()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove page bookmark" else "Add page bookmark",
                        tint = if (isBookmarked) colors.accent else colors.textSecondary
                    )
                }

                // Three-dot Overflow Menu
                Box {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isMoreMenuOpen = true
                        },
                        modifier = Modifier.size(AppSpacing.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = colors.textSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = isMoreMenuOpen,
                        onDismissRequest = { isMoreMenuOpen = false }
                    ) {
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

                        if (onToolsClick != null) {
                            DropdownMenuItem(
                                text = { Text("PDF tools", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Build, contentDescription = null, tint = colors.accent)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onToolsClick()
                                }
                            )
                        }

                        if (onSlideshowClick != null) {
                            DropdownMenuItem(
                                text = { Text("Start slideshow", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Slideshow, contentDescription = null, tint = colors.textSecondary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onSlideshowClick()
                                }
                            )
                        }

                        if (onDocumentDetailsClick != null) {
                            DropdownMenuItem(
                                text = { Text("Document details", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textSecondary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onDocumentDetailsClick()
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

@Preview(name = "ViewerTopBar - Light")
@Composable
private fun ViewerTopBarLightPreview() {
    com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme(darkTheme = false, amoledBlack = false) {
        ViewerTopBar(
            title = "Annual Financial Report 2026.pdf",
            currentPage = 4,
            pageCount = 32,
            visible = true,
            isBookmarked = true,
            onBackClick = {},
            onSearchClick = {},
            onToggleBookmark = {},
            onShareClick = {}
        )
    }
}

@Preview(name = "ViewerTopBar - Dark")
@Composable
private fun ViewerTopBarDarkPreview() {
    com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme(darkTheme = true, amoledBlack = false) {
        ViewerTopBar(
            title = "Annual Financial Report 2026.pdf",
            currentPage = 4,
            pageCount = 32,
            visible = true,
            isBookmarked = false,
            onBackClick = {},
            onSearchClick = {},
            onToggleBookmark = {},
            onShareClick = {}
        )
    }
}

@Preview(name = "ViewerTopBar - AMOLED")
@Composable
private fun ViewerTopBarAmoledPreview() {
    com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme(darkTheme = true, amoledBlack = true) {
        ViewerTopBar(
            title = "Annual Financial Report 2026.pdf",
            currentPage = 4,
            pageCount = 32,
            visible = true,
            isBookmarked = true,
            onBackClick = {},
            onSearchClick = {},
            onToggleBookmark = {},
            onShareClick = {}
        )
    }
}

