package com.abhisheksati.mydoc.ui.storage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.domain.repository.StorageRepository
import com.abhisheksati.mydoc.tools.cleaner.StorageCleanerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StorageViewModel(
    private val context: Context,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageAnalysisUiState())
    val uiState: StateFlow<StorageAnalysisUiState> = _uiState.asStateFlow()

    private val cleanerManager = StorageCleanerManager()

    init {
        refresh()
    }

    fun selectVolume(index: Int) {
        if (index != _uiState.value.selectedVolumeIndex && index in _uiState.value.volumes.indices) {
            _uiState.update { it.copy(selectedVolumeIndex = index) }
            analyzeStorage(_uiState.value.volumes[index])
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val volumes = withContext(Dispatchers.IO) {
                storageRepository.getStorageVolumes()
            }

            if (volumes.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No storage volumes found") }
                return@launch
            }

            val currentIndex = _uiState.value.selectedVolumeIndex.coerceIn(0, volumes.lastIndex)
            val selectedVolume = volumes[currentIndex]

            _uiState.update {
                it.copy(
                    volumes = volumes,
                    selectedVolumeIndex = currentIndex,
                    totalBytes = selectedVolume.totalBytes,
                    usedBytes = selectedVolume.usedBytes,
                    freeBytes = selectedVolume.freeBytes,
                    usedRatio = selectedVolume.usedRatio
                )
            }

            analyzeStorage(selectedVolume)
        }
    }

    private fun analyzeStorage(volume: StorageVolume) {
        viewModelScope.launch {
            val (stats, appCount, appBytes, largeFilesBytes, largeFilesCount) = withContext(Dispatchers.IO) {
                gatherStorageStats(volume)
            }

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    totalBytes = volume.totalBytes,
                    usedBytes = volume.usedBytes,
                    freeBytes = volume.freeBytes,
                    usedRatio = volume.usedRatio,
                    categories = stats,
                    userAppCount = appCount,
                    userAppBytes = appBytes,
                    lastRefreshedMs = System.currentTimeMillis()
                )
            }

            // Perform lightweight quick scan for junk cleaner recommendations
            startQuickCleanScan(volume, largeFilesCount, largeFilesBytes)
        }
    }

    private fun startQuickCleanScan(volume: StorageVolume, largeFilesCount: Int, largeFilesBytes: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningCleaner = true) }
            val quickSummary = withContext(Dispatchers.IO) {
                try {
                    val root = volume.file
                    if (root.canRead()) {
                        val result = cleanerManager.scanDirectory(root)
                        StorageCleanQuickSummary(
                            tempFilesCount = result.tempFiles.size,
                            tempFilesBytes = result.tempFiles.sumOf { it.length() },
                            duplicateGroupsCount = result.duplicateGroups.size,
                            duplicateWastedBytes = result.duplicateGroups.sumOf { it.wastedBytes },
                            emptyFoldersCount = result.emptyFolders.size,
                            largeFilesCount = largeFilesCount.coerceAtLeast(result.largeFiles.size),
                            largeFilesBytes = if (largeFilesBytes > 0L) largeFilesBytes else result.largeFiles.sumOf { it.length() },
                            totalReclaimableBytes = result.totalReclaimableBytes
                        )
                    } else {
                        StorageCleanQuickSummary(largeFilesCount = largeFilesCount, largeFilesBytes = largeFilesBytes)
                    }
                } catch (_: Exception) {
                    StorageCleanQuickSummary(largeFilesCount = largeFilesCount, largeFilesBytes = largeFilesBytes)
                }
            }

            _uiState.update {
                it.copy(
                    isScanningCleaner = false,
                    cleanSummary = quickSummary
                )
            }
        }
    }

    private fun gatherStorageStats(volume: StorageVolume): StorageAnalysisData {
        val totalUsed = volume.usedBytes
        val totalCapacity = volume.totalBytes

        // 1. MediaStore queries by category
        val downloads = queryMediaCategoryStats(FileCategory.DOWNLOADS)
        val images = queryMediaCategoryStats(FileCategory.IMAGES)
        val videos = queryMediaCategoryStats(FileCategory.VIDEOS)
        val audio = queryMediaCategoryStats(FileCategory.AUDIO)
        val documents = queryMediaCategoryStats(FileCategory.DOCUMENTS)
        val apks = queryMediaCategoryStats(FileCategory.APKS)
        val archives = queryMediaCategoryStats(FileCategory.ARCHIVES)
        val largeFiles = queryMediaCategoryStats(FileCategory.LARGE_FILES)

        // 2. Installed User Apps
        var appCount = 0
        var appBytes = 0L
        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!isSystem) {
                    appCount++
                    val apkFile = File(app.sourceDir ?: "")
                    if (apkFile.exists()) {
                        appBytes += apkFile.length()
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. System & Other calculation
        val accountedBytes = images.bytes + videos.bytes + audio.bytes + documents.bytes + apks.bytes + archives.bytes + appBytes
        val systemOtherBytes = (totalUsed - accountedBytes).coerceAtLeast(0L)

        // 4. Map into category statistics with ratios
        fun createStat(
            type: StorageCategoryType,
            bytes: Long,
            fileCount: Int
        ): StorageCategoryStat {
            val pctUsed = if (totalUsed > 0L) (bytes.toFloat() / totalUsed.toFloat()) else 0f
            val pctTotal = if (totalCapacity > 0L) (bytes.toFloat() / totalCapacity.toFloat()) else 0f
            return StorageCategoryStat(
                type = type,
                bytes = bytes,
                fileCount = fileCount,
                percentageOfUsed = pctUsed,
                percentageOfTotal = pctTotal
            )
        }

        val categoryList = listOf(
            createStat(StorageCategoryType.DOWNLOADS, downloads.bytes, downloads.count),
            createStat(StorageCategoryType.IMAGES, images.bytes, images.count),
            createStat(StorageCategoryType.VIDEOS, videos.bytes, videos.count),
            createStat(StorageCategoryType.AUDIO, audio.bytes, audio.count),
            createStat(StorageCategoryType.DOCUMENTS, documents.bytes, documents.count),
            createStat(StorageCategoryType.APKS, apks.bytes, apks.count),
            createStat(StorageCategoryType.ARCHIVES, archives.bytes, archives.count),
            createStat(StorageCategoryType.APPS, appBytes, appCount),
            createStat(StorageCategoryType.SYSTEM_OTHER, systemOtherBytes, 1)
        )

        return StorageAnalysisData(
            stats = categoryList,
            userAppCount = appCount,
            userAppBytes = appBytes,
            largeFilesBytes = largeFiles.bytes,
            largeFilesCount = largeFiles.count
        )
    }

    private fun queryMediaCategoryStats(category: FileCategory): CategoryCountAndSize {
        var totalBytes = 0L
        var count = 0

        val (selection, _) = when (category) {
            FileCategory.DOCUMENTS -> {
                val sel = "${MediaStore.Files.FileColumns.DATA} LIKE '%.pdf' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.doc' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.docx' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.xls' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.xlsx' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.ppt' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.pptx' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.txt' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.csv' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.md' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.epub'"
                Pair(sel, null)
            }
            FileCategory.IMAGES -> {
                val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.jpg' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.jpeg' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.png' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.webp' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.gif'"
                Pair(sel, null)
            }
            FileCategory.VIDEOS -> {
                val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO} OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.mp4' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.mkv' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.mov' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.webm'"
                Pair(sel, null)
            }
            FileCategory.AUDIO -> {
                val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO} OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.mp3' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.wav' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.m4a' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.flac' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.aac'"
                Pair(sel, null)
            }
            FileCategory.APKS -> {
                val sel = "${MediaStore.Files.FileColumns.DATA} LIKE '%.apk' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.xapk'"
                Pair(sel, null)
            }
            FileCategory.ARCHIVES -> {
                val sel = "${MediaStore.Files.FileColumns.DATA} LIKE '%.zip' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.rar' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.7z' OR " +
                        "${MediaStore.Files.FileColumns.DATA} LIKE '%.tar'"
                Pair(sel, null)
            }
            FileCategory.LARGE_FILES -> {
                val sel = "${MediaStore.Files.FileColumns.SIZE} >= 52428800"
                Pair(sel, null)
            }
            FileCategory.DOWNLOADS -> {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir != null && downloadDir.exists() && downloadDir.isDirectory) {
                    val files = downloadDir.listFiles()?.filter { it.isFile } ?: emptyList()
                    return CategoryCountAndSize(bytes = files.sumOf { it.length() }, count = files.size)
                }
                return CategoryCountAndSize(0L, 0)
            }
        }

        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns.SIZE)
            context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                while (cursor.moveToNext()) {
                    if (sizeIndex != -1) {
                        val s = cursor.getLong(sizeIndex)
                        if (s > 0L) totalBytes += s
                    }
                    count++
                }
            }
        } catch (_: Exception) {}

        return CategoryCountAndSize(bytes = totalBytes, count = count)
    }

    private data class CategoryCountAndSize(val bytes: Long, val count: Int)

    private data class StorageAnalysisData(
        val stats: List<StorageCategoryStat>,
        val userAppCount: Int,
        val userAppBytes: Long,
        val largeFilesBytes: Long,
        val largeFilesCount: Int
    )

    class Factory(
        private val context: Context,
        private val storageRepository: StorageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StorageViewModel::class.java)) {
                return StorageViewModel(context, storageRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
