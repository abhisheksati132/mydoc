package com.abhisheksati.mydoc.discovery

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File

/** SAF metadata without adding another document-provider library or broad-storage permission. */
internal data class SelectedDocument(val uri: Uri,val name: String,val type: String,val size: Long,val modified: Long) {
    val isDirectory: Boolean get()=type==DocumentsContract.Document.MIME_TYPE_DIR
    companion object {
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
        fun children(context: Context,parent: SelectedDocument): List<SelectedDocument> {
            val children=DocumentsContract.buildChildDocumentsUriUsingTree(parent.uri,DocumentsContract.getDocumentId(parent.uri))
            return buildList {
                context.contentResolver.query(children,arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    OpenableColumns.DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,OpenableColumns.SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED),null,null,null)?.use { c ->
                    while(c.moveToNext()) {
                        val id=c.getString(c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                        add(fromCursor(c,DocumentsContract.buildDocumentUriUsingTree(parent.uri,id),null))
                    }
                }
            }
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
