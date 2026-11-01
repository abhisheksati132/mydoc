package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.EditorialAction
import com.abhisheksati.mydoc.designsystem.components.VividIconTile
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsDark
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.VividGradients
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageAnalysisUiState
import com.abhisheksati.mydoc.ui.storage.StorageCategoryStat
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType
import java.io.File

private data class IndexEntry(
    val title: String,
    val icon: ImageVector,
    val meta: String,
    val family: VividGradients.Family,
    val onClick: () -> Unit
)

/**
 * The Files index, vivid premium: a storage card with a spectrum gauge, then
 * collections, devices, pinned folders and utilities as soft cards with
 * gradient plaques hue-coded per kind.
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
    fun stat(type: StorageCategoryType): StorageCategoryStat? =
        storageUiState.categories.firstOrNull { it.type == type }

    fun filesMeta(stat: StorageCategoryStat?): String = when {
        stat == null -> "Scanning…"
        stat.fileCount == 0 -> "Empty"
        stat.fileCount == 1 -> "1 file · ${stat.formattedSize}"
        else -> "${stat.fileCount} files · ${stat.formattedSize}"
    }

    val collections = listOf(
        IndexEntry(
            title = "Documents",
            icon = StorageCategoryType.DOCUMENTS.icon,
            meta = filesMeta(stat(StorageCategoryType.DOCUMENTS)),
            family = VividGradients.Family.BRAND,
            onClick = { onCategoryClick(FileCategory.DOCUMENTS) }
        ),
        IndexEntry(
            title = "Downloads",
            icon = StorageCategoryType.DOWNLOADS.icon,
            meta = filesMeta(stat(StorageCategoryType.DOWNLOADS)),
            family = VividGradients.Family.OCEAN,
            onClick = { onCategoryClick(FileCategory.DOWNLOADS) }
        ),
        IndexEntry(
            title = "Images",
            icon = StorageCategoryType.IMAGES.icon,
            meta = filesMeta(stat(StorageCategoryType.IMAGES)),
            family = VividGradients.Family.SUNSET,
            onClick = { onCategoryClick(FileCategory.IMAGES) }
        ),
        IndexEntry(
            title = "Videos",
            icon = StorageCategoryType.VIDEOS.icon,
            meta = filesMeta(stat(StorageCategoryType.VIDEOS)),
            family = VividGradients.Family.GOLD,
            onClick = { onCategoryClick(FileCategory.VIDEOS) }
        ),
        IndexEntry(
            title = "Audio",
            icon = StorageCategoryType.AUDIO.icon,
            meta = filesMeta(stat(StorageCategoryType.AUDIO)),
            family = VividGradients.Family.FOREST,
            onClick = { onCategoryClick(FileCategory.AUDIO) }
        ),
        IndexEntry(
            title = "Apps",
            icon = StorageCategoryType.APPS.icon,
            meta = if (storageUiState.userAppCount > 0) {
                val noun = if (storageUiState.userAppCount == 1) "app" else "apps"
                "${storageUiState.userAppCount} $noun · ${StorageVolume.formatBytes(storageUiState.userAppBytes)}"
            } else {
                "Scanning…"
            },
            family = VividGradients.Family.GRAPE,
            onClick = onAppsClick
        )
    )

    val devices = storageUiState.volumes.drop(1)
    val hasDevices = devices.isNotEmpty()
    val paletteKey = if (LocalIsDark.current) if (LocalIsOled.current) "b" else "d" else "l"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item(key = "storage_header_$paletteKey") {
            AppSectionHeader(title = "Storage")
        }
        item(key = "storage_readout_$paletteKey") {
            StorageReadout(
                storageUiState = storageUiState,
                onBrowseInternalStorage = onBrowseInternalStorage,
                onCleanClick = onCleanClick
            )
        }

        item(key = "collections_header_$paletteKey") {
            AppSectionHeader(title = "Collections")
        }
        items(
            count = collections.size,
            key = { index -> "collection_${collections[index].title}_$paletteKey" }
        ) { index ->
            val entry = collections[index]
            VividEntryCard(
                title = entry.title,
                meta = entry.meta,
                icon = entry.icon,
                family = entry.family,
                onClick = entry.onClick
            )
        }

        if (hasDevices) {
            item(key = "devices_header_$paletteKey") {
                AppSectionHeader(title = "Devices")
            }
            items(
                count = devices.size,
                key = { index -> "device_${devices[index].path}_$paletteKey" }
            ) { index ->
                val volume = devices[index]
                VividEntryCard(
                    title = volume.title,
                    meta = "${StorageVolume.formatBytes(volume.freeBytes)} free of ${StorageVolume.formatBytes(volume.totalBytes)}",
                    icon = Icons.Outlined.SdCard,
                    family = VividGradients.Family.OCEAN,
                    onClick = { onBrowseStorageVolume(volume) }
                )
            }
        }

        if (pinnedFolders.isNotEmpty()) {
            item(key = "pinned_header_$paletteKey") {
                AppSectionHeader(title = "Pinned")
            }
            items(
                count = pinnedFolders.size,
                key = { index -> "pinned_${pinnedFolders[index].absolutePath}_$paletteKey" }
            ) { index ->
                val folder = pinnedFolders[index]
                VividEntryCard(
                    title = folder.name,
                    meta = folder.absolutePath,
                    icon = Icons.Outlined.Folder,
                    family = VividGradients.Family.GOLD,
                    onClick = { onFolderClick(folder) }
                )
            }
        }

        item(key = "utilities_header_$paletteKey") {
            AppSectionHeader(title = "Utilities")
        }
        item(key = "utilities_rows_$paletteKey") {
            VividEntryCard(
                title = "Recycle Bin",
                meta = "Recently deleted, kept for 30 days",
                icon = Icons.Outlined.DeleteSweep,
                family = VividGradients.Family.FIRE,
                trailing = if (trashCount > 0) {
                    {
                        Text(
                            text = "$trashCount",
                            style = LocalAppTypography.current.captionStrong.withTabularFigures(),
                            color = LocalAppColors.current.accent
                        )
                    }
                } else {
                    null
                },
                onClick = onOpenTrash
            )
        }
    }
}

/** One index entry: soft card, gradient plaque, quiet chevron or custom trailing. */
@Composable
private fun VividEntryCard(
    title: String,
    meta: String,
    icon: ImageVector,
    family: VividGradients.Family,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val shape = AppRadius.cardShape

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                bottom = AppSpacing.small
            )
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .pressScaleEffect(onClick = onClick)
            .padding(AppSpacing.small + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        VividIconTile(
            icon = icon,
            family = family,
            size = 40.dp,
            iconSize = 20.dp,
            shape = shape
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyStrong,
                color = colors.ink,
                maxLines = 1
            )
            Text(
                text = meta,
                style = typography.caption.withTabularFigures(),
                color = colors.inkMuted,
                maxLines = 1
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * The storage figure: a soft card, the used space set large, and a spectrum
 * gauge that makes the number feel physical.
 */
@Composable
private fun StorageReadout(
    storageUiState: StorageAnalysisUiState,
    onBrowseInternalStorage: () -> Unit,
    onCleanClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val reclaimable = storageUiState.cleanSummary.totalReclaimableBytes
    val usedRatio = storageUiState.usedRatio.coerceIn(0f, 1f)
    val shape = AppRadius.largeShape

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.xSmall)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .pressScaleEffect(onClick = onBrowseInternalStorage)
            .padding(AppSpacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Internal storage",
                style = typography.caption,
                color = colors.inkMuted
            )
            Box(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.xSmall),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (storageUiState.formattedUsed.isNotEmpty()) {
                    storageUiState.formattedUsed
                } else {
                    "—"
                },
                style = typography.title.withTabularFigures(),
                color = colors.ink
            )
            Text(
                text = if (storageUiState.formattedTotal.isNotEmpty()) {
                    "  used of ${storageUiState.formattedTotal}"
                } else {
                    "  calculating storage"
                },
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = AppSpacing.xSmall)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.small)
                .height(8.dp)
                .clip(AppRadius.roundShape)
                .background(colors.track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(usedRatio)
                    .clip(AppRadius.roundShape)
                    .background(VividGradients.wordmark(colors))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${storageUiState.formattedFree} free · ${storageUiState.usedPercentage}% used",
                style = typography.caption.withTabularFigures(),
                color = colors.inkMuted
            )
            if (reclaimable > 0) {
                EditorialAction(
                    label = "Clean ${StorageVolume.formatBytes(reclaimable)}",
                    onClick = onCleanClick
                )
            }
        }
    }
}
