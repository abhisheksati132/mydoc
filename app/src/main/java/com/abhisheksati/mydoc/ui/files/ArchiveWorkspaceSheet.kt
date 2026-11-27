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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.archive.*
import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.domain.archive.*
import kotlinx.coroutines.*
import java.io.File

internal fun File.isSupportedArchive(): Boolean = extension.lowercase() in setOf(
    "zip", "7z", "rar", "tar", "gz", "gzip", "bz2", "bzip2", "xz", "tgz", "tbz", "tbz2", "txz", "jar", "cab", "iso") ||
    extension.matches(Regex("\\d{3,}|[r-zR-Z]\\d{2}"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveWorkspaceSheet(file: File, onDismiss: () -> Unit, onOpenMember: (File) -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var listing by remember(file) { mutableStateOf<ArchiveListing?>(null) }
    var problem by remember(file) { mutableStateOf<ArchiveProblem?>(null) }
    var invalid by remember(file) { mutableStateOf(false) }
    var previewTooLarge by remember(file) { mutableStateOf(false) }
    var password by remember(file) { mutableStateOf("") }
    var loading by remember(file) { mutableStateOf(true) }
    var reload by remember(file) { mutableIntStateOf(0) }
    var folder by rememberSaveable(file.absolutePath) { mutableStateOf("") }
    var query by rememberSaveable(file.absolutePath) { mutableStateOf("") }
    var sortSize by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable(file.absolutePath, stateSaver = StringSetSaver) { mutableStateOf(emptySet<String>()) }
    var destination by rememberSaveable(file.absolutePath) { mutableStateOf(file.parentFile?.absolutePath.orEmpty()) }
    var conflict by rememberSaveable { mutableStateOf(ArchiveConflictPolicy.NEW_FOLDER) }
    var jobsOpen by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var replaceExisting by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }
    val center = remember { ArchiveJobs.get(context) }
    val jobs by center.state.collectAsState()
    LaunchedEffect(jobs.count { it.status == ArchiveJobStatus.COMPLETED }) { onChanged() }
    LaunchedEffect(file, reload) {
        loading = true; problem = null; invalid = false
        val secret = password.takeIf { it.isNotEmpty() }?.toCharArray()
        try { listing = NativeArchiveEngine(context).list(file, secret) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { problem = (error as? ArchiveFailure)?.problem ?: ArchiveProblem.ENGINE }
        finally { secret?.fill('\u0000'); loading = false }
    }
    fun enqueue(title: String, request: ArchiveRequest) {
        val secret = password.takeIf { it.isNotEmpty() }?.toCharArray()
        try { center.submit(resources.getString(R.string.arc_task_title, title, file.name), request, secret); jobsOpen = true }
        catch (_: Exception) { invalid = true }
        finally { secret?.fill('\u0000') }
    }
    fun up() { folder = folder.substringBeforeLast('/', ""); query = "" }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(folder) { listState.scrollToItem(0) }
    AppModalSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        BackHandler(enabled = folder.isNotEmpty()) { up() }
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding().padding(horizontal = 20.dp), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(file.name, style = MaterialTheme.typography.headlineSmall)
                listing?.let { Text("${it.type.uppercase()} · ${it.files.size} · ${Formatter.formatShortFileSize(context, it.totalBytes)}", style = MaterialTheme.typography.bodyMedium) }
                TextButton(onClick = { jobsOpen = true }) { Text(stringResource(R.string.arc_jobs)) }
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (problem != null) item {
                Text(stringResource(archiveProblemText(problem)), color = MaterialTheme.colorScheme.error)
                if (problem == ArchiveProblem.PASSWORD_REQUIRED || problem == ArchiveProblem.WRONG_PASSWORD) {
                    OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.arc_password_required)) },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = { reload++ }) { Text(stringResource(if (problem == ArchiveProblem.PASSWORD_REQUIRED || problem == ArchiveProblem.WRONG_PASSWORD) R.string.arc_unlock else R.string.arc_reload)) }
            }
            listing?.takeIf { problem == null }?.let { loaded ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val title = stringResource(R.string.arc_test)
                        OutlinedButton(onClick = { enqueue(title, ArchiveRequest.Test(file)) }, enabled = !loading) { Text(title) }
                        TextButton(onClick = { sortSize = !sortSize }) { Text(stringResource(if (sortSize) R.string.arc_size else R.string.arc_sort)) }
                    }
                    if (loaded.encrypted) OutlinedTextField(password, { password = it },
                        label = { Text(stringResource(R.string.arc_password_required)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.arc_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (folder.isNotEmpty()) TextButton(onClick = { up() }) { Text(stringResource(R.string.archive_up)) }
                    Text(if (folder.isEmpty()) "/" else folder, style = MaterialTheme.typography.bodySmall)
                    if (selected.isNotEmpty()) TextButton(onClick = { selected = emptySet() }) { Text(stringResource(R.string.zip_clear_selection, selected.size)) }
                }
                val children = loaded.members.filter {
                    if (query.isNotBlank()) it.path.contains(query, true)
                    else it.path.substringBeforeLast('/', "") == folder
                }.sortedWith(compareBy<ArchiveMember> { !it.directory }.thenBy { if (sortSize) -it.bytes else 0 }.thenBy { it.name.lowercase() })
                if (children.isEmpty()) item { Text(stringResource(R.string.archive_empty), Modifier.padding(vertical = 20.dp)) }
                items(children, key = { it.path }) { member ->
                    val inherited = selected.any { ancestor -> member.path.startsWith("$ancestor/") }
                    val label = stringResource(R.string.arc_select_entry, member.path)
                    ListItem(headlineContent = { Text(member.name) },
                        supportingContent = { Text(if (query.isNotBlank()) member.path else if (member.directory) stringResource(R.string.archive_folder) else Formatter.formatShortFileSize(context, member.bytes)) },
                        leadingContent = if (member.directory) { { Icon(Icons.Outlined.Folder, null) } } else null,
                        trailingContent = { Checkbox(member.path in selected || inherited, onCheckedChange = { checked ->
                            selected = if (checked) selected.filterNot { it.startsWith("${member.path}/") }.toSet() + member.path else selected - member.path
                        }, enabled = !loading && !inherited, modifier = Modifier.semantics { contentDescription = label }) },
                        modifier = Modifier.clickable(enabled = !loading) {
                            if (member.directory) { folder = member.path; query = "" }
                            else if (member.bytes > 32L * 1024 * 1024) { previewTooLarge = true }
                            else scope.launch {
                                loading = true
                                val secret = password.takeIf { it.isNotEmpty() }?.toCharArray()
                                try {
                                    val root = File(context.cacheDir, "shared/archive-previews").apply { check(isDirectory || mkdirs()) }
                                    withContext(Dispatchers.IO) {
                                        val existing = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                                        require(existing <= 128L * 1024 * 1024 - member.bytes) { "Preview cache is full" }
                                    }
                                    val output = NativeArchiveEngine(context).extract(file, root, setOf(member.path), secret)
                                    onOpenMember(File(output, member.path))
                                } catch (error: CancellationException) { throw error }
                                catch (error: Exception) { problem = (error as? ArchiveFailure)?.problem ?: ArchiveProblem.ENGINE }
                                finally { secret?.fill('\u0000'); loading = false }
                            }
                        })
                }
                item {
                    OutlinedTextField(destination, { destination = it }, label = { Text(stringResource(R.string.arc_destination)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { picker = "destination" }) { Text(stringResource(R.string.arc_choose_folder)) }
                    val conflictLabels = listOf(R.string.arc_new_folder, R.string.arc_skip, R.string.arc_keep_both).map { stringResource(it) }
                    ArchiveChoice(stringResource(R.string.arc_conflicts), conflict, ArchiveConflictPolicy.entries, { conflictLabels[it.ordinal] }) { conflict = it }
                    if (conflict != ArchiveConflictPolicy.NEW_FOLDER) Text(stringResource(R.string.arc_merge_help), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.arc_safe_destination), style = MaterialTheme.typography.bodySmall)
                    val title = stringResource(R.string.arc_extract)
                    Button(onClick = {
                        if (File(destination).isDirectory) enqueue(title, ArchiveRequest.Extract(file, File(destination), selected.takeIf { it.isNotEmpty() }, conflict))
                        else invalid = true
                    }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selected.isEmpty()) stringResource(R.string.archive_extract_all) else stringResource(R.string.arc_extract_selection, selected.size))
                    }
                }
                if (loaded.type.lowercase() in setOf("zip", "7z") && !Regex(".*\\.\\d{3,}").matches(file.name)) item {
                    Text(stringResource(R.string.arc_edit), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.arc_edit_help), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { editMode = "remove" }, enabled = selected.isNotEmpty() && !loading) { Text(stringResource(R.string.arc_remove)) }
                    TextButton(onClick = { editText = selected.singleOrNull().orEmpty(); editMode = "rename" }, enabled = selected.size == 1 && !loading) { Text(stringResource(R.string.arc_rename)) }
                    TextButton(onClick = { picker = "add" }, enabled = !loading) { Text(stringResource(R.string.arc_add)) }
                }
            }
            if (invalid) item { Text(stringResource(R.string.arc_invalid), color = MaterialTheme.colorScheme.error) }
            if (previewTooLarge) item { Text(stringResource(R.string.arc_preview_large), color = MaterialTheme.colorScheme.error) }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tools_close)) } }
        }
    }
    if (jobsOpen) ArchiveJobsSheet { jobsOpen = false }
    if (picker != null) ArchiveLocationPicker(File(destination), picker == "destination", { picker = null }) {
        if (picker == "destination") destination = it.absolutePath else { editText = it.absolutePath; replaceExisting = false; editMode = "add" }
        picker = null
    }
    if (editMode != null) AppAlertDialog(onDismissRequest = { editMode = null }, title = { Text(stringResource(R.string.arc_edit)) },
        text = { Column {
            Text(stringResource(R.string.arc_edit_help))
            if (editMode != "remove") OutlinedTextField(editText, { editText = it; replaceExisting = false }, label = { Text(stringResource(if (editMode == "add") R.string.arc_add_path else R.string.arc_new_path)) }, singleLine = true)
            else Text(stringResource(R.string.arc_selected, selected.size))
            if (editMode == "add" && listing?.members?.any { it.path == File(editText).name } == true) ArchiveToggle(stringResource(R.string.arc_replace_in_copy), replaceExisting) { replaceExisting = it }
        } }, confirmButton = {
            val title = stringResource(R.string.arc_edit)
            TextButton(enabled = editMode != "add" || listing?.members?.none { it.path == File(editText).name } != false || replaceExisting, onClick = {
                try {
                    require(File(destination).isDirectory)
                    val request = when (editMode) {
                        "remove" -> ArchiveRequest.Edit(file, File(destination), remove = selected)
                        "rename" -> { ArchiveListingParser.safePath(editText); ArchiveRequest.Edit(file, File(destination), renames = mapOf(selected.single() to editText)) }
                        else -> {
                            val incoming = File(editText)
                            require(incoming.exists())
                            ArchiveRequest.Edit(file, File(destination), remove = if (replaceExisting) setOf(incoming.name) else emptySet(), additions = listOf(incoming))
                        }
                    }
                    enqueue(title, request); editMode = null
                } catch (_: Exception) { invalid = true; editMode = null }
            }) { Text(stringResource(R.string.arc_queue)) }
        }, dismissButton = { TextButton(onClick = { editMode = null }) { Text(stringResource(R.string.action_cancel)) } })
}
