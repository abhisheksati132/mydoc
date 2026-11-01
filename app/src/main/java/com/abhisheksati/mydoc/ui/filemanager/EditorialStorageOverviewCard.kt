package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.StorageChartColors
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageAnalysisUiState
import com.abhisheksati.mydoc.ui.storage.StorageCategoryStat
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType

/**
 * Editorial Storage Overview Card:
 * Clean, restrained storage overview with 1-tap cleaner trigger and tabular breakdown.
 */
@Composable
fun EditorialStorageOverviewCard(
    storageUiState: StorageAnalysisUiState,
    isOled: Boolean,
    onCleanClick: () -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val totalBytes = storageUiState.totalBytes.coerceAtLeast(1L)
    val usedBytes = storageUiState.usedBytes

    fun getCatBytes(type: StorageCategoryType): Long {
        return storageUiState.categories.firstOrNull { it.type == type }?.bytes ?: 0L
    }

    val docsBytes = getCatBytes(StorageCategoryType.DOCUMENTS)
    val imagesBytes = getCatBytes(StorageCategoryType.IMAGES)
    val videosBytes = getCatBytes(StorageCategoryType.VIDEOS)
    val audioBytes = getCatBytes(StorageCategoryType.AUDIO)
    val appsBytes = if (storageUiState.userAppBytes > 0) storageUiState.userAppBytes else getCatBytes(StorageCategoryType.APPS)

    val docsWeight = (docsBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    val imagesWeight = (imagesBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    val videosWeight = (videosBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    val audioWeight = (audioBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    val appsWeight = (appsBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    val otherUsedWeight = ((usedBytes - (docsBytes + imagesBytes + videosBytes + audioBytes + appsBytes)).coerceAtLeast(0L).toFloat() / totalBytes).coerceIn(0f, 1f)

    Surface(
        shape = AppRadius.cardShape,
        color = if (isOled) colors.surface else colors.surfaceContainer,
        border = BorderStroke(0.5.dp, colors.line),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Total used readout and Clean button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Device Storage",
                            style = typography.captionStrong,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (storageUiState.formattedUsed.isNotEmpty() && storageUiState.formattedTotal.isNotEmpty()) {
                            "${storageUiState.formattedUsed} used of ${storageUiState.formattedTotal}"
                        } else {
                            "Calculating storage..."
                        },
                        style = typography.titleSmall.withTabularFigures(),
                        color = colors.textPrimary
                    )
                }

                // 1-Tap Clean button
                val reclaimable = storageUiState.cleanSummary.totalReclaimableBytes
                Button(
                    onClick = onCleanClick,
                    shape = AppRadius.pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reclaimable > 0) colors.accent else colors.surface,
                        contentColor = if (reclaimable > 0) colors.onAccent else colors.textPrimary
                    ),
                    border = if (reclaimable <= 0) BorderStroke(0.5.dp, colors.line) else null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (reclaimable > 0) "Clean ${StorageVolume.formatBytes(reclaimable)}" else "Clean",
                        style = typography.captionStrong
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment clean progress bar (6dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.line)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (docsWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(docsWeight.coerceAtLeast(0.01f))
                                .background(StorageChartColors.documents)
                        )
                    }
                    if (imagesWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(imagesWeight.coerceAtLeast(0.01f))
                                .background(StorageChartColors.images)
                        )
                    }
                    if (videosWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(videosWeight.coerceAtLeast(0.01f))
                                .background(StorageChartColors.videos)
                        )
                    }
                    if (audioWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(audioWeight.coerceAtLeast(0.01f))
                                .background(StorageChartColors.audio)
                        )
                    }
                    if (appsWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(appsWeight.coerceAtLeast(0.01f))
                                .background(StorageChartColors.apps)
                        )
                    }
                    if (otherUsedWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(otherUsedWeight.coerceAtLeast(0.01f))
                                .background(StorageChartColors.other)
                        )
                    }
                    val freeWeight = (storageUiState.freeBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                    if (freeWeight > 0.01f) {
                        Spacer(modifier = Modifier.weight(freeWeight))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageLegendChip(
                    label = "Docs",
                    sizeText = StorageVolume.formatBytes(docsBytes),
                    dotColor = StorageChartColors.documents,
                    onClick = { onCategoryClick(FileCategory.DOCUMENTS) }
                )
                StorageLegendChip(
                    label = "Photos",
                    sizeText = StorageVolume.formatBytes(imagesBytes),
                    dotColor = StorageChartColors.images,
                    onClick = { onCategoryClick(FileCategory.IMAGES) }
                )
                StorageLegendChip(
                    label = "Videos",
                    sizeText = StorageVolume.formatBytes(videosBytes),
                    dotColor = StorageChartColors.videos,
                    onClick = { onCategoryClick(FileCategory.VIDEOS) }
                )
                StorageLegendChip(
                    label = "Audio",
                    sizeText = StorageVolume.formatBytes(audioBytes),
                    dotColor = StorageChartColors.audio,
                    onClick = { onCategoryClick(FileCategory.AUDIO) }
                )
                StorageLegendChip(
                    label = "Free",
                    sizeText = storageUiState.formattedFree,
                    dotColor = colors.line,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun StorageLegendChip(
    label: String,
    sizeText: String,
    dotColor: Color,
    onClick: () -> Unit
) {
    val typography = LocalAppTypography.current
    val colors = LocalAppColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(AppRadius.microShape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ",
            style = typography.captionStrong,
            color = colors.textPrimary
        )
        Text(
            text = sizeText,
            style = typography.micro.withTabularFigures(),
            color = colors.textSecondary
        )
    }
}
