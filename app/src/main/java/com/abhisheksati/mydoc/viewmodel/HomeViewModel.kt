package com.abhisheksati.mydoc.viewmodel

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import com.abhisheksati.mydoc.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class HomeUiState(
    val searchQuery: String = "",
    val documents: List<DocumentFile> = emptyList(),
    val recents: List<DocumentFile> = emptyList(),
    val continueItems: List<LibraryItem> = emptyList(),
    val libraryItems: List<LibraryItem> = emptyList(),
    val recentLibraryItems: List<LibraryItem> = emptyList(),
    val recentlyAdded: List<LibraryItem> = emptyList(),
    val countsByKind: Map<MediaKind, Pair<Int, Long>> = emptyMap(),
    val sortOrder: DocumentSortOrder = DocumentSortOrder.LAST_OPENED,
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

private data class DocumentFilters(
    val query: String,
    val sortOrder: DocumentSortOrder,
    val errorMessage: String? = null
)

/** Selection survives only for rows still visible in recents; the hero card is not a recent row. */
internal fun pruneRecentSelection(selected: Set<String>, recents: List<LibraryItem>, heroUri: String?): Set<String> {
    val visible = recents.mapTo(HashSet()) { it.uri }
    return selected.filterTo(HashSet()) { it != heroUri && it in visible }
}

class HomeViewModel(
    private val repository: DocumentRepository,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(settingsRepository.defaultStartupTab.value)
    val currentTab: StateFlow<HomeTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _errorFlow = MutableStateFlow<String?>(null)

    // Recents selection lives here, not in the feed composable: switching tabs disposes the
    // Home pane, and a selection that evaporates on return makes bulk actions unusable.
    private val _selectedRecentUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedRecentUris: StateFlow<Set<String>> = _selectedRecentUris.asStateFlow()

    private val _documentsSource = repository.getAllDeviceDocuments()

    private val _filterFlow = combine(
        _searchQuery,
        settingsRepository.sortOrder,
        _errorFlow
    ) { query, sortOrder, errorMessage ->
        DocumentFilters(query, sortOrder, errorMessage)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getRecents(),
        _documentsSource,
        _filterFlow,
        libraryRepository.getAllItems(),
        libraryRepository.continueItems(10)
    ) { recents, allDocs, filters, allLibraryItems, continueItems ->
        val (query, sortOrder, errorMessage) = filters

        val filteredDocs = allDocs.filter { doc ->
            if (query.isBlank()) true
            else doc.displayName.contains(query.trim(), ignoreCase = true)
        }

        val sortedDocs = when (sortOrder) {
            DocumentSortOrder.LAST_OPENED -> filteredDocs.sortedByDescending { it.lastOpenedTimestamp }
            DocumentSortOrder.NAME_ASC -> filteredDocs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
            DocumentSortOrder.NAME_DESC -> filteredDocs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.displayName })
            DocumentSortOrder.SIZE_DESC -> filteredDocs.sortedByDescending { it.sizeBytes }
            DocumentSortOrder.SIZE_ASC -> filteredDocs.sortedBy { it.sizeBytes }
        }

        val filteredLibItems = if (query.isBlank()) {
            allLibraryItems
        } else {
            allLibraryItems.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }


        val recentDocs = recents.filter { it.lastOpenedTimestamp > 0 }.map { doc ->
            LibraryItem(
                uri = doc.uri,
                name = doc.displayName,
                kind = MediaKind.DOCUMENT,
                mimeType = doc.mimeType,
                sizeBytes = doc.sizeBytes,
                lastModified = doc.lastOpenedTimestamp,
                isFavorite = doc.isFavorite,
                lastPage = doc.lastPage,
                pageCount = doc.pageCount,
                progressFraction = if (doc.pageCount > 0) (doc.lastPage.toFloat() / doc.pageCount).coerceIn(0f, 1f) else 0f
            )
        }

        val dynamicRecents = (recentDocs + continueItems)
            .distinctBy { it.uri }
            .sortedByDescending { it.lastModified }
            .take(20)

        HomeUiState(
            searchQuery = query,
            documents = sortedDocs,
            recents = recents,
            continueItems = continueItems,
            libraryItems = filteredLibItems,
            recentLibraryItems = dynamicRecents,
            recentlyAdded = allLibraryItems.take(20),
            countsByKind = emptyMap(),
            sortOrder = sortOrder,
            isSearching = query.isNotBlank(),
            isLoading = false,
            errorMessage = errorMessage
        )
    }.onEach { state ->
        // A selected row that became the hero or left recents is no longer a row; keeping its URI
        // produced a "0 selected" bar whose Share/Remove silently acted on fewer items.
        val heroUri = state.continueItems.firstOrNull()?.uri
        _selectedRecentUris.update { pruneRecentSelection(it, state.recentLibraryItems, heroUri) }
    }.flowOn(Dispatchers.Default)
     .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun clearError() {
        _errorFlow.value = null
    }

    fun setError(message: String?) {
        _errorFlow.value = message
    }

    fun selectTab(tab: HomeTab) {
        _currentTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: DocumentSortOrder) {
        settingsRepository.setSortOrder(order)
    }

    fun removeFromRecents(document: DocumentFile) {
        viewModelScope.launch {
            repository.removeFromRecents(document.uri)
            libraryRepository.removeMediaPlayback(document.uri)
            _selectedRecentUris.update { it - document.uri }
        }
    }

    fun removeFromRecents(documents: List<DocumentFile>) {
        if (documents.isEmpty()) return
        viewModelScope.launch {
            documents.forEach {
                repository.removeFromRecents(it.uri)
                libraryRepository.removeMediaPlayback(it.uri)
            }
            clearRecentSelection()
        }
    }

    fun removeFromRecents(item: LibraryItem) {
        viewModelScope.launch {
            if (item.kind == MediaKind.DOCUMENT) {
                repository.removeFromRecents(item.uri)
            } else {
                libraryRepository.removeMediaPlayback(item.uri)
            }
            _selectedRecentUris.update { it - item.uri }
        }
    }

    fun removeLibraryItemsFromRecents(items: List<LibraryItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            items.forEach { item ->
                if (item.kind == MediaKind.DOCUMENT) {
                    repository.removeFromRecents(item.uri)
                } else {
                    libraryRepository.removeMediaPlayback(item.uri)
                }
            }
            clearRecentSelection()
        }
    }

    fun toggleRecentSelection(uri: String) {
        _selectedRecentUris.update { if (uri in it) it - uri else it + uri }
    }

    fun selectAllRecents(uris: List<String>) {
        _selectedRecentUris.value = uris.toSet()
    }

    fun clearRecentSelection() {
        _selectedRecentUris.value = emptySet()
    }

    fun clearAllRecents() {
        clearRecentSelection()
        viewModelScope.launch {
            libraryRepository.clearAllRecents()
        }
    }

    fun onDocumentOpened(uri: Uri, onResult: (Result<DocumentFile>) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerOpenedDocument(uri)
            onResult(result)
        }
    }

    class Factory(
        private val repository: DocumentRepository,
        private val settingsRepository: SettingsRepository,
        private val libraryRepository: LibraryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository, settingsRepository, libraryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ")
        }
    }
}
