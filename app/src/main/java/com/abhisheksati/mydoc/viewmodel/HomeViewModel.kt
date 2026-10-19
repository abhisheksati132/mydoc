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
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HomeUiState(
    val selectedCategory: DocumentCategory = DocumentCategory.ALL,
    val searchQuery: String = "",
    val documents: List<DocumentFile> = emptyList(),
    val continueReadingDoc: DocumentFile? = null,
    val recents: List<DocumentFile> = emptyList(),
    val favorites: List<DocumentFile> = emptyList(),
    val sortOrder: DocumentSortOrder = DocumentSortOrder.LAST_OPENED,
    val isSearching: Boolean = false,
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: DocumentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(settingsRepository.defaultStartupTab.value)
    val currentTab: StateFlow<HomeTab> = _currentTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DocumentCategory.ALL)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getRecents(),
        repository.getFavorites(),
        _selectedCategory,
        _searchQuery,
        settingsRepository.sortOrder
    ) { recents, favorites, category, query, sortOrder ->
        val sourceList = when (category) {
            DocumentCategory.FAVORITES -> favorites
            DocumentCategory.PDF -> recents.filter { it.category == DocumentCategory.PDF }
            DocumentCategory.WORD -> recents.filter { it.category == DocumentCategory.WORD }
            DocumentCategory.EXCEL -> recents.filter { it.category == DocumentCategory.EXCEL }
            DocumentCategory.PPT -> recents.filter { it.category == DocumentCategory.PPT }
            DocumentCategory.TEXT -> recents.filter { it.category == DocumentCategory.TEXT }
            DocumentCategory.ALL -> recents
        }

        val filtered = if (query.isBlank()) {
            sourceList
        } else {
            val trimmed = query.trim()
            sourceList.filter { it.displayName.contains(trimmed, ignoreCase = true) }
        }

        val sorted = when (sortOrder) {
            DocumentSortOrder.LAST_OPENED -> filtered.sortedByDescending { it.lastOpenedTimestamp }
            DocumentSortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            DocumentSortOrder.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
            DocumentSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            DocumentSortOrder.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
        }

        val continueDoc = recents.firstOrNull { it.lastPage > 0 && it.pageCount > 0 } ?: recents.firstOrNull()

        HomeUiState(
            selectedCategory = category,
            searchQuery = query,
            documents = sorted,
            continueReadingDoc = continueDoc,
            recents = recents,
            favorites = favorites,
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
