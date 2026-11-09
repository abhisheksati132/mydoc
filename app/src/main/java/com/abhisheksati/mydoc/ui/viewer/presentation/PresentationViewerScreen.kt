package com.abhisheksati.mydoc.ui.viewer.presentation

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Scaffold
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
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.ui.viewer.controls.edgeFlingToDocument
import com.abhisheksati.mydoc.ui.viewer.controls.PrintHelper
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
                input.copyTo(output)
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

    var cachedFileOnDisk by remember { mutableStateOf<File?>(null) }
    var presentationData by remember { mutableStateOf<PptxPresentation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var readingMode by remember { mutableStateOf(ReadingMode.NORMAL) }
    var isVisualMode by remember { mutableStateOf(true) }
    var isJumpDialogOpen by remember { mutableStateOf(false) }

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    var isThumbnailStripOpen by remember { mutableStateOf(false) }
    var isSlideshowMode by rememberSaveable(document.uri) { mutableStateOf(startInSlideshow) }
    var isNotesSheetOpen by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(document.uri) {
        onDispose {
            cachedFileOnDisk?.delete()
            cachedFileOnDisk = null
        }
    }

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(document.uri)
                val ext = document.extension.lowercase().ifBlank { "pptx" }
                val cacheDir = File(context.cacheDir, "pres_cache").apply { mkdirs() }
                val tempFile = File.createTempFile("pres_", ".$ext", cacheDir)
                cachedFileOnDisk = tempFile

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
                    val parsed = tempFile.inputStream().use { PptxParser().parse(it, context.cacheDir) }
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
                cachedFileOnDisk?.delete()
                cachedFileOnDisk = null
                errorMessage = UserMessages.readFailure("PresentationViewer", t).resolve(context)
                isLoading = false
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

    // Smart Resume: Restore last viewed slide on open
    var resumeReady by remember(document.uri, effectiveSlideCount) { mutableStateOf(false) }
    LaunchedEffect(document.uri, effectiveSlideCount) {
        if (effectiveSlideCount > 0) {
            withContext(Dispatchers.IO) {
                val savedState = app?.documentRepository?.getViewState(document.uri)
                val targetSlide = savedState?.lastPage?.coerceIn(0, effectiveSlideCount - 1) ?: 0
                if (targetSlide > 0) {
                    withContext(Dispatchers.Main) {
                        pagerState.scrollToPage(targetSlide)
                        lazyListState.scrollToItem(targetSlide)
                    }
                }
            }
            resumeReady = true
        }
    }

    // Smart Resume: Persist slide progress on change
    LaunchedEffect(document.uri, currentSlideIndex, effectiveSlideCount, resumeReady) {
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
        zoomScale = 1f
        panOffset = Offset.Zero
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
    val searchMatches = remember(searchQuery, effectiveSlides) {
        if (searchQuery.length < 2) emptyList<Int>()
        else {
            val list = mutableListOf<Int>()
            effectiveSlides.forEachIndexed { idx, slide ->
                val inTitle = slide.title.contains(searchQuery, ignoreCase = true)
                val inContent = slide.content.any { it.contains(searchQuery, ignoreCase = true) }
                val inVisual = slide.visualElements.any { el ->
                    el.paragraphs.any { p -> p.plainText.contains(searchQuery, ignoreCase = true) } ||
                    el.tableRows.any { row -> row.any { cell -> cell.contains(searchQuery, ignoreCase = true) } }
                }
                if (inTitle || inContent || inVisual) {
                    list.add(idx)
                }
            }
            list
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

    val secondaryTextColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> DocumentCanvasColors.invertedMuted
        ReadingMode.SEPIA -> DocumentCanvasColors.sepiaMuted
        ReadingMode.EYE_COMFORT -> DocumentCanvasColors.eyeComfortMuted
        ReadingMode.NORMAL -> colors.textSecondary
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                PresentationTopBar(
                document = document,
                effectiveSlideCount = effectiveSlideCount,
                isSlideshowMode = isSlideshowMode,
                isSearchOpen = isSearchOpen,
                searchQuery = searchQuery,
                searchMatchesCount = searchMatches.size,
                currentMatchIndex = currentMatchIndex,
                onQueryChanged = {
                    searchQuery = it
                    currentMatchIndex = 0
                },
                onNextMatch = {
                    if (searchMatches.isNotEmpty()) {
                        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
                        scope.launch { pagerState.animateScrollToPage(searchMatches[currentMatchIndex]) }
                    }
                },
                onPreviousMatch = {
                    if (searchMatches.isNotEmpty()) {
                        currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size) % searchMatches.size
                        scope.launch { pagerState.animateScrollToPage(searchMatches[currentMatchIndex]) }
                    }
                },
                onCloseSearch = {
                    isSearchOpen = false
                    searchQuery = ""
                },
                onOpenSearch = { isSearchOpen = true },
                onBackClick = onBackClick,
                isContinuousVertical = isContinuousVertical,
                onToggleContinuousVertical = {
                    val newMode = !isContinuousVertical
                    continuousReader = newMode
                    scope.launch {
                        if (newMode) {
                            lazyListState.scrollToItem(pagerState.currentPage)
                        } else {
                            pagerState.scrollToPage(lazyListState.firstVisibleItemIndex)
                        }
                    }
                },
                onStartSlideshow = {
                    pagerState.requestScrollToPage(currentSlideIndex)
                    isSlideshowMode = true
                },
                isVisualMode = isVisualMode,
                onToggleVisualMode = { isVisualMode = !isVisualMode },
                readingMode = readingMode,
                onCycleReadingMode = {
                    readingMode = when (readingMode) {
                        ReadingMode.NORMAL -> ReadingMode.INVERTED_DARK
                        ReadingMode.INVERTED_DARK -> ReadingMode.SEPIA
                        ReadingMode.SEPIA -> ReadingMode.EYE_COMFORT
                        ReadingMode.EYE_COMFORT -> ReadingMode.NORMAL
                    }
                },
                onOpenJumpDialog = { isJumpDialogOpen = true },
                onToggleThumbnailStrip = { isThumbnailStripOpen = !isThumbnailStripOpen },
                onOpenNotesSheet = { isNotesSheetOpen = true },
                onShareClick = onShareClick,
                onOpenInOfficeApp = { openPresentationInOfficeApp(context, document) },
                onPrintClick = {
                    PrintHelper.printDocument(context, document) { msg ->
                        context.showMessage(msg)
                    }
                },
                )
            }
        },
        bottomBar = {
            if (effectiveSlideCount > 0 && !isSlideshowMode) {
                PresentationBottomBar(
                    effectiveSlideCount = effectiveSlideCount,
                    currentSlideIndex = currentSlideIndex,
                    effectiveSlides = effectiveSlides,
                    isThumbnailStripOpen = isThumbnailStripOpen,
                    onToggleThumbnailStrip = { isThumbnailStripOpen = !isThumbnailStripOpen },
                    onSelectSlide = { index ->
                        scope.launch {
                            if (isContinuousVertical) {
                                lazyListState.animateScrollToItem(index)
                            } else {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    onPreviousSlide = {
                        scope.launch {
                            if (currentSlideIndex > 0) {
                                if (isContinuousVertical) lazyListState.animateScrollToItem(currentSlideIndex - 1)
                                else pagerState.animateScrollToPage(currentSlideIndex - 1)
                            }
                        }
                    },
                    onNextSlide = {
                        scope.launch {
                            if (currentSlideIndex < effectiveSlideCount - 1) {
                                if (isContinuousVertical) lazyListState.animateScrollToItem(currentSlideIndex + 1)
                                else pagerState.animateScrollToPage(currentSlideIndex + 1)
                            }
                        }
                    },
                    onOpenJumpDialog = { isJumpDialogOpen = true },
                    onOpenNotesSheet = { isNotesSheetOpen = true },
                    bgColor = bgColor,
                    cardBgColor = cardBgColor,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
            }
        },
        containerColor = bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .edgeFlingToDocument(
                    atFirstPage = { currentSlideIndex == 0 },
                    atLastPage = { currentSlideIndex >= effectiveSlideCount - 1 },
                    // Presenting is its own mode; a stray fling must not walk out of the deck.
                    onPrevious = if (isSlideshowMode) null else onSwipeToPreviousDocument,
                    onNext = if (isSlideshowMode) null else onSwipeToNextDocument
                )
                .padding(if (isSlideshowMode) PaddingValues(0.dp) else innerPadding)
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
            } else if (isContinuousVertical) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = AppSpacing.paddingSmall, vertical = AppSpacing.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(effectiveSlideCount, key = { it }) { pageIndex ->
                        val slide = effectiveSlides.getOrNull(pageIndex)
                        if (slide != null) {
                            val slideAspect = if (slide.aspectRatio.isFinite() && slide.aspectRatio > 0.1f) slide.aspectRatio else (16f / 9f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(slideAspect),
                                contentAlignment = Alignment.Center
                            ) {
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
                            }
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isSlideshowMode) 0.dp else AppSpacing.paddingMedium)
                            .pointerInput(isSlideshowMode) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        if (isSlideshowMode) {
                                            val w = size.width
                                            if (offset.x < w * 0.25f) {
                                                if (pagerState.currentPage > 0) {
                                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                }
                                            } else {
                                                if (pagerState.currentPage < effectiveSlideCount - 1) {
                                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                }
                                            }
                                        }
                                    },
                                    onDoubleTap = {
                                        zoomScale = if (zoomScale > 1.2f) 1f else 2.5f
                                        panOffset = Offset.Zero
                                    }
                                )
                            }
                            .then(
                                if (zoomScale > 1.05f) {
                                    Modifier.pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                            if (zoomScale > 1f) {
                                                val maxPan = 500f * (zoomScale - 1f)
                                                panOffset = Offset(
                                                    x = (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                                                    y = (panOffset.y + pan.y).coerceIn(-maxPan, maxPan)
                                                )
                                            } else {
                                                panOffset = Offset.Zero
                                            }
                                        }
                                    }
                                } else Modifier
                            )
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val slide = effectiveSlides.getOrNull(pageIndex)
                        if (slide != null) {
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

            // Speaker Notes Bottom Sheet
            PresentationSpeakerNotesSheet(
                isOpen = isNotesSheetOpen,
                currentSlideIndex = currentSlideIndex,
                notes = effectiveSlides.getOrNull(currentSlideIndex)?.notes ?: "",
                onDismiss = { isNotesSheetOpen = false }
            )
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
