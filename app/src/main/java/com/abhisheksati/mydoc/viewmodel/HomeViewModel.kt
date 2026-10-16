package com.abhisheksati.mydoc.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val selectedCategory: DocumentCategory = DocumentCategory.ALL,
    val searchQuery: String = "",
    val documents: List<DocumentFile> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(DocumentCategory.ALL)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getRecents(),
        repository.getFavorites(),
        _selectedCategory,
        _searchQuery
    ) { recents, favorites, category, query ->
        val sourceList = when (category) {
            DocumentCategory.FAVORITES -> favorites
            DocumentCategory.PDF -> recents.filter { it.category == DocumentCategory.PDF }
            DocumentCategory.WORD -> recents.filter { it.category == DocumentCategory.WORD }
            DocumentCategory.EXCEL -> recents.filter { it.category == DocumentCategory.EXCEL }
            DocumentCategory.PPT -> recents.filter { it.category == DocumentCategory.PPT }
            DocumentCategory.ALL -> recents
        }

        val filtered = if (query.isBlank()) {
            sourceList
        } else {
            val trimmed = query.trim()
            sourceList.filter { it.displayName.contains(trimmed, ignoreCase = true) }
        }

        HomeUiState(
            selectedCategory = category,
            searchQuery = query,
            documents = filtered,
            isSearching = query.isNotBlank(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun selectCategory(category: DocumentCategory) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
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

    fun onDocumentOpened(uri: Uri, onResult: (Result<DocumentFile>) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerOpenedDocument(uri)
            onResult(result)
        }
    }

    class Factory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
