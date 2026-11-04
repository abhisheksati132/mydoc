package com.abhisheksati.mydoc.ui.viewer.text

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DocumentCanvasColors
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingPrefs
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.tools.tts.DocumentTtsManager
import com.abhisheksati.mydoc.ui.tts.DocumentTtsBar
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import com.abhisheksati.mydoc.ui.viewer.controls.edgeFlingToDocument
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerCapabilities
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerScaffold
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

private sealed class MdNode {
    data class Heading(val level: Int, val raw: String, val originalIndex: Int) : MdNode()
    data class Paragraph(val raw: String, val originalIndex: Int) : MdNode()
    data class ListItem(val raw: String, val ordered: Boolean, val marker: String, val originalIndex: Int) : MdNode()
    data class BlockQuote(val raw: String, val originalIndex: Int) : MdNode()
    data class Rule(val originalIndex: Int) : MdNode()
}

private val mdHeadingRegex = Regex("^(#{1,6})\\s+(.*)$")
private val mdOrderedListRegex = Regex("^\\s*(\\d+)[.)]\\s+(.*)$")
private val mdUnorderedListRegex = Regex("^\\s*[-*+]\\s+(.*)$")
private val mdRuleRegex = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
private val mdInlineRegex = Regex("\\*\\*\\*.+?\\*\\*\\*|\\*\\*.+?\\*\\*|__.+?__|~~.+?~~|\\*[^*\\s].*?\\*|`[^`]+`")

private fun parseMarkdownLine(raw: String, index: Int): MdNode? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    mdHeadingRegex.find(trimmed)?.let { return MdNode.Heading(it.groupValues[1].length, it.groupValues[2], index) }
    if (mdRuleRegex.matches(trimmed)) return MdNode.Rule(index)
    mdOrderedListRegex.find(trimmed)?.let { return MdNode.ListItem(it.groupValues[2], ordered = true, marker = "${it.groupValues[1]}.", index) }
    mdUnorderedListRegex.find(trimmed)?.let { return MdNode.ListItem(it.groupValues[1], ordered = false, marker = "•", index) }
    if (trimmed.startsWith("> ")) return MdNode.BlockQuote(trimmed.substring(2), index)
    return MdNode.Paragraph(trimmed, index)
}

/** Inline `**bold**`, `*italic*`, `` `code` `` and `~~strike~~` spans, with search hits kept on top. */
private fun buildInlineSpan(
    raw: String,
    query: String,
    base: SpanStyle,
    mono: FontFamily,
    highlight: SpanStyle,
): AnnotatedString = buildAnnotatedString {
    val lowerQuery = query.lowercase()
    var plainFrom = 0
    fun appendPlain(end: Int) {
        if (end <= plainFrom) return
        val segment = raw.substring(plainFrom, end)
        var from = 0
        while (from < segment.length) {
            val hit = if (query.isNotBlank()) segment.lowercase().indexOf(lowerQuery, from) else -1
            if (hit == -1) {
                withStyle(base) { append(segment.substring(from)) }
                break
            }
            if (hit > from) withStyle(base) { append(segment.substring(from, hit)) }
            withStyle(base.copy(background = highlight.background, color = highlight.color)) {
                append(segment.substring(hit, hit + query.length))
            }
            from = hit + query.length
        }
    }
    mdInlineRegex.findAll(raw).forEach { match ->
        appendPlain(match.range.first)
        val token = match.value
        val body = when {
            token.startsWith("***") && token.endsWith("***") && token.length > 6 ->
                Triple(token.substring(3, token.length - 3), true, true)
            token.startsWith("**") && token.endsWith("**") && token.length > 4 ->
                Triple(token.substring(2, token.length - 2), true, false)
            token.startsWith("__") && token.endsWith("__") && token.length > 4 ->
                Triple(token.substring(2, token.length - 2), true, false)
            token.startsWith("~~") && token.endsWith("~~") && token.length > 4 ->
                Triple(token.substring(2, token.length - 2), false, false)
            token.startsWith("`") && token.endsWith("`") && token.length > 2 ->
                Triple(token.substring(1, token.length - 1), false, false)
            else -> Triple(token.substring(1, token.length - 1), false, true)
        }
        val (text, strong, emphasis) = body
        val style = when {
            token.startsWith("`") -> base.copy(fontFamily = mono)
            else -> base.copy(
                fontWeight = if (strong) FontWeight.Bold else base.fontWeight,
                fontStyle = if (emphasis) androidx.compose.ui.text.font.FontStyle.Italic else base.fontStyle,
                textDecoration = if (token.startsWith("~~")) TextDecoration.LineThrough else base.textDecoration,
            )
        }
        withStyle(style) { append(text) }
        plainFrom = match.range.last + 1
    }
    appendPlain(raw.length)
}

