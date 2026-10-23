package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TextFormat
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppSlider
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import kotlin.math.roundToInt

/**
 * Modern, ergonomic bottom control bar for document viewing.
 * Features:
 * 1. Interactive page scrubber with floating bubble displaying target page number.
 * 2. Three labeled primary actions: Contents (outline), Pages (grid overview), and Appearance (Aa).
 * 3. Theme-token-driven colors and 48dp+ touch targets.
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
    sheetItems: List<OutlineItem> = emptyList(),
    onSheetSelect: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainer)
                .navigationBarsPadding()
        ) {
            // Optional spreadsheet/section tabs
            if (sheetItems.size > 1 && onSheetSelect != null) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.paddingSmall, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sheetItems.size) { idx ->
                        val item = sheetItems[idx]
                        val nextItemPage = sheetItems.getOrNull(idx + 1)?.pageIndex ?: Int.MAX_VALUE
                        val isSelected = currentPage >= item.pageIndex && currentPage < nextItemPage
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSheetSelect(item.pageIndex)
                            },
                            label = { Text(item.title, fontSize = 12.sp) },
                            shape = RoundedCornerShape(AppSpacing.chipCornerRadius),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent,
                                selectedLabelColor = colors.surface
                            )
                        )
                    }
                }
                AppDivider()
            }

            // Scrubber area with floating target page bubble
            if (pageCount > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.paddingMedium)
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Floating page bubble during dragging
                    AnimatedVisibility(
                        visible = isDragging,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)) + scaleIn(spring(stiffness = Spring.StiffnessHigh)),
                        exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) + scaleOut(spring(stiffness = Spring.StiffnessHigh))
                    ) {
                        val targetDisplayPage = (scrubPage.roundToInt() + 1).coerceIn(1, pageCount)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                                .background(colors.surfaceVariant)
                                .border(
                                    width = AppSpacing.dividerThickness,
                                    color = colors.accent,
                                    shape = RoundedCornerShape(AppSpacing.chipCornerRadius)
                                )
                                .padding(horizontal = AppSpacing.paddingMedium, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Page $targetDisplayPage of $pageCount",
                                style = typography.caption.withTabularFigures(),
                                color = colors.accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Scrubber slider
                    AppSlider(
                        value = if (isDragging) scrubPage else currentPage.toFloat(),
                        onValueChange = { newVal ->
                            isDragging = true
                            scrubPage = newVal
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            val target = scrubPage.roundToInt().coerceIn(0, pageCount - 1)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPageScrubbed(target)
                        },
                        valueRange = 0f..(pageCount - 1).toFloat(),
                        steps = if (pageCount in 2..60) pageCount - 2 else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.divider
                        ),
                        thumbSize = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                    )
                }
            }

            AppDivider()

            // Primary labeled actions: Contents, Pages, Appearance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = AppSpacing.paddingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomBarActionButton(
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    label = "Contents",
                    contentDescription = "Document contents and outline",
                    enabled = hasOutline,
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

@Composable
private fun BottomBarActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

    val contentColor = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.38f)

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = typography.caption,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
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

@Preview(name = "ViewerBottomBar - AMOLED")
@Composable
private fun ViewerBottomBarAmoledPreview() {
    AppDesignSystemTheme(darkTheme = true, amoledBlack = true) {
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
