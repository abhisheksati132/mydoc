package com.abhisheksati.mydoc.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The offline `saved_docs` backup, staged thumbnails and conversion caches all resolve a
 * document through this identity. It used to be `hashCode()`, so two different URIs could be
 * handed one another's file — content from someone else's document appearing under the wrong
 * name, with no error anywhere. These tests hold the line against that class of bug returning.
 */
class DocumentIdentityTest {

    private val uriA = "content://com.android.providers.media.documents/document/document%3A9637"
    private val uriB = "content://com.android.providers.media.documents/document/document%3A9638"

    @Test
    fun `the identity is not a 32-bit hash`() {
        // Java's String.hashCode collides on these two by design; a 32-bit cache key inherits it.
        assertEquals("Aa".hashCode(), "BB".hashCode())
        assertNotEquals(
            DocumentIdentity.ofText("content://provider/Aa"),
            DocumentIdentity.ofText("content://provider/BB")
        )
        // 128 bits, not 8 hex characters of a signed int.
        assertEquals(32, DocumentIdentity.ofText(uriA).length)
        assertFalse(DocumentIdentity.ofText(uriA).startsWith("-"))
    }

    @Test
    fun `neighbouring documents get different identities`() {
        assertNotEquals(DocumentIdentity.ofText(uriA), DocumentIdentity.ofText(uriB))
    }

    @Test
    fun `an identity is stable so a cache can still be hit`() {
        assertEquals(DocumentIdentity.ofText(uriA), DocumentIdentity.ofText(uriA))
    }

    @Test
    fun `parts cannot be blended into one another`() {
        assertNotEquals(DocumentIdentity.ofText("a", "bc"), DocumentIdentity.ofText("ab", "c"))
    }

    @Test
    fun `a backup is named for its uri, not for its display name`() {
        val renamed = DocumentIdentity.savedDocsName(uriA, "Other name.pdf")
        val original = DocumentIdentity.savedDocsName(uriA, "Report.pdf")
        // The digest carries no underscore, so the first one separates key from name.
        assertEquals(original.substringBefore('_'), renamed.substringBefore('_'))
        assertNotEquals(original, DocumentIdentity.savedDocsName(uriB, "Report.pdf"))
    }

    @Test
    fun `a copy matches only the uri it was saved for`() {
        val copyOfA = File.createTempFile("saved", ".pdf").apply {
            writeText("bytes")
            deleteOnExit()
        }
        val namedForA = File(copyOfA.parentFile, DocumentIdentity.savedDocsName(uriA, "Report.pdf"))
        copyOfA.renameTo(namedForA)

        assertTrue(DocumentIdentity.isSavedCopyOf(namedForA, uriA))
        assertFalse("a neighbouring document must never claim this copy",
            DocumentIdentity.isSavedCopyOf(namedForA, uriB))

        val empty = File(namedForA.parentFile, namedForA.name + ".empty")
        empty.createNewFile()
        empty.deleteOnExit()
        assertFalse("an empty file is not a usable copy",
            DocumentIdentity.isSavedCopyOf(empty, uriA))
    }

    @Test
    fun `a display name is never a lookup key`() {
        // Two documents called the same thing must not share a backup name.
        val first = DocumentIdentity.savedDocsName(uriA, "Report.pdf")
        val second = DocumentIdentity.savedDocsName(uriB, "Report.pdf")
        assertNotEquals(first, second)
    }

    @Test
    fun `thousands of uris do not bucket together`() {
        val keys = (1..5_000).map { DocumentIdentity.ofText("content://doc/$it/report.pdf") }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `unsafe characters are stripped from the name part only`() {
        val name = DocumentIdentity.savedDocsName(uriA, "../../etc/passwd")
        assertFalse(name.substringAfter('_').contains("/"))
        assertEquals("document", DocumentIdentity.sanitizeDisplayName(null))
    }
}
