package com.abhisheksati.mydoc

import android.app.Application
import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.data.repository.DocumentRepositoryImpl
import com.abhisheksati.mydoc.domain.repository.DocumentRepository

class MyDocApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var documentRepository: DocumentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        documentRepository = DocumentRepositoryImpl(this, database)
    }
}
