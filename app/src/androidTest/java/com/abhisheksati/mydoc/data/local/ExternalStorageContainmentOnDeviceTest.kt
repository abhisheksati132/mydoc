package com.abhisheksati.mydoc.data.local

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The resolver's last tier turns a `primary:<relative path>` document id into a real file. External
 * storage on a device is a FUSE mount that actually contains symlinks and shared parents, so the
 * containment rule is checked against the tree the app really opens, not only against a temp dir.
 */
@RunWith(AndroidJUnit4::class)
class ExternalStorageContainmentOnDeviceTest {

    private val root: File by lazy {
        Environment.getExternalStorageDirectory() ?: error("no external storage on this device")
    }

    @Test
    fun aRealRelativePathResolvesInsideExternalStorage() {
        val marker = File(root, "Download/mydoc-containment-${System.nanoTime()}.txt")
        marker.parentFile?.mkdirs()
        try {
            marker.writeText("inside")
            val resolved = DocumentStreamResolver.externalStorageChild(root, "Download/${marker.name}")
            assertEquals(marker.canonicalFile, resolved?.canonicalFile)
            assertEquals("inside", resolved!!.readText())
        } finally {
            marker.delete()
        }
    }

    @Test
    fun climbingOutOfExternalStorageIsRefusedEvenThoughTheTargetExists() {
        // The app's own private directory really is readable, so this refuses a target the pre-fix
        // code could have handed back — not merely a path that happens not to exist.
        val private = InstrumentationRegistry.getInstrumentation().targetContext.dataDir
        assertTrue("${private.path} is not a readable directory on this device", private.canRead())
        val climb = root.canonicalFile.toPath().relativize(private.toPath()).toString()

        // The pre-fix code did exactly this join, so the danger is real rather than theoretical.
        assertEquals(private.canonicalFile, File(root, climb).canonicalFile)

        assertNull("the id '$climb' escaped external storage", DocumentStreamResolver.externalStorageChild(root, climb))
    }

    @Test
    fun anAbsolutePathOrTheRootItselfIsNotHonoured() {
        assertNull(DocumentStreamResolver.externalStorageChild(root, "/system/build.prop"))
        assertNull(DocumentStreamResolver.externalStorageChild(root, ""))
        assertNull(DocumentStreamResolver.externalStorageChild(root, "."))
        assertNull(
            "naming external storage itself is not a document",
            DocumentStreamResolver.externalStorageChild(root, "Download/../")
        )
    }
}
