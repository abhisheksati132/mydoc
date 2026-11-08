package com.abhisheksati.mydoc.discovery

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
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiscoveryScreen(onBack: ()->Unit,onOpen: (LibraryItem)->Unit) {
    val context=LocalContext.current
    val app=context.applicationContext as MyDocApp
    val store=remember { DiscoveryStore.get(app) }
    val scope=rememberCoroutineScope()
    val revision by store.revision.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var extension by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("Choose files or a folder to build your private, offline index. Indexing stops when you leave this screen.") }
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
    BackHandler { onBack() }
    LaunchedEffect(query,tag,extension,revision) {
        delay(180)
        try { results=store.search(query.trim(),tag.trim().lowercase(java.util.Locale.ROOT),extension);collections=store.collections() }
        catch(e: Exception) { if(e is CancellationException)throw e;message="Could not search the local index: ${e.message}" }
    }
    fun mutate(operation: suspend ()->Unit) {
        scope.launch {
            try { operation() } catch(e: CancellationException) { throw e }
            catch(e: Exception) { message="Could not save this change: ${e.message}" }
        }
    }
    var relinkTarget by remember { mutableStateOf<DiscoveredFile?>(null) }
    fun index(uris: List<Uri>,folder: Boolean) {
        if(uris.isEmpty() || working)return
        uris.forEach { uri -> runCatching { context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        working=true
        job=scope.launch {
            try { DocumentIndexer(app,store).selected(uris,folder) { status -> scope.launch { message=status } } }
            catch(cancelled: CancellationException) { message="Indexing stopped. Completed files are saved.";throw cancelled }
            catch(e: Exception) { message="Could not index this selection: ${e.message}" }
            finally { working=false }
        }
    }
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { index(it,false) }
    val folder=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let { uri->index(listOf(uri),true) } }
    val relink=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val old=relinkTarget
        if(uri!=null && old!=null && !working) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            working=true
            job=scope.launch {
                try {
                    DocumentIndexer(app,store).selected(listOf(uri),false) { status->scope.launch { message=status } }
                    val added=store.file(uri.toString())
                    check(added!=null && !added.status.startsWith("Unavailable")) { "The new location could not be read; the previous reference was kept." }
                    store.setTags(uri.toString(),old.tags)
                    if(old.uri!=uri.toString())store.remove(old.uri)
                    message="File reconnected. Tags were preserved."
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) { message=e.message ?: "Could not reconnect this file" }
                finally { working=false;relinkTarget=null }
            }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        AppTopBar(title="Contents & collections",onBack=onBack,isGlass=false)
        Text("Search snapshots stay on this device. Up to 1,000 selected files; 200 pages / 200,000 characters per document. Locked files and image-only scans are not text-indexed.",Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp)) {
            TextButton(onClick={pick.launch(arrayOf("*/*"))},enabled=!working) { Text("Add files") }
            TextButton(onClick={folder.launch(null)},enabled=!working) { Text("Add folder") }
            TextButton(onClick={
                working=true
                job=scope.launch {
                    try { DocumentIndexer(app,store).selected(store.files().map { Uri.parse(it.uri) },false) { status->scope.launch { message=status } } }
                    catch(e: CancellationException) { throw e }
                    catch(e: Exception) { message="Refresh failed: ${e.message}" }
                    finally { working=false }
                }
            },enabled=!working) { Text("Refresh") }
            if(working) TextButton(onClick={job?.cancel()}) { Text("Stop") }
            TextButton(onClick={clearDialog=true},enabled=!working) { Text("Clear local library") }
        }
        if(working) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message,Modifier.padding(horizontal=16.dp,vertical=4.dp),style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(query,{query=it.take(300);collectionName=""},label={Text("Words inside documents or filename")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(tag,{tag=it.take(40)},label={Text("Tag")},singleLine=true,modifier=Modifier.weight(1f))
            OutlinedTextField(extension,{extension=it.take(16)},label={Text("File type (pdf, xlsx…)")},singleLine=true,modifier=Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp)) {
            TextButton(onClick={query="";tag="";extension="";collectionName=""}) { Text("All selected files") }
            TextButton(onClick={saveDialog=true}) { Text("Save smart collection") }
            for(rule in collections) InputChip(selected=collectionName==rule.name,onClick={
                collectionName=rule.name;query=rule.query;tag=rule.tag;extension=rule.extension
            },label={Text(rule.name)},modifier=Modifier.padding(horizontal=4.dp))
        }
        if(collectionName.isNotEmpty()) TextButton(onClick={mutate { store.deleteCollection(collectionName);collectionName="" }}) { Text("Delete collection: $collectionName") }
        Text("${results.size} results • refresh after changing or moving files",Modifier.padding(horizontal=16.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium)
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(results.isEmpty()) item { Text("No matching files. Add documents, refresh the index, or clear the filters.",Modifier.padding(16.dp)) }
            items(results,key={it.uri}) { file ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(file.name,style=MaterialTheme.typography.titleSmall)
                        Text(file.uri,maxLines=2,style=MaterialTheme.typography.bodySmall)
                        if(file.snippet.isNotEmpty()) Text("Page ${file.page+1}: ${file.snippet}",Modifier.padding(vertical=6.dp))
                        Text("${file.status} • snapshot ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(file.indexedAt))}",style=MaterialTheme.typography.bodySmall)
                        if(file.tags.isNotEmpty()) Text("Tags: ${file.tags}",style=MaterialTheme.typography.labelMedium)
                        androidx.compose.foundation.layout.FlowRow {
                            TextButton(onClick={scope.launch {
                                try {
                                    val reachable=withContext(Dispatchers.IO) { context.contentResolver.openAssetFileDescriptor(Uri.parse(file.uri),"r")?.use { true } ?: false }
                                    check(reachable) { "File missing or permission expired. Add its new location to reconnect it." }
                                    val kind=when { file.mime.startsWith("image/")->MediaKind.PHOTO;file.mime.startsWith("audio/")->MediaKind.AUDIO;file.mime.startsWith("video/")->MediaKind.VIDEO;else->MediaKind.DOCUMENT }
                                    if(kind==MediaKind.DOCUMENT && file.snippet.isNotEmpty()) app.documentRepository.saveViewState(ViewState(file.uri,lastPage=file.page))
                                    onOpen(LibraryItem(file.uri,file.name,kind,file.mime,file.size,file.modified))
                                } catch(e: Exception) { if(e is CancellationException)throw e;message=e.message ?: "Cannot open this file" }
                            }}) { Text("Open") }
                            TextButton(onClick={relinkTarget=file;relink.launch(arrayOf("*/*"))},enabled=!working) { Text("Relink") }
                            TextButton(onClick={tagsForUri=file.uri;tagsForName=file.name;tagsValue=file.tags}) { Text("Edit tags") }
                            TextButton(onClick={mutate { store.remove(file.uri) }}) { Text("Remove from index") }
                        }
                    }
                }
            }
        }
    }
    if(tagsForUri!=null) AlertDialog(onDismissRequest={tagsForUri=null},title={Text("Tags for ${tagsForName}")},text={
        OutlinedTextField(tagsValue,{tagsValue=it.take(1000)},label={Text("Comma-separated tags")},supportingText={Text("Up to 20 tags, 40 characters each. Tags do not move or rename the original file.")})
    },confirmButton={TextButton(onClick={val uri=tagsForUri!!;mutate { store.setTags(uri,tagsValue) };tagsForUri=null}) { Text("Save") }},dismissButton={TextButton(onClick={tagsForUri=null}) { Text("Cancel") }})
    if(saveDialog) AlertDialog(onDismissRequest={saveDialog=false},title={Text("Save smart collection")},text={
        Column {
            OutlinedTextField(collectionName,{collectionName=it.take(60)},label={Text("Collection name")})
            Text("Uses the current words, tag and file-type filters. Matching files update after indexing. Saving the same name replaces that rule.")
        }
    },confirmButton={TextButton(enabled=collectionName.isNotBlank(),onClick={mutate { store.saveCollection(SmartCollection(collectionName,query,tag,extension)) };saveDialog=false}) { Text("Save") }},dismissButton={TextButton(onClick={saveDialog=false}) { Text("Cancel") }})
    if(clearDialog) AlertDialog(onDismissRequest={clearDialog=false},title={Text("Clear the local library?")},text={Text("Removes indexed text and tags from MyDoc. Your original files and saved collection rules stay in place.")},confirmButton={TextButton(onClick={mutate { store.clear() };clearDialog=false}) { Text("Clear") }},dismissButton={TextButton(onClick={clearDialog=false}) { Text("Cancel") }})
}
