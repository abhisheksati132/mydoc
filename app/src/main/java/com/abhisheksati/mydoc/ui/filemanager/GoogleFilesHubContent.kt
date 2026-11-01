package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.foundation.background
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
import com.abhisheksati.mydoc.designsystem.components.EditorialRow
import com.abhisheksati.mydoc.designsystem.components.EditorialRule
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
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
 * The Files index: storage as one measured figure on a gauge, then the collections,
 * the storage devices, the pinned folders and the utilities as ruled sections with
 * folio numbers. No cards, no tiles, no coloured containers on this page.
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

    val storageFolio = "01"
    val collectionsFolio = "02"
    val devicesFolio = "03"
    val pinnedFolio = if (hasDevices) "04" else "03"
    val utilitiesFolio = if (pinnedFolders.isNotEmpty()) {
        if (hasDevices) "05" else "04"
    } else {
        if (hasDevices) "04" else "03"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item(key = "storage_header") {
            AppSectionHeader(
                title = "Storage",
                index = storageFolio,
                showRule = false
            )
        }
        item(key = "storage_readout") {
            StorageReadout(
                storageUiState = storageUiState,
                onBrowseInternalStorage = onBrowseInternalStorage,
                onCleanClick = onCleanClick
            )
        }

        item(key = "collections_header") {
            AppSectionHeader(
                title = "Collections",
                index = collectionsFolio,
                showRule = false
            )
        }
        items(
            count = collections.size,
            key = { index -> "collection_${collections[index].title}" }
        ) { index ->
            val entry = collections[index]
            EditorialRow(
                title = entry.title,
                meta = entry.meta,
                leading = { IndexGlyph(entry.icon) },
                trailing = { TrailingChevron() },
                onClick = entry.onClick,
                divider = index < collections.lastIndex
            )
        }

        if (hasDevices) {
            item(key = "devices_header") {
                AppSectionHeader(title = "Devices", index = devicesFolio)
            }
            items(
                count = devices.size,
                key = { index -> "device_${devices[index].path}" }
            ) { index ->
                val volume = devices[index]
                EditorialRow(
                    title = volume.title,
                    meta = "${StorageVolume.formatBytes(volume.freeBytes)} free of ${StorageVolume.formatBytes(volume.totalBytes)}",
                    leading = { IndexGlyph(Icons.Outlined.SdCard) },
                    trailing = { TrailingChevron() },
                    onClick = { onBrowseStorageVolume(volume) },
                    divider = index < devices.lastIndex
                )
            }
        }

        if (pinnedFolders.isNotEmpty()) {
            item(key = "pinned_header") {
                AppSectionHeader(title = "Pinned", index = pinnedFolio)
            }
            items(
                count = pinnedFolders.size,
                key = { index -> "pinned_${pinnedFolders[index].absolutePath}" }
            ) { index ->
                val folder = pinnedFolders[index]
                EditorialRow(
                    title = folder.name,
                    meta = folder.absolutePath,
                    leading = { IndexGlyph(Icons.Outlined.Folder) },
                    trailing = { TrailingChevron() },
                    onClick = { onFolderClick(folder) },
                    divider = index < pinnedFolders.lastIndex
                )
            }
        }

        item(key = "utilities_header") {
            AppSectionHeader(title = "Utilities", index = utilitiesFolio)
        }
        item(key = "utilities_rows") {
            EditorialRow(
                title = "Recycle Bin",
                meta = "Recently deleted, kept for 30 days",
                leading = { IndexGlyph(Icons.Outlined.DeleteSweep) },
                trailing = {
                    if (trashCount > 0) {
                        Text(
                            text = "$trashCount",
                            style = LocalAppTypography.current.captionStrong.withTabularFigures(),
                            color = LocalAppColors.current.accent
                        )
                    } else {
                        TrailingChevron()
                    }
                },
                onClick = onOpenTrash,
                divider = false
            )
        }
    }
}

/**
 * The one figure on the page: used space set large, a two-stroke gauge, and the
 * reclaimable bytes offered as a text action rather than a filled button.
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressScaleEffect(onClick = onBrowseInternalStorage)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppSpacing.screenPadding,
                        end = AppSpacing.screenPadding,
                        bottom = AppSpacing.small
                    ),
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
                    .padding(horizontal = AppSpacing.screenPadding),
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
                    .padding(
                        start = AppSpacing.screenPadding,
                        end = AppSpacing.screenPadding,
                        top = AppSpacing.small
                    )
                    .height(AppSpacing.rule)
                    .background(colors.track)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(usedRatio)
                        .background(colors.ink)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.screenPadding,
                    end = AppSpacing.screenPadding,
                    top = AppSpacing.small,
                    bottom = AppSpacing.small
                ),
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

        EditorialRule(
            modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
            thickness = AppSpacing.hairline,
            color = colors.line
        )
    }
}

@Composable
private fun IndexGlyph(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = LocalAppColors.current.inkMuted,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun TrailingChevron() {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = LocalAppColors.current.inkMuted,
        modifier = Modifier.size(16.dp)
    )
}
