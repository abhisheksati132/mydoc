package com.abhisheksati.mydoc.ui.viewer.presentation

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.ui.viewer.controls.ZoomableViewport
import com.abhisheksati.mydoc.ui.viewer.controls.rememberZoomState
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.util.copyBounded
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.Button
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.util.SecureLog
import java.io.File
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.abhisheksati.mydoc.domain.model.ReadingPrefs
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DocumentCanvasColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.engine.OfficeSourceReader
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxPresentation
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxSlide
import com.abhisheksati.mydoc.rendering.engine.presentation.VisualElementType
import com.abhisheksati.mydoc.util.UserMessages
import com.abhisheksati.mydoc.MyDocApp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerCapabilities
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerChrome
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerScaffold
import com.abhisheksati.mydoc.ui.viewer.controls.OutlineDrawer
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import com.abhisheksati.mydoc.ui.viewer.controls.ReadingControlsBottomSheet
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.ui.viewer.controls.edgeFlingToDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.abhisheksati.mydoc.designsystem.components.showMessage

fun openPresentationInOfficeApp(context: Context, document: DocumentFile) {
    try {
        val uri = Uri.parse(document.uri)
        val ext = document.extension.lowercase()
        val mimeType = when (ext) {
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            else -> document.mimeType
        }

        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        // displayName comes from an external SEND/VIEW intent: keep only a leaf file name.
        val leafName = File(document.displayName.filterNot { it.isISOControl() }.replace('\\', '/')).name
        val safeName = if (leafName.isBlank() || leafName == "." || leafName == "..") {
            "presentation_${System.currentTimeMillis()}.${document.extension.lowercase().ifBlank { "pptx" }}"
        } else {
            leafName
        }
        // A basename-only staging path was reused whenever a non-empty file already existed, so a
        // second `report.pptx` - or an edited one - was handed over as the earlier copy. Each
        // handoff gets its own directory and always writes the bytes the reader is looking at.
        val handoffDir = File(sharedDir, "handoff-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        val targetFile = File(handoffDir, safeName)

        val stream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri)
            ?: return
        stream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyBounded(output)
            }
        }

        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.pres_open_in)))
    } catch (_: Exception) {
        try {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.docs.editors.slides")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (_: Exception) {
            context.showMessage(context.getString(R.string.pres_need_office), long = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeToNextDocument: (() -> Unit)? = null,
    onSwipeToPreviousDocument: (() -> Unit)? = null,
    startInSlideshow: Boolean = false
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as? MyDocApp

    var presentationData by remember(document.uri) { mutableStateOf<PptxPresentation?>(null) }
    var isLoading by remember(document.uri) { mutableStateOf(true) }
    var errorMessage by remember(document.uri) { mutableStateOf<String?>(null) }

    var isVisualMode by rememberSaveable { mutableStateOf(true) }
    var isJumpDialogOpen by remember { mutableStateOf(false) }

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember(document.uri) { mutableIntStateOf(-1) }

    var isContentsOpen by remember { mutableStateOf(false) }
    var isAppearanceOpen by remember { mutableStateOf(false) }
    var bookmarks by remember(document.uri) {
        mutableStateOf(app?.settingsRepository?.getBookmarks(document.uri).orEmpty())
    }
    val settings = app?.settingsRepository
    val readingPrefs = settings?.readingPrefs?.collectAsStateWithLifecycle()?.value ?: ReadingPrefs()
    var isSlideshowMode by rememberSaveable(document.uri) { mutableStateOf(startInSlideshow) }
    var isNotesSheetOpen by remember { mutableStateOf(false) }

    val readingMode = readingPrefs.readingMode
    val view = LocalView.current
    DisposableEffect(view, readingPrefs.keepScreenOn, readingPrefs.brightness) {
        val window = (context as? android.app.Activity)?.window
        val oldBrightness = window?.attributes?.screenBrightness
        val oldKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = readingPrefs.keepScreenOn
        if (window != null && readingPrefs.brightness >= 0f) {
            window.attributes = window.attributes.apply { screenBrightness = readingPrefs.brightness }
        }
        onDispose {
            view.keepScreenOn = oldKeepScreenOn
            if (window != null && oldBrightness != null) {
                window.attributes = window.attributes.apply { screenBrightness = oldBrightness }
            }
        }
    }
    val zoomState = rememberZoomState(key = document.uri)
    var isControlsVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            var stagedFile: File? = null
            try {
                val uri = Uri.parse(document.uri)
                val ext = document.extension.lowercase().ifBlank { "pptx" }
                val cacheDir = File(context.cacheDir, "pres_cache").apply { mkdirs() }
                val tempFile = File.createTempFile("pres_", ".$ext", cacheDir)
                stagedFile = tempFile

                val inputStream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri)
                    ?: throw IllegalStateException("Could not open presentation stream")
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        // A stream can lie about its length, so the budget is enforced while copying.
                        val chunk = ByteArray(64 * 1024)
                        var written = 0L
                        while (true) {
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            val read = input.read(chunk)
                            if (read < 0) break
                            written += read
                            if (written > OfficeSourceReader.MAX_SOURCE_BYTES) {
                                throw IllegalStateException(
                                    resources.getString(
                                        R.string.pres_too_large,
                                        (OfficeSourceReader.MAX_SOURCE_BYTES / (1024 * 1024)).toInt()
                                    )
                                )
                            }
                            output.write(chunk, 0, read)
                        }
                    }
                }

                // Parse with native Kotlin XML parser
                try {
                    val parsed = PptxParser().parse(tempFile)
                    presentationData = parsed
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    SecureLog.warn("PresentationViewer", "native presentation parse failed") { document.displayName }
                    errorMessage = UserMessages.readFailure("PresentationViewer", e).resolve(context)
                }

                // A binary .ppt whose record layer produced nothing still holds readable strings.
                // Recover them only for CFBF containers: this used to run on any zero-slide file,
                // so a corrupt or unsupported deck was scraped into fabricated slides and shown
                // as if it had parsed.
                if (presentationData?.isEmpty != false && isCompoundFile(tempFile)) {
                    try {
                        val fallback = PptxParser().parseBinaryPptFallback(tempFile.readBytes())
                        if (fallback.slides.isNotEmpty()) {
                            presentationData = fallback.copy(recovered = true)
                            errorMessage = null
                        }
                    } catch (e: Throwable) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                    }
                }

                isLoading = false
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                SecureLog.error("PresentationViewer", "could not load presentation", t) { document.displayName }
                errorMessage = UserMessages.readFailure("PresentationViewer", t).resolve(context)
                isLoading = false
            } finally {
                stagedFile?.delete()
            }
        }
    }

    val rawSlides = presentationData?.slides ?: emptyList()
    val isNativeEmpty = presentationData?.isEmpty ?: true
    val effectiveSlideCount = if (!isNativeEmpty) rawSlides.size else 0
    val pagerState = rememberPagerState(pageCount = { effectiveSlideCount.coerceAtLeast(1) })
    val lazyListState = rememberLazyListState()
    var continuousReader by rememberSaveable(document.uri) { mutableStateOf(true) }
    val isContinuousVertical = continuousReader && !isSlideshowMode
    val effectiveSlides: List<PptxSlide> = if (!isNativeEmpty) rawSlides else emptyList()

    val currentSlideIndex by remember(isContinuousVertical, effectiveSlideCount) {
        derivedStateOf {
            if (isContinuousVertical) lazyListState.firstVisibleItemIndex.coerceIn(0, (effectiveSlideCount - 1).coerceAtLeast(0))
            else pagerState.currentPage
        }
    }

    // Same rule as every other reader: once the deck advances, the masthead folds into a pill.
    val isCondensed by remember(isContinuousVertical, effectiveSlideCount) {
        derivedStateOf {
            if (isContinuousVertical) {
                lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 80
            } else {
                pagerState.currentPage > 0
            }
        }
    }

    // Smart Resume: Restore last viewed slide on open
    var resumeReady by remember(document.uri, effectiveSlideCount) { mutableStateOf(false) }
    LaunchedEffect(document.uri, effectiveSlideCount) {
        if (effectiveSlideCount > 0) {
            withContext(Dispatchers.IO) {
                val savedState = app?.documentRepository?.getViewState(document.uri)
                val targetSlide = savedState?.lastPage?.coerceIn(0, effectiveSlideCount - 1) ?: 0
                withContext(Dispatchers.Main) {
                    pagerState.requestScrollToPage(targetSlide)
                    lazyListState.requestScrollToItem(targetSlide)
                }
            }
            resumeReady = true
        }
    }

    // Smart Resume: Persist slide progress on change
    LaunchedEffect(document.uri, currentSlideIndex, effectiveSlideCount, resumeReady, readingMode) {
        if (resumeReady && effectiveSlideCount > 0) {
            withContext(Dispatchers.IO) {
                app?.documentRepository?.updatePageProgress(document.uri, currentSlideIndex, effectiveSlideCount)
                app?.documentRepository?.saveViewState(
                    com.abhisheksati.mydoc.domain.model.ViewState(
                        uri = document.uri,
                        lastPage = currentSlideIndex,
                        readingMode = readingMode
                    )
                )
            }
        }
    }

    // Reset zoom when swiping slides
    LaunchedEffect(currentSlideIndex) {
        zoomState.reset()
    }

    // Volume key slide navigation
    LaunchedEffect(effectiveSlideCount, isContinuousVertical) {
        if (effectiveSlideCount > 0) {
            com.abhisheksati.mydoc.MainActivity.pageTurnEvents.collect { isNext ->
                val current = if (isContinuousVertical) lazyListState.firstVisibleItemIndex else pagerState.currentPage
                val target = if (isNext) {
                    (current + 1).coerceAtMost(effectiveSlideCount - 1)
                } else {
                    (current - 1).coerceAtLeast(0)
                }
                if (target != current) {
                    if (isContinuousVertical) {
                        lazyListState.animateScrollToItem(target)
                    } else {
                        pagerState.animateScrollToPage(target)
                    }
                }
            }
        }
    }

    // In-slide keyword search match indices
    var searchMatches by remember(document.uri, searchQuery, effectiveSlides) {
        mutableStateOf(emptyList<Int>())
    }
    LaunchedEffect(searchQuery, effectiveSlides) {
        if (searchQuery.length < 2) return@LaunchedEffect
        val query = searchQuery
        searchMatches = withContext(Dispatchers.Default) {
            effectiveSlides.mapIndexedNotNull { index, slide ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val matches = slide.title.contains(query, ignoreCase = true) ||
                    slide.content.any { it.contains(query, ignoreCase = true) } ||
                    slide.visualElements.any { element ->
                        element.paragraphs.any { it.plainText.contains(query, ignoreCase = true) } ||
                            element.tableRows.any { row -> row.any { it.contains(query, ignoreCase = true) } }
                    }
                index.takeIf { matches }
            }
        }
    }

    BackHandler(enabled = isSlideshowMode || isSearchOpen) {
        if (isSearchOpen) {
            isSearchOpen = false
            searchQuery = ""
        } else if (isSlideshowMode) {
            if (startInSlideshow) onBackClick() else {
                lazyListState.requestScrollToItem(pagerState.currentPage)
                isSlideshowMode = false
            }
        }
    }

    val colors = com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors.current
    val isAppDark = LocalIsOled.current

    val bgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> DocumentCanvasColors.blackPage
        ReadingMode.SEPIA -> DocumentCanvasColors.sepiaPage
        ReadingMode.EYE_COMFORT -> DocumentCanvasColors.eyeComfortPage
        ReadingMode.NORMAL -> if (isAppDark) DocumentCanvasColors.blackPage else colors.background
    }

    val cardBgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> DocumentCanvasColors.invertedPage
        ReadingMode.SEPIA -> DocumentCanvasColors.sepiaSpreadPage
        ReadingMode.EYE_COMFORT -> DocumentCanvasColors.eyeComfortSpreadPage
        ReadingMode.NORMAL -> if (isAppDark) DocumentCanvasColors.invertedPage else Color.White
    }

    val textColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> DocumentCanvasColors.invertedInk
        ReadingMode.SEPIA -> DocumentCanvasColors.sepiaInk
        ReadingMode.EYE_COMFORT -> DocumentCanvasColors.eyeComfortInk
        ReadingMode.NORMAL -> colors.textPrimary
    }

    val slideContents = remember(effectiveSlides, resources) {
        effectiveSlides.mapIndexed { index, slide ->
            OutlineItem(slide.title.ifBlank { resources.getString(R.string.slide_label, index + 1) }, index)
        }
    }
    val showReaderControls = isControlsVisible && !isSlideshowMode
    // The shared scaffold floats the chrome over the slides, so the deck pads by the bars it
    // rests under instead of receiving a Scaffold inner padding. Slideshow stays edge to edge.
    val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentTopPadding = when {
        isSlideshowMode -> 0.dp
        isControlsVisible -> statusBarsTop + ViewerChrome.topBarHeight + AppSpacing.paddingSmall
        else -> 16.dp
    }
    val contentBottomPadding = when {
        isSlideshowMode -> 0.dp
        isControlsVisible -> navBarsBottom + ViewerChrome.bottomBarHeight + AppSpacing.paddingSmall
        else -> 24.dp
    }
    val goToSlide: (Int) -> Unit = { index ->
        scope.launch {
            if (isContinuousVertical) lazyListState.scrollToItem(index)
            else pagerState.scrollToPage(index)
        }
    }
    val moveSearch: (Int) -> Unit = { direction ->
        if (searchMatches.isNotEmpty()) {
            currentMatchIndex = if (currentMatchIndex < 0) {
                if (direction > 0) 0 else searchMatches.lastIndex
            } else {
                (currentMatchIndex + direction).mod(searchMatches.size)
            }
            goToSlide(searchMatches[currentMatchIndex])
        }
    }

    ViewerScaffold(
        document = document,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        modifier = modifier,
        capabilities = ViewerCapabilities(
            supportsSearch = true,
            supportsOutline = true,
            supportsPageGrid = true,
            supportsSlideshow = true,
            supportsTts = false,
            supportsZoom = true
        ),
        currentPage = currentSlideIndex,
        pageCount = effectiveSlideCount,
        isControlsVisible = showReaderControls && effectiveSlideCount > 0,
        isCondensed = isCondensed,
        readingPrefs = readingPrefs,
        onSearchClick = { isSearchOpen = true },
        isBookmarked = currentSlideIndex in bookmarks,
        onToggleBookmark = {
            settings?.toggleBookmark(document.uri, currentSlideIndex)
            bookmarks = settings?.getBookmarks(document.uri).orEmpty()
        },
        onOutlineClick = { isContentsOpen = true },
        onPagesClick = { isJumpDialogOpen = true },
        sheetItems = slideContents,
        onSheetSelect = goToSlide,
        onSlideshowClick = if (effectiveSlideCount > 0) {
            {
                pagerState.requestScrollToPage(currentSlideIndex)
                isSlideshowMode = true
            }
        } else null,
        onReflowClick = if (effectiveSlideCount > 0) { { isVisualMode = !isVisualMode } } else null,
        reflowLabel = stringResource(if (isVisualMode) R.string.pres_menu_outline else R.string.pres_menu_visual),
        onDetailsClick = if (effectiveSlideCount > 0) { { isNotesSheetOpen = true } } else null,
        documentDetailsLabel = stringResource(R.string.pres_speaker_notes),
        onAppearanceClick = { isAppearanceOpen = true },
        searchOverlay = {
            SearchOverlay(
                visible = isSearchOpen && showReaderControls,
                query = searchQuery,
                matchCount = searchMatches.size,
                currentMatchIndex = currentMatchIndex.coerceIn(0, (searchMatches.size - 1).coerceAtLeast(0)),
                onQueryChanged = {
                    searchQuery = it
                    currentMatchIndex = -1
                },
                onNextMatch = { moveSearch(1) },
                onPreviousMatch = { moveSearch(-1) },
                onCloseSearch = {
                    isSearchOpen = false
                    searchQuery = ""
                    currentMatchIndex = -1
                }
            )
        },
        overlays = {
            OutlineDrawer(
                outline = slideContents,
                isOpen = isContentsOpen,
                onDismiss = { isContentsOpen = false },
                onItemSelected = goToSlide,
                bookmarks = bookmarks,
                onRemoveBookmark = { page ->
                    settings?.toggleBookmark(document.uri, page)
                    bookmarks = settings?.getBookmarks(document.uri).orEmpty()
                }
            )
            ReadingControlsBottomSheet(
                isOpen = isAppearanceOpen,
                onDismiss = { isAppearanceOpen = false },
                document = document,
                readingMode = readingMode,
                onReadingModeChange = { mode -> settings?.updateReadingPrefs { it.copy(readingMode = mode) } },
                isContinuousScroll = isContinuousVertical,
                onScrollModeChange = { continuous ->
                    val page = currentSlideIndex
                    if (continuous != continuousReader) {
                        if (continuous) lazyListState.requestScrollToItem(page)
                        else pagerState.requestScrollToPage(page)
                        continuousReader = continuous
                    }
                },
                isDualPageView = false,
                onDualPageViewChange = {},
                showDualPageControl = false,
                currentBrightness = readingPrefs.brightness,
                onBrightnessChange = { value -> settings?.updateReadingPrefs { it.copy(brightness = value) } },
                isKeepScreenOn = readingPrefs.keepScreenOn,
                onKeepScreenOnChange = { value -> settings?.updateReadingPrefs { it.copy(keepScreenOn = value) } }
            )
            PageJumpDialog(
                isOpen = isJumpDialogOpen,
                currentPage = currentSlideIndex,
                pageCount = effectiveSlideCount,
                onJump = { page ->
                    scope.launch {
                        if (isContinuousVertical) lazyListState.scrollToItem(page)
                        else pagerState.scrollToPage(page)
                    }
                },
                onDismiss = { isJumpDialogOpen = false }
            )
            PresentationSpeakerNotesSheet(
                isOpen = isNotesSheetOpen,
                currentSlideIndex = currentSlideIndex,
                notes = effectiveSlides.getOrNull(currentSlideIndex)?.notes ?: "",
                onDismiss = { isNotesSheetOpen = false }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .edgeFlingToDocument(
                    atFirstPage = { currentSlideIndex == 0 },
                    atLastPage = { currentSlideIndex >= effectiveSlideCount - 1 },
                    onPrevious = if (isSlideshowMode) null else onSwipeToPreviousDocument,
                    onNext = if (isSlideshowMode) null else onSwipeToNextDocument
                )
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                AppProgressIndicator()
            } else if (errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(AppSpacing.paddingLarge)
                ) {
                    Text(
                        text = errorMessage ?: stringResource(R.string.pres_error_reading),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { openPresentationInOfficeApp(context, document) }) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pres_open_with_office))
                    }
                }
            } else if (effectiveSlideCount == 0) {
                PresentationEmptyRecoveryCard(
                    onOpenExternal = {
                        openPresentationInOfficeApp(context, document)
                    }
                )
            } else {
                ZoomableViewport(
                    zoomState = zoomState,
                    isContinuous = isContinuousVertical,
                    isDualPage = false,
                    isDoubleTapEnabled = true,
                    onToggleControls = {
                        if (!isSlideshowMode) isControlsVisible = !isControlsVisible
                    },
                    onPrevPage = {
                        if (currentSlideIndex > 0) {
                            scope.launch {
                                if (isContinuousVertical) lazyListState.animateScrollToItem(currentSlideIndex - 1)
                                else pagerState.animateScrollToPage(currentSlideIndex - 1)
                            }
                        }
                    },
                    onNextPage = {
                        if (currentSlideIndex < effectiveSlideCount - 1) {
                            scope.launch {
                                if (isContinuousVertical) lazyListState.animateScrollToItem(currentSlideIndex + 1)
                                else pagerState.animateScrollToPage(currentSlideIndex + 1)
                            }
                        }
                    }
                ) {
                    if (isContinuousVertical) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = contentTopPadding + if (isControlsVisible) 8.dp else 0.dp,
                                bottom = contentBottomPadding + if (isControlsVisible) 16.dp else 0.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(effectiveSlideCount, key = { it }) { pageIndex ->
                                val slide = effectiveSlides.getOrNull(pageIndex)
                                if (slide != null) {
                                    val slideAspect = if (slide.aspectRatio.isFinite() && slide.aspectRatio > 0.1f) slide.aspectRatio else (16f / 9f)
                                    Surface(
                                        shape = AppRadius.cardShape,
                                        color = cardBgColor,
                                        tonalElevation = 2.dp,
                                        shadowElevation = 3.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(slideAspect)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            val hasMeaningfulVisuals = slide.visualElements.any {
                                                it.type == VisualElementType.IMAGE ||
                                                it.tableRows.isNotEmpty() ||
                                                it.paragraphs.any { p -> p.runs.any { r -> r.text.isNotBlank() } }
                                            }

                                            if (isVisualMode && hasMeaningfulVisuals) {
                                                VisualSlideCard(
                                                    slide = slide,
                                                    cardBgColor = cardBgColor,
                                                    defaultTextColor = textColor,
                                                    highlightQuery = if (isSearchOpen) searchQuery else ""
                                                )
                                            } else {
                                                OutlineSlideCard(
                                                    slide = slide,
                                                    cardBgColor = cardBgColor,
                                                    textColor = if (cardBgColor.isColorDark()) DocumentCanvasColors.invertedInk else DocumentCanvasColors.onLightInk,
                                                    highlightQuery = if (isSearchOpen) searchQuery else ""
                                                )
                                            }

                                            Surface(
                                                shape = AppRadius.pillShape,
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "${pageIndex + 1} / $effectiveSlideCount",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = contentTopPadding,
                                bottom = contentBottomPadding
                            )
                        ) { pageIndex ->
                            val slide = effectiveSlides.getOrNull(pageIndex)
                            if (slide != null) {
                                val slideAspect = if (slide.aspectRatio.isFinite() && slide.aspectRatio > 0.1f) slide.aspectRatio else (16f / 9f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (isSlideshowMode) 0.dp else 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = if (isSlideshowMode) RoundedCornerShape(0.dp) else AppRadius.cardShape,
                                        color = cardBgColor,
                                        tonalElevation = if (isSlideshowMode) 0.dp else 2.dp,
                                        shadowElevation = if (isSlideshowMode) 0.dp else 4.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(slideAspect)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            val hasMeaningfulVisuals = slide.visualElements.any {
                                                it.type == VisualElementType.IMAGE ||
                                                it.tableRows.isNotEmpty() ||
                                                it.paragraphs.any { p -> p.runs.any { r -> r.text.isNotBlank() } }
                                            }

                                            if (isVisualMode && hasMeaningfulVisuals) {
                                                VisualSlideCard(
                                                    slide = slide,
                                                    cardBgColor = cardBgColor,
                                                    defaultTextColor = textColor,
                                                    highlightQuery = if (isSearchOpen) searchQuery else ""
                                                )
                                            } else {
                                                OutlineSlideCard(
                                                    slide = slide,
                                                    cardBgColor = cardBgColor,
                                                    textColor = if (cardBgColor.isColorDark()) DocumentCanvasColors.invertedInk else DocumentCanvasColors.onLightInk,
                                                    highlightQuery = if (isSearchOpen) searchQuery else ""
                                                )
                                            }

                                            if (!isSlideshowMode) {
                                                Surface(
                                                    shape = AppRadius.pillShape,
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${pageIndex + 1} / $effectiveSlideCount",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // In Slideshow mode: Minimal floating exit button
                if (isSlideshowMode) {
                    IconButton(
                        onClick = {
                            if (startInSlideshow) onBackClick() else {
                                lazyListState.requestScrollToItem(pagerState.currentPage)
                                isSlideshowMode = false
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(AppSpacing.paddingMedium)
                            .background(colors.scrim, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.pres_exit_slideshow),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** OLE2 / CFBF magic: the only containers a binary PowerPoint record scrape can legitimately be. */
private fun isCompoundFile(file: java.io.File): Boolean {
    val header = ByteArray(8)
    val read = runCatching { file.inputStream().use { it.read(header) } }.getOrDefault(-1)
    return read == 8 &&
        header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte() &&
        header[2] == 0x11.toByte() && header[3] == 0xE0.toByte()
}
