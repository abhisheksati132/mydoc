package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchiveLocationPicker(initial: File, folderOnly: Boolean, onDismiss: () -> Unit, onChoose: (File) -> Unit) {
    var folder by remember { mutableStateOf(if (initial.isDirectory) initial else initial.parentFile ?: initial) }
    var entries by remember { mutableStateOf<List<File>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(folder) {
        loading = true; failed = false
        entries = withContext(Dispatchers.IO) {
            runCatching {
                Files.newDirectoryStream(folder.toPath()).use { paths ->
                    paths.asSequence().take(5_000).filter { !Files.isSymbolicLink(it) }.map { it.toFile() }
                        .filter { !folderOnly || it.isDirectory }.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }).toList()
                }
            }.getOrElse { failed = true; emptyList() }
        }
        loading = false
    }
    AppModalSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(stringResource(if (folderOnly) R.string.arc_destination else R.string.arc_add), style = MaterialTheme.typography.headlineSmall)
                Text(folder.absolutePath, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { folder.parentFile?.let { folder = it } }, enabled = folder.parentFile != null) { Text(stringResource(R.string.archive_up)) }
                Button(onClick = { onChoose(folder) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.arc_use_folder)) }
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (failed) item { Text(stringResource(R.string.arc_location_failed), color = MaterialTheme.colorScheme.error) }
            items(entries, key = { it.absolutePath }) { file ->
                Column(Modifier.fillMaxWidth().clickable { if (file.isDirectory) folder = file else onChoose(file) }.padding(vertical = 12.dp)) {
                    Text(file.name, style = MaterialTheme.typography.titleSmall)
                    if (file.isDirectory) Text(stringResource(R.string.archive_folder), style = MaterialTheme.typography.bodySmall)
                }
            }
            item { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
        }
    }
}
