package com.abhisheksati.mydoc.ui.storage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume

enum class StorageCategoryType(
    val title: String,
    val color: Color,
    val icon: ImageVector,
    val fileCategory: FileCategory?
) {
    IMAGES("Images", Color(0xFF4CAF50), Icons.Outlined.Image, FileCategory.IMAGES),
    VIDEOS("Videos", Color(0xFFE91E63), Icons.Outlined.Videocam, FileCategory.VIDEOS),
    AUDIO("Audio", Color(0xFF9C27B0), Icons.Outlined.AudioFile, FileCategory.AUDIO),
    DOCUMENTS("Documents", Color(0xFF3F51B5), Icons.Outlined.Description, FileCategory.DOCUMENTS),
    APKS("APKs", Color(0xFF009688), Icons.Outlined.Android, FileCategory.APKS),
    ARCHIVES("Archives", Color(0xFFFFA000), Icons.Outlined.FolderZip, FileCategory.ARCHIVES),
    APPS("Apps & Data", Color(0xFF2196F3), Icons.Outlined.Apps, null),
    LARGE_FILES("Large Files (>50MB)", Color(0xFFFF5722), Icons.Outlined.DataUsage, FileCategory.LARGE_FILES),
    SYSTEM_OTHER("System & Other", Color(0xFF78909C), Icons.Outlined.SettingsSuggest, null)
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
