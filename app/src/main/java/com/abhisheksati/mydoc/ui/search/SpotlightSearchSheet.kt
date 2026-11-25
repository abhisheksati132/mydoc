package com.abhisheksati.mydoc.ui.search

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.abhisheksati.mydoc.R
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.animation.itemMotion
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.domain.model.DocumentFilter
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.ui.files.LibraryThumbnail

/** Media means every indexed photo/video/audio kind, not a hand-picked extension list that
 *  silently drops heic, gif, webm, m4a and friends. */
internal fun spotlightFilterMatches(item: LibraryItem, filter: String): Boolean = when {
    filter == "Media" -> item.kind != MediaKind.DOCUMENT
    else -> DocumentFilter.fromLabel(filter)?.matches(item.extension) ?: true
}

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
    onBrowseFolders: (() -> Unit)? = null,
    initialContents: Boolean = false
) {
    var showContents by rememberSaveable { mutableStateOf(initialContents) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var showOptions by rememberSaveable { mutableStateOf(false) }
    if (showContents) {
        com.abhisheksati.mydoc.discovery.DiscoveryScreen(onBack = { showContents = false }, onOpen = onItemClick)
        return
    }
    val scheme = MaterialTheme.colorScheme
    val reducedMotion = LocalReducedMotion.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as? com.abhisheksati.mydoc.MyDocApp
    val discoveryStore = remember(app) {
        app?.let { com.abhisheksati.mydoc.discovery.DiscoveryStore.get(it) }
    }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var contentHits by remember { mutableStateOf(emptyList<com.abhisheksati.mydoc.discovery.DiscoveredFile>()) }
    val normalizedQuery = query.trim()
    LaunchedEffect(normalizedQuery, selectedFilter, discoveryStore) {
        // Hits from the previous query must not stand in for the new one while the debounce
        // runs; the list showed the old query's content hits for those 350 ms.
        contentHits = emptyList()
        if (discoveryStore == null || normalizedQuery.length < 2 || selectedFilter == "Media") {
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(350)
        val hits = runCatching { discoveryStore.search(normalizedQuery, "", "") }
            .getOrDefault(emptyList())
            .filter { it.snippet.isNotBlank() }
            .filter { hit ->
                selectedFilter == "All" ||
                    DocumentFilter.fromLabel(selectedFilter)?.matches(hit.name.substringAfterLast('.', "")) == true
            }
            .take(40)
        // A slower search that finishes after the query changed must not overwrite the newer
        // results; the effect is cancelled, but assignment below is not a suspension point.
        kotlinx.coroutines.currentCoroutineContext().ensureActive()  // imported extension
        contentHits = hits
    }

    val filteredItems = remember(items, query, selectedFilter) {
        val q = query.trim().lowercase()
        items.filter { item ->
            val matchesFilter = spotlightFilterMatches(item, selectedFilter)
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FileSearchField(query, { query = it }, Modifier.weight(1f))
            Surface(shape = AppRadius.roundShape, color = scheme.surfaceContainerHigh) {
                IconToggleButton(checked = showOptions, onCheckedChange = { showOptions = it }) {
                    Icon(Icons.Outlined.Tune, stringResource(R.string.search_options), tint = scheme.onSurface)
                }
            }
        }
        AnimatedVisibility(showOptions,
            enter = fadeIn(if (reducedMotion) androidx.compose.animation.core.snap() else AppMotion.fadeFast),
            exit = fadeOut(if (reducedMotion) androidx.compose.animation.core.snap() else AppMotion.fadeFast)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.search_index_scope_hint), style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showContents = true }) { Text(stringResource(R.string.spotlight_search_subtitle)) }
                    if (onBrowseFolders != null) TextButton(onClick = { onDismiss(); onBrowseFolders() }) {
                        Text(stringResource(R.string.home_browse_files))
                    }
                }
            }
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
                    label = { Text(if (label == "All") stringResource(R.string.tools_filter_all) else label) }
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

        if (filteredItems.isEmpty() && contentHits.isEmpty()) {
            AppEmptyState(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                items(
                    items = filteredItems,
                    key = { it.identityKey }
                ) { item ->
                    SearchResultRow(item = item,
                        subtitle = stringResource(R.string.files_item_meta, item.extension.uppercase(java.util.Locale.ROOT), item.formattedSize),
                        onClick = { onDismiss(); onItemClick(item) }, modifier = itemMotion(reducedMotion))
                }

                if (contentHits.isNotEmpty()) {
                    item(key = "inside_documents_header") {
                        Text(
                            text = stringResource(R.string.search_inside_documents),
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        items = contentHits,
                        key = { "inside_${it.uri}" }
                    ) { hit ->
                        val hitItem = LibraryItem(hit.uri, hit.name, MediaKind.DOCUMENT, hit.mime, hit.size, hit.modified)
                        SearchResultRow(item = hitItem,
                            subtitle = stringResource(R.string.search_inside_hit, hit.page + 1, hit.snippet),
                            onClick = {
                                coroutineScope.launch {
                                    app?.documentRepository?.let { repo ->
                                        runCatching {
                                            val existing = repo.getViewState(hit.uri)
                                            repo.saveViewState((existing ?: ViewState(hit.uri)).copy(lastPage = hit.page))
                                        }
                                    }
                                    onDismiss()
                                    onItemClick(hitItem)
                                }
                            }, modifier = itemMotion(reducedMotion))
                    }
                }
            }
        }
    }
}
