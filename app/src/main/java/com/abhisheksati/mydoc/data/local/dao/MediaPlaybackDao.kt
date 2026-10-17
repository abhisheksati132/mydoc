package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhisheksati.mydoc.data.local.entity.MediaPlaybackEntity

@Dao
interface MediaPlaybackDao {

    @Query("SELECT * FROM media_playback WHERE uri = :uri LIMIT 1")
    suspend fun getPlaybackPosition(uri: String): MediaPlaybackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackPosition(entity: MediaPlaybackEntity)

    @Query("DELETE FROM media_playback WHERE uri = :uri")
    suspend fun deletePlaybackPosition(uri: String)
}
