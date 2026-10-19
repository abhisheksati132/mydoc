package com.abhisheksati.mydoc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.abhisheksati.mydoc.data.local.dao.FavoriteDao
import com.abhisheksati.mydoc.data.local.dao.RecentDao
import com.abhisheksati.mydoc.data.local.dao.ViewStateDao
import com.abhisheksati.mydoc.data.local.entity.FavoriteDocumentEntity
import com.abhisheksati.mydoc.data.local.entity.RecentDocumentEntity
import com.abhisheksati.mydoc.data.local.entity.ViewStateEntity

@Database(
    entities = [
        RecentDocumentEntity::class,
        FavoriteDocumentEntity::class,
        ViewStateEntity::class,
        com.abhisheksati.mydoc.data.local.entity.MediaPlaybackEntity::class,
        com.abhisheksati.mydoc.data.local.entity.TrashEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentDao(): RecentDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun viewStateDao(): ViewStateDao
    abstract fun mediaPlaybackDao(): com.abhisheksati.mydoc.data.local.dao.MediaPlaybackDao
    abstract fun trashDao(): com.abhisheksati.mydoc.data.local.dao.TrashDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mydoc.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
