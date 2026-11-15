package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A saved passage plus an optional study note, scoped to one document. */
@Entity(
    tableName = "highlights",
    indices = [Index("documentUri")]
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentUri: String,
    val pageIndex: Int,
    val text: String,
    val note: String? = null,
    val createdTimestamp: Long
)
