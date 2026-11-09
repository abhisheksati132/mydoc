package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.DropdownMenuItem
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

/**
 * Editorial "Ink & Seal" viewer masthead:
 * - Expanded: full-width glass header with document title, tabular page counter, and actions.
 * - Condensed: collapses on vertical scroll into a floating glass pill with truncated title, page badge, and quick actions.
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
    modifier: Modifier = Modifier,
    isCondensed: Boolean = false,
    onToolsClick: (() -> Unit)? = null,
    onOcrClick: (() -> Unit)? = null,
    onTtsClick: (() -> Unit)? = null,
    onSlideshowClick: (() -> Unit)? = null,
    onReflowClick: (() -> Unit)? = null,
    reflowLabel: String? = null,
    onDocumentDetailsClick: (() -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val colors = AppTheme.colors
    val reducedMotion = LocalReducedMotion.current
    var isMoreMenuOpen by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) EnterTransition.None else slideInVertically(
            initialOffsetY = { -it },
            animationSpec = AppMotion.snappy()
        ) + fadeIn(AppMotion.fadeFast),
        exit = if (reducedMotion) ExitTransition.None else slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = AppMotion.snappy()
        ) + fadeOut(AppMotion.fadeFast),
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = isCondensed,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None using null
                } else {
                    fadeIn(AppMotion.fadeFast) togetherWith fadeOut(AppMotion.fadeFast)
                }
            },
            label = "ViewerTopBarCondense"
        ) { condensed ->
            if (condensed) {
                // Slim floating glass pill when scrolling down
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassSurface(
                        shape = AppRadius.roundShape,
                        elevation = 8.dp,
                        hazeState = hazeState.takeIf { condensed == isCondensed }
                    ) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AdaptiveIconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.viewer_back),
                                    tint = androidx.compose.material3.LocalContentColor.current,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = formatMiddleTruncated(title, maxChars = 16),
                                style = AppTheme.typography.captionStrong,
                                color = colors.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(horizontal = 6.dp)
                            )

                            if (pageCount > 0) {
                                Surface(
                                    shape = AppRadius.roundShape,
                                    color = colors.accent.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.viewer_page_ratio, currentPage + 1, pageCount),
                                        style = AppTheme.typography.metadataStrong,
                                        color = colors.accent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            AdaptiveIconButton(
                                onClick = onSearchClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = stringResource(R.string.viewer_search),
                                    tint = androidx.compose.material3.LocalContentColor.current,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AdaptiveIconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = if (isBookmarked) stringResource(R.string.viewer_bookmark_remove) else stringResource(R.string.viewer_bookmark_add),
                                    tint = androidx.compose.material3.LocalContentColor.current,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box {
                                AdaptiveIconButton(
                                    onClick = { isMoreMenuOpen = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = stringResource(R.string.viewer_more_options),
                                        tint = androidx.compose.material3.LocalContentColor.current,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                OverflowDropdown(
                                    expanded = isMoreMenuOpen,
                                    onDismiss = { isMoreMenuOpen = false },
                                    onShareClick = onShareClick,
                                    onTtsClick = onTtsClick,
                                    onOcrClick = onOcrClick,
                                    onToolsClick = onToolsClick,
                                    onSlideshowClick = onSlideshowClick,
                                    onReflowClick = onReflowClick,
                                    reflowLabel = reflowLabel,
                                    onDocumentDetailsClick = onDocumentDetailsClick
                                )
                            }
                        }
                    }
                }
            } else {
                // Expanded masthead
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    elevation = 6.dp,
                    hazeState = hazeState.takeIf { condensed == isCondensed }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AdaptiveIconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.viewer_back),
                                    tint = androidx.compose.material3.LocalContentColor.current
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = formatMiddleTruncated(title, maxChars = 28),
                                    style = AppTheme.typography.documentName,
                                    color = colors.inkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (pageCount > 0) {
                                    Text(
                                        text = stringResource(R.string.viewer_page_of, currentPage + 1, pageCount),
                                        style = AppTheme.typography.metadata,
                                        color = colors.inkMuted
                                    )
                                }
                            }

                            AdaptiveIconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = stringResource(R.string.viewer_search),
                                    tint = androidx.compose.material3.LocalContentColor.current
                                )
                            }

                            AdaptiveIconButton(onClick = onToggleBookmark) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = if (isBookmarked) stringResource(R.string.viewer_bookmark_remove) else stringResource(R.string.viewer_bookmark_add),
                                    tint = androidx.compose.material3.LocalContentColor.current
                                )
                            }

                            Box {
                                AdaptiveIconButton(onClick = { isMoreMenuOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = stringResource(R.string.viewer_more_options),
                                        tint = androidx.compose.material3.LocalContentColor.current
                                    )
                                }

                                OverflowDropdown(
                                    expanded = isMoreMenuOpen,
                                    onDismiss = { isMoreMenuOpen = false },
                                    onShareClick = onShareClick,
                                    onTtsClick = onTtsClick,
                                    onOcrClick = onOcrClick,
                                    onToolsClick = onToolsClick,
                                    onSlideshowClick = onSlideshowClick,
                                    onReflowClick = onReflowClick,
                                    reflowLabel = reflowLabel,
                                    onDocumentDetailsClick = onDocumentDetailsClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShareClick: () -> Unit,
    onTtsClick: (() -> Unit)?,
    onOcrClick: (() -> Unit)?,
    onToolsClick: (() -> Unit)?,
    onSlideshowClick: (() -> Unit)?,
    onReflowClick: (() -> Unit)?,
    reflowLabel: String?,
    onDocumentDetailsClick: (() -> Unit)?
) {
    AppDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        OverflowAction(
            label = stringResource(R.string.viewer_action_share),
            icon = Icons.Outlined.Share,
            onClick = {
                onDismiss()
                onShareClick()
            }
        )

        if (onTtsClick != null) {
            OverflowAction(
                label = stringResource(R.string.viewer_action_tts),
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                onClick = {
                    onDismiss()
                    onTtsClick()
                }
            )
        }

        if (onOcrClick != null) {
            OverflowAction(
                label = stringResource(R.string.viewer_action_ocr),
                icon = Icons.Outlined.TextFields,
                onClick = {
                    onDismiss()
                    onOcrClick()
                }
            )
        }

        if (onToolsClick != null) {
            OverflowAction(
                label = stringResource(R.string.viewer_action_tools),
                icon = Icons.Outlined.Build,
                onClick = {
                    onDismiss()
                    onToolsClick()
                }
            )
        }

        if (onSlideshowClick != null) {
            OverflowAction(
                label = stringResource(R.string.viewer_action_slideshow),
                icon = Icons.Outlined.Slideshow,
                onClick = {
                    onDismiss()
                    onSlideshowClick()
                }
            )
        }

        if (onReflowClick != null) {
            OverflowAction(
                label = reflowLabel ?: stringResource(R.string.viewer_action_reflow),
                icon = Icons.Outlined.TextFields,
                onClick = {
                    onDismiss()
                    onReflowClick()
                }
            )
        }

        if (onDocumentDetailsClick != null) {
            OverflowAction(
                label = stringResource(R.string.viewer_action_details),
                icon = Icons.Outlined.Info,
                onClick = {
                    onDismiss()
                    onDocumentDetailsClick()
                }
            )
        }
    }
}

@Composable
private fun OverflowAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = AppTheme.typography.body,
                color = colors.inkPrimary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.inkSecondary
            )
        },
        onClick = onClick
    )
}

@Preview(name = "ViewerTopBar - Expanded Light")
@Composable
private fun ViewerTopBarExpandedLightPreview() {
    AppDesignSystemTheme(darkTheme = false, amoledBlack = false) {
        ViewerTopBar(
            title = "Annual Financial Report 2026.pdf",
            currentPage = 4,
            pageCount = 32,
            visible = true,
            isBookmarked = true,
            isCondensed = false,
            onBackClick = {},
            onSearchClick = {},
            onToggleBookmark = {},
            onShareClick = {}
        )
    }
}

@Preview(name = "ViewerTopBar - Condensed Dark")
@Composable
private fun ViewerTopBarCondensedDarkPreview() {
    AppDesignSystemTheme(darkTheme = true, amoledBlack = false) {
        ViewerTopBar(
            title = "Annual Financial Report 2026.pdf",
            currentPage = 4,
            pageCount = 32,
            visible = true,
            isBookmarked = true,
            isCondensed = true,
            onBackClick = {},
            onSearchClick = {},
            onToggleBookmark = {},
            onShareClick = {}
        )
    }
}
