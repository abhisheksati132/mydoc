package com.abhisheksati.mydoc.discovery

import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A provider may hand back a directory with an unbounded number of rows. readChildren
 * must stop at the caller's budget and say so, instead of materialising the whole
 * listing first, and the truncated flag must be exact at the boundary.
 */
@RunWith(AndroidJUnit4::class)
class SelectedDocumentChildrenTest {

    private val parentUri: Uri =
        DocumentsContract.buildTreeDocumentUri("com.abhisheksati.mydoc.test", "root")

    private fun cursor(rows: Int): MatrixCursor {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            OpenableColumns.DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            OpenableColumns.SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        return MatrixCursor(projection).apply {
            for (i in 0 until rows) {
                addRow(arrayOf<Any>("doc$i", "Doc $i", "application/pdf", i.toLong(), 0L))
            }
        }
    }

    @Test
    fun wideDirectoryStopsAtTheBudgetAndReportsTruncation() = runBlocking {
        cursor(1300).use { c ->
            val page = SelectedDocument.readChildren(c, parentUri, 1200)
            assertEquals(1200, page.entries.size)
            assertTrue("more rows existed than the budget allowed", page.truncated)
            assertEquals("doc1199", page.entries.last().uri.lastPathSegment)
        }
    }

    @Test
    fun exactFitIsNotReportedAsTruncated() = runBlocking {
        cursor(1200).use { c ->
            val page = SelectedDocument.readChildren(c, parentUri, 1200)
            assertEquals(1200, page.entries.size)
            assertFalse("nothing was cut off", page.truncated)
        }
    }

    @Test
    fun anEmptyDirectoryYieldsNoEntriesAndNoTruncation() = runBlocking {
        cursor(0).use { c ->
            val page = SelectedDocument.readChildren(c, parentUri, 1200)
            assertTrue(page.entries.isEmpty())
            assertFalse(page.truncated)
        }
    }

    @Test
    fun rowsKeepTheirDisplayNamesAndSizes() = runBlocking {
        cursor(2).use { c ->
            val page = SelectedDocument.readChildren(c, parentUri, 1200)
            assertEquals("Doc 1", page.entries[1].name)
            assertEquals(1L, page.entries[1].size)
            assertEquals("application/pdf", page.entries[1].type)
        }
    }
}
