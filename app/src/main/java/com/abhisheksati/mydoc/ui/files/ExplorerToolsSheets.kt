package com.abhisheksati.mydoc.ui.files

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.ExplorerArchive
import com.abhisheksati.mydoc.data.local.ExplorerFolderInsights
import com.abhisheksati.mydoc.data.local.ExplorerTransfers
import com.abhisheksati.mydoc.domain.model.*
import com.abhisheksati.mydoc.util.UserMessages
import kotlinx.coroutines.CancellationException
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerArchiveSheet(file: File, onDismiss: () -> Unit, onExtract: () -> Unit, canExtract: Boolean = true,
                         onOpenMember: ((String, ArchiveStamp?) -> Unit)? = null,
                         onExtractSelected: ((Set<String>, ArchiveStamp?) -> Unit)? = null) {
    var index by remember(file) { mutableStateOf<ArchiveIndex?>(null) }
    var failed by remember(file) { mutableStateOf(false) }
    var folder by rememberSaveable(file.absolutePath) { mutableStateOf("") }
    var query by rememberSaveable(file.absolutePath) { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    var selected by remember(file) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(file) {
        try { index = ExplorerArchive().read(file) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        BackHandler(enabled = folder.isNotEmpty()) { folder = folder.removeSuffix("/").substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }; query = "" }
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(folder) { listState.scrollToItem(0) }
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(file.name, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.archive_read_only), style = MaterialTheme.typography.bodySmall)
            }
            if (failed) item { Text(stringResource(R.string.archive_failed), color = MaterialTheme.colorScheme.error) }
            val loaded = index
            if (loaded == null && !failed) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (loaded != null) {
                item {
                    Text(stringResource(R.string.archive_summary, loaded.fileCount, displayBytes(loaded.totalBytes)))
                    if (onExtractSelected != null) {
                        Text(stringResource(R.string.zip_selection_help), style = MaterialTheme.typography.bodySmall)
                        if (selected.isNotEmpty()) TextButton(onClick = { selected = emptySet() }, enabled = canExtract) { Text(stringResource(R.string.zip_clear_selection, selected.size)) }
                        if (!canExtract) { Text(stringResource(R.string.zip_working)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    }
                    if (folder.isNotEmpty()) TextButton(onClick = { folder = folder.removeSuffix("/").substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }; query = "" }) { Text(stringResource(R.string.archive_up)) }
                    Text(if (folder.isEmpty()) "/" else folder, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                item { OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.archive_filter)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                val children = loaded.children(folder).filter { it.name.contains(query, ignoreCase = true) }
                if (children.isEmpty()) item { Text(stringResource(R.string.archive_empty), Modifier.padding(vertical = 20.dp)) }
                items(children, key = { it.path }) { node ->
                    val inherited = selected.any { it.endsWith('/') && node.path != it && node.path.startsWith(it) }
                    val selectionLabel = stringResource(R.string.zip_select_entry, node.path)
                    ListItem(
                        headlineContent = { Text(node.name) },
                        supportingContent = { Text(if (node.directory) stringResource(R.string.archive_folder) else displayBytes(node.bytes)) },
                        leadingContent = if (node.directory) { { Icon(Icons.Outlined.Folder, contentDescription = null) } } else null,
                        trailingContent = if (onExtractSelected != null) { {
                            Checkbox(checked = node.path in selected || inherited, enabled = canExtract && !inherited,
                                modifier = Modifier.semantics { contentDescription = selectionLabel },
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected.filterNot { node.directory && it.startsWith(node.path) }.toSet() + node.path else selected - node.path
                                })
                        } } else null,
                        modifier = when {
                            node.directory -> Modifier.clickable(enabled = canExtract) { folder = node.path; query = "" }
                            onOpenMember != null -> Modifier.clickable(enabled = canExtract) { onOpenMember(node.path, loaded.stamp) }
                            else -> Modifier
                        }
                    )
                }
                item { Button(onClick = { confirm = true }, enabled = canExtract && (loaded.fileCount > 0 || selected.isNotEmpty()), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (selected.isNotEmpty()) R.string.zip_extract_selected else R.string.archive_extract_all))
                } }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tools_close)) } }
        }
    }

    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text(stringResource(if (selected.isNotEmpty()) R.string.zip_extract_selected else R.string.archive_extract_all)) },
        text = { Text(if (selected.isNotEmpty()) stringResource(R.string.zip_selected_confirm, selected.size) else stringResource(R.string.archive_confirm, file.name)) },
        confirmButton = { TextButton(enabled = canExtract, onClick = {
            confirm = false
            if (selected.isNotEmpty()) onExtractSelected?.invoke(selected, index?.stamp) else onExtract()
        }) { Text(stringResource(if (selected.isNotEmpty()) R.string.zip_extract_selected else R.string.archive_extract_all)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerFolderInsightsSheet(directory: File, onDismiss: () -> Unit, onOpenFolder: (File) -> Unit) {
    var result by remember(directory) { mutableStateOf<FolderInsights?>(null) }
    var failed by remember(directory) { mutableStateOf(false) }
    LaunchedEffect(directory) {
        try { result = ExplorerFolderInsights().scan(directory) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(stringResource(R.string.folder_insights_title), style = MaterialTheme.typography.headlineSmall)
                Text(directory.absolutePath, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.folder_insights_scope), style = MaterialTheme.typography.bodySmall)
            }
            if (result == null && !failed) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.folder_insights_scanning)) }
            if (failed) item { Text(stringResource(R.string.folder_insights_failed), color = MaterialTheme.colorScheme.error) }
            result?.let { data ->
                item {
                    Text(displayBytes(data.totalBytes), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.folder_insights_summary, data.fileCount, data.folderCount))
                    if (data.incomplete) Text(stringResource(R.string.folder_insights_incomplete), color = MaterialTheme.colorScheme.error)
                }
                item { Text(stringResource(R.string.folder_insights_subfolders), style = MaterialTheme.typography.titleMedium) }
                items(data.folders.take(50), key = { "folder:${it.file.path}" }) { entry ->
                    Column(Modifier.fillMaxWidth().clickable { onOpenFolder(entry.file) }.padding(vertical = 8.dp)) {
                        Text(entry.file.name, style = MaterialTheme.typography.titleSmall)
                        Text(displayBytes(entry.bytes))
                        LinearProgressIndicator(progress = { if (data.totalBytes == 0L) 0f else (entry.bytes.toDouble() / data.totalBytes).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (data.folders.isEmpty()) item { Text(stringResource(R.string.folder_insights_no_subfolders)) }
                if (data.folders.size > 50) item { Text(stringResource(R.string.folder_insights_top_folders)) }
                item { Text(stringResource(R.string.folder_insights_largest), style = MaterialTheme.typography.titleMedium) }
                items(data.largestFiles, key = { "file:${it.file.path}" }) { entry ->
                    ListItem(headlineContent = { Text(entry.file.name) },
                        supportingContent = { Text(entry.file.relativeTo(directory).path) },
                        trailingContent = { Text(displayBytes(entry.bytes)) })
                }
                if (data.largestFiles.isEmpty()) item { Text(stringResource(R.string.folder_insights_no_files)) }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tools_close)) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerTransfersSheet(center: ExplorerTransfers, onDismiss: () -> Unit) {
    val transfers by center.state.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.transfers_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.transfers_scope), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = center::clearFinished, enabled = transfers.any { !it.running }) { Text(stringResource(R.string.transfers_clear)) }
            }
            if (transfers.isEmpty()) item { Text(stringResource(R.string.transfers_empty)) }
            items(transfers, key = { it.id }) { transfer ->
                val failureMessages = remember(transfer.failures) {
                    transfer.failures.map { failure -> failure.name to UserMessages.actionFailed(
                        if (transfer.operation == ClipboardOperation.COPY) R.string.transfers_copy else R.string.transfers_move, failure.cause) }
                }
                val status = when (transfer.status) {
                    TransferStatus.RUNNING -> R.string.transfers_running
                    TransferStatus.STOPPING -> R.string.transfers_stopping
                    TransferStatus.COMPLETED -> R.string.transfers_completed
                    TransferStatus.PARTIAL -> R.string.transfers_partial
                    TransferStatus.FAILED -> R.string.transfers_failed
                    TransferStatus.CANCELLED -> R.string.transfers_cancelled
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(if (transfer.operation == ClipboardOperation.COPY) R.string.transfers_copy else R.string.transfers_move), style = MaterialTheme.typography.titleMedium)
                    Text(transfer.destination.absolutePath, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(status))
                    Text(stringResource(R.string.transfers_counts, transfer.completed, transfer.total, transfer.failed))
                    if (transfer.currentName.isNotEmpty()) Text(transfer.currentName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(progress = { ((transfer.completed + transfer.failed).toFloat() / transfer.total.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    if (transfer.failed > 0) {
                        failureMessages.forEach { (name, message) ->
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(message.resolve(LocalContext.current), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(stringResource(R.string.transfers_failure_help), color = MaterialTheme.colorScheme.error)
                    }
                    if (transfer.running) TextButton(onClick = center::cancel, enabled = transfer.status != TransferStatus.STOPPING) { Text(stringResource(R.string.transfers_stop)) }
                    HorizontalDivider()
                }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tools_close)) } }
        }
    }
}

@Composable
private fun displayBytes(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)
