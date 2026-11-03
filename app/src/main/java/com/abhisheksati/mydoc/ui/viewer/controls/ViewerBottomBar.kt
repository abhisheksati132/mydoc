package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TextFormat
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppSlider
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import kotlin.math.roundToInt

/**
 * Editorial "Paper under glass" reader footer:
 * - Hosted on a tactile GlassSurface with specular gradient rim.
 * - Sheet tabs for multi-sheet workbooks.
 * - Monospace live page scrubber with Stamp Vermilion slider.
 * - Triple primary actions: Contents, Pages, Appearance.
 */
@Composable
fun ViewerBottomBar(
    currentPage: Int,
    pageCount: Int,
    visible: Boolean,
    hasOutline: Boolean,
    onOutlineClick: () -> Unit,
    onPagesClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onPageScrubbed: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isCondensed: Boolean = false,
    sheetItems: List<OutlineItem> = emptyList(),
    onSheetSelect: ((Int) -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val colors = AppTheme.colors

    var isDragging by remember { mutableStateOf(false) }
    var scrubPage by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
        modifier = modifier
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            elevation = 8.dp,
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
                            .padding(horizontal = 8.dp, vertical = 4.dp),
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

                // Page scrubber with live monospace bubble
                if (pageCount > 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 6.dp, bottom = 2.dp)
                    ) {
                        AnimatedVisibility(
                            visible = isDragging,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp),
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
                            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val target = (scrubPage.roundToInt() + 1).coerceIn(1, pageCount)
                                Text(
                                    text = "Page $target of $pageCount",
                                    style = AppTheme.typography.metadataStrong,
                                    color = colors.accent
                                )
                            }
                        }

                        AppSlider(
                            value = if (isDragging) scrubPage else currentPage.toFloat(),
                            onValueChange = { newVal ->
                                isDragging = true
                                scrubPage = newVal
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                val target = scrubPage.roundToInt().coerceIn(0, pageCount - 1)
                                onPageScrubbed(target)
                            },
                            valueRange = 0f..(pageCount - 1).toFloat(),
                            steps = if (pageCount in 2..60) pageCount - 2 else 0,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.inkMuted.copy(alpha = 0.2f)
                            ),
                            thumbSize = 12.dp,
                            trackHeight = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }
                }

                // Action buttons: Contents, Pages, Appearance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomBarActionButton(
                        icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        label = "Contents",
                        contentDescription = "Document contents and outline",
                        enabled = true,
                        onClick = onOutlineClick,
                        modifier = Modifier.weight(1f)
                    )

                    BottomBarActionButton(
                        icon = Icons.Outlined.PhotoLibrary,
                        label = "Pages",
                        contentDescription = "Thumbnail page grid",
                        onClick = onPagesClick,
                        modifier = Modifier.weight(1f)
                    )

                    BottomBarActionButton(
                        icon = Icons.Outlined.TextFormat,
                        label = "Appearance",
                        contentDescription = "Reading appearance settings",
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
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = AppTheme.typography.captionStrong,
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
            .height(56.dp)
            .clickable(
                enabled = enabled,
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
            color = contentColor
        )
    }
}

@Preview(name = "ViewerBottomBar - Light")
@Composable
private fun ViewerBottomBarLightPreview() {
    AppDesignSystemTheme(darkTheme = false, amoledBlack = false) {
        ViewerBottomBar(
            currentPage = 3,
            pageCount = 24,
            visible = true,
            hasOutline = true,
            onOutlineClick = {},
            onPagesClick = {},
            onAppearanceClick = {},
            onPageScrubbed = {}
        )
    }
}

@Preview(name = "ViewerBottomBar - Dark")
@Composable
private fun ViewerBottomBarDarkPreview() {
    AppDesignSystemTheme(darkTheme = true, amoledBlack = false) {
        ViewerBottomBar(
            currentPage = 3,
            pageCount = 24,
            visible = true,
            hasOutline = true,
            onOutlineClick = {},
            onPagesClick = {},
            onAppearanceClick = {},
            onPageScrubbed = {}
        )
    }
}
