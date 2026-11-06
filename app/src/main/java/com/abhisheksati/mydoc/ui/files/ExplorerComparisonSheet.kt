package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.ExplorerComparison
import com.abhisheksati.mydoc.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerComparisonSheet(folders: List<File>, onDismiss: () -> Unit) {
    val available = remember(folders) { folders.distinctBy { it.absolutePath } }
    var left by remember { mutableStateOf(available.firstOrNull()) }
    var right by remember { mutableStateOf(available.getOrNull(1)) }
    var result by remember { mutableStateOf<FolderComparison?>(null) }
    var running by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var differencesOnly by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.compare_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.compare_help), style = MaterialTheme.typography.bodySmall)
                if (available.size < 2) Text(stringResource(R.string.compare_need_tabs))
            }
            item {
                ComparisonFolderSelector(R.string.compare_left, available, left, !running) { left = it; result = null; failed = false }
                ComparisonFolderSelector(R.string.compare_right, available, right, !running) { right = it; result = null; failed = false }
            }
            item {
                Button(onClick = {
                    val a = left ?: return@Button
                    val b = right ?: return@Button
                    running = true; failed = false; result = null
                    job = scope.launch {
                        try { result = ExplorerComparison().compare(a, b) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { failed = true }
                        finally { running = false }
                    }
                }, enabled = !running && left != null && right != null && left != right, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.compare_start)) }
                if (running) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.compare_running))
                    TextButton(onClick = { job?.cancel() }) { Text(stringResource(R.string.action_cancel)) }
                }
                if (failed) Text(stringResource(R.string.compare_failed), color = MaterialTheme.colorScheme.error)
            }
            result?.let { data ->
                item {
                    val counts = data.entries.groupingBy { it.status }.eachCount()
                    ComparisonStatus.entries.forEach { status -> Text(stringResource(R.string.compare_count, stringResource(comparisonLabel(status)), counts[status] ?: 0)) }
                    if (data.incomplete) Text(stringResource(R.string.compare_incomplete), color = MaterialTheme.colorScheme.error)
                    FilterChip(selected = differencesOnly, onClick = { differencesOnly = !differencesOnly }, label = { Text(stringResource(R.string.compare_differences_only)) })
                }
                val rows = data.entries.filter { !differencesOnly || it.status != ComparisonStatus.MATCHING }
                if (rows.isEmpty()) item { Text(stringResource(R.string.compare_no_rows)) }
                items(rows, key = { it.path }) { entry ->
                    ListItem(headlineContent = { Text(entry.path) },
                        supportingContent = { Text(stringResource(comparisonLabel(entry.status))) })
                }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tools_close)) } }
        }
    }
}

@Composable
private fun ComparisonFolderSelector(label: Int, folders: List<File>, selected: File?, enabled: Boolean, onSelect: (File) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && folders.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(selected?.absolutePath ?: stringResource(R.string.compare_choose))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            folders.forEach { folder -> DropdownMenuItem(text = { Text(folder.absolutePath) }, onClick = { expanded = false; onSelect(folder) }) }
        }
    }
}

private fun comparisonLabel(status: ComparisonStatus): Int = when (status) {
    ComparisonStatus.MATCHING -> R.string.compare_matching
    ComparisonStatus.CHANGED -> R.string.compare_changed
    ComparisonStatus.LEFT_ONLY -> R.string.compare_left_only
    ComparisonStatus.RIGHT_ONLY -> R.string.compare_right_only
    ComparisonStatus.UNKNOWN -> R.string.compare_unknown
}
