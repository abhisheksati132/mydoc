package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageAnalysisUiState
import com.abhisheksati.mydoc.ui.storage.StorageCategoryStat
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType

/**
 * 2-Columns Grid of 6 Core Collections
 */
@Composable
fun EditorialCategoryGrid(
    storageUiState: StorageAnalysisUiState,
    isOled: Boolean,
    onCategoryClick: (FileCategory) -> Unit,
    onAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    fun findCategoryStat(type: StorageCategoryType): StorageCategoryStat? {
        return storageUiState.categories.firstOrNull { it.type == type }
    }

    val downloadsStat = findCategoryStat(StorageCategoryType.DOWNLOADS)
    val imagesStat = findCategoryStat(StorageCategoryType.IMAGES)
    val videosStat = findCategoryStat(StorageCategoryType.VIDEOS)
    val audioStat = findCategoryStat(StorageCategoryType.AUDIO)
    val docsStat = findCategoryStat(StorageCategoryType.DOCUMENTS)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Downloads & Documents
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EditorialCategoryCard(
                title = "Documents",
                countText = docsStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = docsStat?.formattedSize ?: "",
                icon = Icons.Outlined.Description,
                tint = DocumentFormatColors.wordTint,
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.DOCUMENTS) }
            )
            EditorialCategoryCard(
                title = "Downloads",
                countText = downloadsStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = downloadsStat?.formattedSize ?: "",
                icon = Icons.Outlined.Download,
                tint = colors.accent,
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.DOWNLOADS) }
            )
        }

        // Row 2: Images & Videos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EditorialCategoryCard(
                title = "Images",
                countText = imagesStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = imagesStat?.formattedSize ?: "",
                icon = Icons.Outlined.Image,
                tint = DocumentFormatColors.mediaTint,
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.IMAGES) }
            )
            EditorialCategoryCard(
                title = "Videos",
                countText = videosStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = videosStat?.formattedSize ?: "",
                icon = Icons.Outlined.Videocam,
                tint = DocumentFormatColors.mediaTint,
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.VIDEOS) }
            )
        }

        // Row 3: Audio & Apps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EditorialCategoryCard(
                title = "Audio",
                countText = audioStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = audioStat?.formattedSize ?: "",
                icon = Icons.Outlined.AudioFile,
                tint = DocumentFormatColors.mediaTint,
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.AUDIO) }
            )
            EditorialCategoryCard(
                title = "Apps",
                countText = if (storageUiState.userAppCount > 0) "${storageUiState.userAppCount} apps" else "Scanning...",
                sizeText = if (storageUiState.userAppBytes > 0) StorageVolume.formatBytes(storageUiState.userAppBytes) else "",
                icon = Icons.Outlined.Apps,
                tint = colors.accent,
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = onAppsClick
            )
        }
    }
}

/**
 * Individual Category Card in the 2-Column Grid
 */
@Composable
fun EditorialCategoryCard(
    title: String,
    countText: String,
    sizeText: String,
    icon: ImageVector,
    tint: Color,
    isOled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    Surface(
        shape = AppRadius.cardShape,
        color = if (isOled) colors.surface else colors.surfaceContainer,
        border = BorderStroke(0.5.dp, colors.line),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AppRadius.cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppRadius.microShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val sub = if (sizeText.isNotEmpty()) "$countText • $sizeText" else countText
                Text(
                    text = sub,
                    style = typography.micro.withTabularFigures(),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
