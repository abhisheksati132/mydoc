package com.abhisheksati.mydoc.ui.viewer.text

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.ui.theme.Spacing
import com.abhisheksati.mydoc.ui.viewer.controls.PrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var readingMode by remember { mutableStateOf(ReadingMode.NORMAL) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isMenuOpen by remember { mutableStateOf(false) }
    var fontScale by remember { mutableFloatStateOf(1.0f) }
    var showLineNumbers by remember { mutableStateOf(true) }

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(document.uri)
                val inputStream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, document.displayName)
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

    val bgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color.Black
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.background
    }

    val textColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color.White
        ReadingMode.SEPIA -> Color(0xFF3A3125)
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.onBackground
    }

    val secondaryTextColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFAAAAAA)
        ReadingMode.SEPIA -> Color(0xFF5A4E3A)
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val listState = rememberLazyListState()

    // Volume key line scrolling
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            com.abhisheksati.mydoc.MainActivity.pageTurnEvents.collect { isNext ->
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
                                ReadingMode.SEPIA -> ReadingMode.NORMAL
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Brightness4,
                                contentDescription = "Toggle Reading Mode",
                                tint = if (readingMode != ReadingMode.NORMAL) MaterialTheme.colorScheme.primary else secondaryTextColor
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
                                onDismissRequest = { isMenuOpen = false }
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
                                    text = { Text(if (showLineNumbers) "Hide Line Numbers" else "Show Line Numbers") },
                                    onClick = {
                                        isMenuOpen = false
                                        showLineNumbers = !showLineNumbers
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zoom In") },
                                    onClick = {
                                        isMenuOpen = false
                                        fontScale = (fontScale + 0.15f).coerceAtMost(2.0f)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zoom Out") },
                                    onClick = {
                                        isMenuOpen = false
                                        fontScale = (fontScale - 0.15f).coerceAtLeast(0.7f)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor)
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
                            placeholder = { Text("Search text…") },
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
                        text = errorMessage ?: "Error reading file",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(Spacing.large)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                ) {
                    itemsIndexed(lines) { index, line ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (showLineNumbers) {
                                Text(
                                    text = "${index + 1}",
                                    color = secondaryTextColor.copy(alpha = 0.5f),
                                    fontSize = (12 * fontScale).sp,
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
                                fontSize = (14 * fontScale).sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = (20 * fontScale).sp
                            )
                        }
                    }
                }
            }
        }
    }
}
