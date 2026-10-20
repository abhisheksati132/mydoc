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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Badge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageAnalysisUiState
import com.abhisheksati.mydoc.ui.storage.StorageCategoryStat
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType
import java.io.File

/**
 * Files by Google inspired unified Hub View:
 * 1. Master Storage Analysis card with multi-color segmented bar & quick clean button.
 * 2. 6 core categories split in a sleek 2-column grid.
 * 3. Storage Devices section with Internal Storage (and SD card if present).
 * 4. Tools & Protection section (Recycle Bin with badge, Safe Folder, Wi-Fi Transfer).
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
    onOpenVault: () -> Unit,
    onOpenWifiTransfer: () -> Unit,
    pinnedFolders: List<File>,
    onFolderClick: (File) -> Unit,
    trashCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = colors.background == DarkAppColors.background

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Top Storage Analysis Master Box
        item {
            FilesByGoogleStorageCard(
                uiState = storageUiState,
                isOled = isOled,
                onCleanClick = onCleanClick
            )
        }

        // 2. Categories Section Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Categories",
                    style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "6 categories",
                    style = typography.caption.copy(fontSize = 12.sp),
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. 2-Columns Categories Grid (Downloads, Images, Videos, Audio, Documents, Apps)
        item {
            GoogleFilesCategoryGrid(
                storageUiState = storageUiState,
                isOled = isOled,
                onCategoryClick = onCategoryClick,
                onAppsClick = onAppsClick
            )
        }

        // 4. Storage Devices Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage devices",
                    style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
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

        // External Storage Card (if SD card or second volume available)
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
                    style = typography.title.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
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
                            shape = RoundedCornerShape(12.dp),
                            color = if (isOled) Color(0xFF141720) else colors.surface,
                            border = BorderStroke(1.dp, colors.divider.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { onFolderClick(folder) }
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = folder.name,
                                    style = typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Tools & Protection Section (Recycle Bin, Safe Folder, Wi-Fi Transfer)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Tools & Protection",
                style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isOled) Color(0xFF101217) else colors.surface,
                border = BorderStroke(1.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    // Recycle Bin Item
                    HubUtilityRow(
                        icon = Icons.Outlined.DeleteSweep,
                        iconTint = Color(0xFFFF9800),
                        iconBackground = Color(0xFFFF9800).copy(alpha = 0.14f),
                        title = "Recycle Bin",
                        subtitle = "Recently deleted items kept for 30 days",
                        badgeText = if (trashCount > 0) "$trashCount items" else null,
                        badgeColor = colors.accent,
                        onClick = onOpenTrash
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(colors.divider.copy(alpha = 0.4f))
                    )

                    // Safe Folder (Vault) Item
                    HubUtilityRow(
                        icon = Icons.Outlined.Lock,
                        iconTint = Color(0xFF4CAF50),
                        iconBackground = Color(0xFF4CAF50).copy(alpha = 0.14f),
                        title = "Safe Folder",
                        subtitle = "Encrypted vault with biometric or PIN lock",
                        badgeText = null,
                        badgeColor = Color.Transparent,
                        onClick = onOpenVault
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(colors.divider.copy(alpha = 0.4f))
                    )

                    // Wi-Fi PC Transfer Item
                    HubUtilityRow(
                        icon = Icons.Outlined.Wifi,
                        iconTint = Color(0xFF03A9F4),
                        iconBackground = Color(0xFF03A9F4).copy(alpha = 0.14f),
                        title = "Wi-Fi PC File Transfer",
                        subtitle = "Transfer files wirelessly using your browser",
                        badgeText = null,
                        badgeColor = Color.Transparent,
                        onClick = onOpenWifiTransfer
                    )
                }
            }
        }
    }
}

/**
 * Master Storage Analysis Box with multi-color segmented progress bar and Quick Clean button
 */
