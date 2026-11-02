package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated

/**
 * Reader masthead: back, title with a page counter, search, bookmark and overflow.
 * Material 3 surface with a standard divider; no gradient rule.
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
    val scheme = MaterialTheme.colorScheme
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
        Surface(color = scheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = scheme.onSurface
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = formatMiddleTruncated(title, maxChars = 30),
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pageCount > 0) {
                            Text(
                                text = "Page ${currentPage + 1} of $pageCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search text in document",
                            tint = scheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove page bookmark" else "Add page bookmark",
                            tint = if (isBookmarked) scheme.primary else scheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { isMoreMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More options",
                                tint = scheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                        expanded = isMoreMenuOpen,
                        onDismissRequest = { isMoreMenuOpen = false }
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

                HorizontalDivider(color = scheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun OverflowAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
