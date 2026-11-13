package com.abhisheksati.mydoc.discovery

import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiscoveryScreen(onBack: ()->Unit,onOpen: (LibraryItem)->Unit) {
    val context=LocalContext.current
    val resources=LocalResources.current
    val app=context.applicationContext as MyDocApp
    val store=remember { DiscoveryStore.get(app) }
    val scope=rememberCoroutineScope()
    val revision by store.revision.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var extension by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf(resources.getString(R.string.discovery_initial_message)) }
    var results by remember { mutableStateOf(emptyList<DiscoveredFile>()) }
    var collections by remember { mutableStateOf(emptyList<SmartCollection>()) }
    var working by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var tagsForUri by rememberSaveable { mutableStateOf<String?>(null) }
    var tagsForName by rememberSaveable { mutableStateOf("") }
    var tagsValue by rememberSaveable { mutableStateOf("") }
    var saveDialog by remember { mutableStateOf(false) }
    var collectionName by rememberSaveable { mutableStateOf("") }
    var clearDialog by remember { mutableStateOf(false) }
    BackHandler {
        job?.cancel()
        onBack()
    }
    DisposableEffect(Unit) {
        onDispose {
            job?.cancel()
        }
    }
    // Folder roots are persisted, but the stored index goes stale as files change on disk;
    // re-index them on entry instead of waiting for a manual Refresh.
    LaunchedEffect(Unit) {
        val roots = store.folders()
        if (roots.isNotEmpty() && !working) {
            working = true
            message = resources.getString(R.string.discovery_reindexing_folders)
            job = scope.launch {
                try {
                    DocumentIndexer(app, store).selected(roots.map { Uri.parse(it) }, true) { status ->
                        scope.launch { message = status }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    message = resources.getString(R.string.discovery_refresh_failed, e.message.orEmpty())
                } finally {
                    working = false
                }
            }
        }
    }
    LaunchedEffect(query,tag,extension,revision) {
        delay(180)
        try { results=store.search(query.trim(),tag.trim().lowercase(java.util.Locale.ROOT),extension);collections=store.collections() }
        catch(e: Exception) { if(e is CancellationException)throw e;message=resources.getString(R.string.discovery_search_failed, e.message.orEmpty()) }
    }
    fun mutate(operation: suspend ()->Unit) {
        scope.launch {
            try { operation() } catch(e: CancellationException) { throw e }
            catch(e: Exception) { message=resources.getString(R.string.discovery_save_failed, e.message.orEmpty()) }
        }
    }
    var relinkTarget by remember { mutableStateOf<DiscoveredFile?>(null) }
    fun index(uris: List<Uri>,folder: Boolean) {
        if(uris.isEmpty() || working)return
        uris.forEach { uri -> runCatching { context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        working=true
        job=scope.launch {
            try { DocumentIndexer(app,store).selected(uris,folder) { status -> scope.launch { message=status } } }
            catch(cancelled: CancellationException) { message=resources.getString(R.string.discovery_indexing_stopped);throw cancelled }
            catch(e: Exception) { message=resources.getString(R.string.discovery_index_failed, e.message.orEmpty()) }
            finally { working=false }
        }
    }
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { index(it,false) }
    val folder=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        it?.let { uri ->
            scope.launch { store.addFolder(uri.toString()) }
            index(listOf(uri), true)
        }
    }
    val relink=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val old=relinkTarget
        if(uri!=null && old!=null && !working) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            working=true
            job=scope.launch {
                try {
                    DocumentIndexer(app,store).selected(listOf(uri),false) { status->scope.launch { message=status } }
                    val added=store.file(uri.toString())
                    check(added!=null && !added.status.startsWith("Unavailable")) { resources.getString(R.string.discovery_relink_unreadable) }
                    store.setTags(uri.toString(),old.tags)
                    if(old.uri!=uri.toString())store.remove(old.uri)
                    message=resources.getString(R.string.discovery_relink_done)
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) { message=e.message ?: resources.getString(R.string.discovery_relink_failed) }
                finally { working=false;relinkTarget=null }
            }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        AppTopBar(title=stringResource(R.string.discovery_title),onBack=onBack,isGlass=false)
        Text(stringResource(R.string.discovery_snapshots_disclaimer),Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp)) {
            TextButton(onClick={pick.launch(arrayOf("*/*"))},enabled=!working) { Text(stringResource(R.string.discovery_add_files)) }
            TextButton(onClick={folder.launch(null)},enabled=!working) { Text(stringResource(R.string.discovery_add_folder)) }
            TextButton(onClick={
                working=true
                job=scope.launch {
                    try {
                        val folderUris = store.folders().map { Uri.parse(it) }
                        val fileUris = store.files().map { Uri.parse(it.uri) }
                        if (folderUris.isNotEmpty()) {
                            DocumentIndexer(app,store).selected(folderUris, true) { status -> scope.launch { message = status } }
                        }
                        if (fileUris.isNotEmpty()) {
                            DocumentIndexer(app,store).selected(fileUris, false) { status -> scope.launch { message = status } }
                        }
                    }
                    catch(e: CancellationException) { throw e }
                    catch(e: Exception) { message=resources.getString(R.string.discovery_refresh_failed, e.message.orEmpty()) }
                    finally { working=false }
                }
            },enabled=!working) { Text(stringResource(R.string.menu_refresh)) }
            if(working) TextButton(onClick={job?.cancel()}) { Text(stringResource(R.string.action_stop)) }
            TextButton(onClick={clearDialog=true},enabled=!working) { Text(stringResource(R.string.discovery_clear_local_library)) }
        }
        if(working) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message,Modifier.padding(horizontal=16.dp,vertical=4.dp),style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(query,{query=it.take(300);collectionName=""},label={Text(stringResource(R.string.discovery_words_search_label))},singleLine=true,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(tag,{tag=it.take(40)},label={Text(stringResource(R.string.discovery_tag_label))},singleLine=true,modifier=Modifier.weight(1f))
            OutlinedTextField(extension,{extension=it.take(16)},label={Text(stringResource(R.string.discovery_file_type_label))},singleLine=true,modifier=Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp)) {
            TextButton(onClick={query="";tag="";extension="";collectionName=""}) { Text(stringResource(R.string.discovery_all_selected_files)) }
            TextButton(onClick={saveDialog=true}) { Text(stringResource(R.string.discovery_save_smart_collection)) }
            for(rule in collections) InputChip(selected=collectionName==rule.name,onClick={
                collectionName=rule.name;query=rule.query;tag=rule.tag;extension=rule.extension
            },label={Text(rule.name)},modifier=Modifier.padding(horizontal=4.dp))
        }
        if(collectionName.isNotEmpty()) TextButton(onClick={mutate { store.deleteCollection(collectionName);collectionName="" }}) { Text(stringResource(R.string.discovery_delete_collection, collectionName)) }
        Text(stringResource(R.string.discovery_results_count, results.size),Modifier.padding(horizontal=16.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium)
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(results.isEmpty()) item { Text(stringResource(R.string.discovery_no_matching_files),Modifier.padding(16.dp)) }
            items(results,key={it.uri}) { file ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(file.name,style=MaterialTheme.typography.titleSmall)
                        Text(file.uri,maxLines=2,style=MaterialTheme.typography.bodySmall)
                        if(file.snippet.isNotEmpty()) Text(stringResource(R.string.discovery_snippet_page, file.page+1, file.snippet),Modifier.padding(vertical=6.dp))
                        Text(stringResource(R.string.discovery_snapshot_line, file.status, java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(file.indexedAt))),style=MaterialTheme.typography.bodySmall)
                        if(file.tags.isNotEmpty()) Text(stringResource(R.string.discovery_tags_prefix, file.tags),style=MaterialTheme.typography.labelMedium)
                        androidx.compose.foundation.layout.FlowRow {
                            TextButton(onClick={scope.launch {
                                try {
                                    val reachable=withContext(Dispatchers.IO) { context.contentResolver.openAssetFileDescriptor(Uri.parse(file.uri),"r")?.use { true } ?: false }
                                    check(reachable) { resources.getString(R.string.discovery_file_missing) }
                                    val kind=when { file.mime.startsWith("image/")->MediaKind.PHOTO;file.mime.startsWith("audio/")->MediaKind.AUDIO;file.mime.startsWith("video/")->MediaKind.VIDEO;else->MediaKind.DOCUMENT }
                                    if(kind==MediaKind.DOCUMENT && file.snippet.isNotEmpty()) {
                                        val existing = app.documentRepository.getViewState(file.uri)
                                        val updated = existing?.copy(lastPage = file.page) ?: ViewState(file.uri, lastPage = file.page)
                                        app.documentRepository.saveViewState(updated)
                                    }
                                    onOpen(LibraryItem(file.uri,file.name,kind,file.mime,file.size,file.modified))
                                } catch(e: Exception) { if(e is CancellationException)throw e;message=e.message ?: resources.getString(R.string.discovery_open_failed) }
                            }}) { Text(stringResource(R.string.action_open)) }
                            TextButton(onClick={relinkTarget=file;relink.launch(arrayOf("*/*"))},enabled=!working) { Text(stringResource(R.string.discovery_relink)) }
                            TextButton(onClick={tagsForUri=file.uri;tagsForName=file.name;tagsValue=file.tags}) { Text(stringResource(R.string.discovery_edit_tags)) }
                            TextButton(onClick={mutate { store.remove(file.uri) }}) { Text(stringResource(R.string.discovery_remove_from_index)) }
                        }
                    }
                }
            }
        }
    }
    if(tagsForUri!=null) AppAlertDialog(onDismissRequest={tagsForUri=null},title={Text(stringResource(R.string.discovery_tags_for_title, tagsForName))},text={
        OutlinedTextField(tagsValue,{tagsValue=it.take(1000)},label={Text(stringResource(R.string.discovery_comma_separated_tags))},supportingText={Text(stringResource(R.string.discovery_tags_supporting_text))})
    },confirmButton={TextButton(onClick={val uri=tagsForUri!!;mutate { store.setTags(uri,tagsValue) };tagsForUri=null}) { Text(stringResource(R.string.action_save)) }},dismissButton={TextButton(onClick={tagsForUri=null}) { Text(stringResource(R.string.action_cancel)) }})
    if(saveDialog) AppAlertDialog(onDismissRequest={saveDialog=false},title={Text(stringResource(R.string.discovery_save_smart_collection))},text={
        Column {
            OutlinedTextField(collectionName,{collectionName=it.take(60)},label={Text(stringResource(R.string.discovery_collection_name_label))})
            Text(stringResource(R.string.discovery_smart_collection_desc))
        }
    },confirmButton={TextButton(enabled=collectionName.isNotBlank(),onClick={mutate { store.saveCollection(SmartCollection(collectionName,query,tag,extension)) };saveDialog=false}) { Text(stringResource(R.string.action_save)) }},dismissButton={TextButton(onClick={saveDialog=false}) { Text(stringResource(R.string.action_cancel)) }})
    if(clearDialog) AppAlertDialog(onDismissRequest={clearDialog=false},title={Text(stringResource(R.string.discovery_clear_library_title))},text={Text(stringResource(R.string.discovery_clear_library_desc))},confirmButton={TextButton(onClick={mutate { store.clear() };clearDialog=false}) { Text(stringResource(R.string.action_clear)) }},dismissButton={TextButton(onClick={clearDialog=false}) { Text(stringResource(R.string.action_cancel)) }})
}
