package com.abhisheksati.mydoc.data.local

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MigrationTest {

    @Test
    fun testMigration_4_to_5_dropsShelfTables() {
        val executedStatements = mutableListOf<String>()

        val invocationHandler = InvocationHandler { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        }

        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            invocationHandler
        ) as SupportSQLiteDatabase

        MIGRATION_4_5.migrate(fakeDb)

        assertEquals(2, executedStatements.size)
        assertTrue(executedStatements.any { it.contains("DROP TABLE IF EXISTS `shelf_assignments`") })
        assertTrue(executedStatements.any { it.contains("DROP TABLE IF EXISTS `shelves`") })
    }

    @Test
    fun testMigration_3_to_5_runsWithoutError() {
        val executedStatements = mutableListOf<String>()

        val invocationHandler = InvocationHandler { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        }

        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            invocationHandler
        ) as SupportSQLiteDatabase

        MIGRATION_3_5.migrate(fakeDb)

        // Version 3 -> 5 has no schema changes because shelves was only added in 4 and removed in 5
        assertEquals(0, executedStatements.size)
    }
}
