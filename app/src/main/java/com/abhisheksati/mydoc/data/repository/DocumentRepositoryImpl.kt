package com.abhisheksati.mydoc.data.repository

import com.abhisheksati.mydoc.util.DocumentIdentity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.data.local.OfflineCopyPolicy
import com.abhisheksati.mydoc.data.local.OfflineCopyStamp
import com.abhisheksati.mydoc.data.local.entity.FavoriteDocumentEntity
import com.abhisheksati.mydoc.data.local.entity.RecentDocumentEntity
import com.abhisheksati.mydoc.data.local.entity.ViewStateEntity
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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

    override fun getRecents(): Flow<List<DocumentFile>> {
        return combine(
            recentDao.getAllRecents(),
            favoriteDao.getFavoriteUris()
        ) { recents, favorites ->
            val favUris = favorites.toSet()
            recents.map { recent ->
                DocumentFile(
                    uri = recent.uri,
                    displayName = recent.displayName,
                    sizeBytes = recent.sizeBytes,
                    mimeType = recent.mimeType,
                    lastOpenedTimestamp = recent.lastOpenedTimestamp,
                    pageCount = recent.pageCount,
                    lastPage = recent.lastPage,
                    isFavorite = favUris.contains(recent.uri)
                )
            }
        }
    }

    override fun getFavorites(): Flow<List<DocumentFile>> {
        return combine(
            favoriteDao.getAllFavorites(),
            recentDao.getAllRecents()
        ) { favorites, recents ->
            val recentsMap = recents.associateBy { it.uri }
            favorites.map { fav ->
                val recent = recentsMap[fav.uri]
                DocumentFile(
                    uri = fav.uri,
                    displayName = fav.displayName,
                    sizeBytes = fav.sizeBytes,
                    mimeType = fav.mimeType,
                    lastOpenedTimestamp = recent?.lastOpenedTimestamp ?: fav.addedTimestamp,
                    pageCount = recent?.pageCount ?: 0,
                    lastPage = recent?.lastPage ?: 0,
                    isFavorite = true
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    override fun getAllDeviceDocuments(): Flow<List<DocumentFile>> = combine(
        favoriteDao.getFavoriteUris(),
        recentDao.getAllRecents()
    ) { favorites, recents ->
        val favUris = favorites.toSet()
        val recentsMap = recents.associateBy { it.uri }

        withContext(Dispatchers.IO) {
            val deviceDocs = mutableListOf<DocumentFile>()
            val seenPaths = mutableSetOf<String>()
            val seenUris = mutableSetOf<String>()
            // Identity is the URI (or the real path), not name+size: two different files can
            // share both of those, and one of them used to disappear from the Library.

            try {
                val uri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                )
                val selection = """
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.pdf' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.doc' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.docx' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.xls' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.xlsx' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.ppt' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.pptx' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.txt' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.csv' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.md' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.epub' OR
                    ${MediaStore.Files.FileColumns.DATA} LIKE '%.fb2'
                """.trimIndent()

                contentResolver.query(
                    uri,
                    projection,
                    selection,
                    null,
                    "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val data = cursor.getString(dataCol) ?: continue
                        val file = File(data)
                        val docUri = if (file.exists()) {
                            Uri.fromFile(file).toString()
                        } else {
                            val id = cursor.getLong(idCol)
                            ContentUris.withAppendedId(uri, id).toString()
                        }

                        val name = cursor.getString(nameCol) ?: file.name
                        val size = cursor.getLong(sizeCol)
                        if (seenPaths.contains(data) || seenUris.contains(docUri)) continue
                        seenPaths.add(data)
                        seenUris.add(docUri)

                        val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                        val dateModified = cursor.getLong(dateCol) * 1000L

                        val recent = recentsMap[docUri]

                        deviceDocs.add(
                            DocumentFile(
                                uri = docUri,
                                displayName = name,
                                sizeBytes = size,
                                mimeType = mime,
                                lastOpenedTimestamp = recent?.lastOpenedTimestamp ?: dateModified,
                                pageCount = recent?.pageCount ?: 0,
                                lastPage = recent?.lastPage ?: 0,
                                isFavorite = favUris.contains(docUri)
                            )
                        )
                    }
                }
                        } catch (e: SecurityException) {
                android.util.Log.w("DocumentRepository", "Permission denied reading MediaStore documents", e)
            } catch (e: Exception) {
                android.util.Log.w("DocumentRepository", "Error querying MediaStore documents", e)
            }

            // Also merge any recents that might not be indexed in MediaStore
            recents.forEach { recent ->
                val recentUri = recent.uri
                val recentPath = try { Uri.parse(recentUri).path } catch (_: Exception) { null }
                val isSeen = seenUris.contains(recentUri) ||
                    (recentPath != null && seenPaths.contains(recentPath))
                if (!isSeen) {
                    seenUris.add(recentUri)
                    if (recentPath != null) seenPaths.add(recentPath)
                    deviceDocs.add(
                        DocumentFile(
                            uri = recent.uri,
                            displayName = recent.displayName,
                            sizeBytes = recent.sizeBytes,
                            mimeType = recent.mimeType,
                            lastOpenedTimestamp = recent.lastOpenedTimestamp,
                            pageCount = recent.pageCount,
                            lastPage = recent.lastPage,
                            isFavorite = favUris.contains(recent.uri)
                        )
                    )
                }
            }

            deviceDocs.distinctBy { it.uri }
        }
    }.flowOn(Dispatchers.Default)

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
                val cacheName = DocumentIdentity.savedDocsName(uri.toString(), metadata.displayName)
                val localFile = java.io.File(savedDocsDir, cacheName)
                val metaFile = java.io.File(savedDocsDir, OfflineCopyPolicy.metaFileFor(cacheName))
                val recorded = runCatching {
                    if (metaFile.exists()) OfflineCopyPolicy.parse(metaFile.readText()) else null
                }.getOrNull()
                val source = OfflineCopyStamp(metadata.sizeBytes, metadata.lastModified)
                if (OfflineCopyPolicy.needsRefresh(recorded, localFile.length(), source)) {
                    // Write to a temp file then rename: opening localFile's own outputStream would
                    // truncate it to 0 before the copy, destroying the backup when the resolved
                    // stream is this same saved_docs file (tier-2 fallback).
                    val tmpFile = java.io.File(savedDocsDir, "$cacheName.tmp")
                    com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri)?.use { input ->
                        tmpFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tmpFile.exists() && tmpFile.length() > 0L) {
                        if (!tmpFile.renameTo(localFile)) {
                            tmpFile.copyTo(localFile, overwrite = true)
                            tmpFile.delete()
                        }
                        // The stamp is only written for a copy that completed, so an interrupted
                        // refresh is retried next time instead of being believed current.
                        runCatching { metaFile.writeText(OfflineCopyPolicy.format(source)) }
                    } else {
                        tmpFile.delete()
                        metaFile.delete()
                    }
                }
                localFile.setLastModified(System.currentTimeMillis())
                com.abhisheksati.mydoc.data.local.DocumentStreamResolver.trimSavedDocsCache(savedDocsDir)
            } catch (e: java.io.IOException) {
                android.util.Log.w("DocumentRepository", "Failed caching local document copy", e)
            } catch (e: Exception) {
                android.util.Log.w("DocumentRepository", "Unexpected error caching local document", e)
            }

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
        var lastModified: Long = 0
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
                        // Providers are not obliged to report it, and it is in seconds.
                        val modifiedIndex = cursor.getColumnIndex(
                            android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
                        )
                        if (modifiedIndex != -1 && !cursor.isNull(modifiedIndex)) {
                            lastModified = cursor.getLong(modifiedIndex) * 1000L
                        }
                    }
                }
                        } catch (e: SecurityException) {
                android.util.Log.w("DocumentRepository", "Permission denied querying document metadata for $uri", e)
            } catch (e: Exception) {
                android.util.Log.w("DocumentRepository", "Error querying document metadata for $uri", e)
            }
        } else if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            displayName = file.name
            sizeBytes = file.length()
            lastModified = file.lastModified()
        }

        if (displayName.isNullOrBlank()) {
            displayName = uri.lastPathSegment ?: "Document"
        }

        if (sizeBytes <= 0) {
            try {
                contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    sizeBytes = pfd.statSize
                }
                        } catch (e: java.io.FileNotFoundException) {
                android.util.Log.w("DocumentRepository", "File not found when inspecting size for $uri", e)
            } catch (e: Exception) {
                android.util.Log.w("DocumentRepository", "Error opening file descriptor for size calculation", e)
            }
        }

        if (mimeType.isNullOrBlank()) {
            val ext = displayName.substringAfterLast('.', "").lowercase()
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
            displayName = displayName,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            lastModified = lastModified
        )
    }


    private data class DocumentMetadata(
        val displayName: String,
        val sizeBytes: Long,
        val mimeType: String,
        /** When the document itself changed, 0 when no provider reported it. */
        val lastModified: Long
    )
}
