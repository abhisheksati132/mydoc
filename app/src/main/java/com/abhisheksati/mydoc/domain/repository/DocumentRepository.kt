package com.abhisheksati.mydoc.domain.repository

import android.net.Uri
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.Shelf
import com.abhisheksati.mydoc.domain.model.ViewState
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getRecents(): Flow<List<DocumentFile>>
    fun getFavorites(): Flow<List<DocumentFile>>
    suspend fun registerOpenedDocument(uri: Uri): Result<DocumentFile>
    suspend fun toggleFavorite(document: DocumentFile)
    suspend fun removeFromRecents(uri: String)
    suspend fun updatePageProgress(uri: String, lastPage: Int, pageCount: Int)
    suspend fun getViewState(uri: String): ViewState?
    suspend fun saveViewState(viewState: ViewState)
    suspend fun clearAllRecents()
    suspend fun replaceDocument(oldUri: String, newUri: Uri): Result<DocumentFile>

    // Shelves Operations
    fun getAllShelves(): Flow<List<Shelf>>
    suspend fun createShelf(name: String, colorIndex: Int, folderUri: String? = null): Long
    suspend fun updateShelf(shelf: Shelf)
    suspend fun deleteShelf(id: Long)
    suspend fun assignDocumentToShelf(documentUri: String, shelfId: Long)
    suspend fun unassignDocument(documentUri: String)
}
