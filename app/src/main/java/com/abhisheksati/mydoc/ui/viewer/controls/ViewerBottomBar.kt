package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TextFormat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.heightIn
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.rendering.engine.OutlineItem

/**
 * Shared reader actions with optional sheet tabs for documents that have named sections.
 */
@Composable
fun ViewerBottomBar(
    currentPage: Int,
    visible: Boolean,
    hasOutline: Boolean,
    onOutlineClick: () -> Unit,
    // Null when the reader has no page navigation.
    onPagesClick: (() -> Unit)?,
    onAppearanceClick: () -> Unit,
    modifier: Modifier = Modifier,
    sheetItems: List<OutlineItem> = emptyList(),
    onSheetSelect: ((Int) -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val reducedMotion = LocalReducedMotion.current

    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) EnterTransition.None else slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = AppMotion.snappy()
        ) + fadeIn(AppMotion.snappy()),
        exit = if (reducedMotion) ExitTransition.None else slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = AppMotion.snappy()
        ) + fadeOut(AppMotion.snappy()),
        modifier = modifier
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            elevation = 2.dp,
            backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
            hazeState = hazeState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Multi-sheet workbook tabs (Excel, PPT, etc.)
                if (sheetItems.size > 1 && onSheetSelect != null) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(sheetItems) { idx, item ->
                            val nextItemPage = sheetItems.getOrNull(idx + 1)?.pageIndex ?: Int.MAX_VALUE
                            val isSelected = currentPage >= item.pageIndex && currentPage < nextItemPage
                            SheetTab(
                                label = item.title,
                                selected = isSelected,
                                onClick = { onSheetSelect(item.pageIndex) }
                            )
                        }
                    }
                }

                // Action buttons: Contents, Pages, Appearance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomBarActionButton(
                        icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        label = stringResource(R.string.viewer_contents),
                        contentDescription = stringResource(R.string.viewer_contents_description),
                        enabled = hasOutline,
                        onClick = onOutlineClick,
                        modifier = Modifier.weight(1f)
                    )

                    BottomBarActionButton(
                        icon = Icons.Outlined.PhotoLibrary,
                        label = stringResource(R.string.viewer_pages),
                        contentDescription = stringResource(R.string.viewer_pages_description),
                        enabled = onPagesClick != null,
                        onClick = { onPagesClick?.invoke() },
                        modifier = Modifier.weight(1f)
                    )

                    BottomBarActionButton(
                        icon = Icons.Outlined.TextFormat,
                        label = stringResource(R.string.viewer_appearance),
                        contentDescription = stringResource(R.string.viewer_appearance_description),
                        onClick = onAppearanceClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = if (selected) AppTheme.typography.captionStrong else AppTheme.typography.caption,
            color = if (selected) colors.accent else colors.inkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(
                    color = if (selected) colors.accent else Color.Transparent,
                    shape = AppRadius.roundShape
                )
        )
    }
}

@Composable
private fun BottomBarActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val contentColor = if (enabled) colors.inkPrimary else colors.inkMuted.copy(alpha = 0.38f)

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = AppTheme.typography.caption,
            color = contentColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Preview(name = "ViewerBottomBar - Light")
@Composable
private fun ViewerBottomBarLightPreview() {
    AppDesignSystemTheme(darkTheme = false, amoledBlack = false) {
        ViewerBottomBar(
            currentPage = 3,
            visible = true,
            hasOutline = true,
            onOutlineClick = {},
            onPagesClick = {},
            onAppearanceClick = {}
        )
    }
}

@Preview(name = "ViewerBottomBar - Dark")
@Composable
private fun ViewerBottomBarDarkPreview() {
    AppDesignSystemTheme(darkTheme = true, amoledBlack = false) {
        ViewerBottomBar(
            currentPage = 3,
            visible = true,
            hasOutline = true,
            onOutlineClick = {},
            onPagesClick = {},
            onAppearanceClick = {}
        )
    }
}
