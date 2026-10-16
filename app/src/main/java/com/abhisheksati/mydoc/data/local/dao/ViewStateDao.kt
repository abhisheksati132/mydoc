package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhisheksati.mydoc.data.local.entity.ViewStateEntity

@Dao
interface ViewStateDao {

    @Query("SELECT * FROM view_states WHERE uri = :uri LIMIT 1")
    suspend fun getViewState(uri: String): ViewStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveViewState(viewState: ViewStateEntity)

    @Query("DELETE FROM view_states WHERE uri = :uri")
    suspend fun deleteViewState(uri: String)
}
