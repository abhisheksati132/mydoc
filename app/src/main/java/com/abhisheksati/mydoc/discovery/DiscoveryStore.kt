package com.abhisheksati.mydoc.discovery

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class DiscoveredFile(val uri: String, val name: String, val mime: String, val size: Long,
    val modified: Long, val status: String, val indexedAt: Long, val tags: String = "", val page: Int = 0,
    val snippet: String = "")
data class SmartCollection(val name: String, val query: String, val tag: String, val extension: String)

internal fun contentMatchQuery(query: String): String = Regex("[\\p{L}\\p{N}][\\p{L}\\p{M}\\p{N}_]*").findAll(query.take(300))
    .map { "\"${it.value}*\"" }.take(12).joinToString(" AND ")
internal fun normalizedTags(value: String): List<String> = value.split(',').map { it.trim().lowercase(java.util.Locale.ROOT).take(40) }
    .filter { it.isNotBlank() }.distinct().take(20)

/** Separate, private, rebuildable index. Existing reading history and Room migrations stay intact. */
class DiscoveryStore internal constructor(context: Context, dbName: String = "discovery.db") : SQLiteOpenHelper(context, dbName, null, 1) {
    val revision = MutableStateFlow(0L)
    private val lock = Mutex()
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE files(uri TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL,mime TEXT NOT NULL,size INTEGER NOT NULL,modified INTEGER NOT NULL,status TEXT NOT NULL,indexedAt INTEGER NOT NULL)")
        db.execSQL("CREATE VIRTUAL TABLE pages USING fts4(uri,page,body,notindexed=uri,notindexed=page,tokenize=unicode61)")
        db.execSQL("CREATE TABLE tags(uri TEXT NOT NULL,tag TEXT NOT NULL,PRIMARY KEY(uri,tag))")
        db.execSQL("CREATE INDEX tags_by_tag ON tags(tag)")
        db.execSQL("CREATE TABLE collections(name TEXT PRIMARY KEY NOT NULL,query TEXT NOT NULL,tag TEXT NOT NULL,extension TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    private suspend fun <T> access(block: (SQLiteDatabase) -> T): T = withContext(Dispatchers.IO) {
        lock.withLock { block(writableDatabase) }
    }
    suspend fun put(file: DiscoveredFile, pages: List<Pair<Int, String>>) = access { db ->
        db.beginTransaction()
        try {
            val v = ContentValues().apply {
                put("uri",file.uri);put("name",file.name);put("mime",file.mime);put("size",file.size)
                put("modified",file.modified);put("status",file.status);put("indexedAt",file.indexedAt)
            }
            db.insertWithOnConflict("files",null,v,SQLiteDatabase.CONFLICT_REPLACE)
            db.delete("pages","uri=?",arrayOf(file.uri))
            pages.forEach { (page,text) -> db.insertOrThrow("pages",null,ContentValues().apply {
                put("uri",file.uri);put("page",page);put("body",text)
            }) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        revision.value++
    }
    suspend fun setTags(uri: String, value: String) = access { db ->
        db.beginTransaction()
        try {
            db.delete("tags","uri=?",arrayOf(uri))
            normalizedTags(value).forEach { tag -> db.insertOrThrow("tags",null,ContentValues().apply { put("uri",uri);put("tag",tag) }) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        revision.value++
    }
    suspend fun remove(uri: String) = access { db ->
        db.beginTransaction()
        try {
            for (table in listOf("pages","tags","files")) db.delete(table,"uri=?",arrayOf(uri))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        revision.value++
    }
    suspend fun clear() = access { db ->
        db.beginTransaction()
        try { for (table in listOf("pages","tags","files","folders")) runCatching { db.delete(table,null,null) };db.setTransactionSuccessful() }
        finally { db.endTransaction() }
        revision.value++
    }
    suspend fun addFolder(uri: String) = access { db ->
        db.execSQL("CREATE TABLE IF NOT EXISTS folders(uri TEXT PRIMARY KEY NOT NULL)")
        val v = ContentValues().apply { put("uri", uri) }
        db.insertWithOnConflict("folders", null, v, SQLiteDatabase.CONFLICT_IGNORE)
        revision.value++
    }
    suspend fun folders(): List<String> = access { db ->
        db.execSQL("CREATE TABLE IF NOT EXISTS folders(uri TEXT PRIMARY KEY NOT NULL)")
        buildList {
            db.rawQuery("SELECT uri FROM folders", null).use { c ->
                while (c.moveToNext()) add(c.getString(0))
            }
        }
    }
    suspend fun saveCollection(rule: SmartCollection) = access { db ->
        require(rule.name.isNotBlank() && rule.name.length<=60)
        db.insertWithOnConflict("collections",null,ContentValues().apply {
            put("name",rule.name.trim());put("query",rule.query.take(300));put("tag",rule.tag.trim().lowercase(java.util.Locale.ROOT))
            put("extension",rule.extension.trim().removePrefix(".").lowercase(java.util.Locale.ROOT))
        },SQLiteDatabase.CONFLICT_REPLACE)
        revision.value++
    }
    suspend fun collections(): List<SmartCollection> = access { db ->
        buildList { db.rawQuery("SELECT name,query,tag,extension FROM collections ORDER BY name",null).use { c ->
            while(c.moveToNext()) add(SmartCollection(c.getString(0),c.getString(1),c.getString(2),c.getString(3)))
        } }
    }
    suspend fun deleteCollection(name: String) = access { db -> db.delete("collections","name=?",arrayOf(name));revision.value++ }
    suspend fun file(uri: String): DiscoveredFile? = access { db ->
        db.rawQuery("SELECT f.*,COALESCE((SELECT group_concat(tag, ', ') FROM tags t WHERE t.uri=f.uri),'') FROM files f WHERE f.uri=?",arrayOf(uri)).use { c ->
            if(!c.moveToFirst()) null else DiscoveredFile(c.getString(0),c.getString(1),c.getString(2),c.getLong(3),c.getLong(4),c.getString(5),c.getLong(6),c.getString(7))
        }
    }
    suspend fun files(): List<DiscoveredFile> = search("", "", "")
    suspend fun search(query: String, tag: String, extension: String): List<DiscoveredFile> = access { db ->
        val found = linkedMapOf<String,DiscoveredFile>()
        val tags = "COALESCE((SELECT group_concat(tag, ', ') FROM tags t WHERE t.uri=f.uri),'')"
        val suffix = extension.trim().removePrefix(".").lowercase(java.util.Locale.ROOT)
        val filter = " AND (?='' OR EXISTS(SELECT 1 FROM tags t WHERE t.uri=f.uri AND t.tag=?)) AND (?='' OR lower(f.name) LIKE ? ESCAPE '\\')"
        val escape: (String)->String = { it.replace("\\","\\\\").replace("%","\\%").replace("_","\\_") }
        val filters = arrayOf(tag,tag,suffix,"%.${escape(suffix)}")
        fun read(sql: String, args: Array<String>) { db.rawQuery(sql,args).use { c ->
            while(c.moveToNext()) {
                val f=DiscoveredFile(c.getString(0),c.getString(1),c.getString(2),c.getLong(3),c.getLong(4),c.getString(5),c.getLong(6),c.getString(7),c.getInt(8),c.getString(9))
                found.putIfAbsent(f.uri,f)
            }
        } }
        val match=contentMatchQuery(query)
        if(query.isNotBlank() && match.isNotEmpty()) {
            // Choose one matching page per document before applying the result limit.
            // Otherwise a long book can occupy every result slot by itself.
            val ids=mutableListOf<Long>()
            db.rawQuery("SELECT MIN(pages.rowid) FROM pages JOIN files f ON f.uri=pages.uri WHERE pages MATCH ?$filter GROUP BY f.uri ORDER BY f.name LIMIT 1000",
                arrayOf(match)+filters).use { c ->while(c.moveToNext())ids.add(c.getLong(0)) }
            for(id in ids) read("SELECT f.*,$tags,CAST(pages.page AS INTEGER),snippet(pages,'','', '…',2,18) FROM pages JOIN files f ON f.uri=pages.uri WHERE pages MATCH ? AND pages.rowid=?",
                arrayOf(match,id.toString()))
        }
        read("SELECT f.*,$tags,0,'' FROM files f WHERE (?='' OR f.name LIKE ? ESCAPE '\\')$filter ORDER BY f.name LIMIT 1000",
            arrayOf(query,"%${escape(query)}%")+filters)
        found.values.toList()
    }
    companion object {
        @Volatile private var instance: DiscoveryStore? = null
        fun get(context: Context): DiscoveryStore = instance ?: synchronized(this) {
            instance ?: DiscoveryStore(context.applicationContext).also { instance = it }
        }

        /** Creates an isolated instance for tests to protect production user data. */
        fun forTesting(context: Context, dbName: String = "discovery_test.db"): DiscoveryStore =
            DiscoveryStore(context.applicationContext, dbName)
    }
}
