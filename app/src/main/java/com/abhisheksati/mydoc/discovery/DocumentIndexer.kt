package com.abhisheksati.mydoc.discovery

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.domain.model.DocumentFormats
import com.abhisheksati.mydoc.domain.model.ConverterKind
import com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer
import com.abhisheksati.mydoc.rendering.engine.NativeOfficeConverter
import com.abhisheksati.mydoc.rendering.engine.OfficeConversion
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.util.SecureLog
import kotlinx.coroutines.*
import java.io.File

private const val MAX_FILES = 1000
private const val MAX_DEPTH = 12
private const val MAX_DIRECTORIES = 200

/** A failure whose message is written for the reader, not raw exception text from a parser. */
private class IndexFailure(message: String) : Exception(message)

/** Explicitly selected files only; no hidden background crawl and no OCR/network requests. */
class DocumentIndexer(private val context: Context,private val store: DiscoveryStore) {
    suspend fun selected(uris: List<Uri>, folder: Boolean, progress: (String)->Unit) = withContext(Dispatchers.IO) {
        val files=mutableListOf<Uri>()
        var limit=false
        if(folder) {
            val pending=java.util.ArrayDeque<Pair<SelectedDocument,Int>>()
            var directories=0
            for(uri in uris) SelectedDocument.root(context,uri)?.let { pending.add(it to 0) }
            while(pending.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                if(directories>=MAX_DIRECTORIES) { limit=true;break }
                val (dir,depth)=pending.removeFirst();directories++
                if(depth>=MAX_DEPTH) { limit=true;continue }
                // One directory can be arbitrarily wide. Cap the rows read from it: every entry
                // spends either a file slot (MAX_FILES) or a directory slot (MAX_DIRECTORIES), so a
                // directory that yields more rows than both budgets combined cannot be finished
                // inside the selection limits anyway.
                val page=SelectedDocument.children(context,dir,MAX_FILES+MAX_DIRECTORIES)
                if(page.truncated) limit=true
                for(child in page.entries) {
                    currentCoroutineContext().ensureActive()
                    if(files.size>=MAX_FILES) { limit=true;pending.clear();break }
                    if(child.isDirectory) {
                        // Queue growth is bounded too: a deep tree of empty folders must not
                        // outgrow the selection limits before a single file is found.
                        if(directories+pending.size>=MAX_DIRECTORIES) limit=true
                        else pending.add(child to depth+1)
                    } else files.add(child.uri)
                }
            }
        } else { files.addAll(uris.take(MAX_FILES));limit=uris.size>MAX_FILES }
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
    /**
     * Index the offline copy MyDoc already keeps for [uri]. Runs after a document is opened so
     * Spotlight can search its text without asking the user to add folders first.
     */
    suspend fun indexSavedCopy(uri: Uri, file: File, name: String, mime: String) = withContext(Dispatchers.IO) {
        if (!file.isFile || file.length() <= 0L) return@withContext
        val previous = store.file(uri.toString())
        if (previous != null && previous.size == file.length() && previous.modified == file.lastModified()) {
            return@withContext
        }
        val ext = name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
        val pages = mutableListOf<Pair<Int, String>>()
        var status = "Name and tags only — no text extractor for this format"
        var converted: File? = null
        try {
            if (file.length() > 16L * 1024 * 1024) {
                status = "Not text-indexed: source exceeds 16 MiB"
            } else if (ext in setOf("txt", "md", "csv", "tsv", "log", "json", "xml", "html")) {
                pages.add(0 to file.readText().take(200_000))
                status = "Text indexed"
            } else {
                val converter = DocumentFormats.converterFor(ext)
                if (converter != ConverterKind.NONE || ext in setOf("pdf", "epub", "fb2", "xps")) {
                    val pdf = if (converter != ConverterKind.NONE) {
                        val result = NativeOfficeConverter.convert(context, Uri.fromFile(file), name)
                        val success = result as? OfficeConversion.Success
                            ?: throw IndexFailure((result as? OfficeConversion.Failure)?.message ?: "Could not render document")
                        // The shared content-addressed cache belongs to the converter; only a
                        // per-call encrypted render is ours to delete.
                        if (success.deleteWhenClosed) converted = success.pdf
                        success.pdf
                    } else file
                    val renderer = MuPdfRenderer(filePath = pdf.absolutePath)
                    try {
                        if (renderer.isPasswordProtected) throw IndexFailure("Locked file: text not indexed")
                        val total = renderer.pageCount
                        if (total <= 0) throw IndexFailure("Corrupt or invalid document")
                        var remaining = 200_000
                        var indexed = 0
                        for (p in 0 until minOf(200, total)) {
                            currentCoroutineContext().ensureActive()
                            if (remaining <= 0) break
                            val text = renderer.getPageText(p).orEmpty().take(remaining)
                            if (text.isNotBlank()) pages.add(p to text)
                            remaining -= text.length
                            indexed++
                        }
                        status = when {
                            pages.isEmpty() -> "No text layer: scanned pages require OCR"
                            indexed < total || remaining == 0 -> "Partial: $indexed of $total pages, up to 200,000 characters"
                            else -> "Indexed $indexed pages"
                        }
                    } finally {
                        renderer.close()
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            pages.clear()
            status = failureStatus(e)
        } finally {
            converted?.delete()
        }
        store.put(
            DiscoveredFile(uri.toString(), name, mime, file.length(), file.lastModified(), status, System.currentTimeMillis()),
            pages
        )
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
        var converted: File?=null
        try {
            if(source==null) throw IndexFailure("File missing or permission expired; relink its location")
            if(size>16L*1024*1024) status="Not text-indexed: source exceeds 16 MiB" else {
                val converter=DocumentFormats.converterFor(ext)
                if(ext in setOf("txt","md","csv","tsv","log","json","xml","html")) {
                    val stream=DocumentStreamResolver.openStream(context,uri) ?: throw IndexFailure("Permission expired or file missing")
                    val text=stream.bufferedReader().use { reader ->
                        val b=CharArray(200_001);var n=0
                        while(n<b.size) { currentCoroutineContext().ensureActive();val count=reader.read(b,n,b.size-n);if(count<0)break;n+=count }
                        String(b,0,n)
                    }
                    pages.add(0 to text.take(200_000));status=if(text.length>200_000) "Partial: first 200,000 characters" else "Text indexed"
                } else if(converter!=ConverterKind.NONE || ext in setOf("pdf","epub","fb2","xps")) {
                    val staged=File.createTempFile("index-",".$ext",context.cacheDir).also { temporary=it }
                    val stream=DocumentStreamResolver.openStream(context,uri) ?: throw IndexFailure("Permission expired or file missing")
                    stream.use { input -> staged.outputStream().use { output ->
                        val buffer=ByteArray(64*1024);var read=0L
                        while(true) { currentCoroutineContext().ensureActive();val n=input.read(buffer);if(n<0)break
                            read+=n;if(read>16L*1024*1024) throw IndexFailure("Source exceeds 16 MiB");output.write(buffer,0,n)
                        }
                    } }
                    val pdf=if(converter!=ConverterKind.NONE) {
                        val result=NativeOfficeConverter.convert(context,Uri.fromFile(staged),name)
                        val success=result as? OfficeConversion.Success
                            ?: throw IndexFailure((result as? OfficeConversion.Failure)?.message ?: "Could not render document")
                        if(success.deleteWhenClosed) converted=success.pdf
                        success.pdf
                    } else staged
                    val renderer=MuPdfRenderer(filePath=pdf.absolutePath)
                    try {
                        if(renderer.isPasswordProtected) throw IndexFailure("Locked file: text not indexed")
                        val total=renderer.pageCount
                        if(total <= 0) throw IndexFailure("Corrupt or invalid document")
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
        catch(e: Exception) { pages.clear();status=failureStatus(e) }
        finally { temporary?.delete();converted?.delete() }
        store.put(DiscoveredFile(uri.toString(),name,mime,size,modified,status,System.currentTimeMillis()),pages)
    }

    /** Reader-facing failure text; raw parser/IO exception messages stay in the secure log. */
    private fun failureStatus(error: Throwable): String {
        SecureLog.warn("DocumentIndexer", "Text indexing failed", error)
        return "Unavailable: " + ((error as? IndexFailure)?.message ?: "could not read file")
    }
}
