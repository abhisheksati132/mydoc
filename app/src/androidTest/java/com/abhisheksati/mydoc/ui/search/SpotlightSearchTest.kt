package com.abhisheksati.mydoc.ui.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SpotlightSearchTest {

    @get:Rule
    val compose = createComposeRule()

    private fun item(uri: String, name: String) = LibraryItem(
        uri = uri,
        name = name,
        kind = MediaKind.DOCUMENT,
        mimeType = "application/pdf",
        sizeBytes = 1024L,
        lastModified = 1_730_000_000_000L
    )

    /**
     * Guards the bug the count label used to lie about: a MediaStore row and a directory scan
     * hand back the same file under two URIs, so it listed twice while the header counted the
     * unfiltered collection.
     */
    @Test
    fun duplicateIdentityAppearsOnceAndTheCountMatchesTheList() {
        compose.setContent {
            MaterialTheme {
                SpotlightSearchSheet(
                    items = listOf(
                        item("content://media/document/1", "alpha.pdf"),
                        item("file:///sdcard/Documents/alpha.pdf", "alpha.pdf"),
                        item("content://media/document/2", "beta.pdf")
                    ),
                    onItemClick = {},
                    onDismiss = {}
                )
            }
        }

        compose.onNodeWithText("2 items").assertIsDisplayed()
        compose.onAllNodesWithText("alpha.pdf").assertCountEquals(1)
    }

    @Test
    fun queryNarrowsTheListAndTheCountTogether() {
        compose.setContent {
            MaterialTheme {
                SpotlightSearchSheet(
                    items = listOf(
                        item("content://media/document/1", "alpha.pdf"),
                        item("content://media/document/2", "beta.pdf")
                    ),
                    onItemClick = {},
                    onDismiss = {}
                )
            }
        }

        compose.onNode(hasSetTextAction()).performTextInput("beta")

        compose.onNodeWithText("1 item").assertIsDisplayed()
        compose.onNodeWithText("beta.pdf").assertIsDisplayed()
        compose.onNodeWithText("alpha.pdf").assertDoesNotExist()
    }

    @Test
    fun tappingAResultReportsThatItem() {
        var opened: LibraryItem? = null
        compose.setContent {
            MaterialTheme {
                SpotlightSearchSheet(
                    items = listOf(
                        item("content://media/document/1", "alpha.pdf"),
                        item("content://media/document/2", "beta.pdf")
                    ),
                    onItemClick = { opened = it },
                    onDismiss = {}
                )
            }
        }

        compose.onNodeWithText("beta.pdf").performClick()

        assertEquals("content://media/document/2", opened?.uri)
    }
}
