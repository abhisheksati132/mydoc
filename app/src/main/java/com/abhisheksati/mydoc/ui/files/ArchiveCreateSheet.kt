package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.archive.ArchiveJobs
import com.abhisheksati.mydoc.data.archive.ArchiveRequest
import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.domain.archive.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveCreateSheet(sources: List<File>, parent: File, onDismiss: () -> Unit, onQueued: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var name by rememberSaveable { mutableStateOf(if (sources.size == 1) sources.first().nameWithoutExtension else "Archive") }
    var destination by rememberSaveable { mutableStateOf(parent.absolutePath) }
    var format by rememberSaveable { mutableStateOf(ArchiveFormat.ZIP) }
    var compression by rememberSaveable { mutableStateOf(ArchiveCompression.BALANCED) }
    var password by remember { mutableStateOf("") }
    var encryptNames by rememberSaveable { mutableStateOf(false) }
    var solid by rememberSaveable { mutableStateOf(true) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var split by rememberSaveable { mutableStateOf("") }
    var dictionary by rememberSaveable { mutableIntStateOf(32) }
    var threads by rememberSaveable { mutableIntStateOf(2) }
    var invalid by remember { mutableStateOf(false) }
    var chooseFolder by remember { mutableStateOf(false) }
    AppModalSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text(stringResource(R.string.arc_create), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.arc_create_hint), style = MaterialTheme.typography.bodyMedium)
            }
            item { OutlinedTextField(name, { name = it; invalid = false }, label = { Text(stringResource(R.string.arc_name)) }, suffix = { Text(".${format.extension}") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { ArchiveChoice(stringResource(R.string.arc_format), format, ArchiveFormat.entries, { it.extension.uppercase() }) { format = it; if (!it.encryption) password = ""; if (it != ArchiveFormat.SEVEN_ZIP) encryptNames = false } }
            item {
                val labels = listOf(R.string.arc_store, R.string.arc_fast, R.string.arc_balanced, R.string.arc_maximum).map { stringResource(it) }
                ArchiveChoice(stringResource(R.string.arc_compression), compression, ArchiveCompression.entries, { labels[it.ordinal] }) { compression = it; dictionary = it.dictionaryMiB }
            }
            item {
                OutlinedTextField(destination, { destination = it; invalid = false }, label = { Text(stringResource(R.string.arc_destination)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { chooseFolder = true }) { Text(stringResource(R.string.arc_choose_folder)) }
            }
            if (format.encryption) item {
                OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.arc_password)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                if (format == ArchiveFormat.SEVEN_ZIP && password.isNotEmpty()) ArchiveToggle(stringResource(R.string.arc_encrypt_names), encryptNames) { encryptNames = it }
            }
            item { TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.arc_advanced)) } }
            if (advanced) {
                if (format == ArchiveFormat.SEVEN_ZIP) item {
                    ArchiveToggle(stringResource(R.string.arc_solid), solid) { solid = it }
                    val label = stringResource(R.string.arc_dictionary, dictionary)
                    Text(label)
                    Slider(dictionary.toFloat(), { dictionary = it.toInt() }, valueRange = 8f..64f, steps = 6, modifier = Modifier.semantics { contentDescription = label })
                }
                item {
                    val label = stringResource(R.string.arc_threads, threads)
                    Text(label)
                    Slider(threads.toFloat(), { threads = it.toInt() }, valueRange = 1f..4f, steps = 2, modifier = Modifier.semantics { contentDescription = label })
                }
                item {
                    OutlinedTextField(split, { split = it.filter(Char::isDigit).take(6) }, label = { Text(stringResource(R.string.arc_split)) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.arc_split_help), style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Text(stringResource(R.string.arc_safe_destination), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.arc_limits), style = MaterialTheme.typography.bodySmall)
                if (invalid) Text(stringResource(R.string.arc_invalid), color = MaterialTheme.colorScheme.error)
            }
            if (sources.size > 1 && sources.all { it.isSupportedArchive() }) item {
                for (checkOnly in listOf(false, true)) {
                    val batchTitle = stringResource(if (checkOnly) R.string.arc_batch_test else R.string.arc_batch_extract)
                    OutlinedButton(onClick = {
                        val secret = password.takeIf { it.isNotEmpty() }?.toCharArray()
                        try {
                            val center = ArchiveJobs.get(context)
                            require(File(destination).isDirectory && center.state.value.count { it.active } + sources.size <= 20)
                            sources.forEach { source ->
                                val request = if (checkOnly) ArchiveRequest.Test(source) else ArchiveRequest.Extract(source, File(destination))
                                center.submit(resources.getString(R.string.arc_task_title, batchTitle, source.name), request, secret)
                            }
                            password = ""; onQueued()
                        } catch (_: Exception) { invalid = true } finally { secret?.fill('\u0000') }
                    }, modifier = Modifier.fillMaxWidth()) { Text(batchTitle) }
                }
            }
            item {
                val title = stringResource(R.string.arc_create)
                Button(onClick = {
                    val secret = password.takeIf { it.isNotEmpty() }?.toCharArray()
                    try {
                        require(name.isNotBlank() && name.none { it.isISOControl() || it in "/\\:" } && name !in listOf(".", ".."))
                        require(File(destination).isDirectory && (split.isBlank() || (split.toLongOrNull() ?: 0) >= 1))
                        require(secret == null || secret.size <= 1024 && secret.none { it == '\n' || it == '\r' })
                        val finalName = if (name.endsWith(".${format.extension}", true)) name else "$name.${format.extension}"
                        require(finalName.toByteArray(Charsets.UTF_8).size <= 200)
                        val options = ArchiveCreateOptions(format, compression, solid, encryptNames && secret != null,
                            split.toLongOrNull()?.times(1024L * 1024), dictionary, threads)
                        ArchiveJobs.get(context).submit(resources.getString(R.string.arc_task_title, title, finalName), ArchiveRequest.Create(sources, File(destination), finalName, options), secret)
                        password = ""
                        onQueued()
                    } catch (_: Exception) { invalid = true } finally { secret?.fill('\u0000') }
                }, enabled = sources.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.arc_queue)) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    }
    if (chooseFolder) ArchiveLocationPicker(File(destination), true, { chooseFolder = false }) { destination = it.absolutePath; chooseFolder = false }
}

@Composable internal fun ArchiveToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange = null)
    }
}

@Composable internal fun <T> ArchiveChoice(label: String, selected: T, values: List<T>, display: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${display(selected)}") }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value -> DropdownMenuItem(text = { Text(display(value)) }, onClick = { onSelect(value); expanded = false }) }
        }
    }
}
