package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.abhisheksati.mydoc.data.local.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    @Query("SELECT * FROM highlights WHERE documentUri = :documentUri ORDER BY pageIndex ASC, createdTimestamp ASC")
    suspend fun getForDocument(documentUri: String): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE documentUri = :documentUri ORDER BY pageIndex ASC, createdTimestamp ASC")
    fun observeForDocument(documentUri: String): Flow<List<HighlightEntity>>

    @Insert
    suspend fun insert(highlight: HighlightEntity): Long

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun delete(id: Long)
}
