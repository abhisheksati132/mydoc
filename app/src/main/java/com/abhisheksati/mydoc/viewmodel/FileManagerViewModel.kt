package com.abhisheksati.mydoc.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import com.abhisheksati.mydoc.domain.model.ClipboardState
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.FileSortOrder
import com.abhisheksati.mydoc.domain.model.FolderProperties
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.domain.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayDeque
import java.util.Locale

@Immutable
data class FileManagerUiState(
    val currentDirectory: File,
    val storageVolumes: List<StorageVolume> = emptyList(),
    val items: List<StorageItem> = emptyList(),
    val filteredItems: List<StorageItem> = emptyList(),
    val breadcrumbTrail: List<File> = emptyList(),
    val selectedFiles: Set<File> = emptySet(),
    val clipboardState: ClipboardState? = null,
    val isSelectionMode: Boolean = false,
    val sortOrder: FileSortOrder = FileSortOrder(FileSortField.NAME, isAscending = true),
    val isGridView: Boolean = false,
    val showHiddenFiles: Boolean = false,
    val searchQuery: String = "",
    val activeCategory: FileCategory? = null,
    val isLoading: Boolean = false,
    val activeOperation: String? = null,
    val userMessage: String? = null,
    val canGoBack: Boolean = false,
    val selectedProperties: FolderProperties? = null,
    val calculatedChecksum: String? = null,
    val trashItems: List<com.abhisheksati.mydoc.domain.model.TrashItem> = emptyList(),
    val isTrashOpen: Boolean = false,
    val lastTrashedItems: List<com.abhisheksati.mydoc.domain.model.TrashItem> = emptyList()
)


