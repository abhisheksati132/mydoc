package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Slideshow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.VividRule
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures

/**
 * The reader's masthead: back, document title with a tabular page counter, search,
 * bookmark, and the overflow index. Closed by the 3dp spectrum rule — the same
 * signature stroke as every other header in the app.
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
    onReflowClick: (() -> Unit)? = null,
    onDocumentDetailsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
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
                .background(colors.background)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.listRowHeight)
                    .padding(horizontal = AppSpacing.xSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarIconAction(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = colors.ink,
                    onClick = onBackClick
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AppSpacing.small)
                ) {
                    Text(
                        text = formatMiddleTruncated(title, maxChars = 30),
                        style = typography.titleSmall,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (pageCount > 0) {
                        Text(
                            text = "PAGE ${(currentPage + 1).toString().padStart(2, '0')} / " +
                                pageCount.toString().padStart(2, '0'),
                            style = typography.overline.withTabularFigures(),
                            color = colors.inkMuted,
                            maxLines = 1
                        )
                    }
                }

                BarIconAction(
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search text in document",
                    tint = colors.inkMuted,
                    onClick = onSearchClick
                )

                BarIconAction(
                    icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove page bookmark" else "Add page bookmark",
                    tint = if (isBookmarked) colors.accent else colors.inkMuted,
                    onClick = onToggleBookmark
                )

                Box {
                    BarIconAction(
                        icon = Icons.Outlined.MoreVert,
                        contentDescription = "More options",
                        tint = colors.inkMuted,
                        onClick = { isMoreMenuOpen = true }
                    )

                    DropdownMenu(
                        expanded = isMoreMenuOpen,
                        onDismissRequest = { isMoreMenuOpen = false },
                        shape = AppRadius.pillShape,
                        containerColor = colors.surface,
                        border = BorderStroke(AppSpacing.hairline, colors.line),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        OverflowAction(
                            label = "Share document",
                            icon = Icons.Outlined.Share,
                            onClick = {
                                isMoreMenuOpen = false
                                onShareClick()
                            }
                        )

                        if (onTtsClick != null) {
                            OverflowAction(
                                label = "Read aloud",
                                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                onClick = {
                                    isMoreMenuOpen = false
                                    onTtsClick()
                                }
                            )
                        }

                        if (onOcrClick != null) {
                            OverflowAction(
                                label = "Extract text",
                                icon = Icons.Outlined.TextFields,
                                onClick = {
                                    isMoreMenuOpen = false
                                    onOcrClick()
                                }
                            )
                        }

                        if (onToolsClick != null) {
                            OverflowAction(
                                label = "PDF tools",
                                icon = Icons.Outlined.Build,
                                onClick = {
                                    isMoreMenuOpen = false
                                    onToolsClick()
                                }
                            )
                        }

                        if (onSlideshowClick != null) {
                            OverflowAction(
                                label = "Start slideshow",
                                icon = Icons.Outlined.Slideshow,
                                onClick = {
                                    isMoreMenuOpen = false
                                    onSlideshowClick()
                                }
                            )
                        }

                        if (onReflowClick != null) {
                            OverflowAction(
                                label = "Reflow reading mode",
                                icon = Icons.Outlined.TextFields,
                                onClick = {
                                    isMoreMenuOpen = false
                                    onReflowClick()
                                }
                            )
                        }

                        if (onDocumentDetailsClick != null) {
                            OverflowAction(
                                label = "Document details",
                                icon = Icons.Outlined.Info,
                                onClick = {
                                    isMoreMenuOpen = false
                                    onDocumentDetailsClick()
                                }
                            )
                        }
                    }
                }
            }

            VividRule()
        }
    }
}

@Composable
private fun BarIconAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(AppSpacing.minTouchTarget)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun OverflowAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    DropdownMenuItem(
        text = { Text(label, style = typography.body, color = colors.ink) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(18.dp)
            )
        },
        onClick = onClick
    )
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
