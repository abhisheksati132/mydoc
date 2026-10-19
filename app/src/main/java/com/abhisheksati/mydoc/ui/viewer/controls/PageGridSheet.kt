package com.abhisheksati.mydoc.ui.viewer.controls

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance 3-column page thumbnail grid overview.
 * Enables visual scanning across documents and instant 1-tap jumping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageGridSheet(
    isOpen: Boolean,
    pageCount: Int,
    currentPage: Int,
    bookmarks: Set<Int>,
    onDismiss: () -> Unit,
    onPageSelected: (Int) -> Unit,
    renderThumbnail: (page: Int, scale: Float) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    if (!isOpen || pageCount <= 0) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gridState = rememberLazyGridState()

    var jumpQuery by remember { mutableStateOf("") }

    // Scroll automatically to currently open page on launch
    LaunchedEffect(currentPage) {
        if (currentPage in 0 until pageCount) {
            gridState.scrollToItem(currentPage)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = AppSpacing.cornerRadius, topEnd = AppSpacing.cornerRadius),
        tonalElevation = AppSpacing.elevation,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = AppSpacing.paddingSmall)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.divider, RoundedCornerShape(2.dp))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.paddingMedium)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Page Overview",
                        style = typography.title,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$pageCount pages total",
                        style = typography.caption,
                        color = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close page overview",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

            // Fast Page Number Jump Bar
            OutlinedTextField(
                value = jumpQuery,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    jumpQuery = filtered
                    val pageNum = filtered.toIntOrNull()
                    if (pageNum != null && pageNum in 1..pageCount) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPageSelected(pageNum - 1)
                        onDismiss()
                    }
                },
                placeholder = {
                    Text(
                        text = "Jump directly to page… (1 - $pageCount)",
                        style = typography.body,
                        color = colors.textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(AppSpacing.cornerRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.divider,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
            AppDivider()

            // 3-Column Thumbnails Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(
                    vertical = AppSpacing.paddingMedium,
                    horizontal = AppSpacing.paddingSmall
                ),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.paddingMedium),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(pageCount, key = { it }) { pageIndex ->
                    val isCurrent = pageIndex == currentPage
                    val isBookmarked = bookmarks.contains(pageIndex)

                    @Suppress("ProduceStateDoesNotAssignValue")
                    val thumbnail by produceState<Bitmap?>(initialValue = null, pageIndex) {
                        value = withContext(Dispatchers.Default) {
                            renderThumbnail(pageIndex, 0.35f)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPageSelected(pageIndex)
                                onDismiss()
                            }
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.707f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface)
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                    color = if (isCurrent) colors.accent else colors.divider.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = thumbnail
                            if (bmp != null && !bmp.isRecycled) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page ${pageIndex + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AppProgressIndicator()
                            }

                            // Bookmark badge overlay
                            if (isBookmarked) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .background(colors.accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = "Bookmarked",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Page badge
                        Text(
                            text = "${pageIndex + 1}",
                            style = typography.caption.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            ),
                            color = if (isCurrent) colors.accent else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
