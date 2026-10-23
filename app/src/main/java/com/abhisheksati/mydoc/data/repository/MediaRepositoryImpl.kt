package com.abhisheksati.mydoc.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.data.local.entity.MediaPlaybackEntity
import com.abhisheksati.mydoc.domain.model.ExifMetadata
import com.abhisheksati.mydoc.domain.model.MediaFilter
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase
) : MediaRepository {

    private val contentResolver = context.contentResolver
    private val mediaPlaybackDao = database.mediaPlaybackDao()

    override fun getMedia(filter: MediaFilter): Flow<List<MediaItem>> = flow {
        val items = queryMediaStore(filter)
        emit(items)
    }.flowOn(Dispatchers.IO)

    private fun queryMediaStore(filter: MediaFilter): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        if (filter == MediaFilter.ALL || filter == MediaFilter.PHOTOS) {
            queryImages(mediaList)
        }

        if (filter == MediaFilter.ALL || filter == MediaFilter.VIDEOS) {
            queryVideos(mediaList)
        }

        if (filter == MediaFilter.ALL || filter == MediaFilter.AUDIO) {
            queryAudio(mediaList)
        }

        // Sort by date added descending (newest first)
        mediaList.sortByDescending { it.dateAddedSec }
        return mediaList
    }

    private fun queryImages(target: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val orientCol = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val name = cursor.getString(nameCol) ?: "Image"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateMod = cursor.getLong(dateModCol)
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val orient = if (orientCol != -1) cursor.getInt(orientCol) else 0
                    val bucketId = if (bucketIdCol != -1) cursor.getString(bucketIdCol) else null
                    val bucketName = if (bucketNameCol != -1) cursor.getString(bucketNameCol) else null
                    val dataPath = if (dataCol != -1) cursor.getString(dataCol) else null

                    target.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            sizeBytes = size,
                            mimeType = mime,
                            dateAddedSec = dateAdded,
                            dateModifiedSec = dateMod,
                            width = width,
                            height = height,
                            durationMs = 0L,
                            orientation = orient,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            isVideo = false,
                            filePath = dataPath
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun queryVideos(target: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val name = cursor.getString(nameCol) ?: "Video"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateMod = cursor.getLong(dateModCol)
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val bucketId = if (bucketIdCol != -1) cursor.getString(bucketIdCol) else null
                    val bucketName = if (bucketNameCol != -1) cursor.getString(bucketNameCol) else null
                    val dataPath = if (dataCol != -1) cursor.getString(dataCol) else null

                    target.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            sizeBytes = size,
                            mimeType = mime,
                            dateAddedSec = dateAdded,
                            dateModifiedSec = dateMod,
                            width = width,
                            height = height,
                            durationMs = duration,
                            orientation = 0,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            isVideo = true,
                            filePath = dataPath
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun queryAudio(target: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val name = cursor.getString(nameCol) ?: "Audio"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "audio/mpeg"
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateMod = cursor.getLong(dateModCol)
                    val dur = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val dataPath = if (dataCol != -1) cursor.getString(dataCol) else null

                    target.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            sizeBytes = size,
                            mimeType = mime,
                            dateAddedSec = dateAdded,
                            dateModifiedSec = dateMod,
                            durationMs = dur,
                            isAudio = true,
                            filePath = dataPath
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    override suspend fun getMediaItem(uri: Uri): MediaItem? = withContext(Dispatchers.IO) {
        var name = "Media"
        var size: Long = 0
        var mime = contentResolver.getType(uri) ?: "application/octet-stream"
        var resolvedPath: String? = null

        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                        val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (dataIdx != -1) resolvedPath = cursor.getString(dataIdx)
                    }
                }
            } catch (_: Exception) {}
        } else if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            name = file.name
            size = file.length()
            resolvedPath = file.absolutePath
        }

        val isVideo = mime.startsWith("video/") || mime.startsWith("audio/") ||
                name.endsWith(".mp4", ignoreCase = true) ||
                name.endsWith(".mkv", ignoreCase = true) ||
                name.endsWith(".webm", ignoreCase = true) ||
                name.endsWith(".3gp", ignoreCase = true) ||
                name.endsWith(".mov", ignoreCase = true)

        MediaItem(
            id = uri.hashCode().toLong(),
            uri = uri,
            displayName = name,
            sizeBytes = size,
            mimeType = mime,
            dateAddedSec = System.currentTimeMillis() / 1000,
            isVideo = isVideo,
            filePath = resolvedPath
        )
    }

    override suspend fun extractExif(uri: Uri): ExifMetadata = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val latLong = exif.latLong
                ExifMetadata(
                    width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0),
                    height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0),
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE),
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL),
                    aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                    exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" },
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY),
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" },
                    dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
                    latitude = latLong?.get(0),
                    longitude = latLong?.get(1)
                )
            } ?: ExifMetadata()
        } catch (_: Throwable) {
            ExifMetadata()
        }
    }

    override suspend fun getPlaybackPosition(uri: String): Long = withContext(Dispatchers.IO) {
        mediaPlaybackDao.getPlaybackPosition(uri)?.positionMs ?: 0L
    }

    override suspend fun savePlaybackPosition(uri: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        mediaPlaybackDao.savePlaybackPosition(
            MediaPlaybackEntity(
                uri = uri,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }
}
