package com.abhisheksati.mydoc.ui.files

import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.ExplorerPreferences

@Composable
fun SaveExplorerSearchDialog(onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    var label by rememberSaveable { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val valid = label.trim().length in 1..80 && label.none { it.isISOControl() }
    AppAlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.saved_search_save)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label, { label = it; failed = false }, label = { Text(stringResource(R.string.saved_search_label)) }, singleLine = true)
                if (failed) Text(stringResource(R.string.saved_search_save_failed), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { if (onSave(label.trim())) onDismiss() else failed = true }) { Text(stringResource(R.string.saved_search_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerSavedSearchesSheet(preferences: ExplorerPreferences, onDismiss: () -> Unit, onRun: (String) -> Unit) {
    val searches by preferences.savedSearches.collectAsState()
    var deleteFailed by remember { mutableStateOf(false) }
    AppModalSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.saved_searches_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.saved_searches_help), style = MaterialTheme.typography.bodySmall)
            }
            if (deleteFailed) item { Text(stringResource(R.string.saved_search_delete_failed), color = MaterialTheme.colorScheme.error) }
            if (searches.isEmpty()) item { Text(stringResource(R.string.saved_searches_empty)) }
            items(searches, key = { it.id }) { search ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(search.label, style = MaterialTheme.typography.titleMedium)
                    Text(search.directory, style = MaterialTheme.typography.bodySmall)
                    if (search.filters.name.isNotBlank()) Text(stringResource(R.string.saved_search_filter, stringResource(R.string.explorer_name), search.filters.name))
                    if (search.filters.extension.isNotBlank()) Text(stringResource(R.string.saved_search_filter, stringResource(R.string.explorer_extension), search.filters.extension))
                    if (search.filters.minBytes != null || search.filters.maxBytes != null) Text(stringResource(R.string.saved_search_bytes,
                        search.filters.minBytes?.toString() ?: stringResource(R.string.saved_search_no_limit),
                        search.filters.maxBytes?.toString() ?: stringResource(R.string.saved_search_no_limit)))
                    search.modifiedWithinDays?.let { Text(stringResource(R.string.saved_search_last_days, it)) }
                    if (search.filters.recursive) Text(stringResource(R.string.explorer_subfolders))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onRun(search.id) }) { Text(stringResource(R.string.saved_search_run)) }
                        IconButton(onClick = { deleteFailed = !preferences.deleteSearch(search.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.saved_search_delete, search.label))
                        }
                    }
                    HorizontalDivider()
                }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tools_close)) } }
        }
    }
}
