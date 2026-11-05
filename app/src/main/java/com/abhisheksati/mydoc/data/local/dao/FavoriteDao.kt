package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhisheksati.mydoc.data.local.entity.FavoriteDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    companion object {
        /** A list nobody can scroll to the end of is not a list; the index behind it is what keeps the query cheap. */
        const val MAX_LISTED = 500
    }


    @Query("SELECT * FROM favorite_documents ORDER BY addedTimestamp DESC LIMIT :limit")
    fun getAllFavorites(limit: Int = MAX_LISTED): Flow<List<FavoriteDocumentEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_documents WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteDocumentEntity)

    @Query("DELETE FROM favorite_documents WHERE uri = :uri")
    suspend fun deleteFavorite(uri: String)
}
