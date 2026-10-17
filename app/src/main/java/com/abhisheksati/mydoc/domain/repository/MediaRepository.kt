package com.abhisheksati.mydoc.domain.repository

import android.net.Uri
import com.abhisheksati.mydoc.domain.model.ExifMetadata
import com.abhisheksati.mydoc.domain.model.MediaFilter
import com.abhisheksati.mydoc.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMedia(filter: MediaFilter = MediaFilter.ALL): Flow<List<MediaItem>>
    suspend fun getMediaItem(uri: Uri): MediaItem?
    suspend fun extractExif(uri: Uri): ExifMetadata
    suspend fun getPlaybackPosition(uri: String): Long
    suspend fun savePlaybackPosition(uri: String, positionMs: Long, durationMs: Long)
}
