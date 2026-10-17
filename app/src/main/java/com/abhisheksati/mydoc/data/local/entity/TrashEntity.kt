package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.abhisheksati.mydoc.domain.model.TrashItem

@Entity(tableName = "trash_items")
data class TrashEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val displayName: String,
    val sizeBytes: Long,
    val deletedTimestamp: Long,
    val isDirectory: Boolean
) {
    fun toDomain(): TrashItem = TrashItem(
        id = id,
        originalPath = originalPath,
        trashPath = trashPath,
        displayName = displayName,
        sizeBytes = sizeBytes,
        deletedTimestamp = deletedTimestamp,
        isDirectory = isDirectory
    )

    companion object {
        fun fromDomain(item: TrashItem): TrashEntity = TrashEntity(
            id = item.id,
            originalPath = item.originalPath,
            trashPath = item.trashPath,
            displayName = item.displayName,
            sizeBytes = item.sizeBytes,
            deletedTimestamp = item.deletedTimestamp,
            isDirectory = item.isDirectory
        )
    }
}
