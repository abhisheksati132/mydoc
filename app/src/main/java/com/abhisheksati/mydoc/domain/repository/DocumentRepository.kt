package com.abhisheksati.mydoc.domain.repository

import android.net.Uri
import com.abhisheksati.mydoc.domain.model.DocumentFile
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
}
