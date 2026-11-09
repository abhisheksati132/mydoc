package com.abhisheksati.mydoc.ui.files

import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.ExplorerRename
import com.abhisheksati.mydoc.domain.model.*
import java.io.File

@Composable
fun ExplorerSearchDialog(directory: File, initial: ExplorerSearchOptions?, onDismiss: () -> Unit, onSearch: (ExplorerSearchOptions) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var extension by rememberSaveable { mutableStateOf(initial?.extension.orEmpty()) }
    var minimum by rememberSaveable { mutableStateOf(initial?.minBytes?.div(1024)?.toString().orEmpty()) }
    var maximum by rememberSaveable { mutableStateOf(initial?.maxBytes?.div(1024)?.toString().orEmpty()) }
    var days by rememberSaveable { mutableStateOf(initial?.modifiedWithinDays?.toString() ?: initial?.modifiedAfter?.let { ((System.currentTimeMillis() - it) / 86_400_000L).coerceAtLeast(1).toString() }.orEmpty()) }
    var recursive by rememberSaveable { mutableStateOf(initial?.recursive ?: false) }
    fun bytes(text: String): Long? = text.toLongOrNull()?.takeIf { it in 0..(Long.MAX_VALUE / 1024) }?.times(1024)
    val dayCount = days.toLongOrNull()?.takeIf { it in 1..365_000 }
    val options = ExplorerSearchOptions(name, extension, bytes(minimum), bytes(maximum),
        if (days.isBlank()) null else dayCount?.let { System.currentTimeMillis() - it * 86_400_000L }, recursive, dayCount?.toInt())
    val valid = options.isValid && (minimum.isBlank() || bytes(minimum) != null) &&
        (maximum.isBlank() || bytes(maximum) != null) && (days.isBlank() || dayCount != null)
    AppAlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.explorer_advanced_search)) },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.explorer_search_scope, directory.absolutePath), style = MaterialTheme.typography.bodySmall)
                ExplorerField(name, { name = it }, R.string.explorer_name)
                ExplorerField(extension, { extension = it }, R.string.explorer_extension)
                ExplorerField(minimum, { minimum = it }, R.string.explorer_min_size, numeric = true)
                ExplorerField(maximum, { maximum = it }, R.string.explorer_max_size, numeric = true)
                ExplorerField(days, { days = it }, R.string.explorer_modified_days, numeric = true)
                ExplorerToggle(stringResource(R.string.explorer_subfolders), recursive) { recursive = it }
                if (!valid) Text(stringResource(R.string.explorer_invalid_filters), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSearch(options) }) { Text(stringResource(R.string.cd_search)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } })
}

@Composable
fun ExplorerRenameDialog(files: List<File>, onDismiss: () -> Unit, onApply: (ExplorerRenamePlan) -> Unit) {
    var prefix by rememberSaveable { mutableStateOf("") }
    var suffix by rememberSaveable { mutableStateOf("") }
    var find by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var numbered by rememberSaveable { mutableStateOf(false) }
    var preserveExtension by rememberSaveable { mutableStateOf(true) }
    var start by rememberSaveable { mutableStateOf("1") }
    var plan by remember { mutableStateOf<ExplorerRenamePlan?>(null) }
    var previewOptions by remember { mutableStateOf<ExplorerRenameOptions?>(null) }
    var failed by remember { mutableStateOf(false) }
    val options = ExplorerRenameOptions(prefix, suffix, find, replacement, numbered, start.toIntOrNull() ?: -1, preserveExtension)
    LaunchedEffect(files, options) {
        plan = null
        failed = false
        try { plan = ExplorerRename().preview(files, options); previewOptions = options }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    AppAlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.explorer_batch_rename)) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExplorerField(prefix, { prefix = it }, R.string.explorer_prefix)
                ExplorerField(suffix, { suffix = it }, R.string.explorer_suffix)
                ExplorerField(find, { find = it }, R.string.explorer_find)
                ExplorerField(replacement, { replacement = it }, R.string.explorer_replace)
                ExplorerToggle(stringResource(R.string.explorer_keep_extension), preserveExtension) { preserveExtension = it }
                ExplorerToggle(stringResource(R.string.explorer_numbering), numbered) { numbered = it }
                if (numbered) ExplorerField(start, { start = it }, R.string.explorer_start_number, numeric = true)
                HorizontalDivider()
                Text(stringResource(R.string.explorer_preview), style = MaterialTheme.typography.titleSmall)
                val preview = plan.takeIf { previewOptions == options }
                if (preview == null && !failed) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (failed) Text(stringResource(R.string.explorer_preview_failed), color = MaterialTheme.colorScheme.error)
                preview?.entries?.forEach { entry ->
                    Text(entry.source.name, style = MaterialTheme.typography.bodySmall)
                    Text("→ ${entry.target.name}", style = MaterialTheme.typography.bodyMedium,
                        color = if (entry.problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    if (entry.problem != null) Text(stringResource(R.string.explorer_rename_conflict), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(enabled = previewOptions == options && plan?.canApply == true, onClick = { plan?.takeIf { previewOptions == options }?.let(onApply) }) { Text(stringResource(R.string.action_rename)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } })
}

@Composable
private fun ExplorerField(value: String, onValueChange: (String) -> Unit, label: Int, numeric: Boolean = false) {
    OutlinedTextField(value, onValueChange, label = { Text(stringResource(label)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ExplorerToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // One named switch rather than two independently focusable controls.
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange, modifier = Modifier.semantics { contentDescription = label })
    }
}
