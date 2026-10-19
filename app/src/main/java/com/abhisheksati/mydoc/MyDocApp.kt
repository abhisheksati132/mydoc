package com.abhisheksati.mydoc

import android.app.Application
import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.data.repository.DocumentRepositoryImpl
import com.abhisheksati.mydoc.domain.repository.DocumentRepository

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build
import com.abhisheksati.mydoc.data.repository.MediaRepositoryImpl
import com.abhisheksati.mydoc.domain.repository.MediaRepository

import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyDocApp : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var documentRepository: DocumentRepository
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var settingsRepository: com.abhisheksati.mydoc.data.local.SettingsRepository
        private set

    lateinit var storageRepository: com.abhisheksati.mydoc.domain.repository.StorageRepository
        private set

    lateinit var wifiServerManager: com.abhisheksati.mydoc.network.WifiServerManager
        private set

    lateinit var libreOfficeEngineManager: com.abhisheksati.mydoc.rendering.engine.libreoffice.LibreOfficeEngineManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Crash-Guard & Emergency Recovery: If the process previously terminated during native init,
        // clear the marker and disable the engine so the app starts cleanly without looping,
        // while preserving downloaded files so the user never has to re-download.
        val initMarker = java.io.File(filesDir, "libreoffice_init.marker")
        val crashDetected = initMarker.exists()
        if (crashDetected) {
            initMarker.delete()
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                com.artifex.mupdf.fitz.Context.init()
            } catch (_: Throwable) {}
        }
        database = AppDatabase.getInstance(this)
        documentRepository = DocumentRepositoryImpl(this, database)
        mediaRepository = MediaRepositoryImpl(this, database)
        storageRepository = com.abhisheksati.mydoc.data.repository.StorageRepositoryImpl(this, database.trashDao())
        settingsRepository = com.abhisheksati.mydoc.data.local.SettingsRepository(this)
        if (crashDetected) {
            settingsRepository.setLibreOfficeEngineEnabled(false)
        }
        wifiServerManager = com.abhisheksati.mydoc.network.WifiServerManager(this)
        libreOfficeEngineManager = com.abhisheksati.mydoc.rendering.engine.libreoffice.LibreOfficeEngineManager.getInstance(this)
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
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(true)
            .respectCacheHeaders(false)
            .dispatcher(Dispatchers.IO)
            .build()
    }
}
