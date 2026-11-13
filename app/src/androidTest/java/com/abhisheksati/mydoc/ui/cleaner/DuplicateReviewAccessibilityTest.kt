package com.abhisheksati.mydoc.ui.cleaner

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.tools.cleaner.DuplicateGroup
import java.io.File
import org.junit.Rule
import org.junit.Test

class DuplicateReviewAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    private val groups = listOf(
        DuplicateGroup(
            checksum = "abc123",
            size = 1024L,
            original = File("/sdcard/Pictures/keeper.jpg"),
            duplicates = listOf(File("/sdcard/Pictures/copy.jpg"))
        )
    )

    private fun showReview() {
        compose.setContent {
            AppDesignSystemTheme {
                DuplicateReview(
                    groups = groups,
                    keepers = mapOf("abc123" to "/sdcard/Pictures/keeper.jpg"),
                    onKeep = { _, _ -> },
                    onPreview = {},
                    onDismiss = {}
                )
            }
        }
    }

    @Test
    fun keeperRadio_hasAccessibleNameAndSelectedState() {
        showReview()
        val radios = compose.onAllNodesWithContentDescription("Keep this copy")
        radios[0].assertIsSelected()
        radios[1].assertIsNotSelected()
    }

    @Test
    fun groupHeader_usesPluralCopyCount() {
        showReview()
        compose.onNodeWithText("2 identical copies", substring = true).assertExists()
    }
}