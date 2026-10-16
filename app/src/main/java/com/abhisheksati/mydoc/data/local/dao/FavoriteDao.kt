package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhisheksati.mydoc.data.local.entity.FavoriteDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_documents ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteDocumentEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_documents WHERE uri = :uri)")
    fun isFavoriteFlow(uri: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_documents WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteDocumentEntity)

    @Query("DELETE FROM favorite_documents WHERE uri = :uri")
    suspend fun deleteFavorite(uri: String)
}
