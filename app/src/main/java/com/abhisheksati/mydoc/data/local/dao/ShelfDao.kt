package com.abhisheksati.mydoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.abhisheksati.mydoc.data.local.entity.ShelfAssignmentEntity
import com.abhisheksati.mydoc.data.local.entity.ShelfEntity
import kotlinx.coroutines.flow.Flow

data class ShelfWithCount(
    val id: Long,
    val name: String,
    val colorIndex: Int,
    val folderUri: String?,
    val createdAt: Long,
    val fileCount: Int
)

data class DocumentShelfInfo(
    val documentUri: String,
    val shelfId: Long,
    val shelfName: String,
    val colorIndex: Int
)

@Dao
interface ShelfDao {
    @Query("""
        SELECT s.id, s.name, s.colorIndex, s.folderUri, s.createdAt,
               COUNT(sa.documentUri) as fileCount
        FROM shelves s
        LEFT JOIN shelf_assignments sa ON s.id = sa.shelfId
        GROUP BY s.id
        ORDER BY s.createdAt ASC
    """)
    fun getAllShelvesWithCount(): Flow<List<ShelfWithCount>>

    @Query("SELECT * FROM shelves ORDER BY createdAt ASC")
    fun getAllShelves(): Flow<List<ShelfEntity>>

    @Query("SELECT * FROM shelves WHERE id = :id")
    suspend fun getShelfById(id: Long): ShelfEntity?

    @Query("SELECT * FROM shelves WHERE folderUri = :folderUri LIMIT 1")
    suspend fun getShelfByFolderUri(folderUri: String): ShelfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelf: ShelfEntity): Long

    @Update
    suspend fun updateShelf(shelf: ShelfEntity)

    @Query("DELETE FROM shelves WHERE id = :id")
    suspend fun deleteShelf(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignDocumentToShelf(assignment: ShelfAssignmentEntity)

    @Query("DELETE FROM shelf_assignments WHERE documentUri = :documentUri")
    suspend fun unassignDocument(documentUri: String)

    @Query("SELECT shelfId FROM shelf_assignments WHERE documentUri = :documentUri LIMIT 1")
    fun getShelfIdForDocument(documentUri: String): Flow<Long?>

    @Query("SELECT shelfId FROM shelf_assignments WHERE documentUri = :documentUri LIMIT 1")
    suspend fun getShelfIdForDocumentSync(documentUri: String): Long?

    @Query("""
        SELECT sa.documentUri, sa.shelfId, s.name as shelfName, s.colorIndex
        FROM shelf_assignments sa
        INNER JOIN shelves s ON sa.shelfId = s.id
    """)
    fun getAllDocumentShelfInfos(): Flow<List<DocumentShelfInfo>>
}
