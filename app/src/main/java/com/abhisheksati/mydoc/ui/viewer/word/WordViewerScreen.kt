package com.abhisheksati.mydoc.ui.viewer.word

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingPrefs
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxDocument
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import com.abhisheksati.mydoc.rendering.engine.word.DocxRun
import com.abhisheksati.mydoc.tools.tts.DocumentTtsManager
import com.abhisheksati.mydoc.ui.tts.DocumentTtsBar
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerCapabilities
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerScaffold
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerThemeTokens
import kotlinx.coroutines.Dispatchers
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val colors = LocalAppColors.current

    var docxData by remember { mutableStateOf<DocxDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultPrefs = remember { ReadingPrefs() }
    val readingPrefs by (app?.settingsRepository?.readingPrefs ?: remember { kotlinx.coroutines.flow.MutableStateFlow(defaultPrefs) }).collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableStateOf(0) }

    var isTtsVisible by remember { mutableStateOf(false) }
    val ttsManager = remember { DocumentTtsManager(context) }
    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(document.uri)
                val inputStream = DocumentStreamResolver.openStream(context, uri, document.displayName)
                    ?: throw IllegalStateException("Could not open input stream")
                val parsed = inputStream.use { DocxParser().parse(it) }
                docxData = parsed
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to read Word document"
                isLoading = false
            }
        }
    }

    val textColor = ViewerThemeTokens.getTextColor(readingPrefs.readingMode, colors.textPrimary)
    val secondaryTextColor = ViewerThemeTokens.getSecondaryTextColor(readingPrefs.readingMode, colors.textSecondary)

    val blocks = docxData?.blocks ?: emptyList()
    val filteredBlocks = if (searchQuery.isBlank()) {
        blocks
    } else {
        blocks.filter { block ->
            when (block) {
                is DocxBlock.Heading -> block.text.contains(searchQuery, ignoreCase = true)
                is DocxBlock.Paragraph -> block.fullText.contains(searchQuery, ignoreCase = true)
                is DocxBlock.Bullet -> block.runs.any { it.text.contains(searchQuery, ignoreCase = true) }
                is DocxBlock.Table -> block.rows.any { row -> row.any { it.contains(searchQuery, ignoreCase = true) } }
            }
        }
    }

    val lazyListState = rememberLazyListState()

    // Smart Resume: Restore position
    LaunchedEffect(filteredBlocks.size) {
        if (filteredBlocks.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val saved = app?.documentRepository?.getViewState(document.uri)
                val targetIndex = saved?.lastPage?.coerceIn(0, filteredBlocks.size - 1) ?: 0
                if (targetIndex > 0) {
                    withContext(Dispatchers.Main) {
                        lazyListState.scrollToItem(targetIndex)
                    }
                }
            }
        }
    }

    // Smart Resume: Persist progress
    LaunchedEffect(lazyListState.firstVisibleItemIndex, filteredBlocks.size) {
        if (filteredBlocks.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                app?.documentRepository?.updatePageProgress(document.uri, lazyListState.firstVisibleItemIndex, filteredBlocks.size)
                app?.documentRepository?.saveViewState(
                    ViewState(
                        uri = document.uri,
                        lastPage = lazyListState.firstVisibleItemIndex,
                        readingMode = readingPrefs.readingMode
                    )
                )
            }
        }
    }

    // Volume key page turning
    LaunchedEffect(filteredBlocks.size) {
        if (filteredBlocks.isNotEmpty()) {
            MainActivity.pageTurnEvents.collect { isNext ->
                val current = lazyListState.firstVisibleItemIndex
                val target = if (isNext) {
                    (current + 5).coerceAtMost(filteredBlocks.size - 1)
                } else {
                    (current - 5).coerceAtLeast(0)
                }
                lazyListState.animateScrollToItem(target)
            }
        }
    }

    val fullDocumentText = remember(blocks) {
        blocks.joinToString("\n\n") { block ->
            when (block) {
                is DocxBlock.Heading -> block.text
                is DocxBlock.Paragraph -> block.fullText
                is DocxBlock.Bullet -> block.runs.joinToString("") { it.text }
                is DocxBlock.Table -> block.rows.joinToString("\n") { it.joinToString(" | ") }
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    ViewerScaffold(
        document = document,
        capabilities = ViewerCapabilities(
            supportsSearch = true,
            supportsOutline = false,
            supportsPageGrid = false,
            supportsSlideshow = false,
            supportsTts = true,
            supportsZoom = false
        ),
        currentPage = if (filteredBlocks.isNotEmpty()) lazyListState.firstVisibleItemIndex else 0,
        pageCount = filteredBlocks.size.coerceAtLeast(1),
        isControlsVisible = !isSearchOpen,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        onSearchClick = { isSearchOpen = true },
        onTtsClick = { isTtsVisible = true },
        readingPrefs = readingPrefs,
        onPageScrubbed = { page ->
            if (filteredBlocks.isNotEmpty()) {
                val target = page.coerceIn(0, filteredBlocks.size - 1)
                coroutineScope.launch {
                    lazyListState.scrollToItem(target)
                }
            }
        },
        searchOverlay = {
            SearchOverlay(
                visible = isSearchOpen,
                query = searchQuery,
                matchCount = filteredBlocks.size,
                currentMatchIndex = currentMatchIndex,
                onQueryChanged = {
                    searchQuery = it
                    currentMatchIndex = 0
                },
                onNextMatch = {
                    if (filteredBlocks.isNotEmpty()) {
                        currentMatchIndex = (currentMatchIndex + 1) % filteredBlocks.size
                    }
                },
                onPreviousMatch = {
                    if (filteredBlocks.isNotEmpty()) {
                        currentMatchIndex = (currentMatchIndex - 1 + filteredBlocks.size) % filteredBlocks.size
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
                CircularProgressIndicator(color = colors.accent)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessage ?: "Error reading document",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 64.dp, bottom = 64.dp)
            ) {
                items(filteredBlocks) { block ->
                    when (block) {
                        is DocxBlock.Heading -> {
                            val fontSize = when (block.level) {
                                1 -> 24.sp
                                2 -> 20.sp
                                3 -> 17.sp
                                else -> 15.sp
                            }
                            Text(
                                text = block.text,
                                fontSize = fontSize * readingPrefs.fontScale,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        is DocxBlock.Paragraph -> {
                            DocxParagraphView(
                                paragraph = block,
                                textColor = textColor,
                                fontScale = readingPrefs.fontScale,
                                searchQuery = searchQuery
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        is DocxBlock.Bullet -> {
                            Row(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                                Text(
                                    text = "• ",
                                    color = secondaryTextColor,
                                    fontSize = 14.sp * readingPrefs.fontScale,
                                    fontWeight = FontWeight.Bold
                                )
                                DocxRunsView(
                                    runs = block.runs,
                                    textColor = textColor,
                                    fontScale = readingPrefs.fontScale,
                                    searchQuery = searchQuery
                                )
                            }
                        }

                        is DocxBlock.Table -> {
                            DocxTableView(
                                table = block,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor
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
    searchQuery: String
) {
    if (paragraph.runs.isEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        return
    }
    DocxRunsView(
        runs = paragraph.runs,
        textColor = textColor,
        fontScale = fontScale,
        searchQuery = searchQuery
    )
}

@Composable
private fun DocxRunsView(
    runs: List<DocxRun>,
    textColor: Color,
    fontScale: Float,
    searchQuery: String
) {
    val annotatedString = buildAnnotatedString {
        runs.forEach { run ->
            val spanStyle = SpanStyle(
                fontWeight = if (run.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (run.isUnderline) TextDecoration.Underline else TextDecoration.None,
                color = textColor,
                fontSize = 14.sp * fontScale,
                background = if (searchQuery.isNotBlank() && run.text.contains(searchQuery, ignoreCase = true)) {
                    Color(0x66FFEB3B)
                } else Color.Transparent
            )
            pushStyle(spanStyle)
            append(run.text)
            pop()
        }
    }

    Text(
        text = annotatedString,
        lineHeight = (20f * fontScale).sp
    )
}

@Composable
private fun DocxTableView(
    table: DocxBlock.Table,
    textColor: Color,
    secondaryTextColor: Color
) {
    val borderColor = secondaryTextColor.copy(alpha = 0.3f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
    ) {
        table.rows.forEachIndexed { rowIndex, row ->
            val isHeader = rowIndex == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isHeader) borderColor.copy(alpha = 0.15f) else Color.Transparent)
            ) {
                row.forEach { cellText ->
                    Text(
                        text = cellText,
                        color = textColor,
                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, borderColor)
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
