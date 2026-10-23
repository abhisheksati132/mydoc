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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `shelves` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `colorIndex` INTEGER NOT NULL,
                `folderUri` TEXT,
                `createdAt` INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `shelf_assignments` (
                `documentUri` TEXT NOT NULL,
                `shelfId` INTEGER NOT NULL,
                PRIMARY KEY(`documentUri`, `shelfId`),
                FOREIGN KEY(`shelfId`) REFERENCES `shelves`(`id`) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shelf_assignments_shelfId` ON `shelf_assignments` (`shelfId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shelf_assignments_documentUri` ON `shelf_assignments` (`documentUri`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `shelf_assignments`")
        db.execSQL("DROP TABLE IF EXISTS `shelves`")
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
    version = 5,
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_3_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
