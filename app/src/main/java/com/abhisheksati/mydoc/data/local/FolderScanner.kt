package com.abhisheksati.mydoc.data.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FolderScanner {

    private val SUPPORTED_EXTENSIONS = setOf(
        "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "csv", "epub", "md"
    )

    suspend fun scanTree(
        context: Context,
        treeUri: Uri,
        repository: DocumentRepository
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            count = scanDirectory(context, treeUri, rootDocId, repository)
        } catch (_: Exception) {
        }
        count
    }

    private suspend fun scanDirectory(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        repository: DocumentRepository
    ): Int {
        var count = 0
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol)
                    val displayName = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol) ?: ""

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        count += scanDirectory(context, treeUri, docId, repository)
                    } else {
                        val ext = displayName.substringAfterLast('.', "").lowercase()
                        val isSupported = ext in SUPPORTED_EXTENSIONS || mime.startsWith("application/pdf") || mime.startsWith("text/")
                        if (isSupported) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            repository.registerOpenedDocument(docUri)
                            count++
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return count
    }
}
