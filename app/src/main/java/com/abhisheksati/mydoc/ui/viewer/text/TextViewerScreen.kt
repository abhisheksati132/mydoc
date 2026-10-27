package com.abhisheksati.mydoc.ui.viewer.text

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerCapabilities
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerScaffold
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Modern plain text and source code viewer wrapped in unified ViewerScaffold.
 * Features line numbering, monospace formatting, in-line search highlighting, TTS read aloud, and smart resume.
 */
@Composable
fun TextViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
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
    var currentMatchIndex by remember { mutableStateOf(0) }

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
                val reader = BufferedReader(InputStreamReader(inputStream))
                val loadedLines = reader.readLines()
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
    LaunchedEffect(listState.firstVisibleItemIndex, lines.size) {
        if (lines.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                app?.documentRepository?.updatePageProgress(document.uri, listState.firstVisibleItemIndex, lines.size)
                app?.documentRepository?.saveViewState(
                    ViewState(
                        uri = document.uri,
                        lastPage = listState.firstVisibleItemIndex,
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
        currentPage = if (lines.isNotEmpty()) listState.firstVisibleItemIndex else 0,
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
                CircularProgressIndicator(color = colors.accent)
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
                    .padding(horizontal = 16.dp)
                    .padding(top = 64.dp, bottom = 64.dp)
            ) {
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
                                        withStyle(SpanStyle(background = Color(0x66FFEB3B), color = textColor)) {
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
