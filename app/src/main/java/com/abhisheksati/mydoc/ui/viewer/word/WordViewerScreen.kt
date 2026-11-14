package com.abhisheksati.mydoc.ui.viewer.word

import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.stringResource
import com.abhisheksati.mydoc.R
import androidx.compose.ui.text.style.TextAlign
import com.abhisheksati.mydoc.rendering.engine.word.DocxAlignment
import com.abhisheksati.mydoc.rendering.engine.word.DocxParagraphLayout
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.abhisheksati.mydoc.rendering.engine.word.DocxImageDecoder

import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import androidx.compose.foundation.layout.statusBarsPadding
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.util.UserMessages
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DocumentCanvasColors
import com.abhisheksati.mydoc.designsystem.tokens.DocumentReadingType
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.ui.viewer.DocumentProblemScreen
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.ReadingPrefs
import com.abhisheksati.mydoc.domain.model.RenderFidelity
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxDocument
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import com.abhisheksati.mydoc.rendering.engine.word.DocxRun
import com.abhisheksati.mydoc.tools.tts.DocumentTtsManager
import com.abhisheksati.mydoc.ui.tts.DocumentTtsBar
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import com.abhisheksati.mydoc.ui.viewer.controls.edgeFlingToDocument
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerCapabilities
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerChrome
import com.abhisheksati.mydoc.ui.viewer.controls.OutlineDrawer
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerScaffold
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modern Word document viewer wrapped in unified ViewerScaffold.
 * Renders paragraphs, headings, lists, tables, and text formatting.
 */
