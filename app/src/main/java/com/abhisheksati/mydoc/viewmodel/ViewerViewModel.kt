package com.abhisheksati.mydoc.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.util.UserMessages
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import com.abhisheksati.mydoc.rendering.engine.DocumentRenderer
import com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.rendering.engine.SearchMatch
import com.abhisheksati.mydoc.rendering.engine.TextSelectionResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val isFitPage: Boolean = false,
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
    val errorMessage: String? = null,
    /** Why loading stopped, when we know. Drives the specific message instead of one generic one. */
    val failure: com.abhisheksati.mydoc.domain.model.LoadFailure? = null,
    val needsReflowFallback: Boolean = false
)

class ViewerViewModel(
    // Application, not Context: this outlives the Activity that creates it.
    private val context: Application,
    private val repository: DocumentRepository,
    private val documentFile: DocumentFile,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val sessionTempFiles = java.util.concurrent.CopyOnWriteArrayList<java.io.File>()

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var renderer: DocumentRenderer? = null
    private var searchJob: Job? = null
    private var activeDocumentFile: DocumentFile = documentFile

    val documentUri: String
        get() = activeDocumentFile.uri

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
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isPermissionExpired = false,
                    errorMessage = null,
                    failure = null,
                    needsReflowFallback = false
                )
            }

            // Restore per-file view state from Room
            val savedState = repository.getViewState(activeDocumentFile.uri)

            withContext(ioDispatcher) {
                try {
                    val uri = Uri.parse(activeDocumentFile.uri)
                    val ext = activeDocumentFile.extension.lowercase().ifEmpty { "pdf" }

                    // Which converter, if any, is decided by the format registry rather than a
                    // local list. Formats with no converter (legacy .doc/.xls, CSV, ODS, binary
                    // decks) must never be handed to a converter that cannot read them, and a
                    // single-engine format whose converter failed must not fall through to MuPDF,
                    // which would report a healthy file as damaged.
                    val converter = com.abhisheksati.mydoc.domain.model.DocumentFormats.converterFor(ext)
                    val convertedPdfFile: java.io.File? = when (converter) {
                        com.abhisheksati.mydoc.domain.model.ConverterKind.PPTX_TO_PDF ->
                            com.abhisheksati.mydoc.rendering.engine.presentation.PptxConverter.convertPptxToPdf(
                                context = context,
                                uri = uri,
                                displayName = activeDocumentFile.displayName
                            )

                        com.abhisheksati.mydoc.domain.model.ConverterKind.DOCX_TO_PDF ->
                            com.abhisheksati.mydoc.rendering.engine.word.DocxConverter.convertDocxToPdf(
                                context = context,
                                uri = uri,
                                displayName = activeDocumentFile.displayName
                            )

                        com.abhisheksati.mydoc.domain.model.ConverterKind.XLSX_TO_PDF ->
                            com.abhisheksati.mydoc.rendering.engine.spreadsheet.XlsxConverter.convertXlsxToPdf(
                                context = context,
                                uri = uri,
                                displayName = activeDocumentFile.displayName
                            )

                        com.abhisheksati.mydoc.domain.model.ConverterKind.NONE -> null
                    }

                    if (convertedPdfFile == null && converter != com.abhisheksati.mydoc.domain.model.ConverterKind.NONE) {
                        android.util.Log.w("ViewerLoad", "no PDF produced for ${activeDocumentFile.displayName}")
                        val family = com.abhisheksati.mydoc.domain.model.DocumentFormats.familyOf(ext)
                        val isWord = family == com.abhisheksati.mydoc.domain.model.DocumentFamily.WORD
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                needsReflowFallback = isWord,
                                failure = com.abhisheksati.mydoc.domain.model.LoadFailure.CONVERSION_FAILED,
                                errorMessage = if (isWord) {
                                    "MyDoc read the file but could not lay out its pages. " +
                                        "You can read it as reflowed text instead."
                                } else {
                                    "MyDoc read the file but could not lay out its slides. " +
                                        "It likely uses features our converter does not implement."
                                }
                            )
                        }
                        return@withContext
                    }

                    val cacheFile: java.io.File = convertedPdfFile ?: run {
                        val temp = java.io.File(context.cacheDir, "temp_view_${System.currentTimeMillis()}.$ext")
                        val resolveResult = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.resolveToCacheFile(
                            context = context,
                            uri = uri,
                            displayName = activeDocumentFile.displayName,
                            targetCacheFile = temp
                        )
                        sessionTempFiles.add(temp)

                        if (resolveResult.isFailure) {
                            val exception = resolveResult.exceptionOrNull()
                            val typed = exception as? com.abhisheksati.mydoc.domain.model.DocumentLoadException
                            val declaredFailure = typed?.failure
                            val isZeroByte = declaredFailure == com.abhisheksati.mydoc.domain.model.LoadFailure.EMPTY_DOCUMENT ||
                                             exception?.message?.contains("0 bytes", ignoreCase = true) == true
                            val isPerm = declaredFailure == com.abhisheksati.mydoc.domain.model.LoadFailure.PERMISSION_EXPIRED ||
                                         UserMessages.isPermissionFailure(exception)

                            val errorMsg = when {
                                isZeroByte -> "This document is empty (0 bytes) or corrupted and cannot be opened."
                                isPerm -> "MyDoc cannot open this file right now. It may have been moved, or permission to reach it has lapsed."
                                else -> "Unable to load document. The file may be damaged or unavailable."
                            }

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isPermissionExpired = isPerm,
                                    errorMessage = errorMsg,
                                    failure = declaredFailure ?: when {
                                        isPerm -> com.abhisheksati.mydoc.domain.model.LoadFailure.PERMISSION_EXPIRED
                                        isZeroByte -> com.abhisheksati.mydoc.domain.model.LoadFailure.EMPTY_DOCUMENT
                                        else -> com.abhisheksati.mydoc.domain.model.LoadFailure.CORRUPT_DOCUMENT
                                    }
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

                    // 1. Primary Engine: MuPDF Fitz Native Vector Engine
                    // Unlocks sub-pixel vector rendering, structured text extraction, fast quad search, and TOC outlines
                    try {
                        val mupdf = MuPdfRenderer(
                            filePath = cacheFile.absolutePath,
                            mimeType = "application/pdf"
                        )
                        if (mupdf.isPasswordProtected) {
                            activeRenderer = mupdf
                        } else if (mupdf.pageCount > 0) {
                            val size = mupdf.getPageSize(0)
                            if (size.first > 0 && size.second > 0) {
                                activeRenderer = mupdf
                            } else {
                                mupdf.close()
                            }
                        } else {
                            mupdf.close()
                        }
                    } catch (_: Throwable) {
                    }

                    // 2. Resilient Fallback Engine: Android OS Native PdfRenderer with PDFBox text stripper & outline
                    if (activeRenderer == null) {
                        try {
                            val nativeRenderer = com.abhisheksati.mydoc.rendering.engine.AndroidPdfRenderer(
                                file = cacheFile,
                                context = context
                            )
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
                                isPermissionExpired = false,
                                errorMessage = "This document format is damaged or could not be rendered."
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

                        // PageItemView asks for each page's size while composing. Priming the
                        // first screenful here turns those into cache hits instead of native
                        // page loads on the main thread.
                        for (p in initialPage until minOf(initialPage + 6, totalPages)) {
                            activeRenderer.getPageSize(p)
                        }

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
                    val isPerm = UserMessages.isPermissionFailure(e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPermissionExpired = isPerm,
                            errorMessage = UserMessages.readFailure("ViewerViewModel", e)
                        )
                    }
                }
            }
        }
    }

    fun authenticatePassword(password: String) {
        val rend = renderer ?: return
        viewModelScope.launch(ioDispatcher) {
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
                repository.updatePageProgress(activeDocumentFile.uri, 0, totalPages)
            } else {
                _uiState.update { it.copy(isPasswordIncorrect = true) }
            }
        }
    }
    fun getCachedPageBitmap(pageIndex: Int, scale: Float): Bitmap? {
        val rend = renderer ?: return null
        return rend.getCachedPage(pageIndex, scale, _uiState.value.readingMode)
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
                repository.updatePageProgress(activeDocumentFile.uri, page, _uiState.value.pageCount)
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

    fun setFitPage(fitPage: Boolean) {
        _uiState.update { it.copy(isFitPage = fitPage) }
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
        searchJob?.cancel()
        searchJob = null
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
        searchJob?.cancel()
        if (query.length < 2) {
            searchJob = null
            _uiState.update { it.copy(searchMatches = emptyList(), currentMatchIndex = -1) }
            return
        }
        // A full-document MuPDF text search is expensive enough that starting one per
        // keystroke keeps the workers busy with queries the user has already abandoned.
        searchJob = viewModelScope.launch(ioDispatcher) {
            delay(250)
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
                    uri = activeDocumentFile.uri,
                    lastPage = _uiState.value.currentPage,
                    zoomScale = _uiState.value.zoomScale,
                    readingMode = _uiState.value.readingMode,
                    isContinuousScroll = _uiState.value.isContinuousScroll
                )
            )
        }
    }

    suspend fun selectTextAt(pageIndex: Int, point: android.graphics.PointF): TextSelectionResult? = withContext(ioDispatcher) {
        renderer?.selectTextAt(pageIndex, point)
    }

    suspend fun selectTextBetween(pageIndex: Int, start: android.graphics.PointF, end: android.graphics.PointF): TextSelectionResult? = withContext(ioDispatcher) {
        renderer?.selectTextBetween(pageIndex, start, end)
    }

    /** Null means the text layer could not be read; empty means the page has no text. */
    suspend fun getPageText(pageIndex: Int): String? = withContext(ioDispatcher) {
        renderer?.getPageText(pageIndex)
    }

    suspend fun getDocumentText(): String? = withContext(ioDispatcher) {
        renderer?.getDocumentText()
    }

    override fun onCleared() {
        super.onCleared()
        prefetchJob?.cancel()
        prefetchJob = null
        renderer?.close()
        renderer = null
        // The zoom tile cache is a process-wide singleton keyed by document URI, so its
        // bitmaps would otherwise outlive the document they came from.
        com.abhisheksati.mydoc.ui.viewer.controls.ZoomTileCache.clear()
        for (f in sessionTempFiles) {
            try { if (f.exists()) f.delete() } catch (_: Throwable) {}
        }
        sessionTempFiles.clear()
    }

    class Factory(
        private val context: Context,
        private val repository: DocumentRepository,
        private val documentFile: DocumentFile
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ViewerViewModel::class.java)) {
                return ViewerViewModel(context.applicationContext as Application, repository, documentFile) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ")
        }
    }
}
