package com.abhisheksati.mydoc.ui.viewer.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxSlide

/**
 * Bottom navigation and thumbnail strip controller for the presentation viewer.
 */
@Composable
fun PresentationBottomBar(
    effectiveSlideCount: Int,
    currentSlideIndex: Int,
    effectiveSlides: List<PptxSlide>,
    isThumbnailStripOpen: Boolean,
    onToggleThumbnailStrip: () -> Unit,
    onSelectSlide: (Int) -> Unit,
    onPreviousSlide: () -> Unit,
    onNextSlide: () -> Unit,
    onOpenJumpDialog: () -> Unit,
    onOpenNotesSheet: () -> Unit,
    bgColor: Color,
    cardBgColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .navigationBarsPadding()
    ) {
        // Slide Thumbnail Carousel (when open)
        AnimatedVisibility(visible = isThumbnailStripOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.paddingSmall),
                    contentPadding = PaddingValues(horizontal = AppSpacing.paddingMedium),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                ) {
                    items(effectiveSlideCount) { index ->
                        val isSelected = index == currentSlideIndex
                        val slide = effectiveSlides.getOrNull(index)
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(58.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(cardBgColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = MaterialTheme.shapes.small
                                )
                                .clickable { onSelectSlide(index) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else textColor
                                )
                                Text(
                                    text = slide?.title ?: "Slide ${index + 1}",
                                    style = typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AppSpacing.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousSlide,
                enabled = currentSlideIndex > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Previous Slide",
                    tint = if (currentSlideIndex > 0) textColor else secondaryTextColor.copy(alpha = 0.3f)
                )
            }

            IconButton(onClick = onToggleThumbnailStrip) {
                Icon(
                    imageVector = Icons.Outlined.ViewCarousel,
                    contentDescription = "Toggle Thumbnails",
                    tint = if (isThumbnailStripOpen) MaterialTheme.colorScheme.primary else secondaryTextColor
                )
            }

            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(cardBgColor)
                    .clickable { onOpenJumpDialog() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Slide ${currentSlideIndex + 1} of $effectiveSlideCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor
                )
            }

            val currentSlide = effectiveSlides.getOrNull(currentSlideIndex)
            val hasNotes = !currentSlide?.notes.isNullOrBlank()
            IconButton(
                onClick = onOpenNotesSheet,
                enabled = hasNotes
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Speaker Notes",
                    tint = if (hasNotes) MaterialTheme.colorScheme.primary else secondaryTextColor.copy(alpha = 0.25f)
                )
            }

            IconButton(
                onClick = onNextSlide,
                enabled = currentSlideIndex < effectiveSlideCount - 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "Next Slide",
                    tint = if (currentSlideIndex < effectiveSlideCount - 1) textColor else secondaryTextColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}
