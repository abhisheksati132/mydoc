package com.abhisheksati.mydoc.ui.viewer.word

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxDocument
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.ui.theme.Spacing
import com.abhisheksati.mydoc.ui.viewer.controls.PrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var docxData by remember { mutableStateOf<DocxDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var readingMode by remember { mutableStateOf(ReadingMode.NORMAL) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isMenuOpen by remember { mutableStateOf(false) }
    var fontScale by remember { mutableFloatStateOf(1.0f) }
    var isTtsVisible by remember { mutableStateOf(false) }
    val ttsManager = remember { com.abhisheksati.mydoc.tools.tts.DocumentTtsManager(context) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(document.uri)
                val inputStream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, document.displayName)
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

    val colors = com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors.current
    val isAppDark = colors.background == com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors.background

    val bgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color.Black
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.EYE_COMFORT -> Color(0xFFF7F3E9)
        ReadingMode.NORMAL -> if (isAppDark) Color(0xFF000000) else colors.background
    }

    val textColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color.White
        ReadingMode.SEPIA -> Color(0xFF3A3125)
        ReadingMode.EYE_COMFORT -> Color(0xFF2C261F)
        ReadingMode.NORMAL -> colors.textPrimary
    }

    val secondaryTextColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFAAAAAA)
        ReadingMode.SEPIA -> Color(0xFF5A4E3A)
        ReadingMode.EYE_COMFORT -> Color(0xFF5C5245)
        ReadingMode.NORMAL -> colors.textSecondary
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(bgColor)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = document.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = if (isSearchOpen) MaterialTheme.colorScheme.primary else secondaryTextColor
                            )
                        }
                        IconButton(onClick = {
                            readingMode = when (readingMode) {
                                ReadingMode.NORMAL -> ReadingMode.INVERTED_DARK
                                ReadingMode.INVERTED_DARK -> ReadingMode.SEPIA
                                ReadingMode.SEPIA -> ReadingMode.EYE_COMFORT
                                ReadingMode.EYE_COMFORT -> ReadingMode.NORMAL
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Brightness4,
                                contentDescription = "Toggle Reading Mode",
                                tint = if (readingMode != ReadingMode.NORMAL) MaterialTheme.colorScheme.primary else secondaryTextColor
                            )
                        }
                        IconButton(onClick = { isTtsVisible = !isTtsVisible }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = if (isTtsVisible) MaterialTheme.colorScheme.primary else secondaryTextColor
                            )
                        }
                        IconButton(onClick = onShareClick) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = secondaryTextColor
                            )
                        }
                        Box {
                            IconButton(onClick = { isMenuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Options",
                                    tint = secondaryTextColor
                                )
                            }
                            DropdownMenu(
                                expanded = isMenuOpen,
                                onDismissRequest = { isMenuOpen = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                shadowElevation = 8.dp,
                                tonalElevation = 4.dp
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open with another app") },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                    },
                                    onClick = {
                                        isMenuOpen = false
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(document.uri), document.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(intent, "Open with"))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No app available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Print") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Print, contentDescription = null)
                                    },
                                    onClick = {
                                        isMenuOpen = false
                                        PrintHelper.printDocument(context, document)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zoom In (Text)") },
                                    onClick = {
                                        isMenuOpen = false
                                        fontScale = (fontScale + 0.15f).coerceAtMost(2.0f)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zoom Out (Text)") },
                                    onClick = {
                                        isMenuOpen = false
                                        fontScale = (fontScale - 0.15f).coerceAtLeast(0.7f)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = bgColor
                    )
                )

                AnimatedVisibility(
                    visible = isSearchOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search document…") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )
                    }
                }

                AppDivider()
            }
        },
        containerColor = bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgColor)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage ?: "Error reading document",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(Spacing.large)
                    )
                }
            } else {
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
                val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
                val app = context.applicationContext as? com.abhisheksati.mydoc.MyDocApp

                // Smart Resume: Restore last read position on open
                androidx.compose.runtime.LaunchedEffect(filteredBlocks.size) {
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

                // Smart Resume: Persist reading progress on scroll
                androidx.compose.runtime.LaunchedEffect(lazyListState.firstVisibleItemIndex, filteredBlocks.size) {
                    if (filteredBlocks.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            app?.documentRepository?.updatePageProgress(document.uri, lazyListState.firstVisibleItemIndex, filteredBlocks.size)
                            app?.documentRepository?.saveViewState(
                                com.abhisheksati.mydoc.domain.model.ViewState(
                                    uri = document.uri,
                                    lastPage = lazyListState.firstVisibleItemIndex,
                                    readingMode = readingMode
                                )
                            )
                        }
                    }
                }

                androidx.compose.runtime.LaunchedEffect(filteredBlocks.size) {
                    if (filteredBlocks.isNotEmpty()) {
                        com.abhisheksati.mydoc.MainActivity.pageTurnEvents.collect { isNext ->
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

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.large, vertical = Spacing.medium)
                ) {
                    items(filteredBlocks) { block ->
                        when (block) {
                            is DocxBlock.Heading -> {
                                Spacer(modifier = Modifier.height(Spacing.medium))
                                val fontSize = when (block.level) {
                                    1 -> (24 * fontScale).sp
                                    2 -> (20 * fontScale).sp
                                    else -> (17 * fontScale).sp
                                }
                                Text(
                                    text = block.text,
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.padding(vertical = Spacing.extraSmall)
                                )
                            }

                            is DocxBlock.Paragraph -> {
                                val annotated = buildAnnotatedString {
                                    for (run in block.runs) {
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = if (run.isBold) FontWeight.Bold else FontWeight.Normal,
                                                fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                textDecoration = if (run.isUnderline) TextDecoration.Underline else TextDecoration.None,
                                                color = textColor,
                                                fontSize = (15 * fontScale).sp
                                            )
                                        ) {
                                            append(run.text)
                                        }
                                    }
                                }
                                Text(
                                    text = annotated,
                                    lineHeight = (22 * fontScale).sp,
                                    modifier = Modifier.padding(vertical = Spacing.extraSmall)
                                )
                            }

                            is DocxBlock.Bullet -> {
                                Row(modifier = Modifier.padding(vertical = Spacing.extraSmall)) {
                                    Text(
                                        text = "• ",
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        fontSize = (16 * fontScale).sp
                                    )
                                    val annotated = buildAnnotatedString {
                                        for (run in block.runs) {
                                            withStyle(
                                                SpanStyle(
                                                    fontWeight = if (run.isBold) FontWeight.Bold else FontWeight.Normal,
                                                    fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                    textDecoration = if (run.isUnderline) TextDecoration.Underline else TextDecoration.None,
                                                    color = textColor,
                                                    fontSize = (15 * fontScale).sp
                                                )
                                            ) {
                                                append(run.text)
                                            }
                                        }
                                    }
                                    Text(
                                        text = annotated,
                                        lineHeight = (22 * fontScale).sp
                                    )
                                }
                            }

                            is DocxBlock.Table -> {
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = secondaryTextColor.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(Spacing.small)
                                ) {
                                    for (row in block.rows) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            for (cell in row) {
                                                Text(
                                                    text = cell,
                                                    color = textColor,
                                                    fontSize = (13 * fontScale).sp,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(Spacing.extraSmall)
                                                )
                                            }
                                        }
                                        AppDivider()
                                    }
                                }
                                Spacer(modifier = Modifier.height(Spacing.small))
                            }
                        }
                    }
                }
            }

            val documentText = remember(docxData) {
                docxData?.blocks?.mapNotNull { block ->
                    when (block) {
                        is DocxBlock.Paragraph -> block.runs.joinToString("") { it.text }
                        is DocxBlock.Table -> block.rows.joinToString("\n") { row -> row.joinToString(" | ") }
                        is DocxBlock.Heading -> block.text
                        is DocxBlock.Bullet -> block.runs.joinToString("") { it.text }
                    }
                }?.joinToString("\n\n") ?: ""
            }

            com.abhisheksati.mydoc.ui.tts.DocumentTtsBar(
                ttsManager = ttsManager,
                documentText = documentText,
                isVisible = isTtsVisible,
                onClose = { isTtsVisible = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