@Composable
fun WordViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeToNextDocument: (() -> Unit)? = null,
    onSwipeToPreviousDocument: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val colors = LocalAppColors.current

    var docxData by remember { mutableStateOf<DocxDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadFailure by remember { mutableStateOf<com.abhisheksati.mydoc.domain.model.LoadFailure?>(null) }

    val defaultPrefs = remember { ReadingPrefs() }
    val readingPrefs by (app?.settingsRepository?.readingPrefs ?: remember { kotlinx.coroutines.flow.MutableStateFlow(defaultPrefs) }).collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    var isTtsVisible by remember { mutableStateOf(false) }
    val ttsManager = remember { DocumentTtsManager(context) }
    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        loadFailure = null
        val outcome = withContext(Dispatchers.IO) {
            WordDocumentLoader.load(
                openStream = { DocumentStreamResolver.openStream(context, Uri.parse(document.uri)) },
                parse = { stream -> DocxParser().parse(stream) }
            )
        }
        // Every terminal path clears the spinner. The typed-failure branch used to set the message
        // and leave `isLoading` true, so a damaged DOCX spun forever over an empty page.
        isLoading = false
        when (outcome) {
            is WordLoadOutcome.Loaded -> docxData = outcome.document
            is WordLoadOutcome.Failed -> {
                loadFailure = outcome.failure
                errorMessage = (outcome.error as? com.abhisheksati.mydoc.domain.model.DocumentLoadException)?.message
                    ?: UserMessages.readFailure(
                        "WordViewer",
                        outcome.error ?: IllegalStateException(WordDocumentLoader.COULD_NOT_OPEN)
                    ).resolve(context)
            }
        }
    }

    val textColor = ViewerThemeTokens.getTextColor(readingPrefs.readingMode, colors.textPrimary)
    val secondaryTextColor = ViewerThemeTokens.getSecondaryTextColor(readingPrefs.readingMode, colors.textSecondary)

    val blocks = docxData?.blocks ?: emptyList()
    // Search navigates the document; it does not replace it. Filtering the block list used to
    // renumber every block, so the reading position saved while searching pointed at a different
    // paragraph once the search was closed.
    val matches = remember(blocks, searchQuery) { WordViewerModel.matches(blocks, searchQuery) }
    val activeMatchBlock = if (searchQuery.isBlank()) null else matches.getOrNull(currentMatchIndex)?.blockIndex

    val lazyListState = rememberLazyListState()
    // Scroll position is a frequently-changing state object; reading it straight in
    // composition subscribes the whole viewer to every frame. Derive the index once.
    val visibleBlockIndex by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }

    val coroutineScope = rememberCoroutineScope()

    fun moveToMatch(index: Int) {
        val match = matches.getOrNull(index) ?: return
        currentMatchIndex = index
        coroutineScope.launch { lazyListState.scrollToItem(match.blockIndex) }
    }

    // Reflow positions are block indices, not pages. They live under a mode-distinct key so the
    // paginated view of the same URI can never resume a block index as a page number.
    val pagination = remember(blocks) { WordReflowPagination(blocks) }
    val reflowPrefs = remember(context) {
        context.getSharedPreferences(WordReflowProgress.PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Smart Resume: Restore position
    LaunchedEffect(document.uri, blocks.isNotEmpty()) {
        if (blocks.isNotEmpty()) {
            val targetIndex = withContext(Dispatchers.IO) {
                val saved = app?.documentRepository?.getViewState(document.uri)
                val storedBlock = reflowPrefs.getInt(WordReflowProgress.keyFor(document.uri), -1)
                WordReflowProgress.resumeBlockIndex(
                    storedBlockIndex = storedBlock.takeIf { it >= 0 },
                    savedViewState = saved,
                    pagination = pagination,
                    blockCount = blocks.size
                )
            }
            if (targetIndex > 0) {
                lazyListState.scrollToItem(targetIndex)
            }
        }
    }

    // Smart Resume: Persist progress
    LaunchedEffect(document.uri, blocks.isNotEmpty()) {
        if (blocks.isEmpty()) return@LaunchedEffect
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                withContext(Dispatchers.IO) {
                    app?.documentRepository?.updatePageProgress(document.uri, pagination.pageForBlockIndex(index), pagination.effectivePageCount)
                    app?.documentRepository?.saveViewState(
                        WordReflowProgress.viewStateFor(
                            uri = document.uri,
                            blockIndex = index,
                            pagination = pagination,
                            readingMode = readingPrefs.readingMode
                        )
                    )
                    reflowPrefs.edit().putInt(WordReflowProgress.keyFor(document.uri), index).apply()
                }
            }
    }

    // Volume key page turning
    LaunchedEffect(blocks.size) {
        if (blocks.isNotEmpty()) {
            MainActivity.pageTurnEvents.collect { isNext ->
                val current = lazyListState.firstVisibleItemIndex
                val target = if (isNext) {
                    (current + 5).coerceAtMost(blocks.size - 1)
                } else {
                    (current - 5).coerceAtLeast(0)
                }
                lazyListState.animateScrollToItem(target)
            }
        }
    }

    val fullDocumentText = remember(blocks) {
        blocks.joinToString("\n\n") { WordViewerModel.plainText(it) }
    }

    val contents = remember(blocks) {
        blocks.mapIndexedNotNull { index, block ->
            (block as? DocxBlock.Heading)?.let { heading ->
                OutlineItem(heading.text, index, (heading.level - 1).coerceAtLeast(0))
            }
        }
    }
    var showContents by remember(document.uri) { mutableStateOf(false) }
    OutlineDrawer(
        outline = contents,
        isOpen = showContents,
        onDismiss = { showContents = false },
        onItemSelected = { index -> coroutineScope.launch { lazyListState.scrollToItem(index) } }
    )

    var showPageJump by remember { mutableStateOf(false) }
    PageJumpDialog(
        isOpen = showPageJump,
        currentPage = pagination.pageForBlockIndex(visibleBlockIndex),
        pageCount = pagination.effectivePageCount,
        onJump = { page -> coroutineScope.launch { lazyListState.scrollToItem(pagination.blockForPageIndex(page)) } },
        onDismiss = { showPageJump = false }
    )
    ViewerScaffold(
        document = document,
        capabilities = ViewerCapabilities(
            supportsSearch = true,
            supportsOutline = contents.isNotEmpty(),
            supportsPageGrid = false,
            supportsSlideshow = false,
            supportsTts = true,
            supportsZoom = false,
            supportsContinuousScroll = false,
            supportsDualPageView = false
        ),
        currentPage = pagination.pageForBlockIndex(visibleBlockIndex),
        pageCount = pagination.effectivePageCount,
        observedFidelity = RenderFidelity.STRUCTURED_REFLOW,
        isControlsVisible = !isSearchOpen,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        onSearchClick = { isSearchOpen = true },
        onTtsClick = { isTtsVisible = true },
        readingPrefs = readingPrefs,
        sheetItems = contents,
        onOutlineClick = { showContents = true },
        onPagesClick = if (pagination.effectivePageCount > 1) { { showPageJump = true } } else null,
        searchOverlay = {
            SearchOverlay(
                visible = isSearchOpen,
                query = searchQuery,
                matchCount = matches.size,
                currentMatchIndex = if (matches.isEmpty()) 0 else currentMatchIndex.coerceIn(0, matches.size - 1),
                onQueryChanged = {
                    searchQuery = it
                    currentMatchIndex = 0
                },
                onNextMatch = {
                    if (matches.isNotEmpty()) {
                        moveToMatch((currentMatchIndex + 1) % matches.size)
                    }
                },
                onPreviousMatch = {
                    if (matches.isNotEmpty()) {
                        moveToMatch((currentMatchIndex - 1 + matches.size) % matches.size)
                    }
                },
                onCloseSearch = {
                    isSearchOpen = false
                    searchQuery = ""
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        ttsBar = {
            DocumentTtsBar(
                ttsManager = ttsManager,
                documentText = fullDocumentText,
                isVisible = isTtsVisible,
                onClose = { isTtsVisible = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        },
        modifier = modifier
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppProgressIndicator()
            }
        } else if (errorMessage != null) {
            DocumentProblemScreen(
                failure = loadFailure ?: com.abhisheksati.mydoc.domain.model.LoadFailure.CORRUPT_DOCUMENT,
                extension = document.extension,
                onBackClick = onBackClick,
                modifier = Modifier.statusBarsPadding()
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .edgeFlingToDocument(
                        atFirstPage = { lazyListState.firstVisibleItemIndex == 0 &&
                            lazyListState.firstVisibleItemScrollOffset == 0 },
                        atLastPage = { !lazyListState.canScrollForward },
                        onPrevious = onSwipeToPreviousDocument,
                        onNext = onSwipeToNextDocument
                    )
                    .padding(horizontal = AppSpacing.paddingMedium)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = ViewerChrome.topBarHeight, bottom = ViewerChrome.bottomBarHeight)
            ) {
                itemsIndexed(blocks) { index, block ->
                    val ranges = WordViewerModel.rangesIn(blocks, index, matches)
                    val isActiveBlock = activeMatchBlock == index
                    when (block) {
                        DocxBlock.PageBreak -> Spacer(Modifier.height(24.dp))
                        is DocxBlock.Image -> DocxPictureView(block)
                        is DocxBlock.Heading -> {
                            DocxRunsView(
                                runs = listOf(DocxRun(block.text)),
                                textColor = textColor,
                                fontSize = DocumentReadingType.heading(block.level) * readingPrefs.fontScale,
                                bold = true,
                                ranges = ranges,
                                active = isActiveBlock,
                                lineHeight = null,
                                modifier = paragraphModifier(block.layout, 8f, 8f),
                                textAlign = paragraphAlignment(block.layout)
                            )
                        }

                        is DocxBlock.Paragraph -> {
                            DocxParagraphView(
                                paragraph = block,
                                textColor = textColor,
                                fontScale = readingPrefs.fontScale,
                                ranges = ranges,
                                active = isActiveBlock
                            )
                            Spacer(modifier = Modifier.height(if (block.layout.after == null) 6.dp else 0.dp))
                        }

                        is DocxBlock.ListItem -> {
                            Row(
                                modifier = paragraphModifier(block.layout).padding(
                                    // A nested level indents; the marker comes from the document's own
                                    // numbering definition, so an ordered list reads "1." here and
                                    // "1." in the converted PDF instead of becoming a bullet.
                                    start = (12 + 14 * block.level).dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                )
                            ) {
                                Text(
                                    text = block.marker + " ",
                                    color = secondaryTextColor,
                                    fontSize = DocumentReadingType.body * readingPrefs.fontScale,
                                    fontWeight = FontWeight.Bold
                                )
                                DocxRunsView(
                                    runs = block.runs,
                                    textColor = textColor,
                                    fontSize = DocumentReadingType.body * readingPrefs.fontScale,
                                    bold = false,
                                    ranges = ranges,
                                    active = isActiveBlock,
                                    lineHeight = DocumentReadingType.bodyLineHeight * readingPrefs.fontScale,
                                    textAlign = paragraphAlignment(block.layout),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        is DocxBlock.Table -> {
                            DocxTableView(
                                table = block,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                highlightActive = isActiveBlock && ranges.isNotEmpty()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocxParagraphView(
    paragraph: DocxBlock.Paragraph,
    textColor: Color,
    fontScale: Float,
    ranges: List<IntRange>,
    active: Boolean
) {
    DocxRunsView(
        runs = paragraph.runs,
        textColor = textColor,
        fontSize = DocumentReadingType.body * fontScale,
        bold = false,
        ranges = ranges,
        active = active,
        lineHeight = DocumentReadingType.bodyLineHeight * fontScale,
        modifier = paragraphModifier(paragraph.layout).fillMaxWidth(),
        textAlign = paragraphAlignment(paragraph.layout)
    )
}

/**
 * [ranges] are the character offsets in this block that the search matched; [active] marks the
 * block holding the match the overlay is currently on, so "3 of 7" has something next to it.
 */
@Composable
private fun DocxRunsView(
    runs: List<DocxRun>,
    textColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    bold: Boolean,
    ranges: List<IntRange>,
    active: Boolean,
    lineHeight: androidx.compose.ui.unit.TextUnit?,
    modifier: Modifier = Modifier,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    textAlign: TextAlign = TextAlign.Start
) {
    val offsets = WordViewerModel.runOffsets(runs)
    val annotatedString = buildAnnotatedString {
        runs.forEachIndexed { position, run ->
            val span = offsets[position]
            val hit = ranges.any { it.first <= span.last && it.last >= span.first }
            val spanStyle = SpanStyle(
                fontWeight = if (bold) FontWeight.Bold else if (run.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                // A linked run is underlined and tinted even when the document did not underline it.
                textDecoration = if (run.isUnderline || run.linkTarget != null) {
                    TextDecoration.Underline
                } else {
                    TextDecoration.None
                },
                color = if (run.linkTarget != null) linkColor else textColor,
                fontSize = fontSize,
                background = when {
                    !hit -> Color.Transparent
                    active -> DocumentCanvasColors.activeHighlight
                    else -> DocumentCanvasColors.searchHighlight
                }
            )
            pushStyle(spanStyle)
            append(run.text)
            pop()
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier.fillMaxWidth(),
        lineHeight = lineHeight ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        textAlign = textAlign
    )
}

/** Tables are matched and scrolled to; a hit inside a cell tints the cell block rather than a range. */
@Composable
private fun DocxTableView(
    table: DocxBlock.Table,
    textColor: Color,
    secondaryTextColor: Color,
    highlightActive: Boolean
) {
    val borderColor = secondaryTextColor.copy(alpha = 0.3f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlightActive) DocumentCanvasColors.activeHighlight.copy(alpha = 0.35f) else Color.Transparent)
            .border(1.dp, borderColor, MaterialTheme.shapes.extraSmall)
    ) {
        table.rows.forEachIndexed { rowIndex, row ->
            val isHeader = rowIndex == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isHeader) borderColor.copy(alpha = 0.15f) else Color.Transparent)
            ) {
                row.forEach { cell ->
                    Column(modifier = Modifier.weight(1f).border(0.5.dp, borderColor).padding(6.dp)) {
                        Text(
                            text = cell.text,
                            color = textColor,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            fontSize = DocumentReadingType.tableCell
                        )
                        cell.blocks.filterIsInstance<DocxBlock.Image>().forEach { DocxPictureView(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocxPictureView(block: DocxBlock.Image) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, block) {
        value = withContext(kotlinx.coroutines.Dispatchers.IO) { DocxImageDecoder.decode(block.bytes) }
    }
    val image = bitmap
    if (image != null) {
        Image(image.asImageBitmap(), contentDescription = block.description.ifBlank { null },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(
                if (block.widthPoints > 0 && block.heightPoints > 0)
                    (block.widthPoints / block.heightPoints).coerceIn(0.02f, 50f)
                else image.width.toFloat() / image.height).padding(vertical = 6.dp))
    } else Text(stringResource(R.string.word_image_unavailable), modifier = Modifier.padding(8.dp))
}

// Reflow adapts large source indents to a phone while the PDF keeps their physical point values.
private fun paragraphModifier(layout: DocxParagraphLayout, defaultBefore: Float = 0f, defaultAfter: Float = 0f) =
    Modifier.padding(start = (layout.left ?: 0f).coerceIn(0f, 96f).dp,
        end = (layout.right ?: 0f).coerceIn(0f, 96f).dp,
        top = (layout.before ?: defaultBefore).coerceIn(0f, 72f).dp,
        bottom = (layout.after ?: defaultAfter).coerceIn(0f, 72f).dp)

private fun paragraphAlignment(layout: DocxParagraphLayout): TextAlign = when (layout.alignment) {
    DocxAlignment.LEFT -> TextAlign.Left
    DocxAlignment.CENTER -> TextAlign.Center
    DocxAlignment.RIGHT -> TextAlign.Right
    DocxAlignment.JUSTIFY -> TextAlign.Justify
    DocxAlignment.END -> TextAlign.End
    else -> TextAlign.Start
}

/**
 * Maps between reflow block indices and the viewer's synthetic page numbers. Pure so the mapping
 * that decides what gets persisted can be tested without a composition.
 */
internal class WordReflowPagination(private val blocks: List<DocxBlock>) {
    private val pageBreakIndices =
        blocks.mapIndexedNotNull { i, block -> if (block is DocxBlock.PageBreak) i else null }

    val effectivePageCount: Int =
        if (pageBreakIndices.isNotEmpty()) pageBreakIndices.size + 1
        else (blocks.size / 4).coerceAtLeast(1)

    fun pageForBlockIndex(blockIdx: Int): Int {
        if (blocks.isEmpty()) return 0
        if (pageBreakIndices.isNotEmpty()) {
            val idx = pageBreakIndices.indexOfFirst { it >= blockIdx }
            return if (idx == -1) pageBreakIndices.size else idx
        }
        return (blockIdx / 4).coerceIn(0, effectivePageCount - 1)
    }

    fun blockForPageIndex(pageIdx: Int): Int {
        if (blocks.isEmpty()) return 0
        if (pageBreakIndices.isNotEmpty()) {
            if (pageIdx <= 0) return 0
            val breakIdx = pageBreakIndices.getOrNull(pageIdx - 1) ?: return blocks.lastIndex
            return (breakIdx + 1).coerceAtMost(blocks.lastIndex)
        }
        return (pageIdx * 4).coerceIn(0, blocks.lastIndex)
    }
}

/**
 * The reflow reading position is a block index and must never enter [ViewState.lastPage], which
 * the paginated viewer of the same URI resumes as a page. It is kept under a mode-distinct key
 * that only the reflow viewer reads; the shared view state carries the mapped page.
 */
internal object WordReflowProgress {
    const val PREFS_NAME = "word_reflow_progress"

    fun keyFor(uri: String): String = "reflow_block:$uri"

    fun viewStateFor(
        uri: String,
        blockIndex: Int,
        pagination: WordReflowPagination,
        readingMode: ReadingMode
    ): ViewState = ViewState(
        uri = uri,
        lastPage = pagination.pageForBlockIndex(blockIndex),
        readingMode = readingMode
    )

    fun resumeBlockIndex(
        storedBlockIndex: Int?,
        savedViewState: ViewState?,
        pagination: WordReflowPagination,
        blockCount: Int
    ): Int {
        if (blockCount <= 0) return 0
        if (storedBlockIndex != null && storedBlockIndex >= 0) {
            return storedBlockIndex.coerceIn(0, blockCount - 1)
        }
        val savedPage = savedViewState?.lastPage ?: return 0
        return pagination.blockForPageIndex(savedPage).coerceIn(0, blockCount - 1)
    }
}
