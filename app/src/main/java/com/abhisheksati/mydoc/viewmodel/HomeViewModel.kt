package com.abhisheksati.mydoc.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.domain.model.Shelf
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import com.abhisheksati.mydoc.ui.shelves.LibraryTypeFilter
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HomeUiState(
    val shelves: List<Shelf> = emptyList(),
    val selectedShelfId: Long? = null,
    val selectedTypeFilter: LibraryTypeFilter = LibraryTypeFilter.EVERYTHING,
    val selectedCategory: DocumentCategory = DocumentCategory.ALL,
    val searchQuery: String = "",
    val documents: List<DocumentFile> = emptyList(),
    val continueReadingDoc: DocumentFile? = null,
    val recents: List<DocumentFile> = emptyList(),
    val favorites: List<DocumentFile> = emptyList(),
    val unfiledCount: Int = 0,
    val sortOrder: DocumentSortOrder = DocumentSortOrder.LAST_OPENED,
    val isSearching: Boolean = false,
    val isLoading: Boolean = false
)

private data class HomeFilters(
    val shelfId: Long?,
    val typeFilter: LibraryTypeFilter,
    val category: DocumentCategory,
    val query: String,
    val sortOrder: DocumentSortOrder
)

private data class DataSnapshot(
    val recents: List<DocumentFile>,
    val favorites: List<DocumentFile>,
    val shelves: List<Shelf>,
    val libraryDocs: List<DocumentFile>
)

