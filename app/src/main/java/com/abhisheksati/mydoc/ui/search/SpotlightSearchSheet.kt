package com.abhisheksati.mydoc.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.components.horizontalScrollWithFade
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.LibraryItem

/**
 * Editorial Search Overlay:
 * Clean, distraction-free document search with authentic format badging and tabular metadata.
 */
@Composable
fun SpotlightSearchSheet(
    items: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        OverlaySystemBars()
        val colors = AppTheme.colors
        val typography = AppTheme.typography
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        var query by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        val filteredItems = remember(items, query, selectedFilter) {
            val q = query.trim().lowercase()
            items.filter { item ->
                val matchesFilter = when (selectedFilter) {
                    "PDF" -> item.extension.equals("pdf", ignoreCase = true)
                    "Word" -> item.extension.lowercase() in setOf("doc", "docx", "dot", "dotx", "odt", "rtf")
                    "Excel" -> item.extension.lowercase() in setOf("xls", "xlsx", "xlsm", "csv", "ods")
                    "PPT" -> item.extension.lowercase() in setOf("ppt", "pptx", "pps", "odp")
                    "Media" -> item.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "mp4", "mp3", "mkv", "mov", "wav", "flac")
                    else -> true
                }
                val matchesQuery = if (q.isEmpty()) true else {
                    item.name.lowercase().contains(q) ||
                    item.extension.lowercase().contains(q)
                }
                matchesFilter && matchesQuery
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 8 },
            exit = fadeOut() + slideOutVertically { it / 8 }
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.ink
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = AppRadius.cardShape,
                        color = colors.surface,
                        border = BorderStroke(0.5.dp, colors.line),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search documents, notes, media...",
                                        style = typography.body,
                                        color = colors.inkMuted
                                    )
                                }
                                BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    textStyle = typography.bodyMedium.copy(color = colors.ink),
                                    singleLine = true,
                                    cursorBrush = SolidColor(colors.accent)
                                )
                            }

                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                        tint = colors.inkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Pills
                val filterPills = listOf("All", "PDF", "Word", "Excel", "PPT", "Media")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScrollWithFade(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterPills.forEach { label ->
                        val isSelected = selectedFilter == label
                        Surface(
                            shape = AppRadius.pillShape,
                            color = if (isSelected) colors.ink else colors.surfaceContainer,
                            border = BorderStroke(0.5.dp, if (isSelected) colors.ink else colors.line),
                            modifier = Modifier
                                .clip(AppRadius.pillShape)
                                .clickable { selectedFilter = label }
                        ) {
                            Text(
                                text = label,
                                style = if (isSelected) typography.captionStrong else typography.caption,
                                color = if (isSelected) colors.background else colors.inkMuted,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Results Counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (query.isEmpty()) "ALL DOCUMENTS" else "SEARCH RESULTS",
                        style = typography.overline,
                        color = colors.inkMuted
                    )
                    Text(
                        text = "${filteredItems.size} items",
                        style = typography.micro.withTabularFigures(),
                        color = colors.inkMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Results List
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = colors.inkMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (query.isEmpty()) "No documents found" else "No matches for \"$query\"",
                                style = typography.bodyStrong,
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (query.isEmpty()) "Documents in your storage will appear here" else "Check for typos or try searching a different term",
                                style = typography.caption,
                                color = colors.inkMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = filteredItems,
                            key = { it.uri }
                        ) { item ->
                            val formatTint = DocumentFormatColors.getTint(item.extension)
                            Surface(
                                shape = AppRadius.cardShape,
                                color = colors.surface,
                                border = BorderStroke(0.5.dp, colors.line),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScaleEffect(onClick = {
                                        onDismiss()
                                        onItemClick(item)
                                    })
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(AppRadius.microShape)
                                            .background(formatTint.copy(alpha = if (AppTheme.isDark) 0.16f else 0.10f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getSearchFormatIcon(item.extension),
                                            contentDescription = null,
                                            tint = formatTint,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = formatMiddleTruncated(item.name, 36),
                                            style = typography.bodyStrong,
                                            color = colors.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.extension.uppercase(),
                                                style = typography.micro,
                                                color = formatTint
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "•", color = colors.inkMuted, style = typography.micro)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.formattedSize,
                                                style = typography.micro.withTabularFigures(),
                                                color = colors.inkMuted
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
    }
}

private fun getSearchFormatIcon(extension: String): ImageVector {
    return when (extension.lowercase()) {
        "pdf" -> Icons.Outlined.PictureAsPdf
        "doc", "docx", "dot", "dotx", "odt", "rtf" -> Icons.Outlined.Description
        "xls", "xlsx", "xlsm", "csv", "ods" -> Icons.Outlined.TableChart
        "ppt", "pptx", "pps", "odp" -> Icons.Outlined.Slideshow
        "txt", "log", "md", "json", "xml" -> Icons.AutoMirrored.Outlined.Article
        "jpg", "jpeg", "png", "webp", "gif", "svg" -> Icons.Outlined.Image
        "mp4", "mkv", "mov", "avi" -> Icons.Outlined.Videocam
        "mp3", "flac", "wav", "m4a", "ogg" -> Icons.Outlined.AudioFile
        "zip", "rar", "7z", "tar", "gz" -> Icons.Outlined.FolderZip
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}
