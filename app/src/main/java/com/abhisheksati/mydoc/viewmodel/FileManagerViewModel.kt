package com.abhisheksati.mydoc.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import com.abhisheksati.mydoc.util.UserMessages
import com.abhisheksati.mydoc.domain.model.ClipboardState
import com.abhisheksati.mydoc.domain.model.DocumentTypeFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.PartialStorageOperationException
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.FileSortOrder
import com.abhisheksati.mydoc.domain.model.FolderProperties
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.data.local.SettingsRepository
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
    val docTypeFilter: DocumentTypeFilter = DocumentTypeFilter.ALL,
    val isBrowsingStorage: Boolean = false,
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
    private val storageRepository: StorageRepository,
    baseDir: File? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val backStack = ArrayDeque<File>()

    private val initialDir: File = baseDir ?: try {
        Environment.getExternalStorageDirectory() ?: File("/")
    } catch (_: Exception) {
        File("/")
    }

    private var registeredVolumes: List<StorageVolume> = emptyList()

    private val _uiState = MutableStateFlow(
        FileManagerUiState(
            currentDirectory = initialDir,
            breadcrumbTrail = buildBreadcrumb(initialDir),
            isGridView = settingsRepository?.filesGridView?.value ?: false,
            showHiddenFiles = settingsRepository?.showHiddenFiles?.value ?: false
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
        viewModelScope.launch {
            val volumes = storageRepository.getStorageVolumes()
            registeredVolumes = volumes
            _uiState.update { it.copy(storageVolumes = volumes) }
        }
    }

    fun startBrowsingStorage(directory: File = initialDir) {
        backStack.clear()
        _uiState.update { it.copy(isBrowsingStorage = true, activeCategory = null) }
        loadDirectory(directory, addToHistory = false)
    }

    fun exitToHub() {
        backStack.clear()
        _uiState.update {
            it.copy(
                isBrowsingStorage = false,
                activeCategory = null,
                searchQuery = "",
                isSelectionMode = false,
                selectedFiles = emptySet(),
                canGoBack = false
            )
        }
    }

    fun loadDirectory(directory: File, addToHistory: Boolean = true) {
        val originCategory = _uiState.value.activeCategory
        if (addToHistory && directory.absolutePath != _uiState.value.currentDirectory.absolutePath) {
            // Coming out of a category view, the level above is that category's own folder, not
            // whatever directory the hub happened to be pointing at.
            backStack.push(originCategory?.let(::categoryDirectory) ?: _uiState.value.currentDirectory)
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentDirectory = directory,
                    breadcrumbTrail = buildBreadcrumb(directory),
                    // A category view is kept on screen by `activeCategory` alone. Clearing it here
                    // without claiming the directory surface unmounted the pane mid-navigation, so
                    // tapping a folder inside Downloads bounced straight back to the Library hub.
                    isBrowsingStorage = it.isBrowsingStorage || originCategory != null,
                    activeCategory = null,
                    isLoading = true,
                    selectedFiles = emptySet(),
                    isSelectionMode = false,
                    canGoBack = backStack.isNotEmpty() || hasParent(directory) || it.isBrowsingStorage
                )
            }

            val items = storageRepository.listDirectory(
                directory = directory,
                showHidden = _uiState.value.showHiddenFiles,
                sortOrder = _uiState.value.sortOrder
            )

            val filtered = visibleItems(items, _uiState.value)

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
            if (_uiState.value.isBrowsingStorage) {
                exitToHub()
                return true
            }
            return false
        }

        val parent = _uiState.value.currentDirectory.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            loadDirectory(parent, addToHistory = false)
            return true
        }

        if (_uiState.value.isBrowsingStorage) {
            exitToHub()
            return true
        }
        return false
    }

    fun handleBackPress(): Boolean {
        if (_uiState.value.isTrashOpen) {
            closeTrash()
            return true
        }
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
        if (_uiState.value.isBrowsingStorage) {
            return navigateUp()
        }
        return false
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
                userMessage = if (selected.size == 1) {
                    "Copied 1 item to clipboard"
                } else {
                    "Copied ${selected.size} items to clipboard"
                }
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
                        userMessage = if (count == 1) "Pasted 1 item" else "Pasted $count items"
                    )
                }
                loadDirectory(targetDir, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = UserMessages.actionFailed("Paste", err)
                    )
                }
                // A partial batch really did write the items that succeeded; leaving the listing
                // stale would tell the reader nothing arrived.
                if (err is PartialStorageOperationException) {
                    loadDirectory(targetDir, addToHistory = false)
                }
                refreshVolumes()
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
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed("Create folder", err)) }
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
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed("Create file", err)) }
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
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed("Rename", err)) }
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
                        userMessage = UserMessages.actionFailed("Delete", err)
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
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed("Delete", err)) }
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
                        userMessage = UserMessages.actionFailed("Move to Trash", err)
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
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed("Move to Trash", err)) }
            }
        }
    }

    fun moveToTrashFiles(files: List<File>, onComplete: (() -> Unit)? = null) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val result = storageRepository.moveToTrash(files)
            result.onSuccess { trashed ->
                _uiState.update {
                    it.copy(
                        lastTrashedItems = trashed,
                        userMessage = "Moved ${trashed.size} items to Recycle Bin"
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed("Move to Recycle Bin", err)) }
            }
        }
    }

    fun clearLastTrashedItems() {
        _uiState.update { it.copy(lastTrashedItems = emptyList()) }
    }

    fun undoLastTrash() {
        undoTrash(_uiState.value.lastTrashedItems)
    }

    fun undoTrash(items: List<com.abhisheksati.mydoc.domain.model.TrashItem>) {
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
                        userMessage = UserMessages.actionFailed("Restore", err)
                    )
                }
            }
        }
    }

    fun restoreTrashItems(items: List<com.abhisheksati.mydoc.domain.model.TrashItem>, onComplete: (() -> Unit)? = null) {
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
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = UserMessages.actionFailed("Restore", err)
                    )
                }
            }
        }
    }

    fun permanentlyDeleteTrashItems(items: List<com.abhisheksati.mydoc.domain.model.TrashItem>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = storageRepository.deletePermanently(items)
            result.onSuccess { count ->
                _uiState.update { it.copy(userMessage = "Permanently deleted $count items") }
                refreshVolumes()
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed("Delete", err)) }
            }
        }
    }

    fun emptyTrash(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = storageRepository.emptyTrash()
            result.onSuccess {
                _uiState.update { it.copy(userMessage = "Recycle Bin emptied") }
                refreshVolumes()
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed("Empty Recycle Bin", err)) }
            }
        }
    }

    fun openTrash() {
        _uiState.update { it.copy(isTrashOpen = true) }
    }

    fun closeTrash() {
        _uiState.update { it.copy(isTrashOpen = false) }
    }

    fun setDocTypeFilter(filter: DocumentTypeFilter) {
        _uiState.update { state ->
            // Changing what is visible changes what Select All would select, so the selection is
            // cleared rather than left pointing at rows the user can no longer see.
            val next = state.copy(docTypeFilter = filter, selectedFiles = emptySet(), isSelectionMode = false)
            next.copy(filteredItems = visibleItems(next.items, next))
        }
    }

    fun selectCategory(category: FileCategory?) {
        if (category == null) {
            _uiState.update { it.copy(activeCategory = null, canGoBack = _uiState.value.isBrowsingStorage) }
            if (_uiState.value.isBrowsingStorage) {
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            }
            return
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
            val filtered = visibleItems(items, _uiState.value)
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
                        userMessage = UserMessages.actionFailed("Compression", err)
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
                        userMessage = UserMessages.actionFailed("Compression", err)
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
                        userMessage = UserMessages.actionFailed("Extraction", err)
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
        val next = !_uiState.value.isGridView
        _uiState.update { it.copy(isGridView = next) }
        settingsRepository?.setFilesGridView(next)
    }

    fun toggleHiddenFiles() {
        val next = !_uiState.value.showHiddenFiles
        _uiState.update { it.copy(showHiddenFiles = next) }
        settingsRepository?.setShowHiddenFiles(next)
        loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(searchQuery = query, filteredItems = visibleItems(state.items, state.copy(searchQuery = query)))
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

    private fun visibleItems(items: List<StorageItem>, state: FileManagerUiState): List<StorageItem> =
        filterItems(
            items = items,
            query = state.searchQuery,
            docFilter = state.docTypeFilter,
            documentsOnly = state.activeCategory == FileCategory.DOCUMENTS
        )

    /**
     * The one projection the list, the item count and Select All all read. The document-type chip
     * used to filter only inside the composable, so selecting all picked up files the chip had
     * hidden - and a share, cut or trash then acted on them.
     */
    private fun filterItems(
        items: List<StorageItem>,
        query: String,
        docFilter: DocumentTypeFilter,
        documentsOnly: Boolean
    ): List<StorageItem> {
        val q = query.trim().lowercase(Locale.ROOT)
        val searched = if (q.isEmpty()) items else items.filter { it.name.lowercase(Locale.ROOT).contains(q) }
        if (!documentsOnly || docFilter == DocumentTypeFilter.ALL) return searched
        return searched.filter { item ->
            when (docFilter) {
                DocumentTypeFilter.ALL -> true
                DocumentTypeFilter.PDF -> item.fileType == StorageFileType.PDF
                DocumentTypeFilter.WORD -> item.fileType == StorageFileType.WORD
                DocumentTypeFilter.EXCEL -> item.fileType == StorageFileType.EXCEL
                DocumentTypeFilter.PPT -> item.fileType == StorageFileType.PPT
                DocumentTypeFilter.TEXT -> item.fileType == StorageFileType.CODE_TEXT
            }
        }
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

    /**
     * The real folder behind a category, for the categories that are directory listings rather
     * than MediaStore queries. Downloads is the one that shows folders at all, so it is the only
     * case where a folder can be drilled into from a category.
     */
    private fun categoryDirectory(category: FileCategory): File = when (category) {
        FileCategory.DOWNLOADS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        else -> _uiState.value.currentDirectory
    }

    private fun isRootDirectory(dir: File): Boolean {
        return dir.absolutePath == "/storage/emulated/0" ||
                dir.absolutePath == "/" ||
                dir.parentFile == null ||
                registeredVolumes.any { it.path == dir.absolutePath }
    }

    private fun hasParent(dir: File): Boolean {
        if (isRootDirectory(dir)) return false
        // Path math only: this feeds a back affordance, and a directory we are already
        // showing necessarily exists. Statting here would put disk IO on the main thread.
        return dir.parentFile != null
    }

    class Factory(
        private val storageRepository: StorageRepository,
        private val settingsRepository: SettingsRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FileManagerViewModel::class.java)) {
                return FileManagerViewModel(storageRepository, settingsRepository = settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
