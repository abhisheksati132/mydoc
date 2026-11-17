package com.abhisheksati.mydoc.discovery

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.util.DocumentIdentity
import com.abhisheksati.mydoc.util.SecureLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Serializes background text extraction for documents the user has opened. */
object SearchIndexQueue {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val queued: MutableSet<String> =
        java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun enqueue(context: Context, uri: String, name: String, mime: String) {
        if (!queued.add(uri)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                mutex.withLock {
                    val saved = File(File(appContext.filesDir, "saved_docs"), DocumentIdentity.savedDocsName(uri, name))
                    if (saved.isFile) {
                        DocumentIndexer(appContext, DiscoveryStore.get(appContext))
                            .indexSavedCopy(Uri.parse(uri), saved, name, mime)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // Indexing retries the next time the document opens, but swallowing the failure
                // made a document that could never be indexed simply absent from search with
                // no trace to diagnose it from.
                SecureLog.warn("IndexQueue", "Background index failed", failure)
            } finally {
                queued.remove(uri)
            }
        }
    }
}
