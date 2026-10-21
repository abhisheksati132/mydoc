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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class ViewerUiState(
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val zoomScale: Float = 1.0f,
    val readingMode: ReadingMode = ReadingMode.NORMAL,
    val isContinuousScroll: Boolean = true,
    val isDualPageView: Boolean = false,
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
    val isPermissionExpired: Boolean = false,
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
    private var activeDocumentFile: DocumentFile = documentFile

    init {
        loadDocument()
    }

    fun updateDocumentUri(newUri: Uri, onUpdated: ((DocumentFile) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.replaceDocument(activeDocumentFile.uri, newUri)
            res.onSuccess { newDoc ->
                activeDocumentFile = newDoc
                onUpdated?.invoke(newDoc)
                loadDocument()
            }.onFailure {
                val regRes = repository.registerOpenedDocument(newUri)
                regRes.onSuccess { newDoc ->
                    activeDocumentFile = newDoc
                    onUpdated?.invoke(newDoc)
                    loadDocument()
                }
            }
        }
    }

    private fun loadDocument() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isPermissionExpired = false, errorMessage = null) }

            // Restore per-file view state from Room
            val savedState = repository.getViewState(activeDocumentFile.uri)

            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(activeDocumentFile.uri)
                    val ext = activeDocumentFile.extension.lowercase().ifEmpty { "pdf" }

                    val convertedPdfFile: java.io.File? = when (ext) {
                        "pptx" -> com.abhisheksati.mydoc.rendering.engine.presentation.PptxConverter.convertPptxToPdf(
                            context = context,
                            uri = uri,
                            displayName = activeDocumentFile.displayName
                        )
                        "docx" -> com.abhisheksati.mydoc.rendering.engine.word.DocxConverter.convertDocxToPdf(
                            context = context,
                            uri = uri,
                            displayName = activeDocumentFile.displayName
                        )
                        "xlsx", "xls" -> com.abhisheksati.mydoc.rendering.engine.spreadsheet.XlsxConverter.convertXlsxToPdf(
                            context = context,
                            uri = uri,
                            displayName = activeDocumentFile.displayName
                        )
                        else -> null
                    }

                    val cacheFile: java.io.File = convertedPdfFile ?: run {
                        val temp = java.io.File(context.cacheDir, "temp_view_${System.currentTimeMillis()}.$ext")
                        val resolveResult = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.resolveToCacheFile(
                            context = context,
                            uri = uri,
                            displayName = activeDocumentFile.displayName,
                            targetCacheFile = temp
                        )

                        if (resolveResult.isFailure) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isPermissionExpired = true,
                                    errorMessage = "Access permission has expired or file could not be found. Tap below to select it."
                                )
                            }
                            return@withContext
                        }

                        val resolved = resolveResult.getOrThrow()
                        if (resolved.newResolvedUri != null && resolved.newResolvedUri.toString() != activeDocumentFile.uri) {
                            // Background update the URI in Room so subsequent launches resolve immediately
                            launch {
                                repository.replaceDocument(activeDocumentFile.uri, resolved.newResolvedUri)
                            }
                        }
                        temp
                    }

                    var activeRenderer: DocumentRenderer? = null
                    try {
                        val mupdf = MuPdfRenderer(
                            filePath = cacheFile.absolutePath,
                            mimeType = "application/pdf"
                        )
                        if (mupdf.isPasswordProtected) {
                            activeRenderer = mupdf
                        } else if (mupdf.pageCount > 0) {
                            // Smoke test rendering first page to verify native Fitz device works
                            val testBitmap = mupdf.renderPage(0, 0.2f, ReadingMode.NORMAL)
                            if (testBitmap != null) {
                                activeRenderer = mupdf
                            } else {
                                mupdf.close()
                            }
                        } else {
                            mupdf.close()
                        }
                    } catch (_: Throwable) {
                    }

                    // 2. Fallback attempt: Built-in Android OS PdfRenderer
                    if (activeRenderer == null) {
                        try {
                            val nativeRenderer = com.abhisheksati.mydoc.rendering.engine.AndroidPdfRenderer(cacheFile)
                            if (nativeRenderer.pageCount > 0) {
                                activeRenderer = nativeRenderer
                            } else {
                                nativeRenderer.close()
                            }
                        } catch (_: Throwable) {
                        }
                    }

                    if (activeRenderer == null || activeRenderer.pageCount == 0) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPermissionExpired = true,
                                errorMessage = "Unable to display document pages. Please select the file again."
                            )
                        }
                        return@withContext
                    }

                    renderer?.close()
                    renderer = activeRenderer

                    if (activeRenderer.isPasswordProtected) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPasswordRequired = true
                            )
                        }
                    } else {
                        val totalPages = activeRenderer.pageCount
                        val rawOutline = activeRenderer.getOutline()
                        val outline = if (rawOutline.isNotEmpty()) {
                            rawOutline
                        } else {
                            val xlsx = com.abhisheksati.mydoc.rendering.engine.spreadsheet.XlsxConverter.getSheetOutlines(cacheFile.absolutePath)
                            if (xlsx.isNotEmpty()) {
                                xlsx
                            } else {
                                com.abhisheksati.mydoc.rendering.engine.presentation.PptxConverter.getSlideOutlines(cacheFile.absolutePath)
                            }
                        }
                        val initialPage = savedState?.lastPage?.coerceIn(0, maxOf(0, totalPages - 1)) ?: 0
                        val defaultMode = (context.applicationContext as? com.abhisheksati.mydoc.MyDocApp)?.settingsRepository?.defaultReadingMode?.value ?: ReadingMode.NORMAL
                        val initialMode = savedState?.readingMode ?: defaultMode
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

                        repository.updatePageProgress(activeDocumentFile.uri, initialPage, totalPages)
                        prefetchSurroundingPages(initialPage)
                    }
                } catch (e: Exception) {
                    val isPerm = e is SecurityException || (e.localizedMessage?.contains("Permission Denial", ignoreCase = true) == true)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPermissionExpired = isPerm,
                            errorMessage = if (isPerm) "Storage permission has expired for this file." else (e.localizedMessage ?: "Failed to open document")
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

    fun renderRegionBitmap(
        pageIndex: Int,
        region: android.graphics.RectF,
        pageWidthPx: Float,
        outWidth: Int,
        outHeight: Int
    ): Bitmap? {
        val rend = renderer ?: return null
        return rend.renderRegion(
            pageIndex = pageIndex,
            region = region,
            pageWidthPx = pageWidthPx,
            outWidth = outWidth,
            outHeight = outHeight,
            mode = _uiState.value.readingMode
        )
    }

    fun getPageSize(pageIndex: Int): Pair<Float, Float> {
        val rend = renderer ?: return Pair(595f, 842f)
        return rend.getPageSize(pageIndex)
    }

    private var prefetchJob: Job? = null

    private fun prefetchSurroundingPages(currentPage: Int) {
        val rend = renderer ?: return
        val total = _uiState.value.pageCount
        if (total <= 1) return

        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch(Dispatchers.Default) {
            val pagesToPrefetch = listOf(currentPage + 1, currentPage - 1, currentPage + 2)
                .filter { it in 0 until total }
            val currentMode = _uiState.value.readingMode
            for (p in pagesToPrefetch) {
                // Prime both the low-resolution thumbnail and standard resolution tile in LRU cache
                rend.renderPage(p, 0.35f, currentMode)
                rend.renderPage(p, 1.8f, currentMode)
            }
        }
    }

    fun onPageChanged(page: Int) {
        if (page != _uiState.value.currentPage && page in 0 until _uiState.value.pageCount) {
            _uiState.update { it.copy(currentPage = page) }
            persistViewState()
            prefetchSurroundingPages(page)
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
            ReadingMode.SEPIA -> ReadingMode.EYE_COMFORT
            ReadingMode.EYE_COMFORT -> ReadingMode.NORMAL
        }
        _uiState.update { it.copy(readingMode = nextMode) }
        persistViewState()
    }

    fun setReadingMode(mode: ReadingMode) {
        _uiState.update { it.copy(readingMode = mode) }
        persistViewState()
    }

    fun toggleScrollMode() {
        _uiState.update { it.copy(isContinuousScroll = !it.isContinuousScroll) }
        persistViewState()
    }

    fun setContinuousScroll(continuous: Boolean) {
        _uiState.update { it.copy(isContinuousScroll = continuous) }
        persistViewState()
    }

    fun setDualPageView(dual: Boolean) {
        _uiState.update { it.copy(isDualPageView = dual) }
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

    suspend fun getPageText(pageIndex: Int): String = withContext(Dispatchers.IO) {
        renderer?.getPageText(pageIndex).orEmpty()
    }

    suspend fun getDocumentText(): String = withContext(Dispatchers.IO) {
        renderer?.getDocumentText().orEmpty()
    }

    override fun onCleared() {
        super.onCleared()
        prefetchJob?.cancel()
        prefetchJob = null
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