class HomeViewModel(
    private val repository: DocumentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(settingsRepository.defaultStartupTab.value)
    val currentTab: StateFlow<HomeTab> = _currentTab.asStateFlow()

    private val _selectedShelfId = MutableStateFlow<Long?>(null)
    val selectedShelfId: StateFlow<Long?> = _selectedShelfId.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow(LibraryTypeFilter.EVERYTHING)
    val selectedTypeFilter: StateFlow<LibraryTypeFilter> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DocumentCategory.ALL)
    private val _searchQuery = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _documentsSource = settingsRepository.scanFullDeviceStorage.flatMapLatest { scanAll ->
        if (scanAll) {
            repository.getAllDeviceDocuments()
        } else {
            repository.getRecents()
        }
    }

    private val _dataFlow = combine(
        repository.getRecents(),
        repository.getFavorites(),
        repository.getAllShelves(),
        _documentsSource
    ) { recents, favorites, shelves, libraryDocs ->
        DataSnapshot(recents, favorites, shelves, libraryDocs)
    }

    private val _filterFlow = combine(
        _selectedShelfId,
        _selectedTypeFilter,
        _selectedCategory,
        _searchQuery,
        settingsRepository.sortOrder
    ) { shelfId, typeFilter, category, query, sortOrder ->
        HomeFilters(shelfId, typeFilter, category, query, sortOrder)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _dataFlow,
        _filterFlow
    ) { (recents, favorites, shelves, libraryDocs), filters ->
        val (shelfId, typeFilter, category, query, sortOrder) = filters

        val sourceList = when (category) {
            DocumentCategory.FAVORITES -> favorites
            DocumentCategory.PDF -> libraryDocs.filter { it.category == DocumentCategory.PDF }
            DocumentCategory.EPUB -> libraryDocs.filter { it.category == DocumentCategory.EPUB }
            DocumentCategory.WORD -> libraryDocs.filter { it.category == DocumentCategory.WORD }
            DocumentCategory.EXCEL -> libraryDocs.filter { it.category == DocumentCategory.EXCEL }
            DocumentCategory.PPT -> libraryDocs.filter { it.category == DocumentCategory.PPT }
            DocumentCategory.TEXT -> libraryDocs.filter { it.category == DocumentCategory.TEXT }
            DocumentCategory.ALL -> libraryDocs
        }

        val filtered = sourceList.filter { doc ->
            // Shelf filter: null = all, -1 = unfiled, else specific shelf id
            val matchesShelf = when (shelfId) {
                null -> true
                -1L -> doc.shelfId == null
                else -> doc.shelfId == shelfId
            }

            // Type filter
            val matchesType = when (typeFilter) {
                LibraryTypeFilter.EVERYTHING -> true
                LibraryTypeFilter.DOCS -> {
                    doc.extension in listOf("pdf", "epub", "xps", "oxps", "cbz", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "md", "csv")
                }
                LibraryTypeFilter.PHOTOS -> {
                    doc.mimeType.startsWith("image/") || doc.extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
                }
                LibraryTypeFilter.VIDEOS -> {
                    doc.mimeType.startsWith("video/") || doc.extension in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv")
                }
                LibraryTypeFilter.AUDIO -> {
                    doc.mimeType.startsWith("audio/") || doc.extension in listOf("mp3", "wav", "m4a", "flac", "aac", "ogg", "opus")
                }
            }

            // Search query
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                doc.displayName.contains(query.trim(), ignoreCase = true)
            }

            matchesShelf && matchesType && matchesQuery
        }

        val sorted = when (sortOrder) {
            DocumentSortOrder.LAST_OPENED -> filtered.sortedByDescending { it.lastOpenedTimestamp }
            DocumentSortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            DocumentSortOrder.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
            DocumentSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            DocumentSortOrder.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
        }

        val continueDoc = recents.firstOrNull { it.lastPage > 0 && it.pageCount > 0 } ?: recents.firstOrNull()
        val unfiledCount = libraryDocs.count { it.shelfId == null }

        HomeUiState(
            shelves = shelves,
            selectedShelfId = shelfId,
            selectedTypeFilter = typeFilter,
            selectedCategory = category,
            searchQuery = query,
            documents = sorted,
            continueReadingDoc = continueDoc,
            recents = recents,
            favorites = favorites,
            unfiledCount = unfiledCount,
            sortOrder = sortOrder,
            isSearching = query.isNotBlank(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = false)
    )

    fun selectTab(tab: HomeTab) {
        _currentTab.value = tab
    }

    fun selectShelf(shelfId: Long?) {
        _selectedShelfId.value = shelfId
    }

    fun selectTypeFilter(filter: LibraryTypeFilter) {
        _selectedTypeFilter.value = filter
    }

    fun selectCategory(category: DocumentCategory) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: DocumentSortOrder) {
        settingsRepository.setSortOrder(order)
    }

    fun toggleFavorite(document: DocumentFile) {
        viewModelScope.launch {
            repository.toggleFavorite(document)
        }
    }

    fun removeFromRecents(document: DocumentFile) {
        viewModelScope.launch {
            repository.removeFromRecents(document.uri)
        }
    }

    fun clearAllRecents() {
        viewModelScope.launch {
            repository.clearAllRecents()
        }
    }

    fun onDocumentOpened(uri: Uri, onResult: (Result<DocumentFile>) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerOpenedDocument(uri)
            onResult(result)
        }
    }

    fun createShelf(name: String, colorIndex: Int, folderUri: String? = null, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.createShelf(name, colorIndex, folderUri)
            onCreated?.invoke(id)
        }
    }

    fun updateShelf(shelf: Shelf) {
        viewModelScope.launch {
            repository.updateShelf(shelf)
        }
    }

    fun deleteShelf(shelfId: Long) {
        viewModelScope.launch {
            repository.deleteShelf(shelfId)
            if (_selectedShelfId.value == shelfId) {
                _selectedShelfId.value = null
            }
        }
    }

    fun assignDocumentToShelf(documentUri: String, shelfId: Long) {
        viewModelScope.launch {
            repository.assignDocumentToShelf(documentUri, shelfId)
        }
    }

    fun unassignDocument(documentUri: String) {
        viewModelScope.launch {
            repository.unassignDocument(documentUri)
        }
    }

    class Factory(
        private val repository: DocumentRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, settingsRepository) as T
        }
    }
}
