package com.abhisheksati.mydoc.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import com.abhisheksati.mydoc.rendering.engine.DocumentRenderer
import com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.rendering.engine.SearchMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ViewerUiState(
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val zoomScale: Float = 1.0f,
    val readingMode: ReadingMode = ReadingMode.NORMAL,
    val isContinuousScroll: Boolean = true,
    val areControlsVisible: Boolean = true,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchMatches: List<SearchMatch> = emptyList(),
    val currentMatchIndex: Int = -1,
    val outline: List<OutlineItem> = emptyList(),
    val isOutlineDrawerOpen: Boolean = false,
    val isThumbnailStripVisible: Boolean = false,
    val isPasswordRequired: Boolean = false,
    val isPasswordIncorrect: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ViewerViewModel(
    private val context: Context,
    private val repository: DocumentRepository,
    private val documentFile: DocumentFile
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var renderer: DocumentRenderer? = null

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Restore per-file view state from Room
            val savedState = repository.getViewState(documentFile.uri)

            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(documentFile.uri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Could not open input stream for URI: $uri")

                    val bytes = inputStream.use { it.readBytes() }

                    val mupdf = MuPdfRenderer(
                        documentBytes = bytes,
                        mimeType = documentFile.mimeType
                    )
                    renderer = mupdf

                    if (mupdf.isPasswordProtected) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPasswordRequired = true
                            )
                        }
                    } else {
                        val totalPages = mupdf.pageCount
                        val outline = mupdf.getOutline()
                        val initialPage = savedState?.lastPage?.coerceIn(0, maxOf(0, totalPages - 1)) ?: 0
                        val initialMode = savedState?.readingMode ?: ReadingMode.NORMAL
                        val initialContinuous = savedState?.isContinuousScroll ?: true

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pageCount = totalPages,
                                currentPage = initialPage,
                                readingMode = initialMode,
                                isContinuousScroll = initialContinuous,
                                outline = outline
                            )
                        }

                        repository.updatePageProgress(documentFile.uri, initialPage, totalPages)
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "Failed to open document"
                        )
                    }
                }
            }
        }
    }

    fun authenticatePassword(password: String) {
        val rend = renderer ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val success = rend.authenticatePassword(password)
            if (success) {
                val totalPages = rend.pageCount
                val outline = rend.getOutline()
                _uiState.update {
                    it.copy(
                        isPasswordRequired = false,
                        isPasswordIncorrect = false,
                        pageCount = totalPages,
                        outline = outline
                    )
                }
                repository.updatePageProgress(documentFile.uri, 0, totalPages)
            } else {
                _uiState.update { it.copy(isPasswordIncorrect = true) }
            }
        }
    }

    fun renderPageBitmap(pageIndex: Int, scale: Float): Bitmap? {
        val rend = renderer ?: return null
        return rend.renderPage(pageIndex, scale, _uiState.value.readingMode)
    }

    fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        val rend = renderer ?: return Pair(595f, 842f)
        return rend.getPageSize(pageIndex)
    }

    fun onPageChanged(page: Int) {
        if (page != _uiState.value.currentPage && page in 0 until _uiState.value.pageCount) {
            _uiState.update { it.copy(currentPage = page) }
            persistViewState()
            viewModelScope.launch {
                repository.updatePageProgress(documentFile.uri, page, _uiState.value.pageCount)
            }
        }
    }

    fun setZoomScale(scale: Float) {
        _uiState.update { it.copy(zoomScale = scale) }
    }

    fun toggleReadingMode() {
        val nextMode = when (_uiState.value.readingMode) {
            ReadingMode.NORMAL -> ReadingMode.INVERTED_DARK
            ReadingMode.INVERTED_DARK -> ReadingMode.SEPIA
            ReadingMode.SEPIA -> ReadingMode.NORMAL
        }
        _uiState.update { it.copy(readingMode = nextMode) }
        persistViewState()
    }

    fun toggleScrollMode() {
        _uiState.update { it.copy(isContinuousScroll = !it.isContinuousScroll) }
        persistViewState()
    }

    fun toggleControls() {
        _uiState.update { it.copy(areControlsVisible = !it.areControlsVisible) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(areControlsVisible = visible) }
    }

    fun toggleThumbnailStrip() {
        _uiState.update { it.copy(isThumbnailStripVisible = !it.isThumbnailStripVisible) }
    }

    fun setOutlineDrawerOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isOutlineDrawerOpen = isOpen) }
    }

    fun openSearch() {
        _uiState.update { it.copy(isSearchOpen = true) }
    }

    fun closeSearch() {
        _uiState.update {
            it.copy(
                isSearchOpen = false,
                searchQuery = "",
                searchMatches = emptyList(),
                currentMatchIndex = -1
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length >= 2) {
            viewModelScope.launch(Dispatchers.IO) {
                val matches = renderer?.search(query) ?: emptyList()
                _uiState.update {
                    it.copy(
                        searchMatches = matches,
                        currentMatchIndex = if (matches.isNotEmpty()) 0 else -1
                    )
                }
                if (matches.isNotEmpty()) {
                    jumpToPage(matches[0].pageIndex)
                }
            }
        } else {
            _uiState.update { it.copy(searchMatches = emptyList(), currentMatchIndex = -1) }
        }
    }

    fun nextSearchMatch() {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return
        val nextIdx = (_uiState.value.currentMatchIndex + 1) % matches.size
        _uiState.update { it.copy(currentMatchIndex = nextIdx) }
        jumpToPage(matches[nextIdx].pageIndex)
    }

    fun previousSearchMatch() {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return
        val prevIdx = if (_uiState.value.currentMatchIndex - 1 < 0) matches.size - 1 else _uiState.value.currentMatchIndex - 1
        _uiState.update { it.copy(currentMatchIndex = prevIdx) }
        jumpToPage(matches[prevIdx].pageIndex)
    }

    fun jumpToPage(page: Int) {
        val target = page.coerceIn(0, maxOf(0, _uiState.value.pageCount - 1))
        onPageChanged(target)
    }

    private fun persistViewState() {
        viewModelScope.launch {
            repository.saveViewState(
                ViewState(
                    uri = documentFile.uri,
                    lastPage = _uiState.value.currentPage,
                    zoomScale = _uiState.value.zoomScale,
                    readingMode = _uiState.value.readingMode,
                    isContinuousScroll = _uiState.value.isContinuousScroll
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        renderer?.close()
        renderer = null
    }

    class Factory(
        private val context: Context,
        private val repository: DocumentRepository,
        private val documentFile: DocumentFile
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ViewerViewModel(context, repository, documentFile) as T
        }
    }
}
