package com.abhisheksati.mydoc.data.repository

import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaFilter
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import com.abhisheksati.mydoc.domain.repository.LibraryRepository
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class LibraryRepositoryImpl(
    private val documentRepository: DocumentRepository,
    private val mediaRepository: MediaRepository,
    private val database: AppDatabase
) : LibraryRepository {

    private val mediaPlaybackDao = database.mediaPlaybackDao()

    override fun getAllItems(): Flow<List<LibraryItem>> = combine(
        documentRepository.getAllDeviceDocuments(),
        mediaRepository.getMedia(MediaFilter.ALL),
        mediaPlaybackDao.getAllPlaybackPositions()
    ) { docs, media, playbacks ->
        val playbackMap = playbacks.associateBy { it.uri }
        val docItems = docs.map { doc ->
            LibraryItem(
                uri = doc.uri,
                name = doc.displayName,
                kind = MediaKind.DOCUMENT,
                mimeType = doc.mimeType,
                sizeBytes = doc.sizeBytes,
                lastModified = doc.lastOpenedTimestamp,
                isFavorite = doc.isFavorite,
                lastPage = doc.lastPage,
                pageCount = doc.pageCount,
                progressFraction = if (doc.pageCount > 0) (doc.lastPage.toFloat() / doc.pageCount).coerceIn(0f, 1f) else 0f
            )
        }

        val mediaItems = media.map { m ->
            val kind = when {
                m.isAudio -> MediaKind.AUDIO
                m.isVideo -> MediaKind.VIDEO
                else -> MediaKind.PHOTO
            }
            val pb = playbackMap[m.uri.toString()]
            val lastPos = pb?.positionMs ?: 0L
            val dur = if ((pb?.durationMs ?: 0L) > 0L) pb!!.durationMs else m.durationMs
            val progress = if (dur > 0L) (lastPos.toFloat() / dur).coerceIn(0f, 1f) else 0f

            LibraryItem(
                uri = m.uri.toString(),
                name = m.displayName,
                kind = kind,
                mimeType = m.mimeType,
                sizeBytes = m.sizeBytes,
                lastModified = if (m.dateModifiedSec > 0) m.dateModifiedSec * 1000L else m.dateAddedSec * 1000L,
                folder = m.bucketName,
                lastPositionMs = lastPos,
                durationMs = dur,
                progressFraction = progress
            )
        }

        (docItems + mediaItems).sortedByDescending { it.lastModified }
    }

    override fun getItemsByKind(kind: MediaKind): Flow<List<LibraryItem>> {
        return getAllItems().map { list ->
            list.filter { it.kind == kind }
        }
    }

    override fun searchItems(query: String, kind: MediaKind?): Flow<List<LibraryItem>> {
        return getAllItems().map { list ->
            val q = query.trim().lowercase()
            list.filter { item ->
                (kind == null || item.kind == kind) &&
                (q.isEmpty() || item.name.lowercase().contains(q))
            }
        }
    }

    override fun continueItems(limit: Int): Flow<List<LibraryItem>> = combine(
        documentRepository.getRecents(),
        mediaRepository.getMedia(MediaFilter.ALL),
        mediaPlaybackDao.getAllPlaybackPositions()
    ) { recents, media, playbacks ->
        val playbackMap = playbacks.associateBy { it.uri }
        val mediaByUri = media.associateBy { it.uri.toString() }

        val continueDocs = recents
            .filter { it.lastOpenedTimestamp > 0 }
            .map { doc ->
                LibraryItem(
                    uri = doc.uri,
                    name = doc.displayName,
                    kind = MediaKind.DOCUMENT,
                    mimeType = doc.mimeType,
                    sizeBytes = doc.sizeBytes,
                    lastModified = doc.lastOpenedTimestamp,
                    isFavorite = doc.isFavorite,
                    lastPage = doc.lastPage,
                    pageCount = doc.pageCount,
                    progressFraction = if (doc.pageCount > 0) (doc.lastPage.toFloat() / doc.pageCount).coerceIn(0f, 1f) else 0f
                )
            }

        val continueMedia = playbacks
            .filter { it.positionMs > 1000L }
            .mapNotNull { pb ->
                val m = mediaByUri[pb.uri]
                val dur = if (pb.durationMs > 0) pb.durationMs else (m?.durationMs ?: 0L)
                val progress = if (dur > 0L) (pb.positionMs.toFloat() / dur).coerceIn(0f, 1f) else 0f
                if (progress >= 0.98f) return@mapNotNull null
                val kind = when {
                    m?.isAudio == true -> MediaKind.AUDIO
                    m?.isVideo == true -> MediaKind.VIDEO
                    else -> MediaKind.VIDEO
                }
                LibraryItem(
                    uri = pb.uri,
                    name = m?.displayName ?: "Media",
                    kind = kind,
                    mimeType = m?.mimeType ?: "video/mp4",
                    sizeBytes = m?.sizeBytes ?: 0L,
                    lastModified = pb.updatedTimestamp,
                    folder = m?.bucketName,
                    lastPositionMs = pb.positionMs,
                    durationMs = dur,
                    progressFraction = progress
                )
            }

        (continueDocs + continueMedia)
            .sortedByDescending { it.lastModified }
            .take(limit)
    }

    override fun recentlyAdded(limit: Int): Flow<List<LibraryItem>> {
        return getAllItems().map { it.take(limit) }
    }
}
