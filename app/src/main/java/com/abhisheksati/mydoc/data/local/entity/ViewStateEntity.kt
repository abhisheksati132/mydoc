package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "view_states")
data class ViewStateEntity(
    @PrimaryKey
    val uri: String,
    val lastPage: Int,
    val zoomScale: Float,
    val scrollOffsetX: Float,
    val scrollOffsetY: Float,
    val readingMode: String,
    val isContinuousScroll: Boolean,
    val updatedTimestamp: Long
)
