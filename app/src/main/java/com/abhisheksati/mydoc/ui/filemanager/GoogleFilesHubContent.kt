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
 * 2-Columns Grid of 6 Core Collections
 */
@Composable
private fun EditorialCategoryGrid(
    storageUiState: StorageAnalysisUiState,
    isOled: Boolean,
    onCategoryClick: (FileCategory) -> Unit,
    onAppsClick: () -> Unit
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
        modifier = Modifier
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
private fun EditorialCategoryCard(
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

/**
 * Internal Storage Device Card with free space readout and progress bar
 */
@Composable
private fun InternalStorageCard(
    uiState: StorageAnalysisUiState,
    isOled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "internalStorageScale"
    )

    Surface(
        shape = AppRadius.cardShape,
        color = if (isOled) colors.surface else colors.surfaceContainer,
        border = BorderStroke(0.5.dp, colors.line),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AppRadius.cardShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppRadius.microShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Internal Storage",
                    style = typography.bodyStrong,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${uiState.formattedFree} free of ${uiState.formattedTotal}",
                    style = typography.caption.withTabularFigures(),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { uiState.usedRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.accent,
                    trackColor = colors.line
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Open Storage",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Secondary Storage (SD Card) Card
 */
@Composable
private fun SecondaryStorageCard(
    volume: StorageVolume,
    isOled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        shape = AppRadius.cardShape,
        color = if (isOled) colors.surface else colors.surfaceContainer,
        border = BorderStroke(0.5.dp, colors.line),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScaleEffect(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppRadius.microShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SdCard,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volume.title,
                    style = typography.bodyStrong,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${StorageVolume.formatBytes(volume.freeBytes)} free of ${StorageVolume.formatBytes(volume.totalBytes)}",
                    style = typography.caption.withTabularFigures(),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { volume.usedRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.accent,
                    trackColor = colors.line
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
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

/**
 * Editorial Storage Overview Card:
 * Clean, restrained storage overview with 1-tap cleaner trigger and tabular breakdown.
 */
@Composable
private fun EditorialStorageOverviewCard(
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
