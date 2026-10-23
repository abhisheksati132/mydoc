package com.abhisheksati.mydoc.domain.repository

import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import kotlinx.coroutines.flow.Flow

enum class LibrarySort {
    RECENT,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC
}

interface LibraryRepository {
    fun getAllItems(): Flow<List<LibraryItem>>
    fun getItemsByKind(kind: MediaKind): Flow<List<LibraryItem>>
    fun searchItems(query: String, kind: MediaKind? = null): Flow<List<LibraryItem>>
    fun continueItems(limit: Int = 10): Flow<List<LibraryItem>>
    fun recentlyAdded(limit: Int = 20): Flow<List<LibraryItem>>
}
