package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Ergonomic one-handed bottom bar for the viewer.
 * Provides easy thumb access to search, page jump, outline, thumbnails, and reading settings.
 */
@Composable
fun ViewerBottomBar(
    currentPage: Int,
    pageCount: Int,
    visible: Boolean,
    hasOutline: Boolean,
    isThumbnailStripOpen: Boolean,
    onSearchClick: () -> Unit,
    onOutlineClick: () -> Unit,
    onToggleThumbnails: () -> Unit,
    onOpenSettings: () -> Unit,
    onPageTextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .navigationBarsPadding()
        ) {
            AppDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget)
                    .padding(horizontal = AppSpacing.paddingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Outline / TOC button
                if (hasOutline) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOutlineClick()
                        },
                        modifier = Modifier.size(AppSpacing.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = "Outline",
                            tint = colors.textSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(AppSpacing.minTouchTarget))
                }

                // Search button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchClick()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search in document",
                        tint = colors.textSecondary
                    )
                }

                // Page jump pill button (middle reach)
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                        .background(colors.surfaceVariant)
                        .border(
                            width = AppSpacing.dividerThickness,
                            color = colors.divider,
                            shape = RoundedCornerShape(AppSpacing.chipCornerRadius)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPageTextClick()
                        }
                        .padding(horizontal = AppSpacing.paddingMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (pageCount > 0) "${currentPage + 1} / $pageCount" else "0 / 0",
                        style = typography.label,
                        color = colors.textPrimary
                    )
                }

                // Thumbnail strip toggle
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleThumbnails()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "Thumbnails",
                        tint = if (isThumbnailStripOpen) colors.accent else colors.textSecondary
                    )
                }

                // Consolidated reading settings button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSettings()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Reading settings",
                        tint = colors.accent
                    )
                }
            }
        }
    }
}
