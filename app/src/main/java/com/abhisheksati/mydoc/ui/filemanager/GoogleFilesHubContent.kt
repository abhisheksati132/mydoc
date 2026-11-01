package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.StorageChartColors
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageAnalysisUiState
import com.abhisheksati.mydoc.ui.storage.StorageCategoryStat
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType
import java.io.File

/**
 * Editorial Document Library & Storage Hub:
 * 1. Storage Overview with clean progress bar and quick cleaner action.
 * 2. 6 Core Document Collections in disciplined 12dp editorial cards.
 * 3. Physical Storage Volumes (Internal & SD Card) with breadcrumb access.
 * 4. Pinned Quick Access folders and Recycle Bin utility.
 */
@Composable
fun GoogleFilesHubContent(
    storageUiState: StorageAnalysisUiState,
    onBrowseInternalStorage: () -> Unit,
    onBrowseStorageVolume: (StorageVolume) -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onAppsClick: () -> Unit,
    onCleanClick: () -> Unit,
    onOpenTrash: () -> Unit,
    pinnedFolders: List<File>,
    onFolderClick: (File) -> Unit,
    trashCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = LocalIsOled.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        // 0. Storage Overview Card
        item {
            EditorialStorageOverviewCard(
                storageUiState = storageUiState,
                isOled = isOled,
                onCleanClick = onCleanClick,
                onCategoryClick = onCategoryClick
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 1. Collections Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Collections",
                    style = typography.titleSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = "6 categories",
                    style = typography.caption,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 2. 2-Column Collections Grid
        item {
            EditorialCategoryGrid(
                storageUiState = storageUiState,
                isOled = isOled,
                onCategoryClick = onCategoryClick,
                onAppsClick = onAppsClick
            )
        }

        // 3. Storage Devices Section
        item {
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage Devices",
                    style = typography.titleSmall,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Internal Storage Card
        item {
            InternalStorageCard(
                uiState = storageUiState,
                isOled = isOled,
                onClick = onBrowseInternalStorage
            )
        }

        // External Storage Card (if SD card available)
        val secondaryVolumes = storageUiState.volumes.drop(1)
        if (secondaryVolumes.isNotEmpty()) {
            items(secondaryVolumes.size) { index ->
                val volume = secondaryVolumes[index]
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryStorageCard(
                    volume = volume,
                    isOled = isOled,
                    onClick = { onBrowseStorageVolume(volume) }
                )
            }
        }

        // Pinned Folders Strip (if user has pinned folders)
        if (pinnedFolders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Pinned Folders",
                    style = typography.bodyStrong,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pinnedFolders.forEach { folder ->
                        Surface(
                            shape = AppRadius.pillShape,
                            color = colors.surface,
                            border = BorderStroke(0.5.dp, colors.line),
                            modifier = Modifier.clickable {
                                onFolderClick(folder)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = folder.name,
                                    style = typography.label,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Utility Section (Recycle Bin)
        item {
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Utilities",
                style = typography.titleSmall,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = AppRadius.cardShape,
                color = if (isOled) colors.surface else colors.surfaceContainer,
                border = BorderStroke(0.5.dp, colors.line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    HubUtilityRow(
                        icon = Icons.Outlined.DeleteSweep,
                        iconTint = colors.textSecondary,
                        iconBackground = colors.surfaceMuted,
                        title = "Recycle Bin",
                        subtitle = "Recently deleted items kept for 30 days",
                        badgeText = if (trashCount > 0) "$trashCount items" else null,
                        badgeColor = colors.accent,
                        onClick = onOpenTrash
                    )
                }
            }
        }
    }
}

/**
 * Clean Utility list row (Recycle Bin)
 */
@Composable
private fun HubUtilityRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    badgeText: String?,
    badgeColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(AppRadius.microShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyStrong,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = typography.micro,
                color = colors.textSecondary
            )
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(AppRadius.pillShape)
                    .background(badgeColor.copy(alpha = 0.14f))
            ) {
                Text(
                    text = badgeText,
                    style = typography.micro,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.divider,
            modifier = Modifier.size(16.dp)
        )
    }
}
