package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhisheksati.mydoc.data.local.entity.MediaPlaybackEntity

@Dao
interface MediaPlaybackDao {

    companion object {
        /** A list nobody can scroll to the end of is not a list; the index behind it is what keeps the query cheap. */
        const val MAX_LISTED = 500
    }


    @Query("SELECT * FROM media_playback WHERE uri = :uri LIMIT 1")
    suspend fun getPlaybackPosition(uri: String): MediaPlaybackEntity?

    @Query("SELECT * FROM media_playback ORDER BY updatedTimestamp DESC LIMIT :limit")
    fun getAllPlaybackPositions(limit: Int = MAX_LISTED): kotlinx.coroutines.flow.Flow<List<MediaPlaybackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackPosition(entity: MediaPlaybackEntity)

    @Query("DELETE FROM media_playback WHERE uri = :uri")
    suspend fun deletePlaybackPosition(uri: String)

    @Query("DELETE FROM media_playback")
    suspend fun clearAllPlaybacks()
}

