package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_playback",
    indices = [Index("updatedTimestamp")]
)
data class MediaPlaybackEntity(
    @PrimaryKey
    val uri: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedTimestamp: Long
)
