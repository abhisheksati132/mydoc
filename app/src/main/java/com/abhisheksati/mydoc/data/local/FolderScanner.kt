package com.abhisheksati.mydoc.data.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FolderScanner {

    private val SUPPORTED_EXTENSIONS = setOf(
        "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "csv"
    )

    suspend fun scanTree(
        context: Context,
        treeUri: Uri,
        repository: DocumentRepository
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)

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

                    val ext = displayName.substringAfterLast('.', "").lowercase()
                    val isSupported = ext in SUPPORTED_EXTENSIONS || mime == "application/pdf"

                    if (isSupported) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        repository.registerOpenedDocument(docUri)
                        count++
                    }
                }
            }
        } catch (_: Exception) {
        }
        count
    }
}
