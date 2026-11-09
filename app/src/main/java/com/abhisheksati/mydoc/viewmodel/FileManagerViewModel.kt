package com.abhisheksati.mydoc.viewmodel

import com.abhisheksati.mydoc.util.UiText
import com.abhisheksati.mydoc.R
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import com.abhisheksati.mydoc.util.UserMessages
import com.abhisheksati.mydoc.domain.model.ClipboardState
import com.abhisheksati.mydoc.domain.model.DocumentTypeFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
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
import com.abhisheksati.mydoc.data.local.ExplorerClipboard
import com.abhisheksati.mydoc.data.local.ExplorerSearch
import com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions
import com.abhisheksati.mydoc.domain.model.ExplorerRenamePlan
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.drop
import com.abhisheksati.mydoc.data.local.ExplorerPreferences
import com.abhisheksati.mydoc.domain.model.FolderViewPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.abhisheksati.mydoc.data.local.ExplorerZipExtraction
import com.abhisheksati.mydoc.data.local.ZipArchiveChanged
import com.abhisheksati.mydoc.data.local.ZipPreviewCacheFull
import com.abhisheksati.mydoc.data.local.ZipExtractionRecovery
import com.abhisheksati.mydoc.domain.model.ArchiveStamp

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
    val imageFolder: String? = null,
    val isBrowsingStorage: Boolean = false,
    val isLoading: Boolean = false,
    /** Existing rows belong to this same source and may stay visible during refresh. */
    val isRefreshing: Boolean = false,
    val activeOperation: String? = null,
    val userMessage: UiText? = null,
    val canGoBack: Boolean = false,
    val selectedProperties: FolderProperties? = null,
    val calculatedChecksum: String? = null,
    val trashItems: List<com.abhisheksati.mydoc.domain.model.TrashItem> = emptyList(),
    val isTrashOpen: Boolean = false,
    val lastTrashedItems: List<com.abhisheksati.mydoc.domain.model.TrashItem> = emptyList(),
    val advancedSearch: ExplorerSearchOptions? = null,
    val searchIncomplete: Boolean = false,
    val renameRecovery: List<String> = emptyList()
)

/** Chip counts describe the searched items, not the chip-filtered rows: picking PDF must not erase the Word count. */
internal fun documentTypeCounts(items: List<StorageItem>, query: String): Map<DocumentTypeFilter, Int> {
    val q = query.trim().lowercase(Locale.ROOT)
    val searched = if (q.isEmpty()) items else items.filter { it.name.lowercase(Locale.ROOT).contains(q) }
    return mapOf(
        DocumentTypeFilter.ALL to searched.size,
        DocumentTypeFilter.PDF to searched.count { it.fileType == StorageFileType.PDF },
        DocumentTypeFilter.WORD to searched.count { it.fileType == StorageFileType.WORD },
        DocumentTypeFilter.EXCEL to searched.count { it.fileType == StorageFileType.EXCEL },
        DocumentTypeFilter.PPT to searched.count { it.fileType == StorageFileType.PPT },
        DocumentTypeFilter.TEXT to searched.count { it.fileType == StorageFileType.CODE_TEXT }
    )
}

