package com.abhisheksati.mydoc.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Shelf(
    val id: Long = 0,
    val name: String,
    val colorIndex: Int,          // 0..5
    val folderUri: String? = null,
    val fileCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
