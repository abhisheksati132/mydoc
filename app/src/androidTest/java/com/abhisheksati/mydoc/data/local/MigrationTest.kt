package com.abhisheksati.mydoc.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.data.local.dao.FavoriteDao
import com.abhisheksati.mydoc.data.local.entity.FavoriteDocumentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate3to5() {
        // Create version 3 database
        helper.createDatabase("test-db", 3).apply { close() }
        // Run migrations to version 5
        val db = helper.runMigrationsAndValidate("test-db", 5, true, MIGRATION_3_5)
        db.close()
    }

    /**
     * N-29. The favourites and playback lists are ordered by columns that had no index, and neither
     * list was bounded. The migration must add the indexes Room expects, and it must not lose a
     * single record on the way: these rows are the user's data.
     */
    @Test
    fun migrate5to6AddsIndexesAndKeepsEveryRecord() {
        val name = "migrate-5-6"
        helper.createDatabase(name, 5).apply {
            execSQL(
                "INSERT INTO favorite_documents (uri, displayName, sizeBytes, mimeType, addedTimestamp) " +
                    "VALUES ('content://five', 'before.pdf', 10, 'application/pdf', 1000)"
            )
            execSQL(
                "INSERT INTO media_playback (uri, positionMs, durationMs, updatedTimestamp) " +
                    "VALUES ('content://five', 4200, 90000, 2000)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, 6, true, MIGRATION_5_6)
        try {
            val names = migrated.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' " +
                    "AND tbl_name IN ('favorite_documents','media_playback')"
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }

            assertTrue(
                "favourites index missing after migration: $names",
                names.contains("index_favorite_documents_addedTimestamp")
            )
            assertTrue(
                "playback index missing after migration: $names",
                names.contains("index_media_playback_updatedTimestamp")
            )

            val favourites = migrated.query(
                "SELECT displayName FROM favorite_documents WHERE uri = 'content://five'"
            )
            assertTrue("the favourite row did not survive the migration", favourites.moveToFirst())
            assertEquals("before.pdf", favourites.getString(0))
            favourites.close()

            val playbacks = migrated.query(
                "SELECT positionMs FROM media_playback WHERE uri = 'content://five'"
            )
            assertTrue("the playback position did not survive the migration", playbacks.moveToFirst())
            assertEquals(4200L, playbacks.getLong(0))
            playbacks.close()
        } finally {
            migrated.close()
        }
    }

    /** The lists the indexes serve must also be bounded: a list nobody can reach the end of. */
    @Test
    fun favouritesAndPlaybackListsAreBounded(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val total = FavoriteDao.MAX_LISTED + 120
            repeat(total) { index ->
                db.favoriteDao().insertFavorite(
                    FavoriteDocumentEntity(
                        uri = "content://fav/$index",
                        displayName = "f$index.pdf",
                        sizeBytes = 1L,
                        mimeType = "application/pdf",
                        addedTimestamp = index.toLong()
                    )
                )
            }
            val listed = db.favoriteDao().getAllFavorites().first()
            assertEquals(FavoriteDao.MAX_LISTED, listed.size)
            assertEquals("content://fav/${total - 1}", listed.first().uri)
        } finally {
            db.close()
        }
    }
}
