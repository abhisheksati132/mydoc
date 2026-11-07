package com.abhisheksati.mydoc.ui.search

import com.abhisheksati.mydoc.R
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.domain.model.DocumentFilter
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.ui.files.LibraryThumbnail

/**
 * Document search across the library, on the same Material 3 surface language as
 * the Files screen: tonal field, filter chips and thumbnail rows.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SpotlightSearchSheet(
    items: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onBrowseFolders: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredItems = remember(items, query, selectedFilter) {
        val q = query.trim().lowercase()
        items.filter { item ->
            val matchesFilter = when {
                selectedFilter == "Media" ->
                    item.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "mp4", "mp3", "mkv", "mov", "wav", "flac")
                else -> DocumentFilter.fromLabel(selectedFilter)?.matches(item.extension) ?: true
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
            .imePadding()
    ) {
        AppTopBar(title = stringResource(R.string.search_indexed_files), onBack = onDismiss,
            containerColor = scheme.background, isGlass = false)
        Text(stringResource(R.string.search_index_scope_hint), style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        FileSearchField(query, { query = it }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        if (onBrowseFolders != null) TextButton(onClick = { onDismiss(); onBrowseFolders() }, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(stringResource(R.string.home_browse_files))
        }

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

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.search_results),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            Text(
                text = pluralStringResource(R.plurals.search_result_count, filteredItems.size, filteredItems.size),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }

        if (filteredItems.isEmpty()) {
            AppEmptyState(
                title = stringResource(
                        if (items.isEmpty()) R.string.search_no_docs
                        else R.string.search_no_matches, query.ifBlank { selectedFilter }
                    ),
                message = if (items.isEmpty()) {
                    stringResource(R.string.search_docs_hint)
                } else {
                    stringResource(R.string.search_typo_hint)
                },
                actionLabel = if (query.isNotEmpty() || selectedFilter != "All") stringResource(R.string.search_clear_filters) else null,
                onAction = { query = ""; selectedFilter = "All" }
            )
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
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.files_item_meta, item.extension.uppercase(), item.formattedSize),
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
