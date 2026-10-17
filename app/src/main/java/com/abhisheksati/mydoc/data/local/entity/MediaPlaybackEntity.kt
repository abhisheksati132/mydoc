package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_playback")
data class MediaPlaybackEntity(
    @PrimaryKey
    val uri: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedTimestamp: Long
)
