package com.abhisheksati.mydoc.ui.storage

import com.abhisheksati.mydoc.R
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume

enum class StorageCategoryType(
    // A resource id, not a string: this enum is a model, so the label has to be resolved where a
    // configuration is available.
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val fileCategory: FileCategory?
) {
    DOWNLOADS(R.string.cat_downloads, Icons.Outlined.Download, FileCategory.DOWNLOADS),
    IMAGES(R.string.cat_images, Icons.Outlined.Image, FileCategory.IMAGES),
    VIDEOS(R.string.cat_videos, Icons.Outlined.Videocam, FileCategory.VIDEOS),
    AUDIO(R.string.cat_audio, Icons.Outlined.AudioFile, FileCategory.AUDIO),
    DOCUMENTS(R.string.cat_documents, Icons.Outlined.Description, FileCategory.DOCUMENTS),
    APKS(R.string.cat_apks, Icons.Outlined.Android, FileCategory.APKS),
    ARCHIVES(R.string.cat_archives, Icons.Outlined.FolderZip, FileCategory.ARCHIVES),
    APPS(R.string.cat_apps, Icons.Outlined.Apps, null),
    LARGE_FILES(R.string.cat_large_files, Icons.Outlined.DataUsage, FileCategory.LARGE_FILES),
    SYSTEM_OTHER(R.string.cat_system_other, Icons.Outlined.SettingsSuggest, null)
}

data class StorageCategoryStat(
    val type: StorageCategoryType,
    val bytes: Long,
    val fileCount: Int,
    val percentageOfUsed: Float,
    val percentageOfTotal: Float
) {
    val formattedSize: String
        get() = StorageVolume.formatBytes(bytes)
}

data class StorageCleanQuickSummary(
    val tempFilesCount: Int = 0,
    val tempFilesBytes: Long = 0L,
    val duplicateGroupsCount: Int = 0,
    val duplicateWastedBytes: Long = 0L,
    val emptyFoldersCount: Int = 0,
    val largeFilesCount: Int = 0,
    val largeFilesBytes: Long = 0L,
    val totalReclaimableBytes: Long = 0L
)

data class StorageAnalysisUiState(
    val isLoading: Boolean = true,
    val isScanningCleaner: Boolean = false,
    val selectedVolumeIndex: Int = 0,
    val volumes: List<StorageVolume> = emptyList(),
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedRatio: Float = 0f,
    val categories: List<StorageCategoryStat> = emptyList(),
    val userAppCount: Int = 0,
    val userAppBytes: Long = 0L,
    val cleanSummary: StorageCleanQuickSummary = StorageCleanQuickSummary(),
    val lastRefreshedMs: Long = 0L,
    val analyzedVolumePath: String? = null,
    val errorMessage: String? = null
) {
    val activeVolume: StorageVolume?
        get() = volumes.getOrNull(selectedVolumeIndex)

    val formattedTotal: String
        get() = StorageVolume.formatBytes(totalBytes)

    val formattedUsed: String
        get() = StorageVolume.formatBytes(usedBytes)

    val formattedFree: String
        get() = StorageVolume.formatBytes(freeBytes)

    val usedPercentage: Int
        get() = (usedRatio * 100f).toInt().coerceIn(0, 100)

    val freePercentage: Int
        get() = 100 - usedPercentage
}