class FileManagerViewModel(
    private val storageRepository: StorageRepository,
    baseDir: File? = null,
    private val settingsRepository: SettingsRepository? = null,
    val explorerClipboard: ExplorerClipboard = ExplorerClipboard(),
    val explorerPreferences: ExplorerPreferences = settingsRepository?.explorerPreferences ?: ExplorerPreferences(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val backStack = ArrayDeque<File>()

    /** Bumped by every directory/category request; an older, slower reply must not land. */
    private var listRequestGeneration = 0
    /** Bumped by every inspection and by dismissal; a slow walk/hash must not reopen a closed dialog. */
    private var inspectGeneration = 0
    private var advancedSearchJob: Job? = null
    private var checkedNavigationJob: Job? = null
    private var categorySortOrder = FileSortOrder()
    private var archivePreviewJob: Job? = null

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
        explorerClipboard.bind(viewModelScope, storageRepository)
        viewModelScope.launch {
            explorerPreferences.folderViews.collect {
                val state = _uiState.value
                if (state.isBrowsingStorage && state.activeCategory == null) {
                    applyFolderView(folderView(state.currentDirectory))
                }
            }
        }
        viewModelScope.launch { refreshRenameRecovery() }
        viewModelScope.launch { explorerClipboard.state.collect { clip -> _uiState.update { it.copy(clipboardState = clip) } } }
        viewModelScope.launch { explorerClipboard.revision.drop(1).collect { reloadCurrentSource() } }
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
        advancedSearchJob?.cancel()
        ++listRequestGeneration
        backStack.clear()
        _uiState.update {
            it.copy(
                isBrowsingStorage = false,
                advancedSearch = null,
                searchIncomplete = false,
                isLoading = false,
                activeCategory = null,
                searchQuery = "",
                isSelectionMode = false,
                selectedFiles = emptySet(),
                canGoBack = false
            )
        }
    }

    fun loadDirectory(directory: File, addToHistory: Boolean = true, restoreView: Boolean = true) {
        advancedSearchJob?.cancel()
        val originCategory = _uiState.value.activeCategory
        if (addToHistory && directory.absolutePath != _uiState.value.currentDirectory.absolutePath) {
            // Coming out of a category view, the level above is that category's own folder, not
            // whatever directory the hub happened to be pointing at.
            backStack.push(originCategory?.let(::categoryDirectory) ?: _uiState.value.currentDirectory)
        }

        val generation = ++listRequestGeneration
        viewModelScope.launch {
            if (generation != listRequestGeneration) return@launch
            val view = if (restoreView) folderView(directory) else FolderViewPreferences(_uiState.value.sortOrder, _uiState.value.isGridView)
            _uiState.update {
                it.copy(
                    currentDirectory = directory,
                    sortOrder = view.sortOrder,
                    isGridView = view.grid,
                    advancedSearch = null,
                    searchIncomplete = false,
                    breadcrumbTrail = buildBreadcrumb(directory),
                    // A category view is kept on screen by `activeCategory` alone. Clearing it here
                    // without claiming the directory surface unmounted the pane mid-navigation, so
                    // tapping a folder inside Downloads bounced straight back to the Library hub.
                    isBrowsingStorage = it.isBrowsingStorage || originCategory != null,
                    activeCategory = null,
                    isLoading = true,
                    isRefreshing = it.currentDirectory == directory && originCategory == null && it.items.isNotEmpty(),
                    items = if (it.currentDirectory == directory && originCategory == null) it.items else emptyList(),
                    filteredItems = if (it.currentDirectory == directory && originCategory == null) it.filteredItems else emptyList(),
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

            if (generation != listRequestGeneration) return@launch
            val state = _uiState.value
            val sorted = withContext(ioDispatcher) { sortSearchResults(items, state.sortOrder) }
            if (generation != listRequestGeneration) return@launch

            _uiState.update {
                it.copy(
                    items = sorted,
                    filteredItems = visibleItems(sorted, it),
                    isLoading = false,
                    isRefreshing = false
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
        if (parent != null) {
            val generation = ++listRequestGeneration
            viewModelScope.launch {
                // Statting the parent here put disk IO on the main thread inside the back gesture.
                val accessible = withContext(ioDispatcher) { parent.exists() && parent.canRead() }
                if (generation != listRequestGeneration) return@launch
                if (accessible) loadDirectory(parent, addToHistory = false)
                else if (_uiState.value.isBrowsingStorage) exitToHub()
            }
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

    fun copySelected() = prepareClipboard(ClipboardOperation.COPY, _uiState.value.selectedFiles.toList())

    fun cutSelected() = prepareClipboard(ClipboardOperation.CUT, _uiState.value.selectedFiles.toList())

    fun copyItem(file: File) = prepareClipboard(ClipboardOperation.COPY, listOf(file))

    fun cutItem(file: File) = prepareClipboard(ClipboardOperation.CUT, listOf(file))

    private fun prepareClipboard(operation: ClipboardOperation, files: List<File>) {
        if (files.isEmpty()) return
        val clipboard = ClipboardState(operation, files)
        // A running transfer owns the clipboard; rejection must not change the current selection.
        if (!explorerClipboard.set(clipboard)) {
            _uiState.update { it.copy(userMessage = UiText.Res(R.string.transfers_active)) }
            return
        }
        _uiState.update {
            it.copy(
                clipboardState = clipboard,
                selectedFiles = emptySet(),
                isSelectionMode = false,
                userMessage = when {
                    operation == ClipboardOperation.CUT -> UiText.Res(R.string.fm_moving_many, listOf(files.size))
                    files.size == 1 -> UiText.Res(R.string.fm_copied_one)
                    else -> UiText.Res(R.string.fm_copied_many, listOf(files.size))
                }
            )
        }
    }

    fun cancelClipboard() {
        if (!explorerClipboard.set(null)) return
        _uiState.update { it.copy(clipboardState = null) }
    }

    fun pasteClipboard() {
        explorerClipboard.paste(_uiState.value.currentDirectory)
    }

    fun searchAdvanced(options: ExplorerSearchOptions) = searchAdvancedAt(_uiState.value.currentDirectory, options)

    private fun searchAdvancedAt(root: File, options: ExplorerSearchOptions, navigate: Boolean = false) {
        if (!options.isValid) return
        advancedSearchJob?.cancel()
        val generation = ++listRequestGeneration
        val hidden = _uiState.value.showHiddenFiles
        if (navigate) backStack.clear()
        val view = folderView(root)
        _uiState.update { it.copy(currentDirectory = root, breadcrumbTrail = buildBreadcrumb(root),
            isBrowsingStorage = true, activeCategory = null,
            sortOrder = if (navigate) view.sortOrder else it.sortOrder, isGridView = if (navigate) view.grid else it.isGridView,
            canGoBack = if (navigate) true else it.canGoBack,
            advancedSearch = options, searchQuery = "", items = emptyList(), filteredItems = emptyList(), selectedFiles = emptySet(), isSelectionMode = false, isLoading = true, searchIncomplete = false) }
        advancedSearchJob = viewModelScope.launch {
            try {
                val result = ExplorerSearch().search(root, options, hidden)
                if (generation != listRequestGeneration) return@launch
                _uiState.update { state ->
                    val sorted = sortSearchResults(result.items, state.sortOrder)
                    state.copy(items = sorted, filteredItems = visibleItems(sorted, state), isLoading = false, searchIncomplete = result.incomplete)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generation == listRequestGeneration) _uiState.update { it.copy(isLoading = false, userMessage = UiText.Res(R.string.explorer_search_failed)) }
            }
        }
    }

    fun clearAdvancedSearch() = loadDirectory(_uiState.value.currentDirectory, addToHistory = false)

    fun saveCurrentSearch(label: String): Boolean {
        val state = _uiState.value
        val options = state.advancedSearch ?: return false
        return explorerPreferences.saveSearch(label, state.currentDirectory, options)
    }

    fun openSavedSearch(id: String): Job? {
        val saved = explorerPreferences.savedSearches.value.firstOrNull { it.id == id } ?: return null
        return openCheckedFolder(File(saved.directory), R.string.saved_search_folder_missing) {
            if (explorerPreferences.savedSearches.value.any { it.id == id }) {
                searchAdvancedAt(File(saved.directory), saved.optionsAt(), navigate = true)
            }
        }
    }

    fun openTransferDestination(directory: File): Job =
        openCheckedFolder(directory, R.string.transfers_destination_missing) { startBrowsingStorage(directory) }

    private fun openCheckedFolder(directory: File, missingMessage: Int, onAvailable: () -> Unit): Job {
        val generation = listRequestGeneration
        val category = _uiState.value.activeCategory
        checkedNavigationJob?.cancel()
        return viewModelScope.launch {
            val available = withContext(ioDispatcher) { runCatching { directory.isDirectory && directory.canRead() }.getOrDefault(false) }
            if (generation != listRequestGeneration || category != _uiState.value.activeCategory) return@launch
            if (!available) {
                _uiState.update { it.copy(userMessage = UiText.Res(missingMessage)) }
                return@launch
            }
            // Keep the current folder/search intact until the availability check succeeds.
            onAvailable()
        }.also { checkedNavigationJob = it }
    }

    private fun folderView(directory: File): FolderViewPreferences = explorerPreferences.folderView(directory)
        ?: FolderViewPreferences(grid = settingsRepository?.filesGridView?.value ?: false)

    private fun applyFolderView(view: FolderViewPreferences) {
        _uiState.update { current ->
            val sorted = sortSearchResults(current.items, view.sortOrder)
            val next = current.copy(sortOrder = view.sortOrder, isGridView = view.grid, items = sorted)
            next.copy(filteredItems = visibleItems(sorted, next))
        }
    }

    fun resetFolderView() {
        val state = _uiState.value
        if (!state.isBrowsingStorage || state.activeCategory != null) return
        if (!explorerPreferences.resetFolderView(state.currentDirectory)) {
            _uiState.update { it.copy(userMessage = UiText.Res(R.string.folder_view_failed)) }
        } else applyFolderView(folderView(state.currentDirectory))
    }

    private fun rememberFolderView() {
        val state = _uiState.value
        if (state.isBrowsingStorage && state.activeCategory == null &&
            !explorerPreferences.setFolderView(state.currentDirectory, FolderViewPreferences(state.sortOrder, state.isGridView))) {
            _uiState.update { it.copy(userMessage = UiText.Res(R.string.folder_view_failed)) }
        }
    }

    fun applyBatchRename(plan: ExplorerRenamePlan) {
        if (!plan.canApply || _uiState.value.activeOperation != null) return
        _uiState.update { it.copy(activeOperation = "rename", isLoading = true) }
        viewModelScope.launch {
            val result = storageRepository.renameBatch(plan)
            _uiState.update { it.copy(activeOperation = null, isLoading = false, selectedFiles = emptySet(), isSelectionMode = false,
                userMessage = when {
                    result.success -> UiText.Res(R.string.explorer_rename_done)
                    result.unresolvedPaths.isEmpty() -> UiText.Res(R.string.explorer_rename_rolled_back)
                    else -> UiText.Res(R.string.explorer_rename_recovery, listOf(result.unresolvedPaths.joinToString("\n")))
                }) }
            loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            refreshRenameRecovery()
        }
    }

    private suspend fun refreshRenameRecovery() {
        try {
            val recovery = storageRepository.pendingRenameRecovery()
            _uiState.update { it.copy(renameRecovery = recovery) }
        }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { _uiState.update { it.copy(userMessage = UiText.Res(R.string.explorer_recovery_unreadable)) } }
    }

    private fun sortSearchResults(items: List<StorageItem>, order: FileSortOrder): List<StorageItem> {
        val field = when (order.field) {
            FileSortField.NAME -> compareBy<StorageItem> { it.name.lowercase(Locale.ROOT) }
            FileSortField.DATE -> compareBy { it.lastModified }
            FileSortField.SIZE -> compareBy { it.sizeBytes }
            FileSortField.TYPE -> compareBy { it.extension }
        }
        return items.sortedWith(compareBy<StorageItem> { !it.isDirectory }.then(if (order.isAscending) field else field.reversed()))
    }

    fun createNewFolder(name: String) {
        viewModelScope.launch {
            val result = storageRepository.createDirectory(_uiState.value.currentDirectory, name)
            result.onSuccess {
                _uiState.update { it.copy(userMessage = UiText.Res(R.string.fm_folder_created, listOf(name))) }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed(R.string.fm_verb_create_folder, err)) }
            }
        }
    }

    fun createNewFile(name: String) {
        viewModelScope.launch {
            val result = storageRepository.createFile(_uiState.value.currentDirectory, name)
            result.onSuccess {
                _uiState.update { it.copy(userMessage = UiText.Res(R.string.fm_file_created, listOf(name))) }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed(R.string.fm_verb_create_file, err)) }
            }
        }
    }

    fun renameItem(file: File, newName: String) {
        viewModelScope.launch {
            val result = storageRepository.rename(file, newName)
            result.onSuccess {
                _uiState.update { it.copy(userMessage = UiText.Res(R.string.fm_renamed)) }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed(R.string.action_rename, err)) }
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
                        userMessage = UiText.Res(R.string.fm_deleted_many, listOf(count))
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = UserMessages.actionFailed(R.string.action_delete, err)
                    )
                }
            }
        }
    }

    fun deleteSingle(file: File) {
        viewModelScope.launch {
            val result = storageRepository.delete(listOf(file))
            result.onSuccess {
                _uiState.update { it.copy(userMessage = UiText.Res(R.string.fm_deleted_one, listOf(file.name))) }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed(R.string.action_delete, err)) }
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
                        userMessage = UiText.Res(R.string.fm_moved_many, listOf(trashed.size))
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = UserMessages.actionFailed(R.string.fm_verb_move_trash, err)
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
                        userMessage = UiText.Res(R.string.fm_moved_one, listOf(file.name))
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed(R.string.fm_verb_move_trash, err)) }
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
                        userMessage = UiText.Res(R.string.fm_moved_many, listOf(trashed.size))
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed(R.string.photo_move_to_bin_title, err)) }
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
                        userMessage = UiText.Res(R.string.fm_restored_many, listOf(count))
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = UserMessages.actionFailed(R.string.trash_restore, err)
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
                        userMessage = UiText.Res(R.string.fm_restored_many, listOf(count))
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
                        userMessage = UserMessages.actionFailed(R.string.trash_restore, err)
                    )
                }
            }
        }
    }

    fun permanentlyDeleteTrashItems(items: List<com.abhisheksati.mydoc.domain.model.TrashItem>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = storageRepository.deletePermanently(items)
            result.onSuccess { count ->
                _uiState.update { it.copy(userMessage = UiText.Res(R.string.fm_deleted_permanently, listOf(count))) }
                refreshVolumes()
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed(R.string.action_delete, err)) }
            }
        }
    }

    fun emptyTrash(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = storageRepository.emptyTrash()
            result.onSuccess {
                _uiState.update { it.copy(userMessage = UiText.Res(R.string.fm_bin_emptied)) }
                refreshVolumes()
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = UserMessages.actionFailed(R.string.trash_empty_bin, err)) }
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

    fun setImageFolder(folder: String?) {
        _uiState.update { state ->
            val next = state.copy(imageFolder = folder, selectedFiles = emptySet(), isSelectionMode = false)
            next.copy(filteredItems = visibleItems(next.items, next))
        }
    }

    fun selectCategory(category: FileCategory?) {
        advancedSearchJob?.cancel()
        _uiState.update { it.copy(advancedSearch = null, searchIncomplete = false) }
        if (category == null) {
            _uiState.update { it.copy(activeCategory = null, imageFolder = null, canGoBack = _uiState.value.isBrowsingStorage) }
            if (_uiState.value.isBrowsingStorage) {
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
            }
            return
        }

        val generation = ++listRequestGeneration
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeCategory = category,
                    imageFolder = it.imageFolder.takeIf { _ -> it.activeCategory == category },
                    sortOrder = when {
                        it.activeCategory == category -> it.sortOrder
                        category == FileCategory.IMAGES -> FileSortOrder(FileSortField.DATE, isAscending = false)
                        else -> categorySortOrder
                    },
                    isGridView = when {
                        it.activeCategory == category -> it.isGridView
                        category == FileCategory.IMAGES -> true
                        else -> settingsRepository?.filesGridView?.value ?: false
                    },
                    isLoading = true,
                    isRefreshing = it.activeCategory == category && it.items.isNotEmpty(),
                    items = if (it.activeCategory == category) it.items else emptyList(),
                    filteredItems = if (it.activeCategory == category) it.filteredItems else emptyList(),
                    selectedFiles = emptySet(),
                    isSelectionMode = false,
                    canGoBack = true
                )
            }
            val items = storageRepository.queryCategory(category)
            if (generation != listRequestGeneration) return@launch
            val sorted = sortSearchResults(items, _uiState.value.sortOrder)
            val filtered = visibleItems(sorted, _uiState.value)
            _uiState.update {
                it.copy(
                    items = sorted,
                    filteredItems = filtered,
                    isLoading = false,
                    isRefreshing = false
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
                        userMessage = UiText.Res(R.string.fm_archive_created, listOf(zipFile.name))
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = UserMessages.actionFailed(R.string.fm_verb_compression, err)
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
                        userMessage = UiText.Res(R.string.fm_archive_created, listOf(zipFile.name))
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = UserMessages.actionFailed(R.string.fm_verb_compression, err)
                    )
                }
            }
        }
    }

    fun cancelArchivePreview() { archivePreviewJob?.cancel() }

    fun openArchiveMember(archive: File, member: String, cacheRoot: File, expected: ArchiveStamp?, onReady: (File) -> Unit) {
        if (_uiState.value.activeOperation != null) return
        _uiState.update { it.copy(activeOperation = "ZIP preview", isLoading = true) }
        archivePreviewJob = viewModelScope.launch {
            try {
                val file = ExplorerZipExtraction().preview(archive, member, cacheRoot, expected)
                _uiState.update { it.copy(activeOperation = null, isLoading = false) }
                onReady(file)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(userMessage = zipEntryFailure(e)) } }
            finally { _uiState.update { it.copy(activeOperation = null, isLoading = false) } }
        }
    }

    fun extractArchiveSelection(archive: File, selected: Set<String>, expected: ArchiveStamp?) {
        if (_uiState.value.activeOperation != null || selected.isEmpty()) return
        _uiState.update { it.copy(activeOperation = "ZIP extraction", isLoading = true) }
        viewModelScope.launch {
            try {
                val parent = requireNotNull(archive.parentFile)
                val output = ExplorerZipExtraction().extract(archive, selected, parent, expected)
                _uiState.update { it.copy(userMessage = UiText.Res(R.string.fm_extracted, listOf(output.name))) }
                reloadCurrentSource()
                refreshVolumes()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(userMessage = zipEntryFailure(e)) } }
            finally { _uiState.update { it.copy(activeOperation = null, isLoading = false) } }
        }
    }

    private fun zipEntryFailure(error: Exception): UiText = when (error) {
        is ZipPreviewCacheFull -> UiText.Res(R.string.zip_preview_full)
        is ZipArchiveChanged -> UiText.Res(R.string.zip_changed)
        is ZipExtractionRecovery -> UiText.Res(R.string.zip_recovery, listOf(error.directory.absolutePath))
        else -> UiText.Joined(listOf(UiText.Res(R.string.zip_member_failed), UserMessages.actionFailed(R.string.fm_verb_extraction, error)))
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
                        userMessage = UiText.Res(R.string.fm_extracted, listOf(dir.name))
                    )
                }
                loadDirectory(_uiState.value.currentDirectory, addToHistory = false)
                refreshVolumes()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        activeOperation = null,
                        isLoading = false,
                        userMessage = UserMessages.actionFailed(R.string.fm_verb_extraction, err)
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
        if (_uiState.value.activeCategory != null) categorySortOrder = newOrder
        rememberFolderView()
        reloadCurrentSource()
    }

    /** Refresh stays on the current surface: re-run the search, re-query the category, or reload the folder. */
    fun refreshCurrentSource() = reloadCurrentSource()

    /**
     * Sorting or revealing hidden files must reload whatever is on screen. It used to call
     * `loadDirectory` unconditionally, which drops the active category and leaves the reader in a
     * folder they had not opened.
     */
    private fun reloadCurrentSource() {
        _uiState.value.advancedSearch?.let { searchAdvanced(it); return }
        val category = _uiState.value.activeCategory
        if (category != null) {
            selectCategory(category)
        } else {
            loadDirectory(_uiState.value.currentDirectory, addToHistory = false, restoreView = false)
        }
    }

    fun toggleViewMode() {
        val next = !_uiState.value.isGridView
        _uiState.update { it.copy(isGridView = next) }
        if (_uiState.value.isBrowsingStorage && _uiState.value.activeCategory == null) rememberFolderView()
        else settingsRepository?.setFilesGridView(next)
    }

    fun toggleHiddenFiles() {
        val next = !_uiState.value.showHiddenFiles
        _uiState.update { it.copy(showHiddenFiles = next) }
        settingsRepository?.setShowHiddenFiles(next)
        reloadCurrentSource()
    }

    fun clearSearchFilters() {
        setSearchQuery("")
        setDocTypeFilter(DocumentTypeFilter.ALL)
        if (_uiState.value.imageFolder != null) setImageFolder(null)
        if (_uiState.value.advancedSearch != null) clearAdvancedSearch()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(searchQuery = query, filteredItems = visibleItems(state.items, state.copy(searchQuery = query)))
        }
    }

    fun inspectItem(file: File) {
        val generation = ++inspectGeneration
        viewModelScope.launch {
            val (props, checksum) = withContext(ioDispatcher) {
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
                props to checksum
            }
            if (generation != inspectGeneration) return@launch
            _uiState.update {
                it.copy(
                    selectedProperties = props,
                    calculatedChecksum = checksum
                )
            }
        }
    }

    fun dismissPropertiesDialog() {
        ++inspectGeneration
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
            items = if (state.activeCategory == FileCategory.IMAGES && state.imageFolder != null) {
                items.filter { it.file.parent == state.imageFolder }
            } else items,
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
        private val settingsRepository: SettingsRepository? = null,
        private val clipboard: ExplorerClipboard = ExplorerClipboard(),
        private val preferences: ExplorerPreferences = settingsRepository?.explorerPreferences ?: ExplorerPreferences()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FileManagerViewModel::class.java)) {
                return FileManagerViewModel(storageRepository, settingsRepository = settingsRepository, explorerClipboard = clipboard, explorerPreferences = preferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
