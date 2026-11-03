package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhisheksati.mydoc.data.local.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Query("SELECT * FROM trash_items ORDER BY deletedTimestamp DESC")
    fun getAllTrashItems(): Flow<List<TrashEntity>>

    @Query("SELECT * FROM trash_items ORDER BY deletedTimestamp DESC")
    suspend fun getAllTrashItemsSync(): List<TrashEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TrashEntity): Long

    @Query("SELECT * FROM trash_items WHERE deletedTimestamp < :thresholdTimestamp")
    suspend fun getItemsOlderThan(thresholdTimestamp: Long): List<TrashEntity>

    @Query("DELETE FROM trash_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM trash_items")
    suspend fun clearAll()

    @Query("DELETE FROM trash_items WHERE deletedTimestamp < :thresholdTimestamp")
    suspend fun purgeOlderThan(thresholdTimestamp: Long)
}
