package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_documents")
data class RecentDocumentEntity(
    @PrimaryKey
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val lastOpenedTimestamp: Long,
    val pageCount: Int = 0,
    val lastPage: Int = 0
)
