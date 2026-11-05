package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_documents",
    indices = [Index("addedTimestamp")]
)
data class FavoriteDocumentEntity(
    @PrimaryKey
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val addedTimestamp: Long
)