class FileManagerViewModel(
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val backStack = ArrayDeque<File>()

    private val initialDir: File = Environment.getExternalStorageDirectory()

    private val _uiState = MutableStateFlow(
        FileManagerUiState(
            currentDirectory = initialDir,
            breadcrumbTrail = buildBreadcrumb(initialDir)
        )
    )
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    init {
        refreshVolumes()
        loadDirectory(initialDir, addToHistory = false)
        viewModelScope.launch {
            storageRepository.getTrashItems().collect { list ->
                _uiState.update { it.copy(trashItems = list) }
            }
        }
    }

    fun refreshVolumes() {
        val volumes = storageRepository.getStorageVolumes()
        _uiState.update { it.copy(storageVolumes = volumes) }
    }

    fun loadDirectory(directory: File, addToHistory: Boolean = true) {
        if (!directory.exists() || !directory.isDirectory) return

        if (addToHistory && directory.absolutePath != _uiState.value.currentDirectory.absolutePath) {
            backStack.push(_uiState.value.currentDirectory)
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentDirectory = directory,
                    breadcrumbTrail = buildBreadcrumb(directory),
                    activeCategory = null,
                    isLoading = true,
                    selectedFiles = emptySet(),
                    isSelectionMode = false,
                    canGoBack = backStack.isNotEmpty() || hasParent(directory)
                )
            }

            val items = storageRepository.listDirectory(
                directory = directory,
                showHidden = _uiState.value.showHiddenFiles,
                sortOrder = _uiState.value.sortOrder
            )

            val filtered = filterItems(items, _uiState.value.searchQuery)

            _uiState.update {
                it.copy(
                    items = items,
                    filteredItems = filtered,
                    isLoading = false
                )
            }
        }
    }

    fun navigateUp(): Boolean {
        if (_uiState.value.activeCategory != null) {
            selectCategory(null)
            return true
        }

        if (backStack.isNotEmpty()) {
            val prev = backStack.pop()
            loadDirectory(prev, addToHistory = false)
            return true
        }

        if (isRootDirectory(_uiState.value.currentDirectory)) {
            return false
        }

        val parent = _uiState.value.currentDirectory.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            loadDirectory(parent, addToHistory = false)
            return true
        }
        return false
    }

    fun handleBackPress(): Boolean {
        if (_uiState.value.isSelectionMode) {
            clearSelection()
            return true
        }
        if (_uiState.value.searchQuery.isNotEmpty()) {
            setSearchQuery("")
            return true
        }
        if (_uiState.value.activeCategory != null) {
            selectCategory(null)
            return true
        }
        return navigateUp()
    }

    fun navigateToCrumb(folder: File) {
        if (folder.absolutePath != _uiState.value.currentDirectory.absolutePath) {
            loadDirectory(folder, addToHistory = true)
        }
    }

    fun toggleSelection(file: File) {
        _uiState.update { state ->
            val updated = state.selectedFiles.toMutableSet()
            if (updated.contains(file)) {
                updated.remove(file)
            } else {
                updated.add(file)
            }
            state.copy(
                selectedFiles = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allFiles = state.filteredItems.map { it.file }.toSet()
            state.copy(
                selectedFiles = allFiles,
                isSelectionMode = allFiles.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedFiles = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun copySelected() {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboardState = ClipboardState(ClipboardOperation.COPY, selected),
                selectedFiles = emptySet(),
                isSelectionMode = false,
                userMessage = "Copied ${selected.size} items to clipboard"
            )
        }
    }

    fun cutSelected() {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboardState = ClipboardState(ClipboardOperation.CUT, selected),
                selectedFiles = emptySet(),
                isSelectionMode = false,
                userMessage = "Moving ${selected.size} items"
            )
        }
    }

    fun cancelClipboard() {
        _uiState.update { it.copy(clipboardState = null) }
    }

    fun pasteClipboard() {
        val clip = _uiState.value.clipboardState ?: return
        val targetDir = _uiState.value.currentDirectory
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeOperation = if (clip.operation == ClipboardOperation.COPY) "Copying items..." else "Moving items...",
                    isLoading = true
                )
            }

            val result = if (clip.operation == ClipboardOperation.COPY) {
                storageRepository.copy(clip.items, targetDir)
            } else {
                storageRepository.move(clip.items, targetDir)
            }

            result.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        clipboardState = null,
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Pasted $count items"
                    )
                }
                loadDirectory(targetDir, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun createNewFolder(name: String) {
        viewModelScope.launch {
            val result = storageRepository.createDirectory(_uiState.value.currentDirectory, name)
            result.onSuccess {
                _uiState.update { it.copy(userMessage = "Folder '$name' created") }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = err.message ?: "Failed to create folder") }
            }
        }
    }

    fun createNewFile(name: String) {
        viewModelScope.launch {
            val result = storageRepository.createFile(_uiState.value.currentDirectory, name)
            result.onSuccess {
                _uiState.update { it.copy(userMessage = "File '$name' created") }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = err.message ?: "Failed to create file") }
            }
        }
    }

    fun renameItem(file: File, newName: String) {
        viewModelScope.launch {
            val result = storageRepository.rename(file, newName)
            result.onSuccess {
                _uiState.update { it.copy(userMessage = "Renamed successfully") }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = err.message ?: "Rename failed") }
            }
        }
    }

    fun deleteSelected() {
        val files = _uiState.value.selectedFiles.toList()
        if (files.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(activeOperation = "Deleting...", isLoading = true) }
            val result = storageRepository.delete(files)
            result.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        selectedFiles = emptySet(),
                        isSelectionMode = false,
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Deleted $count items"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Delete failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun deleteSingle(file: File) {
        viewModelScope.launch {
            val result = storageRepository.delete(listOf(file))
            result.onSuccess {
                _uiState.update { it.copy(userMessage = "Deleted '${file.name}'") }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Delete failed: ${err.message}") }
            }
        }
    }

    fun moveToTrashSelected() {
        val files = _uiState.value.selectedFiles.toList()
        if (files.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(activeOperation = "Moving to Trash...", isLoading = true) }
            val result = storageRepository.moveToTrash(files)
            result.onSuccess { trashed ->
                _uiState.update {
                    it.copy(
                        selectedFiles = emptySet(),
                        isSelectionMode = false,
                        activeOperation = null,
                        isLoading = false,
                        lastTrashedItems = trashed,
                        userMessage = "Moved ${trashed.size} items to Recycle Bin"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Failed to move to Trash: ${err.message}"
                    )
                }
            }
        }
    }

    fun moveToTrashSingle(file: File) {
        viewModelScope.launch {
            val result = storageRepository.moveToTrash(listOf(file))
            result.onSuccess { trashed ->
                _uiState.update {
                    it.copy(
                        lastTrashedItems = trashed,
                        userMessage = "Moved '${file.name}' to Recycle Bin"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Failed to move to Trash: ${err.message}") }
            }
        }
    }

    fun undoLastTrash() {
        val items = _uiState.value.lastTrashedItems
        if (items.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(activeOperation = "Restoring...", isLoading = true) }
            val result = storageRepository.restoreFromTrash(items)
            result.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        lastTrashedItems = emptyList(),
                        userMessage = "Restored $count items"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Restore failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun restoreTrashItems(items: List<com.abhisheksati.mydoc.domain.model.TrashItem>) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeOperation = "Restoring...", isLoading = true) }
            val result = storageRepository.restoreFromTrash(items)
            result.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Restored $count items"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Restore failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun permanentlyDeleteTrashItems(items: List<com.abhisheksati.mydoc.domain.model.TrashItem>) {
        viewModelScope.launch {
            val result = storageRepository.deletePermanently(items)
            result.onSuccess { count ->
                _uiState.update { it.copy(userMessage = "Permanently deleted $count items") }
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Delete failed: ${err.message}") }
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val result = storageRepository.emptyTrash()
            result.onSuccess {
                _uiState.update { it.copy(userMessage = "Recycle Bin emptied") }
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Failed to empty Recycle Bin: ${err.message}") }
            }
        }
    }

    fun openTrash() {
        _uiState.update { it.copy(isTrashOpen = true) }
    }

    fun closeTrash() {
        _uiState.update { it.copy(isTrashOpen = false) }
    }

    fun selectCategory(category: FileCategory?) {
        if (category == null) {
            _uiState.update { it.copy(activeCategory = null) }
            loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            return
        }

        if (category == FileCategory.DOWNLOADS) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists()) {
                _uiState.update { it.copy(activeCategory = null) }
                loadDirectory(downloadDir, addToHistory = true)
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeCategory = category,
                    isLoading = true,
                    selectedFiles = emptySet(),
                    isSelectionMode = false,
                    canGoBack = true
                )
            }
            val items = storageRepository.queryCategory(category)
            val filtered = filterItems(items, _uiState.value.searchQuery)
            _uiState.update {
                it.copy(
                    items = items,
                    filteredItems = filtered,
                    isLoading = false
                )
            }
        }
    }

    fun compressSelected(archiveName: String) {
        val files = _uiState.value.selectedFiles.toList()
        if (files.isEmpty()) return
        val finalName = if (archiveName.endsWith(".zip", ignoreCase = true)) archiveName else "$archiveName.zip"
        val destinationZip = File(_uiState.value.currentDirectory, finalName)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeOperation = "Compressing ${files.size} items...",
                    isLoading = true
                )
            }
            val result = storageRepository.zip(files, destinationZip) { current, total ->
                _uiState.update {
                    it.copy(activeOperation = "Compressing ($current/$total)...")
                }
            }
            result.onSuccess { zipFile ->
                _uiState.update {
                    it.copy(
                        selectedFiles = emptySet(),
                        isSelectionMode = false,
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Created archive '${zipFile.name}'"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Compression failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun compressSingle(file: File, archiveName: String) {
        val finalName = if (archiveName.endsWith(".zip", ignoreCase = true)) archiveName else "$archiveName.zip"
        val destinationZip = File(file.parentFile ?: _uiState.value.currentDirectory, finalName)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeOperation = "Compressing '${file.name}'...",
                    isLoading = true
                )
            }
            val result = storageRepository.zip(listOf(file), destinationZip)
            result.onSuccess { zipFile ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Created archive '${zipFile.name}'"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Compression failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun extractArchive(file: File) {
        val parent = file.parentFile ?: _uiState.value.currentDirectory
        val folderName = file.nameWithoutExtension.ifEmpty { "Extracted" }
        val targetDir = File(parent, folderName)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeOperation = "Extracting '${file.name}'...",
                    isLoading = true
                )
            }
            val result = storageRepository.unzip(file, targetDir) { count ->
                _uiState.update {
                    it.copy(activeOperation = "Extracting ($count files)...")
                }
            }
            result.onSuccess { dir ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Extracted into '${dir.name}'"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = "Extraction failed: ${err.message}"
                    )
                }
            }
        }
    }


    fun toggleSort(field: FileSortField) {
        val current = _uiState.value.sortOrder
        val newOrder = if (current.field == field) {
            current.copy(isAscending = !current.isAscending)
        } else {
            FileSortOrder(field, isAscending = true)
        }
        _uiState.update { it.copy(sortOrder = newOrder) }
        loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleHiddenFiles() {
        _uiState.update { it.copy(showHiddenFiles = !it.showHiddenFiles) }
        loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterItems(state.items, query)
            state.copy(searchQuery = query, filteredItems = filtered)
        }
    }

    fun inspectItem(file: File) {
        viewModelScope.launch {
            val props = if (file.isDirectory) {
                storageRepository.getFolderProperties(file)
            } else {
                FolderProperties(
                    file = file,
                    totalSizeBytes = file.length(),
                    totalFilesCount = 1,
                    totalFoldersCount = 0,
                    isReadable = file.canRead(),
                    isWritable = file.canWrite()
                )
            }
            val checksum = if (!file.isDirectory && file.length() < 250_000_000L) {
                storageRepository.calculateHash(file, "SHA-256")
            } else null

            _uiState.update {
                it.copy(
                    selectedProperties = props,
                    calculatedChecksum = checksum
                )
            }
        }
    }

    fun dismissPropertiesDialog() {
        _uiState.update {
            it.copy(
                selectedProperties = null,
                calculatedChecksum = null
            )
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun filterItems(items: List<StorageItem>, query: String): List<StorageItem> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return items
        return items.filter { it.name.lowercase(Locale.ROOT).contains(q) }
    }

    private fun buildBreadcrumb(directory: File): List<File> {
        val crumbs = mutableListOf<File>()
        var curr: File? = directory
        while (curr != null) {
            crumbs.add(0, curr)
            // Stop at storage root or any registered volume root
            if (isRootDirectory(curr)) {
                break
            }
            curr = curr.parentFile
        }
        return crumbs
    }

    private fun isRootDirectory(dir: File): Boolean {
        return dir.absolutePath == "/storage/emulated/0" ||
                dir.absolutePath == "/" ||
                dir.parentFile == null ||
                _uiState.value.storageVolumes.any { it.path == dir.absolutePath }
    }

    private fun hasParent(dir: File): Boolean {
        if (isRootDirectory(dir)) return false
        val parent = dir.parentFile ?: return false
        return parent.exists() && parent.canRead()
    }

    class Factory(
        private val storageRepository: StorageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FileManagerViewModel::class.java)) {
                return FileManagerViewModel(storageRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
