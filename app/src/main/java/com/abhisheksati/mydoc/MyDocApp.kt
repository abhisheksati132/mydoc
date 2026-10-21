package com.abhisheksati.mydoc

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.data.repository.DocumentRepositoryImpl
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyDocApp : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var documentRepository: DocumentRepository
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
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(true)
            .respectCacheHeaders(false)
            .dispatcher(Dispatchers.IO)
            .build()
    }
}
