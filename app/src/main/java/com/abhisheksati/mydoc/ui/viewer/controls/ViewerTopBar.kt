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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
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
    showSearch: Boolean = true,
    onToolsClick: (() -> Unit)? = null,
    onOcrClick: (() -> Unit)? = null,
    onTtsClick: (() -> Unit)? = null,
    onSlideshowClick: (() -> Unit)? = null,
    onReflowClick: (() -> Unit)? = null,
    reflowLabel: String? = null,
    onDocumentDetailsClick: (() -> Unit)? = null,
    documentDetailsLabel: String? = null,
    onExportNotesClick: (() -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val colors = AppTheme.colors
    val reducedMotion = LocalReducedMotion.current
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) EnterTransition.None else slideInVertically(AppMotion.snappy()) { -it / 3 } + fadeIn(AppMotion.fadeFast),
        exit = if (reducedMotion) ExitTransition.None else slideOutVertically(AppMotion.snappy()) { -it / 3 } + fadeOut(AppMotion.fadeFast),
        modifier = modifier
    ) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            GlassSurface(shape = AppRadius.roundShape, hazeState = hazeState, elevation = 4.dp) {
                AdaptiveIconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.viewer_back),
                        tint = androidx.compose.material3.LocalContentColor.current)
                }
            }
            GlassSurface(modifier = Modifier.weight(1f), shape = AppRadius.roundShape,
                hazeState = hazeState, elevation = 4.dp) {
                Row(Modifier.heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                        Text(title, style = if (isCondensed) AppTheme.typography.captionStrong else AppTheme.typography.bodyStrong,
                            color = colors.inkPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (pageCount > 0) Text(stringResource(R.string.viewer_page_ratio, currentPage + 1, pageCount),
                            style = AppTheme.typography.metadata, color = colors.inkMuted)
                    }
                    if (showSearch) AdaptiveIconButton(onSearchClick, Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.viewer_search),
                            tint = androidx.compose.material3.LocalContentColor.current, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Box {
                GlassSurface(shape = AppRadius.roundShape, hazeState = hazeState, elevation = 4.dp) {
                    AdaptiveIconButton({ isMoreMenuOpen = true }, Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.viewer_more_options),
                            tint = androidx.compose.material3.LocalContentColor.current)
                    }
                }
                OverflowDropdown(
                    expanded = isMoreMenuOpen, onDismiss = { isMoreMenuOpen = false },
                    onShareClick = onShareClick, onTtsClick = onTtsClick, onOcrClick = onOcrClick,
                    onToolsClick = onToolsClick, onSlideshowClick = onSlideshowClick,
                    onReflowClick = onReflowClick, reflowLabel = reflowLabel,
                    onDocumentDetailsClick = onDocumentDetailsClick, documentDetailsLabel = documentDetailsLabel,
                    onExportNotesClick = onExportNotesClick,
                    onToggleBookmark = onToggleBookmark, isBookmarked = isBookmarked)
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
    onDocumentDetailsClick: (() -> Unit)?,
    documentDetailsLabel: String?,
    onExportNotesClick: (() -> Unit)?,
    onToggleBookmark: (() -> Unit)? = null,
    isBookmarked: Boolean = false
) {
    AppDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (onToggleBookmark != null) OverflowAction(
            label = stringResource(if (isBookmarked) R.string.viewer_bookmark_remove else R.string.viewer_bookmark_add),
            icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            onClick = { onDismiss(); onToggleBookmark() })
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

        if (onExportNotesClick != null) {
            OverflowAction(
                label = stringResource(R.string.viewer_action_export_notes),
                icon = Icons.Outlined.Description,
                onClick = {
                    onDismiss()
                    onExportNotesClick()
                }
            )
        }

        if (onDocumentDetailsClick != null) {
            OverflowAction(
                label = documentDetailsLabel ?: stringResource(R.string.viewer_action_details),
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