/**
 * Modern plain text and source code viewer wrapped in unified ViewerScaffold.
 * Features line numbering, monospace formatting, in-line search highlighting, TTS read aloud, and smart resume.
 */
@Composable
fun TextViewerScreen(
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

    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultPrefs = remember { ReadingPrefs() }
    val readingPrefs by (app?.settingsRepository?.readingPrefs ?: remember { kotlinx.coroutines.flow.MutableStateFlow(defaultPrefs) }).collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    val showLineNumbers by remember { mutableStateOf(true) }
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
                val loadedLines = inputStream.use { BufferedReader(InputStreamReader(it)).readLines() }
                lines = loadedLines
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to read text file"
                isLoading = false
            }
        }
    }

    val textColor = ViewerThemeTokens.getTextColor(readingPrefs.readingMode, colors.textPrimary)
    val secondaryTextColor = ViewerThemeTokens.getSecondaryTextColor(readingPrefs.readingMode, colors.textSecondary)

    val listState = rememberLazyListState()
    // Scroll position is a frequently-changing state object; reading it straight in
    // composition subscribes the whole viewer to every frame. Derive the index once.
    val visibleLineIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // Smart Resume: Restore last read position on open
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val saved = app?.documentRepository?.getViewState(document.uri)
                val targetIndex = saved?.lastPage?.coerceIn(0, lines.size - 1) ?: 0
                if (targetIndex > 0) {
                    withContext(Dispatchers.Main) {
                        listState.scrollToItem(targetIndex)
                    }
                }
            }
        }
    }

    // Smart Resume: Persist reading progress on scroll
    LaunchedEffect(lines.size) {
        if (lines.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                withContext(Dispatchers.IO) {
                    app?.documentRepository?.updatePageProgress(document.uri, index, lines.size)
                    app?.documentRepository?.saveViewState(
                        ViewState(
                            uri = document.uri,
                            lastPage = index,
                            readingMode = readingPrefs.readingMode
                        )
                    )
                }
            }
    }

    // Volume key line scrolling
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            MainActivity.pageTurnEvents.collect { isNext ->
                val current = listState.firstVisibleItemIndex
                val target = if (isNext) {
                    (current + 15).coerceAtMost(lines.size - 1)
                } else {
                    (current - 15).coerceAtLeast(0)
                }
                listState.animateScrollToItem(target)
            }
        }
    }

    val matchingLineIndices = remember(searchQuery, lines) {
        if (searchQuery.isBlank()) emptyList()
        else lines.mapIndexedNotNull { index, line -> if (line.contains(searchQuery, ignoreCase = true)) index else null }
    }

    val isMarkdown = document.displayName.lowercase().endsWith(".md") ||
        document.mimeType.contains("markdown")
    val markdownNodes = remember(lines) {
        if (!isMarkdown) emptyList()
        else lines.mapIndexedNotNull { index, raw -> parseMarkdownLine(raw, index) }
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
        currentPage = if (lines.isNotEmpty()) visibleLineIndex else 0,
        pageCount = lines.size.coerceAtLeast(1),
        isControlsVisible = !isSearchOpen,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        onSearchClick = { isSearchOpen = true },
        onTtsClick = { isTtsVisible = true },
        readingPrefs = readingPrefs,
        onPageScrubbed = { page ->
            if (lines.isNotEmpty()) {
                val target = page.coerceIn(0, lines.size - 1)
                coroutineScope.launch {
                    listState.scrollToItem(target)
                }
            }
        },
        searchOverlay = {
            SearchOverlay(
                visible = isSearchOpen,
                query = searchQuery,
                matchCount = matchingLineIndices.size,
                currentMatchIndex = currentMatchIndex,
                onQueryChanged = {
                    searchQuery = it
                    currentMatchIndex = 0
                },
                onNextMatch = {
                    if (matchingLineIndices.isNotEmpty()) {
                        currentMatchIndex = (currentMatchIndex + 1) % matchingLineIndices.size
                    }
                },
                onPreviousMatch = {
                    if (matchingLineIndices.isNotEmpty()) {
                        currentMatchIndex = (currentMatchIndex - 1 + matchingLineIndices.size) % matchingLineIndices.size
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
                documentText = remember(lines) { lines.joinToString("\n") },
                isVisible = isTtsVisible,
                onClose = { isTtsVisible = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        },
        modifier = modifier
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessage ?: "Error reading file",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .edgeFlingToDocument(
                        atFirstPage = { listState.firstVisibleItemIndex == 0 },
                        atLastPage = { !listState.canScrollForward },
                        onPrevious = onSwipeToPreviousDocument,
                        onNext = onSwipeToNextDocument
                    )
                    .padding(horizontal = 16.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 64.dp, bottom = 64.dp)
            ) {
                if (isMarkdown) {
                    val scale = readingPrefs.fontScale
                    fun styled(raw: String, size: Float, weight: FontWeight = FontWeight.Normal): Pair<AnnotatedString, TextUnit> =
                        buildInlineSpan(
                            raw = raw,
                            query = searchQuery.trim(),
                            base = SpanStyle(
                                color = textColor,
                                fontSize = (size * scale).sp,
                                fontWeight = weight,
                            ),
                            mono = FontFamily.Monospace,
                            highlight = SpanStyle(background = DocumentCanvasColors.searchHighlight, color = textColor),
                        ) to (size * 1.6f * scale).sp
                    itemsIndexed(markdownNodes) { _, node ->
                        when (node) {
                            is MdNode.Heading -> {
                                val size = when (node.level) {
                                    1 -> 28
                                    2 -> 23
                                    3 -> 20
                                    else -> 17
                                }
                                val (text, lineHeight) = styled(node.raw, size.toFloat(), FontWeight.Bold)
                                Column(modifier = Modifier.padding(top = if (node.level <= 2) 20.dp else 12.dp, bottom = 8.dp)) {
                                    Text(text = text, lineHeight = lineHeight)
                                    if (node.level == 1) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .width(44.dp)
                                                .height(3.dp)
                                                .background(colors.accent)
                                        )
                                    }
                                }
                            }
                            is MdNode.Paragraph -> {
                                val (text, lineHeight) = styled(node.raw, 16f)
                                Text(
                                    text = text,
                                    lineHeight = lineHeight,
                                    modifier = Modifier.padding(bottom = 14.dp),
                                )
                            }
                            is MdNode.ListItem -> {
                                val (text, lineHeight) = styled(node.raw, 16f)
                                Row(modifier = Modifier.padding(bottom = 6.dp)) {
                                    Text(
                                        text = node.marker,
                                        color = colors.accent,
                                        fontSize = (16 * scale).sp,
                                        fontWeight = if (node.ordered) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.width(if (node.ordered) 30.dp else 22.dp),
                                    )
                                    Text(
                                        text = text,
                                        lineHeight = lineHeight,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            is MdNode.BlockQuote -> {
                                val (text, lineHeight) = styled(node.raw, 16f)
                                Row(modifier = Modifier.padding(bottom = 14.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp, top = 4.dp)
                                            .width(3.dp)
                                            .height(22.dp)
                                            .background(colors.accent.copy(alpha = 0.6f))
                                    )
                                    Text(
                                        text = text,
                                        color = secondaryTextColor,
                                        lineHeight = lineHeight,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            is MdNode.Rule -> Box(
                                modifier = Modifier
                                    .padding(vertical = 18.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.divider)
                            )
                        }
                    }
                } else {
                    itemsIndexed(lines) { index, line ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (showLineNumbers) {
                            Text(
                                text = "${index + 1}",
                                color = secondaryTextColor.copy(alpha = 0.5f),
                                fontSize = (12 * readingPrefs.fontScale).sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .width(36.dp)
                                    .padding(end = 6.dp)
                            )
                        }

                        val annotated = buildAnnotatedString {
                            val query = searchQuery.trim()
                            if (query.isNotEmpty() && line.contains(query, ignoreCase = true)) {
                                var startIndex = 0
                                val lowerLine = line.lowercase()
                                val lowerQuery = query.lowercase()

                                while (startIndex < line.length) {
                                    val matchIndex = lowerLine.indexOf(lowerQuery, startIndex)
                                    if (matchIndex != -1) {
                                        append(line.substring(startIndex, matchIndex))
                                        withStyle(SpanStyle(background = DocumentCanvasColors.searchHighlight, color = textColor)) {
                                            append(line.substring(matchIndex, matchIndex + query.length))
                                        }
                                        startIndex = matchIndex + query.length
                                    } else {
                                        append(line.substring(startIndex))
                                        break
                                    }
                                }
                            } else {
                                append(line)
                            }
                        }

                        Text(
                            text = annotated,
                            color = textColor,
                            fontSize = (14 * readingPrefs.fontScale).sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (20 * readingPrefs.fontScale).sp
                        )
                    }
                    }
                }
            }
        }
    }
}
