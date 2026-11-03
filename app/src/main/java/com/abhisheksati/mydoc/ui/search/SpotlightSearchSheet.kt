package com.abhisheksati.mydoc.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.ui.files.LibraryThumbnail

/**
 * Document search across the library, on the same Material 3 surface language as
 * the Files screen: tonal field, filter chips and thumbnail rows.
 */
@Composable
fun SpotlightSearchSheet(
    items: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

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
        }.distinctBy { it.identityKey }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "",
            onBack = onDismiss,
            containerColor = scheme.background,
            titleContent = {
                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search documents, notes, media…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                                singleLine = true,
                                cursorBrush = SolidColor(scheme.primary)
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
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        )

        val filterPills = listOf("All", "PDF", "Word", "Excel", "PPT", "Media")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterPills.forEach { label ->
                FilterChip(
                    selected = selectedFilter == label,
                    onClick = { selectedFilter = label },
                    label = { Text(label) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (query.isEmpty()) "All documents" else "Results",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            Text(
                text = if (filteredItems.size == 1) "1 item" else "${filteredItems.size} items",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (query.isEmpty()) "No documents found" else "No matches for \"$query\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (query.isEmpty()) {
                            "Documents in your storage will appear here"
                        } else {
                            "Check for typos or try a different term"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = filteredItems,
                    key = { it.identityKey }
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .clip(AppRadius.cardShape)
                            .clickable {
                                onDismiss()
                                onItemClick(item)
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LibraryThumbnail(
                            item = item,
                            modifier = Modifier.size(width = 44.dp, height = 52.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatMiddleTruncated(item.name, 36),
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.extension.uppercase()} · ${item.formattedSize}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
