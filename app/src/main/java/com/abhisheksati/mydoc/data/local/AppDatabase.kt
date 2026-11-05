package com.abhisheksati.mydoc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abhisheksati.mydoc.data.local.dao.FavoriteDao
import com.abhisheksati.mydoc.data.local.dao.MediaPlaybackDao
import com.abhisheksati.mydoc.data.local.dao.RecentDao
import com.abhisheksati.mydoc.data.local.dao.TrashDao
import com.abhisheksati.mydoc.data.local.dao.ViewStateDao
import com.abhisheksati.mydoc.data.local.entity.FavoriteDocumentEntity
import com.abhisheksati.mydoc.data.local.entity.MediaPlaybackEntity
import com.abhisheksati.mydoc.data.local.entity.RecentDocumentEntity
import com.abhisheksati.mydoc.data.local.entity.TrashEntity
import com.abhisheksati.mydoc.data.local.entity.ViewStateEntity


val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `shelf_assignments`")
        db.execSQL("DROP TABLE IF EXISTS `shelves`")
    }
}

/**
 * The favourites and playback lists are ordered by their timestamp columns, which were unindexed,
 * and both lists were unbounded. The index names below are the ones Room generates, so a mismatch
 * would fail schema validation on open rather than quietly do nothing.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_favorite_documents_addedTimestamp` " +
                "ON `favorite_documents` (`addedTimestamp`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_playback_updatedTimestamp` " +
                "ON `media_playback` (`updatedTimestamp`)"
        )
    }
}

val MIGRATION_3_5 = object : Migration(3, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Direct jump from 3 to 5 (shelves were only added in 4 and removed in 5)
    }
}

@Database(
    entities = [
        RecentDocumentEntity::class,
        FavoriteDocumentEntity::class,
        ViewStateEntity::class,
        MediaPlaybackEntity::class,
        TrashEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentDao(): RecentDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun viewStateDao(): ViewStateDao
    abstract fun mediaPlaybackDao(): MediaPlaybackDao
    abstract fun trashDao(): TrashDao

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
                    .addMigrations(MIGRATION_4_5, MIGRATION_3_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigrationFrom(true, 1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
