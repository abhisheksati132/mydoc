package com.abhisheksati.mydoc.discovery

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.domain.model.DocumentFormats
import com.abhisheksati.mydoc.domain.model.ConverterKind
import com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer
import com.abhisheksati.mydoc.rendering.engine.NativeOfficeConverter
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import kotlinx.coroutines.*
import java.io.File

/** Explicitly selected files only; no hidden background crawl and no OCR/network requests. */
class DocumentIndexer(private val context: Context,private val store: DiscoveryStore) {
    suspend fun selected(uris: List<Uri>, folder: Boolean, progress: (String)->Unit) = withContext(Dispatchers.IO) {
        val files=mutableListOf<Uri>()
        var limit=false
        if(folder) {
            val pending=java.util.ArrayDeque<Pair<SelectedDocument,Int>>()
            for(uri in uris) SelectedDocument.root(context,uri)?.let { pending.add(it to 0) }
            while(pending.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val (dir,depth)=pending.removeFirst()
                if(depth>=12) { limit=true;continue }
                for(child in SelectedDocument.children(context,dir)) {
                    currentCoroutineContext().ensureActive()
                    if(files.size>=1000) { limit=true;pending.clear();break }
                    if(child.isDirectory) pending.add(child to depth+1) else files.add(child.uri)
                }
            }
        } else { files.addAll(uris.take(1000));limit=uris.size>1000 }
        var done=0
        val known=store.files().map { it.uri }.toMutableSet()
        for(uri in files.distinct()) {
            currentCoroutineContext().ensureActive()
            if(known.size>=1000 && uri.toString() !in known) { limit=true;break }
            index(uri)
            known.add(uri.toString())
            done++;progress("Indexed $done of ${files.size} selected files")
        }
        progress("Finished: $done files. " + if(limit) "Selection limit reached; add another folder or batch to continue." else "Refresh after files change. Scanned pages need OCR and are not text-indexed.")
    }
    private suspend fun index(uri: Uri) {
        val previous=store.file(uri.toString())
        val source=runCatching { SelectedDocument.read(context,uri) }.getOrNull()
        val name=runCatching { source?.name }.getOrNull() ?: previous?.name ?: uri.lastPathSegment ?: "Document"
        val size=runCatching { source?.size }.getOrNull() ?: previous?.size ?: 0L
        val modified=runCatching { source?.modified }.getOrNull() ?: previous?.modified ?: 0L
        val mime=runCatching { source?.type ?: context.contentResolver.getType(uri) }.getOrNull() ?: previous?.mime ?: "application/octet-stream"
        val ext=name.substringAfterLast('.',"").lowercase(java.util.Locale.ROOT)
        val pages=mutableListOf<Pair<Int,String>>()
        var status="Name and tags only — no text extractor for this format"
        var temporary: File?=null
        try {
            check(source!=null) { "File missing or permission expired; relink its location" }
            if(size>16L*1024*1024) status="Not text-indexed: source exceeds 16 MiB" else {
                val converter=DocumentFormats.converterFor(ext)
                if(ext in setOf("txt","md","csv","tsv","log","json","xml","html")) {
                    val stream=DocumentStreamResolver.openStream(context,uri) ?: error("Permission expired or file missing")
                    val text=stream.bufferedReader().use { reader ->
                        val b=CharArray(200_001);var n=0
                        while(n<b.size) { currentCoroutineContext().ensureActive();val count=reader.read(b,n,b.size-n);if(count<0)break;n+=count }
                        String(b,0,n)
                    }
                    pages.add(0 to text.take(200_000));status=if(text.length>200_000) "Partial: first 200,000 characters" else "Text indexed"
                } else if(converter!=ConverterKind.NONE || ext in setOf("pdf","epub","fb2","xps")) {
                    val staged=File.createTempFile("index-",".$ext",context.cacheDir).also { temporary=it }
                    val stream=DocumentStreamResolver.openStream(context,uri) ?: error("Permission expired or file missing")
                    stream.use { input -> staged.outputStream().use { output ->
                        val buffer=ByteArray(64*1024);var read=0L
                        while(true) { currentCoroutineContext().ensureActive();val n=input.read(buffer);if(n<0)break
                            read+=n;check(read<=16L*1024*1024) { "Source exceeds 16 MiB" };output.write(buffer,0,n)
                        }
                    } }
                    val pdf=if(converter!=ConverterKind.NONE) {
                        val result=NativeOfficeConverter.convert(context,Uri.fromFile(staged),name)
                        result.pdfFile ?: error((result as? com.abhisheksati.mydoc.rendering.engine.OfficeConversion.Failure)?.message ?: "Could not render document")
                    } else staged
                    val renderer=MuPdfRenderer(filePath=pdf.absolutePath)
                    try {
                        if(renderer.isPasswordProtected) error("Locked file: text not indexed")
                        val total=renderer.pageCount
                        if(total <= 0) error("Corrupt or invalid document")
                        var remaining=200_000
                        var indexed=0
                        for(p in 0 until minOf(200,total)) {
                            currentCoroutineContext().ensureActive()
                            if(remaining<=0)break
                            val text=renderer.getPageText(p).orEmpty().take(remaining)
                            if(text.isNotBlank()) pages.add(p to text)
                            remaining-=text.length;indexed++
                        }
                        status=if(pages.isEmpty()) "No text layer: scanned pages require OCR" else if(indexed<total || remaining==0) "Partial: $indexed of $total pages, up to 200,000 characters" else "Indexed $indexed pages" + if(converter!=ConverterKind.NONE) " (rendered Office print layout)" else ""
                    } finally { renderer.close() }
                }
            }
        } catch(cancelled: CancellationException) { throw cancelled }
        catch(e: Exception) { pages.clear();status="Unavailable: ${e.message ?: "could not read file"}" }
        finally { temporary?.delete() }
        store.put(DiscoveredFile(uri.toString(),name,mime,size,modified,status,System.currentTimeMillis()),pages)
    }
}
