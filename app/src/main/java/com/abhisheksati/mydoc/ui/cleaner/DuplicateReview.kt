package com.abhisheksati.mydoc.ui.cleaner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.tools.cleaner.DuplicateGroup
import com.abhisheksati.mydoc.ui.files.LibraryThumbnail
import kotlinx.coroutines.*
import java.io.File
import java.security.MessageDigest

internal fun duplicateCopies(groups: List<DuplicateGroup>,keepers: Map<String,String>): List<File> =
    groups.flatMap { group ->
        val files=listOf(group.original)+group.duplicates
        val keep=keepers[group.checksum]
        if(keep==null || files.none { it.absolutePath==keep }) emptyList() else files.filter { it.absolutePath!=keep }
    }.distinctBy { it.absolutePath }

/** Re-read full contents before Trash: a scan is a snapshot and files can change during review. */
internal suspend fun verifiedDuplicateCopies(groups: List<DuplicateGroup>,keepers: Map<String,String>): Set<String> = withContext(Dispatchers.IO) {
    fun hash(file: File): String {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val b=ByteArray(64*1024);while(true) {
            coroutineContext.ensureActive();val n=input.read(b);if(n<0)break;digest.update(b,0,n)
        } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    buildSet {
        for(group in groups) {
            ensureActive()
            val keeper=keepers[group.checksum]?.let(::File) ?: continue
            try {
                if(!keeper.isFile || keeper.length()!=group.size || hash(keeper)!=group.checksum)continue
                for(file in listOf(group.original)+group.duplicates) {
                    ensureActive()
                    if(file.absolutePath==keeper.absolutePath)continue
                    if(file.isFile && file.length()==group.size && hash(file)==group.checksum) add(file.absolutePath)
                }
            } catch(e: CancellationException) { throw e } catch(_: Exception) { /* Changed/unreadable groups stay in place. */ }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DuplicateReview(groups: List<DuplicateGroup>,keepers: Map<String,String>,onKeep: (String,String?)->Unit,
    onPreview: (File)->Unit,onDismiss: ()->Unit) {
    var previewPath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    previewPath?.let { path ->
        val file=File(path)
        val size=groups.firstOrNull { group -> (listOf(group.original)+group.duplicates).any { it.absolutePath==path } }?.size ?: 0L
        val ext=file.extension.lowercase(java.util.Locale.ROOT)
        val kind=if(ext in setOf("png","jpg","jpeg","gif","webp","bmp","heic")) MediaKind.PHOTO else MediaKind.DOCUMENT
        Dialog(onDismissRequest={previewPath=null}) {
            Surface(shape=MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("First-page / image preview",style=MaterialTheme.typography.titleMedium)
                    LibraryThumbnail(LibraryItem(android.net.Uri.fromFile(file).toString(),file.name,kind,
                        if(kind==MediaKind.PHOTO) "image/$ext" else "application/octet-stream",size,0),Modifier.fillMaxWidth().height(260.dp))
                    Text(file.name)
                    Text(file.absolutePath,style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={previewPath=null}) { Text("Back to review") }
                    TextButton(onClick={previewPath=null;onPreview(file)}) { Text("Open original") }
                }
            }
        }
    }
    if(previewPath==null) Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(12.dp),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text("Review duplicate copies",style=MaterialTheme.typography.titleLarge)
                Text("Choose one keeper in each group. Other copies in reviewed groups are selected for Trash. Preview files before confirming. Contents are rechecked before moving.",style=MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    for(group in groups) {
                        item("header-${group.checksum}") {
                            Column {
                                HorizontalDivider(Modifier.padding(top=12.dp))
                                Text("${group.duplicates.size+1} identical copies • ${group.size} bytes each",style=MaterialTheme.typography.titleSmall)
                                TextButton(onClick={onKeep(group.checksum,null)}) { Text("Leave this group unchanged") }
                            }
                        }
                        items(listOf(group.original)+group.duplicates,key={group.checksum+it.absolutePath}) { file ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                val ext=file.extension.lowercase(java.util.Locale.ROOT)
                                val kind=if(ext in setOf("png","jpg","jpeg","gif","webp","bmp","heic"))MediaKind.PHOTO else MediaKind.DOCUMENT
                                LibraryThumbnail(LibraryItem(android.net.Uri.fromFile(file).toString(),file.name,kind,
                                    if(kind==MediaKind.PHOTO)"image/$ext" else "application/octet-stream",group.size,0),Modifier.size(48.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(file.name,style=MaterialTheme.typography.titleSmall)
                                    Text(file.absolutePath,style=MaterialTheme.typography.bodySmall)
                                    FlowRow {
                                        RadioButton(selected=keepers[group.checksum]==file.absolutePath,onClick={onKeep(group.checksum,file.absolutePath)})
                                        TextButton(onClick={onKeep(group.checksum,file.absolutePath)}) { Text("Keep this copy") }
                                        TextButton(onClick={previewPath=file.absolutePath}) { Text("Preview") }
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()) { Text("Done reviewing") }
            }
        }
    }
}
