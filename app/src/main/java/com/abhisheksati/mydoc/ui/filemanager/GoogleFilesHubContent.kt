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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.EditorialAction
import com.abhisheksati.mydoc.designsystem.components.PaperSurface
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsDark
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.PaperShape
import com.abhisheksati.mydoc.designsystem.tokens.rememberSheetPalette
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
    val onClick: () -> Unit
)

/**
 * The Files index printed on paper: a storage sheet, then collections, devices,
 * pinned folders and utilities as sheets with flat icon plates.
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
            onClick = { onCategoryClick(FileCategory.DOCUMENTS) }
        ),
        IndexEntry(
            title = "Downloads",
            icon = StorageCategoryType.DOWNLOADS.icon,
            meta = filesMeta(stat(StorageCategoryType.DOWNLOADS)),
            onClick = { onCategoryClick(FileCategory.DOWNLOADS) }
        ),
        IndexEntry(
            title = "Images",
            icon = StorageCategoryType.IMAGES.icon,
            meta = filesMeta(stat(StorageCategoryType.IMAGES)),
            onClick = { onCategoryClick(FileCategory.IMAGES) }
        ),
        IndexEntry(
            title = "Videos",
            icon = StorageCategoryType.VIDEOS.icon,
            meta = filesMeta(stat(StorageCategoryType.VIDEOS)),
            onClick = { onCategoryClick(FileCategory.VIDEOS) }
        ),
        IndexEntry(
            title = "Audio",
            icon = StorageCategoryType.AUDIO.icon,
            meta = filesMeta(stat(StorageCategoryType.AUDIO)),
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

/** One index entry printed on its own sheet, with a flat icon plate. */
@Composable
private fun VividEntryCard(
    title: String,
    meta: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val typography = LocalAppTypography.current
    val paper = rememberSheetPalette()

    PaperSurface(
        palette = paper,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                bottom = AppSpacing.small
            )
            .pressScaleEffect(onClick = onClick),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.small + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(paper.sheetEdge, PaperShape)
                    .border(1.dp, paper.rule, PaperShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = paper.ink,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.bodyStrong,
                    color = paper.ink,
                    maxLines = 1
                )
                Text(
                    text = meta,
                    style = typography.caption.withTabularFigures(),
                    color = paper.inkMuted,
                    maxLines = 1
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = paper.inkMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * The storage figure printed on a sheet: the used space set large, then an
 * accent gauge and a folded corner.
 */
@Composable
private fun StorageReadout(
    storageUiState: StorageAnalysisUiState,
    onBrowseInternalStorage: () -> Unit,
    onCleanClick: () -> Unit
) {
    val typography = LocalAppTypography.current
    val paper = rememberSheetPalette()
    val reclaimable = storageUiState.cleanSummary.totalReclaimableBytes
    val usedRatio = storageUiState.usedRatio.coerceIn(0f, 1f)

    PaperSurface(
        palette = paper,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.xSmall)
            .pressScaleEffect(onClick = onBrowseInternalStorage),
        elevation = 6.dp,
        foldSize = 20.dp
    ) {
        Column(modifier = Modifier.padding(AppSpacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Internal storage",
                    style = typography.caption,
                    color = paper.inkMuted
                )
                Box(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = paper.inkMuted,
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
                    color = paper.ink
                )
                Text(
                    text = if (storageUiState.formattedTotal.isNotEmpty()) {
                        "  used of ${storageUiState.formattedTotal}"
                    } else {
                        "  calculating storage"
                    },
                    style = typography.caption,
                    color = paper.inkMuted,
                    modifier = Modifier.padding(bottom = AppSpacing.xSmall)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.small)
                    .height(6.dp)
                    .background(paper.track)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(usedRatio)
                        .background(paper.accent)
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
                    color = paper.inkMuted
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
}
