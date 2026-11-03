package com.abhisheksati.mydoc.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
}
