package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhisheksati.mydoc.data.local.entity.RecentDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {

    @Query("SELECT * FROM recent_documents ORDER BY lastOpenedTimestamp DESC")
    fun getAllRecents(): Flow<List<RecentDocumentEntity>>

    @Query("SELECT * FROM recent_documents WHERE uri = :uri LIMIT 1")
    suspend fun getRecentByUri(uri: String): RecentDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecent(recent: RecentDocumentEntity)

    @Query("UPDATE recent_documents SET lastPage = :lastPage, pageCount = :pageCount, lastOpenedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updatePageProgress(uri: String, lastPage: Int, pageCount: Int, timestamp: Long)

    @Query("DELETE FROM recent_documents WHERE uri = :uri")
    suspend fun deleteRecent(uri: String)

    @Query("DELETE FROM recent_documents")
    suspend fun clearAllRecents()
}
