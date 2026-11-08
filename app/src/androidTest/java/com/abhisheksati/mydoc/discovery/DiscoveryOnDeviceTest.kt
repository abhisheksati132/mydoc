package com.abhisheksati.mydoc.discovery

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DiscoveryOnDeviceTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val store=DiscoveryStore.get(context)
    @Before fun setup() = runBlocking { store.clear();store.collections().forEach { store.deleteCollection(it.name) } }
    @After fun cleanup() = setup()
    private fun file(uri: String,name: String="Report.pdf") = DiscoveredFile(uri,name,"application/pdf",20,10,"Indexed",100)

    @Test fun fullTextSnippetsPagesAndSmartRulesSurviveUpdates() = runBlocking {
        store.put(file("content://test/a"),listOf(0 to "nothing here",3 to "The nebula budget was approved today. café समाचार"))
        store.setTags("content://test/a","Work, Finance,work")
        val hits=store.search("nebul","work","pdf")
        assertEquals(1,hits.size);assertEquals(3,hits.single().page)
        assertTrue(hits.single().snippet.contains("nebula"))
        assertEquals(1,store.search("समाचार","","").size)
        assertEquals(1,store.search("caf","","").size)
        assertEquals(setOf("finance","work"),hits.single().tags.split(", ").toSet())
        store.saveCollection(SmartCollection("Budget","nebul","work","pdf"))
        val rule=store.collections().single()
        assertEquals(1,store.search(rule.query,rule.tag,rule.extension).size)
        store.put(file("content://test/a"),listOf(0 to "replacement content"))
        assertTrue(store.search(rule.query,rule.tag,rule.extension).isEmpty())
        assertEquals(1,store.search("replacement","work","pdf").size)
        store.remove("content://test/a")
        assertTrue(store.files().isEmpty())
        assertTrue(store.search("replacement","","").isEmpty())
    }
    @Test fun aLongDocumentCannotHideOtherMatchingFilesOrMatchItsUri() = runBlocking {
        store.put(file("content://test/a","A.pdf"),(0 until 200).map { it to "galaxy budget" })
        store.put(file("content://test/b","B.pdf"),listOf(4 to "galaxy forecast"))
        assertEquals(2,store.search("galaxy","","").size)
        assertTrue(store.search("content","","").isEmpty())
        assertTrue(store.search("\"* OR ()","","").isEmpty())
    }
    @Test fun selectedTextIsIndexedLocallyAndSourceIsPreserved() = runBlocking {
        val source=File.createTempFile("index-",".txt",context.cacheDir).apply { writeText("Synthetic local constellation report") }
        try {
            DocumentIndexer(context,store).selected(listOf(Uri.fromFile(source)),false) {}
            assertEquals(source.toURI().path,Uri.parse(store.search("constellation","","").single().uri).path)
            assertEquals("Synthetic local constellation report",source.readText())
            source.writeText("updated astronomy")
            DocumentIndexer(context,store).selected(listOf(Uri.fromFile(source)),false) {}
            assertTrue(store.search("constellation","","").isEmpty())
            assertEquals(1,store.search("astronomy","","").size)
            store.clear()
            assertTrue(source.exists())
        } finally { source.delete() }
    }
    @Test fun lockedOfficeFilesDoNotLeaveSearchableText() = runBlocking {
        val source=File.createTempFile("locked-index-",".xlsx",context.cacheDir)
        try {
            InstrumentationRegistry.getInstrumentation().context.assets.open("office/protected.xlsx").use { input->source.outputStream().use { input.copyTo(it) } }
            DocumentIndexer(context,store).selected(listOf(Uri.fromFile(source)),false) {}
            assertTrue(store.files().single().status.startsWith("Unavailable"))
            assertTrue(store.search("CALC","","").isEmpty())
        } finally { source.delete() }
    }
}
