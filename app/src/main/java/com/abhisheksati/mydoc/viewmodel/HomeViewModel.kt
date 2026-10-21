package com.abhisheksati.mydoc.viewmodel

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.HomeTab
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(HomeTab.DOCUMENTS)
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
        _filterFlow
    ) { recents, allDocs, filters ->
        val (query, sortOrder) = filters

        val filtered = allDocs.filter { doc ->
            if (query.isBlank()) true
            else doc.displayName.contains(query.trim(), ignoreCase = true)
        }

        val sorted = when (sortOrder) {
            DocumentSortOrder.LAST_OPENED -> filtered.sortedByDescending { it.lastOpenedTimestamp }
            DocumentSortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            DocumentSortOrder.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
            DocumentSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            DocumentSortOrder.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
        }

        HomeUiState(
            searchQuery = query,
            documents = sorted,
            recents = recents,
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
            repository.clearAllRecents()
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
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, settingsRepository) as T
        }
    }
}
