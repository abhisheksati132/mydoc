package com.abhisheksati.mydoc.data.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import com.abhisheksati.mydoc.ui.shelves.DetectedFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FolderScanner {

    private val SUPPORTED_EXTENSIONS = setOf(
        "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "csv", "epub", "md"
    )

    suspend fun scanTree(
        context: Context,
        treeUri: Uri,
        repository: DocumentRepository,
        shelfId: Long? = null
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            count = scanDirectory(context, treeUri, rootDocId, repository, shelfId)
        } catch (_: Exception) {
        }
        count
    }

    private suspend fun scanDirectory(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        repository: DocumentRepository,
        shelfId: Long?
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
                        count += scanDirectory(context, treeUri, docId, repository, shelfId)
                    } else {
                        val ext = displayName.substringAfterLast('.', "").lowercase()
                        val isSupported = ext in SUPPORTED_EXTENSIONS || mime.startsWith("application/pdf") || mime.startsWith("text/")
                        if (isSupported) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val regResult = repository.registerOpenedDocument(docUri)
                            if (shelfId != null && regResult.isSuccess) {
                                repository.assignDocumentToShelf(docUri.toString(), shelfId)
                            }
                            count++
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return count
    }

    suspend fun detectSubfolders(
        context: Context,
        treeUri: Uri
    ): List<DetectedFolder> = withContext(Dispatchers.IO) {
        val detected = mutableListOf<DetectedFolder>()
        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            var colorCycle = 0
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol)
                    val displayName = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol) ?: ""

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR && !displayName.startsWith(".")) {
                        val count = countFilesInDir(context, treeUri, docId)
                        if (count > 0) {
                            val subfolderTreeUri = DocumentsContract.buildTreeDocumentUri(treeUri.authority, docId).toString()
                            val isCameraOrTemp = displayName.equals("DCIM", ignoreCase = true) ||
                                displayName.equals("Screenshots", ignoreCase = true) ||
                                displayName.equals("Temp", ignoreCase = true) ||
                                displayName.equals(".thumbnails", ignoreCase = true)

                            detected.add(
                                DetectedFolder(
                                    name = displayName,
                                    path = displayName,
                                    uri = subfolderTreeUri,
                                    fileCount = count,
                                    colorIndex = colorCycle % 6,
                                    isRecommended = !isCameraOrTemp
                                )
                            )
                            colorCycle++
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        detected
    }

    private fun countFilesInDir(context: Context, treeUri: Uri, dirDocId: String): Int {
        var count = 0
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol) ?: ""
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (mime != DocumentsContract.Document.MIME_TYPE_DIR && (ext in SUPPORTED_EXTENSIONS || mime.startsWith("application/pdf"))) {
                        count++
                    }
                }
            }
        } catch (_: Exception) {
        }
        return count
    }
}
