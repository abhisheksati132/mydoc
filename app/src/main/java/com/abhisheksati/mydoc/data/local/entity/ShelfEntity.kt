package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorIndex: Int,          // 0..5 mapping to the theme's 6 shelf colors
    val folderUri: String? = null, // Auto-bind folder URI if created from a detected folder
    val createdAt: Long = System.currentTimeMillis()
)
