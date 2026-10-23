package com.abhisheksati.mydoc

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.data.repository.DocumentRepositoryImpl
import com.abhisheksati.mydoc.data.repository.MediaRepositoryImpl
import com.abhisheksati.mydoc.data.repository.StorageRepositoryImpl
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import com.abhisheksati.mydoc.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyDocApp : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var documentRepository: DocumentRepository
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var storageRepository: StorageRepository
        private set

    lateinit var libraryRepository: com.abhisheksati.mydoc.domain.repository.LibraryRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            try {
                com.artifex.mupdf.fitz.Context.init()
            } catch (_: Throwable) {}
        }
        database = AppDatabase.getInstance(this)
        documentRepository = DocumentRepositoryImpl(this, database)
        mediaRepository = MediaRepositoryImpl(this, database)
        storageRepository = StorageRepositoryImpl(this, database.trashDao())
        libraryRepository = com.abhisheksati.mydoc.data.repository.LibraryRepositoryImpl(documentRepository, mediaRepository, database)
        settingsRepository = SettingsRepository(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(true)
            .respectCacheHeaders(false)
            .dispatcher(Dispatchers.IO)
            .build()
    }
}