@Composable
private fun FilesByGoogleStorageCard(
    uiState: StorageAnalysisUiState,
    isOled: Boolean,
    onCleanClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val healthBadgeText = when {
        uiState.freeBytes > 15L * 1024 * 1024 * 1024 -> "Healthy Storage"
        uiState.freeBytes > 4L * 1024 * 1024 * 1024 -> "Moderate Storage"
        else -> "Storage Running Low"
    }

    val healthBadgeColor = when {
        uiState.freeBytes > 15L * 1024 * 1024 * 1024 -> Color(0xFF4CAF50)
        uiState.freeBytes > 4L * 1024 * 1024 * 1024 -> Color(0xFFFFA000)
        else -> Color(0xFFE53935)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isOled) Color(0xFF101217) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Row: Percentage Used + Health Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${uiState.usedPercentage}%",
                        style = typography.title.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "used",
                        style = typography.body.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = healthBadgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, healthBadgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = healthBadgeText,
                        style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = healthBadgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle size
            Text(
                text = "${uiState.formattedUsed} used of ${uiState.formattedTotal} • ${uiState.formattedFree} free",
                style = typography.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Storage Visualizer Bar
            HubSegmentedStorageBar(
                categories = uiState.categories,
                totalBytes = uiState.totalBytes,
                freeBytes = uiState.freeBytes,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Legend Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayCategories = uiState.categories.filter {
                    it.type != StorageCategoryType.DOWNLOADS && it.bytes > 0L
                }
                displayCategories.forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(cat.type.color)
                        )
                        Text(
                            text = "${cat.type.title}: ${cat.formattedSize}",
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clean Space Action Banner
            val clean = uiState.cleanSummary
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isOled) Color(0xFF171B24) else colors.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (clean.totalReclaimableBytes > 0L) {
                                    "${StorageVolume.formatBytes(clean.totalReclaimableBytes)} Reclaimable Junk"
                                } else "Storage Cleaner",
                                style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Clear cache, duplicates & temp files",
                                style = typography.caption.copy(fontSize = 11.sp),
                                color = colors.textSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onCleanClick,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = if (isOled) Color.Black else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Clean Space",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Segmented storage bar showing proportional category blocks
 */
@Composable
private fun HubSegmentedStorageBar(
    categories: List<StorageCategoryStat>,
    totalBytes: Long,
    freeBytes: Long,
    modifier: Modifier = Modifier
) {
    val barHeight = 12.dp
    val freeColor = Color(0x33888888)

    Box(
        modifier = modifier
            .height(barHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(freeColor)
    ) {
        if (totalBytes > 0L) {
            Row(modifier = Modifier.fillMaxSize()) {
                val nonZero = categories.filter { it.type != StorageCategoryType.DOWNLOADS && it.bytes > 0L }
                for (cat in nonZero) {
                    val weight = (cat.bytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.005f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(barHeight)
                            .background(cat.type.color)
                    )
                }

                val freeWeight = (freeBytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.001f)
                if (freeWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(freeWeight)
                            .height(barHeight)
                            .background(Color.Transparent)
                    )
                }
            }
        }
    }
}

/**
 * 2-Columns Grid of 6 Core Categories:
 * Downloads, Images, Videos, Audio, Documents, Apps
 */
@Composable
private fun GoogleFilesCategoryGrid(
    storageUiState: StorageAnalysisUiState,
    isOled: Boolean,
    onCategoryClick: (FileCategory) -> Unit,
    onAppsClick: () -> Unit
) {
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
        // Row 1: Downloads & Images
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoogleFilesCategoryCard(
                title = "Downloads",
                countText = downloadsStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = downloadsStat?.formattedSize ?: "",
                icon = Icons.Outlined.Download,
                tint = Color(0xFF00ACC1),
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.DOWNLOADS) }
            )
            GoogleFilesCategoryCard(
                title = "Images",
                countText = imagesStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = imagesStat?.formattedSize ?: "",
                icon = Icons.Outlined.Image,
                tint = Color(0xFF4CAF50),
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.IMAGES) }
            )
        }

        // Row 2: Videos & Audio
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoogleFilesCategoryCard(
                title = "Videos",
                countText = videosStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = videosStat?.formattedSize ?: "",
                icon = Icons.Outlined.Videocam,
                tint = Color(0xFFE91E63),
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.VIDEOS) }
            )
            GoogleFilesCategoryCard(
                title = "Audio",
                countText = audioStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = audioStat?.formattedSize ?: "",
                icon = Icons.Outlined.AudioFile,
                tint = Color(0xFF9C27B0),
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.AUDIO) }
            )
        }

        // Row 3: Documents & Apps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoogleFilesCategoryCard(
                title = "Documents",
                countText = docsStat?.let { "${it.fileCount} files" } ?: "Scanning...",
                sizeText = docsStat?.formattedSize ?: "",
                icon = Icons.Outlined.Description,
                tint = Color(0xFF3F51B5),
                isOled = isOled,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick(FileCategory.DOCUMENTS) }
            )
            GoogleFilesCategoryCard(
                title = "Apps",
                countText = if (storageUiState.userAppCount > 0) "${storageUiState.userAppCount} apps" else "Scanning...",
                sizeText = if (storageUiState.userAppBytes > 0) StorageVolume.formatBytes(storageUiState.userAppBytes) else "",
                icon = Icons.Outlined.Apps,
                tint = Color(0xFF2196F3),
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
private fun GoogleFilesCategoryCard(
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
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isOled) Color(0xFF101217) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val sub = if (sizeText.isNotEmpty()) "$countText • $sizeText" else countText
                Text(
                    text = sub,
                    style = typography.caption.copy(fontSize = 11.sp),
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
        shape = RoundedCornerShape(18.dp),
        color = if (isOled) Color(0xFF101217) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Internal storage",
                    style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${uiState.formattedFree} free of ${uiState.formattedTotal}",
                    style = typography.caption.copy(fontSize = 12.sp),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { uiState.usedRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colors.accent,
                    trackColor = colors.divider.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Open Storage",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
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
        shape = RoundedCornerShape(18.dp),
        color = if (isOled) Color(0xFF101217) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF009688).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SdCard,
                    contentDescription = null,
                    tint = Color(0xFF009688),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volume.title,
                    style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${StorageVolume.formatBytes(volume.freeBytes)} free of ${StorageVolume.formatBytes(volume.totalBytes)}",
                    style = typography.caption.copy(fontSize = 12.sp),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { volume.usedRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF009688),
                    trackColor = colors.divider.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Clean Utility list row (Recycle Bin, Safe Folder, Wi-Fi Transfer)
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
                .size(40.dp)
                .clip(CircleShape)
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
                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = typography.caption.copy(fontSize = 11.sp),
                color = colors.textSecondary
            )
        }

        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = badgeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = badgeText,
                    style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
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
            modifier = Modifier.size(18.dp)
        )
    }
}
