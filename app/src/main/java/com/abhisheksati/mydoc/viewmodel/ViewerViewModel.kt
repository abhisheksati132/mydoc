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
import com.abhisheksati.mydoc.util.SecureLog
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
import kotlinx.coroutines.isActive
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
    val isOfficeEncrypted: Boolean = false,
    val isLoading: Boolean = true,
    val isPermissionExpired: Boolean = false,
    val errorMessage: String? = null,
    /** Why loading stopped, when we know. Drives the specific message instead of one generic one. */
    val failure: com.abhisheksati.mydoc.domain.model.LoadFailure? = null,
    val needsReflowFallback: Boolean = false,
    /** Conversion gaps worth telling the reader about (e.g. slides past the layout limit). */
    val conversionNotes: List<String> = emptyList()
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

    private val rendererSlot = com.abhisheksati.mydoc.rendering.engine.RendererSlot()
    private val renderer: DocumentRenderer?
        get() = rendererSlot.current
    private var searchJob: Job? = null
    private var activeDocumentFile: DocumentFile = documentFile
    private var officePassword: String? = null
    private var waitingForOfficePassword = false

    /** Bumped by every load; a result from an older generation is dropped, not published. */
    @Volatile private var loadGeneration = 0
    private var loadJob: Job? = null

    val documentUri: String
        get() = activeDocumentFile.uri

    init {
        loadDocument()
    }

    /**
     * Shows another document in this same viewer session. The engine that was reading the previous
     * one is given back before the new one is opened, so opening a run of documents cannot leave a
     * native MuPDF document alive for every file that was ever looked at.
     */
    fun open(document: DocumentFile) {
        if (document.uri == activeDocumentFile.uri &&
            (_uiState.value.pageCount > 0 || loadJob?.isActive == true)) return
        if (document.uri != activeDocumentFile.uri) {
            officePassword = null
            waitingForOfficePassword = false
        }
        activeDocumentFile = document
        loadDocument()
    }

    private var readerLease = 0

    fun attachReader(document: DocumentFile): Int {
        val lease = ++readerLease
        open(document)
        return lease
    }

    fun detachReader(lease: Int) {
        if (readerLease != lease) return
        ++readerLease
        ++loadGeneration
        loadJob?.cancel()
        loadJob = null
        officePassword = null
        waitingForOfficePassword = false
        releaseSession()
        _uiState.update { it.copy(pageCount = 0) }
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
        val generation = ++loadGeneration
        loadJob?.cancel()
        releaseSession()
        val document = activeDocumentFile
        val password = officePassword
        // One owned load at a time. The constructor and the screen effect can both schedule a
        // load; without a generation stamp the slower document could publish its renderer, page
        // count and progress over the faster, newer one.
        loadJob = viewModelScope.launch {
            _uiState.update {
                if (generation != loadGeneration) it else it.copy(
                    isLoading = true,
                    isPermissionExpired = false,
                    errorMessage = null,
                    failure = null,
                    needsReflowFallback = false,
                    // Password prompts belong to the document that raised them.
                    isPasswordRequired = false,
                    isPasswordIncorrect = false,
                    isOfficeEncrypted = false,
                    conversionNotes = emptyList()
                )
            }

            // Restore per-file view state from Room
            val savedState = repository.getViewState(document.uri)

            withContext(ioDispatcher) {
                try {
                    val uri = Uri.parse(document.uri)
                    val ext = document.extension.lowercase().ifEmpty { "pdf" }

                    // The registry selects the offline Office engine for page-based documents.
                    // CSV/TSV remain in the grid; failed conversions never fall through to MuPDF.
                    val converter = com.abhisheksati.mydoc.domain.model.DocumentFormats.converterFor(ext)
                    // The office converters report *why* they produced nothing: a document over the
                    // size budget must not be announced as one using unimplemented features.
                    val conversion: com.abhisheksati.mydoc.rendering.engine.OfficeConversion? =
                        when (converter) {
                            com.abhisheksati.mydoc.domain.model.ConverterKind.PPTX_TO_PDF ->
                                com.abhisheksati.mydoc.rendering.engine.presentation.PptxConverter.convertPptxToPdf(
                                    context = context,
                                    uri = uri,
                                    displayName = document.displayName,
                                    password = password
                                )

                            com.abhisheksati.mydoc.domain.model.ConverterKind.DOCX_TO_PDF ->
                                com.abhisheksati.mydoc.rendering.engine.word.DocxConverter.convertDocxToPdf(
                                    context = context,
                                    uri = uri,
                                    displayName = document.displayName,
                                    password = password
                                )

                            com.abhisheksati.mydoc.domain.model.ConverterKind.XLSX_TO_PDF ->
                                com.abhisheksati.mydoc.rendering.engine.spreadsheet.XlsxConverter.convertToPages(
                                    context = context,
                                    uri = uri,
                                    displayName = document.displayName,
                                    password = password
                                )

                            com.abhisheksati.mydoc.domain.model.ConverterKind.NONE -> null
                        }
                    if (generation != loadGeneration) {
                        if (conversion is com.abhisheksati.mydoc.rendering.engine.OfficeConversion.Success && conversion.deleteWhenClosed)
                            conversion.pdf.delete()
                        return@withContext
                    }
                    val convertedPdfFile: java.io.File? = conversion?.pdfFile

                    if (conversion is com.abhisheksati.mydoc.rendering.engine.OfficeConversion.Failure) {
                        if (generation != loadGeneration) return@withContext
                        if (conversion.failure == com.abhisheksati.mydoc.domain.model.LoadFailure.PASSWORD_PROTECTED) {
                            waitingForOfficePassword = true
                            _uiState.update { if (generation != loadGeneration) it else it.copy(isLoading = false, isPasswordRequired = true,
                                isPasswordIncorrect = password != null, errorMessage = null, failure = null) }
                            return@withContext
                        }
                        com.abhisheksati.mydoc.rendering.engine.OfficeConversionDiagnostics.lastFailure = conversion.failure
                        SecureLog.warn("ViewerLoad", "document conversion failed") { conversion.message }
                        _uiState.update {
                            if (generation != loadGeneration) it else it.copy(
                                isLoading = false,
                                needsReflowFallback = !com.abhisheksati.mydoc.rendering.engine.OfficeEngineManager.isInstalled(context) && com.abhisheksati.mydoc.domain.model.DocumentFormats
                                    .familyOf(ext) == com.abhisheksati.mydoc.domain.model.DocumentFamily.WORD,
                                failure = conversion.failure,
                                errorMessage = conversion.message
                            )
                        }
                        return@withContext
                    }

                    if (conversion is com.abhisheksati.mydoc.rendering.engine.OfficeConversion.Success) {
                        if (generation != loadGeneration) {
                            if (conversion.deleteWhenClosed) conversion.pdf.delete()
                            return@withContext
                        }
                        if (conversion.deleteWhenClosed) sessionTempFiles.add(conversion.pdf)
                        waitingForOfficePassword = false
                        officePassword = null
                        _uiState.update { if (generation != loadGeneration) it else it.copy(isOfficeEncrypted = conversion.renderPassword != null) }
                    }

                    conversion?.note?.let { note ->
                        _uiState.update { if (generation != loadGeneration) it else it.copy(conversionNotes = listOf(note)) }
                    }

                    if (convertedPdfFile == null && converter != com.abhisheksati.mydoc.domain.model.ConverterKind.NONE) {
                        SecureLog.warn("ViewerLoad", "no PDF produced for document") { document.displayName }
                        val family = com.abhisheksati.mydoc.domain.model.DocumentFormats.familyOf(ext)
                        val isWord = family == com.abhisheksati.mydoc.domain.model.DocumentFamily.WORD
                        _uiState.update {
                            if (generation != loadGeneration) it else it.copy(
                                isLoading = false,
                                needsReflowFallback = isWord && !com.abhisheksati.mydoc.rendering.engine.OfficeEngineManager.isInstalled(context),
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
                            displayName = document.displayName,
                            targetCacheFile = temp
                        )
                        sessionTempFiles.add(temp)

                        if (generation != loadGeneration) { temp.delete(); return@withContext }
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
                                if (generation != loadGeneration) it else it.copy(
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
                        if (resolved.newResolvedUri != null && resolved.newResolvedUri.toString() != document.uri) {
                            // Background update the URI in Room so subsequent launches resolve immediately
                            launch {
                                repository.replaceDocument(document.uri, resolved.newResolvedUri)
                            }
                        }
                        temp
                    }

                    var activeRenderer: DocumentRenderer? = null

                    // 1. Primary Engine: MuPDF Fitz Native Vector Engine
                    // Unlocks sub-pixel vector rendering, structured text extraction, fast quad search, and TOC outlines
                    var mupdf: MuPdfRenderer? = null
                    try {
                        val opened = MuPdfRenderer(
                            filePath = cacheFile.absolutePath,
                            mimeType = "application/pdf"
                        )
                        mupdf = opened
                        val renderPassword = (conversion as? com.abhisheksati.mydoc.rendering.engine.OfficeConversion.Success)?.renderPassword
                        if (opened.isPasswordProtected && renderPassword != null) {
                            check(opened.authenticatePassword(renderPassword)) { "The protected Word preview could not be unlocked" }
                        }
                        if (opened.isPasswordProtected) {
                            activeRenderer = opened
                        } else if (opened.pageCount > 0) {
                            val size = opened.getPageSize(0)
                            if (size.first > 0 && size.second > 0) {
                                activeRenderer = opened
                            }
                        }
                    } catch (_: Throwable) {
                        // A throw after the document was opened used to leave the native handle
                        // with nothing pointing at it.
                    }
                    if (activeRenderer !== mupdf) {
                        mupdf?.close()
                        mupdf = null
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

                    // Android's PdfRenderer cannot open an encrypted document at all, so on this
                    // path a locked file looks empty rather than locked. Establish protection
                    // before trusting the page count.
                    val encryptedOnDisk = activeRenderer?.isPasswordProtected != true &&
                        isEncryptedPdf(cacheFile)
                    when (PdfOpenDecision.choose(
                            rendererOpened = activeRenderer != null,
                            passwordProtected = activeRenderer?.isPasswordProtected == true,
                            canAskForPassword = activeRenderer?.supportsPasswordAuthentication == true,
                            pageCount = activeRenderer?.pageCount ?: 0,
                            encryptedOnDisk = encryptedOnDisk
                        )) {
                        PdfOpenOutcome.DAMAGED, PdfOpenOutcome.EMPTY_DOCUMENT -> {
                            activeRenderer?.close()
                            if (generation != loadGeneration) return@withContext
                            _uiState.update {
                                if (generation != loadGeneration) it else it.copy(
                                    isLoading = false,
                                    isPermissionExpired = false,
                                    errorMessage = "This document format is damaged or could not be rendered.",
                                    failure = com.abhisheksati.mydoc.domain.model.LoadFailure.CORRUPT_DOCUMENT
                                )
                            }
                            return@withContext
                        }

                        PdfOpenOutcome.PASSWORD_UNAVAILABLE -> {
                            activeRenderer?.close()
                            if (generation != loadGeneration) return@withContext
                            _uiState.update {
                                if (generation != loadGeneration) it else it.copy(
                                    isLoading = false,
                                    isPermissionExpired = false,
                                    isPasswordRequired = false,
                                    errorMessage = "This document is password protected, and the rendering " +
                                        "engine available here cannot open protected files.",
                                    failure = com.abhisheksati.mydoc.domain.model.LoadFailure.PASSWORD_PROTECTED
                                )
                            }
                            return@withContext
                        }

                        PdfOpenOutcome.ASK_PASSWORD, PdfOpenOutcome.RENDER -> Unit
                    }

                    val chosen = activeRenderer ?: return@withContext
                    val published = withContext(Dispatchers.Main.immediate + kotlinx.coroutines.NonCancellable) {
                        if (generation != loadGeneration) false else {
                            rendererSlot.replace(chosen)
                            true
                        }
                    }
                    if (!published) { chosen.close(); return@withContext }

                    if (chosen.isPasswordProtected) {
                        if (generation != loadGeneration) return@withContext
                        _uiState.update {
                            if (generation != loadGeneration) it else it.copy(
                                isLoading = false,
                                errorMessage = null,
                                failure = null,
                                pageCount = chosen.pageCount,
                                isPasswordRequired = true
                            )
                        }
                    } else {
                        val totalPages = chosen.pageCount
                        val rawOutline = chosen.getOutline()
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
                            chosen.getPageSize(p)
                        }

                        if (generation != loadGeneration) return@withContext
                        _uiState.update {
                            if (generation != loadGeneration) it else it.copy(
                                isLoading = false,
                                pageCount = totalPages,
                                currentPage = initialPage,
                                readingMode = initialMode,
                                isContinuousScroll = initialContinuous,
                                outline = outline
                            )
                        }

                        repository.updatePageProgress(document.uri, initialPage, totalPages)
                        prefetchSurroundingPages(initialPage)
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    if (generation != loadGeneration) return@withContext
                    val isPerm = UserMessages.isPermissionFailure(e)
                    _uiState.update {
                        if (generation != loadGeneration) it else it.copy(
                            isLoading = false,
                            isPermissionExpired = isPerm,
                            errorMessage = UserMessages.readFailure("ViewerViewModel", e).resolve(context)
                        )
                    }
                }
            }
        }
    }

    /**
     * Whether the file's trailer mentions an encryption dictionary. This decides which message a
     * reader sees when the fallback engine could not open a document; it is not used to decide
     * whether anything is safe, because the only engine that can honour a password here is MuPDF.
     */
    private fun isEncryptedPdf(file: java.io.File): Boolean = try {
        val length = file.length()
        if (length <= 0L) false else {
            val window = minOf(length, 64L * 1024L).toInt()
            java.io.RandomAccessFile(file, "r").use { raf ->
                raf.seek(length - window)
                val tail = ByteArray(window)
                raf.readFully(tail)
                String(tail, Charsets.ISO_8859_1).contains("/Encrypt")
            }
        }
    } catch (_: Throwable) {
        false
    }

    fun authenticatePassword(password: String) {
        if (waitingForOfficePassword) {
            officePassword = password
            waitingForOfficePassword = false
            loadDocument()
            return
        }
        val rend = renderer ?: return
        viewModelScope.launch(ioDispatcher) {
            val success = rend.authenticatePassword(password)
            if (!success) {
                _uiState.update { it.copy(isPasswordIncorrect = true) }
                return@launch
            }
            val totalPages = rend.pageCount
            if (totalPages <= 0) {
                // Authenticated, but there is nothing to show. Say so instead of opening an
                // empty reader, and give the native document back.
                rendererSlot.release()
                _uiState.update {
                    it.copy(
                        isPasswordRequired = false,
                        isPasswordIncorrect = false,
                        isLoading = false,
                        errorMessage = "This document opened but contains no pages.",
                        failure = com.abhisheksati.mydoc.domain.model.LoadFailure.EMPTY_DOCUMENT
                    )
                }
                return@launch
            }
            val outline = rend.getOutline()
            // The saved reading position belongs to this document whether or not the password
            // was needed to reach it; writing page 0 here used to erase it on every unlock.
            val resume = (repository.getViewState(activeDocumentFile.uri)?.lastPage ?: 0)
                .coerceIn(0, totalPages - 1)
            _uiState.update {
                it.copy(
                    isPasswordRequired = false,
                    isPasswordIncorrect = false,
                    pageCount = totalPages,
                    currentPage = resume,
                    outline = outline
                )
            }
            repository.updatePageProgress(activeDocumentFile.uri, resume, totalPages)
            prefetchSurroundingPages(resume)
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

    /** Page size only when already cached; safe to call from composition. */
    fun peekPageSize(pageIndex: Int): Pair<Float, Float>? = renderer?.peekPageSize(pageIndex)

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
            // The renderer's predicate asks "should I stop?"; `isActive` is its opposite. Passing
            // it straight through made every normal search cancel itself before the first page.
            val matches = renderer?.search(query) { !isActive } ?: emptyList()
            // Cancelling the job cannot interrupt the scan once it is running, so the result is
            // dropped here: publishing it would show matches for a query the reader replaced.
            if (!isActive) return@launch
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

    /**
     * Gives back everything that belongs to the document currently on screen. Called when a new
     * document replaces it and when the session ends, so a native engine handle is never alive
     * without a document behind it.
     */
    private fun releaseSession() {
        prefetchJob?.cancel()
        prefetchJob = null
        searchJob?.cancel()
        searchJob = null
        rendererSlot.release()
        // The zoom tile cache is a process-wide singleton keyed by document URI, so its
        // bitmaps would otherwise outlive the document they came from.
        com.abhisheksati.mydoc.ui.viewer.controls.ZoomTileCache.clear()
        for (f in sessionTempFiles) {
            try { if (f.exists()) f.delete() } catch (_: Throwable) {}
        }
        sessionTempFiles.clear()
    }

    override fun onCleared() {
        ++loadGeneration
        officePassword = null
        waitingForOfficePassword = false
        super.onCleared()
        releaseSession()
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
