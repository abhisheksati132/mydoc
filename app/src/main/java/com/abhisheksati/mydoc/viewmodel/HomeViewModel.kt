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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val isLoading: Boolean = false
)

private data class DocumentFilters(
    val query: String,
    val sortOrder: DocumentSortOrder
)

class HomeViewModel(
    private val repository: DocumentRepository,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(HomeTab.HOME)
    val currentTab: StateFlow<HomeTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val _documentsSource = repository.getAllDeviceDocuments()

    private val _filterFlow = combine(
        _searchQuery,
        settingsRepository.sortOrder
    ) { query, sortOrder ->
        DocumentFilters(query, sortOrder)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getRecents(),
        _documentsSource,
        _filterFlow,
        libraryRepository.getAllItems(),
        libraryRepository.continueItems(10)
    ) { recents, allDocs, filters, allLibraryItems, continueItems ->
        val (query, sortOrder) = filters

        val filteredDocs = allDocs.filter { doc ->
            if (query.isBlank()) true
            else doc.displayName.contains(query.trim(), ignoreCase = true)
        }

        val sortedDocs = when (sortOrder) {
            DocumentSortOrder.LAST_OPENED -> filteredDocs.sortedByDescending { it.lastOpenedTimestamp }
            DocumentSortOrder.NAME_ASC -> filteredDocs.sortedBy { it.displayName.lowercase() }
            DocumentSortOrder.NAME_DESC -> filteredDocs.sortedByDescending { it.displayName.lowercase() }
            DocumentSortOrder.SIZE_DESC -> filteredDocs.sortedByDescending { it.sizeBytes }
            DocumentSortOrder.SIZE_ASC -> filteredDocs.sortedBy { it.sizeBytes }
        }

        val filteredLibItems = if (query.isBlank()) {
            allLibraryItems
        } else {
            allLibraryItems.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }

        val counts = MediaKind.entries.associateWith { kind ->
            val itemsForKind = allLibraryItems.filter { it.kind == kind }
            Pair(itemsForKind.size, itemsForKind.sumOf { it.sizeBytes })
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

        val continueUris = continueItems.take(4).map { it.uri }.toSet()
        val dynamicRecents = (recentDocs + continueItems)
            .distinctBy { it.uri }
            .filterNot { it.uri in continueUris }
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
            countsByKind = counts,
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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: DocumentSortOrder) {
        settingsRepository.setSortOrder(order)
    }

    fun removeFromRecents(document: DocumentFile) {
        viewModelScope.launch {
            repository.removeFromRecents(document.uri)
        }
    }

    fun clearAllRecents() {
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
            return HomeViewModel(repository, settingsRepository, libraryRepository) as T
        }
    }
}
