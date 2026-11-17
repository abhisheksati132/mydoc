package com.abhisheksati.mydoc.discovery

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/** One page of a directory listing, with whether the provider had more rows than the caller allowed. */
internal data class ChildrenPage(val entries: List<SelectedDocument>, val truncated: Boolean)

/** SAF metadata without adding another document-provider library or broad-storage permission. */
internal data class SelectedDocument(val uri: Uri,val name: String,val type: String,val size: Long,val modified: Long) {
    val isDirectory: Boolean get()=type==DocumentsContract.Document.MIME_TYPE_DIR
    companion object {
        private val PROJECTION = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            OpenableColumns.DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,OpenableColumns.SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        fun read(context: Context,uri: Uri): SelectedDocument? {
            if(uri.scheme=="file") {
                val f=File(uri.path ?: return null)
                if(!f.exists())return null
                val mime=if(f.isDirectory)DocumentsContract.Document.MIME_TYPE_DIR else android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase(java.util.Locale.ROOT)) ?: "application/octet-stream"
                return SelectedDocument(uri,f.name,mime,f.length(),f.lastModified())
            }
            return context.contentResolver.query(uri,null,null,null,null)?.use { c ->
                if(!c.moveToFirst())null else fromCursor(c,uri,context.contentResolver.getType(uri))
            }
        }
        fun root(context: Context,uri: Uri): SelectedDocument? = read(context,
            DocumentsContract.buildDocumentUriUsingTree(uri,DocumentsContract.getTreeDocumentId(uri)))
        suspend fun children(context: Context,parent: SelectedDocument,maxEntries: Int): ChildrenPage {
            val children=DocumentsContract.buildChildDocumentsUriUsingTree(parent.uri,DocumentsContract.getDocumentId(parent.uri))
            val cursor=context.contentResolver.query(children,PROJECTION,null,null,null)
                ?: return ChildrenPage(emptyList(),false)
            return cursor.use { readChildren(it,parent.uri,maxEntries) }
        }
        /**
         * Consumes at most [maxEntries] rows, checking for cancellation on every row: a provider
         * can hand back an arbitrarily wide directory, and the old loop materialised all of it
         * before any budget applied. [ChildrenPage.truncated] is exact — it probes one extra row
         * instead of assuming a full page was cut short.
         */
        internal suspend fun readChildren(c: Cursor,parentUri: Uri,maxEntries: Int): ChildrenPage {
            val entries=ArrayList<SelectedDocument>(minOf(maxEntries,64))
            while(entries.size<maxEntries) {
                currentCoroutineContext().ensureActive()
                if(!c.moveToNext()) return ChildrenPage(entries,false)
                val id=c.getString(c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                entries.add(fromCursor(c,DocumentsContract.buildDocumentUriUsingTree(parentUri,id),null))
            }
            currentCoroutineContext().ensureActive()
            return ChildrenPage(entries,c.moveToNext())
        }
        private fun fromCursor(c: Cursor,uri: Uri,fallbackMime: String?): SelectedDocument {
            fun text(name: String): String?=c.getColumnIndex(name).takeIf { it>=0 && !c.isNull(it) }?.let(c::getString)
            fun number(name: String): Long=c.getColumnIndex(name).takeIf { it>=0 && !c.isNull(it) }?.let(c::getLong) ?: 0L
            return SelectedDocument(uri,text(OpenableColumns.DISPLAY_NAME) ?: uri.lastPathSegment ?: "Document",
                text(DocumentsContract.Document.COLUMN_MIME_TYPE) ?: fallbackMime ?: "application/octet-stream",
                number(OpenableColumns.SIZE),number(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
        }
    }
}
