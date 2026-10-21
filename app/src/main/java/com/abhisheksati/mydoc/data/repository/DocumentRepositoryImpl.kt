package com.abhisheksati.mydoc.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.data.local.entity.FavoriteDocumentEntity
import com.abhisheksati.mydoc.data.local.entity.RecentDocumentEntity
import com.abhisheksati.mydoc.data.local.entity.ViewStateEntity
import com.abhisheksati.mydoc.data.local.entity.ShelfAssignmentEntity
import com.abhisheksati.mydoc.data.local.entity.ShelfEntity
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.Shelf
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase
) : DocumentRepository {

    private val contentResolver = context.contentResolver
    private val recentDao = database.recentDao()
    private val favoriteDao = database.favoriteDao()
    private val viewStateDao = database.viewStateDao()
    private val shelfDao = database.shelfDao()

    override fun getRecents(): Flow<List<DocumentFile>> {
        return combine(
            recentDao.getAllRecents(),
            favoriteDao.getAllFavorites(),
            shelfDao.getAllDocumentShelfInfos()
        ) { recents, favorites, shelfInfos ->
            val favUris = favorites.map { it.uri }.toSet()
            val shelfMap = shelfInfos.associateBy { it.documentUri }
            recents.map { recent ->
                val shelfInfo = shelfMap[recent.uri]
                DocumentFile(
                    uri = recent.uri,
                    displayName = recent.displayName,
                    sizeBytes = recent.sizeBytes,
                    mimeType = recent.mimeType,
                    lastOpenedTimestamp = recent.lastOpenedTimestamp,
                    pageCount = recent.pageCount,
                    lastPage = recent.lastPage,
                    isFavorite = favUris.contains(recent.uri),
                    shelfId = shelfInfo?.shelfId,
                    shelfName = shelfInfo?.shelfName,
                    shelfColorIndex = shelfInfo?.colorIndex
                )
            }
        }
    }

    override fun getFavorites(): Flow<List<DocumentFile>> {
        return combine(
            favoriteDao.getAllFavorites(),
            recentDao.getAllRecents(),
            shelfDao.getAllDocumentShelfInfos()
        ) { favorites, recents, shelfInfos ->
            val recentsMap = recents.associateBy { it.uri }
            val shelfMap = shelfInfos.associateBy { it.documentUri }
            favorites.map { fav ->
                val recent = recentsMap[fav.uri]
                val shelfInfo = shelfMap[fav.uri]
                DocumentFile(
                    uri = fav.uri,
                    displayName = fav.displayName,
                    sizeBytes = fav.sizeBytes,
                    mimeType = fav.mimeType,
                    lastOpenedTimestamp = recent?.lastOpenedTimestamp ?: fav.addedTimestamp,
                    pageCount = recent?.pageCount ?: 0,
                    lastPage = recent?.lastPage ?: 0,
                    isFavorite = true,
                    shelfId = shelfInfo?.shelfId,
                    shelfName = shelfInfo?.shelfName,
                    shelfColorIndex = shelfInfo?.colorIndex
                )
            }
        }
    }

    override suspend fun registerOpenedDocument(uri: Uri): Result<DocumentFile> = withContext(Dispatchers.IO) {
        runCatching {
            // Attempt to take persistable URI permission so we can reopen this file across app reboots
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some transient intents (e.g. temporary shares) do not permit persistable grants
            }

            val metadata = extractMetadata(uri)
            val now = System.currentTimeMillis()

            // Cache an internal offline copy so documents remain readable even if Android revokes transient URI grants
            try {
                val savedDocsDir = java.io.File(context.filesDir, "saved_docs").apply { mkdirs() }
                val docKey = uri.toString().hashCode().toString(16)
                val safeName = metadata.displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val localFile = java.io.File(savedDocsDir, "${docKey}_$safeName")
                if (!localFile.exists() || localFile.length() == 0L) {
                    com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, metadata.displayName)?.use { input ->
                        localFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (_: Exception) {}

            val existing = recentDao.getRecentByUri(uri.toString())
            val recentEntity = RecentDocumentEntity(
                uri = uri.toString(),
                displayName = metadata.displayName,
                sizeBytes = metadata.sizeBytes,
                mimeType = metadata.mimeType,
                lastOpenedTimestamp = now,
                pageCount = existing?.pageCount ?: 0,
                lastPage = existing?.lastPage ?: 0
            )
            recentDao.upsertRecent(recentEntity)

            val isFav = favoriteDao.isFavorite(uri.toString())
            DocumentFile(
                uri = recentEntity.uri,
                displayName = recentEntity.displayName,
                sizeBytes = recentEntity.sizeBytes,
                mimeType = recentEntity.mimeType,
                lastOpenedTimestamp = now,
                pageCount = recentEntity.pageCount,
                lastPage = recentEntity.lastPage,
                isFavorite = isFav
            )
        }
    }

    override suspend fun replaceDocument(oldUri: String, newUri: Uri): Result<DocumentFile> = withContext(Dispatchers.IO) {
        runCatching {
            val oldRecent = recentDao.getRecentByUri(oldUri)
            val oldFavorite = favoriteDao.isFavorite(oldUri)
            val oldViewState = viewStateDao.getViewState(oldUri)

            val newDocRes = registerOpenedDocument(newUri)
            val newDoc = newDocRes.getOrThrow()

            // Preserve reading progress from old document if new document has none
            val targetPage = if (newDoc.lastPage > 0) newDoc.lastPage else (oldRecent?.lastPage ?: 0)
            val targetPageCount = if (newDoc.pageCount > 0) newDoc.pageCount else (oldRecent?.pageCount ?: 0)
            if (targetPage > 0 || targetPageCount > 0) {
                recentDao.updatePageProgress(
                    uri = newDoc.uri,
                    lastPage = targetPage,
                    pageCount = targetPageCount,
                    timestamp = System.currentTimeMillis()
                )
            }

            // Preserve favorite status
            if (oldFavorite && !favoriteDao.isFavorite(newDoc.uri)) {
                favoriteDao.insertFavorite(
                    FavoriteDocumentEntity(
                        uri = newDoc.uri,
                        displayName = newDoc.displayName,
                        sizeBytes = newDoc.sizeBytes,
                        mimeType = newDoc.mimeType,
                        addedTimestamp = System.currentTimeMillis()
                    )
                )
            }

            // Preserve view state
            if (oldViewState != null) {
                viewStateDao.saveViewState(
                    oldViewState.copy(uri = newDoc.uri, updatedTimestamp = System.currentTimeMillis())
                )
            }

            // Remove stale broken URI
            if (oldUri != newDoc.uri) {
                recentDao.deleteRecent(oldUri)
                favoriteDao.deleteFavorite(oldUri)
                viewStateDao.deleteViewState(oldUri)
            }

            newDoc.copy(
                lastPage = targetPage,
                pageCount = targetPageCount,
                isFavorite = oldFavorite || newDoc.isFavorite
            )
        }
    }

    override suspend fun toggleFavorite(document: DocumentFile) = withContext(Dispatchers.IO) {
        val uri = document.uri
        if (favoriteDao.isFavorite(uri)) {
            favoriteDao.deleteFavorite(uri)
        } else {
            favoriteDao.insertFavorite(
                FavoriteDocumentEntity(
                    uri = uri,
                    displayName = document.displayName,
                    sizeBytes = document.sizeBytes,
                    mimeType = document.mimeType,
                    addedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun removeFromRecents(uri: String) = withContext(Dispatchers.IO) {
        recentDao.deleteRecent(uri)
    }

    override suspend fun clearAllRecents() = withContext(Dispatchers.IO) {
        recentDao.clearAllRecents()
    }

    override suspend fun updatePageProgress(uri: String, lastPage: Int, pageCount: Int) = withContext(Dispatchers.IO) {
        recentDao.updatePageProgress(
            uri = uri,
            lastPage = lastPage,
            pageCount = pageCount,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun getViewState(uri: String): ViewState? = withContext(Dispatchers.IO) {
        viewStateDao.getViewState(uri)?.let { entity ->
            ViewState(
                uri = entity.uri,
                lastPage = entity.lastPage,
                zoomScale = entity.zoomScale,
                scrollOffsetX = entity.scrollOffsetX,
                scrollOffsetY = entity.scrollOffsetY,
                readingMode = runCatching { ReadingMode.valueOf(entity.readingMode) }.getOrDefault(ReadingMode.NORMAL),
                isContinuousScroll = entity.isContinuousScroll,
                updatedTimestamp = entity.updatedTimestamp
            )
        }
    }

    override suspend fun saveViewState(viewState: ViewState) = withContext(Dispatchers.IO) {
        viewStateDao.saveViewState(
            ViewStateEntity(
                uri = viewState.uri,
                lastPage = viewState.lastPage,
                zoomScale = viewState.zoomScale,
                scrollOffsetX = viewState.scrollOffsetX,
                scrollOffsetY = viewState.scrollOffsetY,
                readingMode = viewState.readingMode.name,
                isContinuousScroll = viewState.isContinuousScroll,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    private fun extractMetadata(uri: Uri): DocumentMetadata {
        var displayName: String? = null
        var sizeBytes: Long = 0
        var mimeType: String? = contentResolver.getType(uri)

        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        } else if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            displayName = file.name
            sizeBytes = file.length()
        }

        if (displayName.isNullOrBlank()) {
            displayName = uri.lastPathSegment ?: "Document"
        }

        if (sizeBytes <= 0) {
            try {
                contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    sizeBytes = pfd.statSize
                }
            } catch (_: Exception) {
            }
        }

        if (mimeType.isNullOrBlank()) {
            val ext = displayName!!.substringAfterLast('.', "").lowercase()
            mimeType = when (ext) {
                "pdf" -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "doc" -> "application/msword"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "xls" -> "application/vnd.ms-excel"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "ppt" -> "application/vnd.ms-powerpoint"
                "txt" -> "text/plain"
                "csv" -> "text/csv"
                else -> "application/octet-stream"
            }
        }

        return DocumentMetadata(
            displayName = displayName ?: "Document",
            sizeBytes = sizeBytes,
            mimeType = mimeType
        )
    }

    override fun getAllShelves(): Flow<List<Shelf>> {
        return shelfDao.getAllShelvesWithCount().map { list ->
            list.map { item ->
                Shelf(
                    id = item.id,
                    name = item.name,
                    colorIndex = item.colorIndex,
                    folderUri = item.folderUri,
                    fileCount = item.fileCount,
                    createdAt = item.createdAt
                )
            }
        }
    }

    override suspend fun createShelf(name: String, colorIndex: Int, folderUri: String?): Long = withContext(Dispatchers.IO) {
        val entity = ShelfEntity(
            name = name,
            colorIndex = colorIndex,
            folderUri = folderUri
        )
        shelfDao.insertShelf(entity)
    }

    override suspend fun updateShelf(shelf: Shelf) = withContext(Dispatchers.IO) {
        val entity = ShelfEntity(
            id = shelf.id,
            name = shelf.name,
            colorIndex = shelf.colorIndex,
            folderUri = shelf.folderUri,
            createdAt = shelf.createdAt
        )
        shelfDao.updateShelf(entity)
    }

    override suspend fun deleteShelf(id: Long) = withContext(Dispatchers.IO) {
        shelfDao.deleteShelf(id)
    }

    override suspend fun assignDocumentToShelf(documentUri: String, shelfId: Long) = withContext(Dispatchers.IO) {
        shelfDao.assignDocumentToShelf(ShelfAssignmentEntity(documentUri, shelfId))
    }

    override suspend fun unassignDocument(documentUri: String) = withContext(Dispatchers.IO) {
        shelfDao.unassignDocument(documentUri)
    }

    private data class DocumentMetadata(
        val displayName: String,
        val sizeBytes: Long,
        val mimeType: String
    )
}
