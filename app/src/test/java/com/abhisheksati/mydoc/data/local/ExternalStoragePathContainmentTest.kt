package com.abhisheksati.mydoc.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * H-6/Batch 6: the resolver's last tier rebuilds a real path from a document id that arrives inside
 * an untrusted content URI. It has to stay inside external storage.
 */
class ExternalStoragePathContainmentTest {

    private val root = File(System.getProperty("java.io.tmpdir"), "ext-root-${System.nanoTime()}").apply {
        mkdirs()
        File(this, "Download").mkdirs()
    }

    @Test
    fun anOrdinaryRelativePathResolvesInsideTheRoot() {
        val resolved = DocumentStreamResolver.externalStorageChild(root, "Download/report.pdf")
        assertEquals(File(root, "Download/report.pdf").canonicalFile, resolved?.canonicalFile)
    }

    @Test
    fun dotDotSegmentsCannotClimbOutOfExternalStorage() {
        val secret = File(root.parentFile, "outside-secret-${System.nanoTime()}.txt").apply { writeText("do not read") }
        val relative = root.relativeTo(secret.parentFile!!).path.split(File.separatorChar)
            .joinToString(File.separator) { ".." } + File.separator + secret.name

        assertNull(
            "a path climbing out of the root must be refused: $relative",
            DocumentStreamResolver.externalStorageChild(root, relative)
        )
        secret.delete()
    }

    @Test
    fun anAbsolutePathFromTheUriIsNotHonoured() {
        assertNull(DocumentStreamResolver.externalStorageChild(root, "/etc/passwd"))
        assertNull(DocumentStreamResolver.externalStorageChild(root, "\\\\server\\share\\file"))
    }

    @Test
    fun anEmptyOrRootOnlyPathResolvesToNothing() {
        assertNull(DocumentStreamResolver.externalStorageChild(root, ""))
        assertNull(DocumentStreamResolver.externalStorageChild(root, "   "))
        assertNull("naming the root itself is not a document", DocumentStreamResolver.externalStorageChild(root, "."))
    }

    @Test
    fun aNormalisedPathInsideTheRootIsStillAccepted() {
        val inside = File(root, "Download/./sub/../report.pdf")
        val resolved = DocumentStreamResolver.externalStorageChild(root, "Download/./sub/../report.pdf")
        assertEquals(inside.canonicalFile, resolved?.canonicalFile)
    }
}
