package com.abhisheksati.mydoc.util

import android.net.Uri
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.DocumentFormats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The readable documents sitting next to a file, name-sorted, for swipe-between-documents.
 *
 * This used to run inside a `LaunchedEffect` on the main thread: `listFiles` plus a length and a
 * modification time per hit. On a folder of a few thousand files that stalled the moment a document
 * was opened — the worst possible place to stutter, because the reader is watching a page appear.
 * The work now happens on the IO dispatcher and is cancellable, so leaving the viewer or opening
 * another document abandons the scan instead of finishing it and overwriting newer navigation state.
 */
object DocumentSiblings {

    private const val CANCELLATION_GRANULARITY = 128

    suspend fun readableDocumentsIn(file: File?): List<DocumentFile> {
        val dir = file?.parentFile ?: return emptyList()
        return withContext(Dispatchers.IO) {
            val readableExtensions = DocumentFormats.supportedExtensions
            // One readdir pass first: names cost nothing to filter, whereas filtering a File-per-entry
            // would stat every file in the folder before the first cancellation check.
            val listed = runCatching { dir.list()?.toList() }
            // A folder that cannot be read is an empty sibling list; a cancelled caller is not.
            listed.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            val candidates = listed.getOrNull()
                ?.filter { !it.startsWith(".") && it.substringAfterLast('.', "").lowercase() in readableExtensions }
                ?.sortedBy { it.lowercase() }
                ?: return@withContext emptyList()

            val documents = ArrayList<DocumentFile>(candidates.size)
            candidates.forEachIndexed { index, name ->
                if (index % CANCELLATION_GRANULARITY == 0) currentCoroutineContext().ensureActive()
                val candidate = File(dir, name)
                if (!candidate.isFile) return@forEachIndexed
                documents += DocumentFile(
                    uri = Uri.fromFile(candidate).toString(),
                    displayName = name,
                    sizeBytes = candidate.length(),
                    mimeType = "",
                    lastOpenedTimestamp = candidate.lastModified()
                )
            }
            currentCoroutineContext().ensureActive()
            documents
        }
    }
}
