package com.abhisheksati.mydoc.ui.media

import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PhotoFilmstripAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    private fun photo(name: String, id: Long) = MediaItem(
        id = id,
        uri = Uri.parse("content://media/external/images/media/$id"),
        displayName = name,
        sizeBytes = 1024L,
        mimeType = "image/jpeg",
        dateAddedSec = 1700000000L
    )

    @Test
    fun selectedThumbnail_publishesSelectedState() {
        val photos = listOf(photo("dawn.jpg", 1), photo("sunset.jpg", 2))
        compose.setContent {
            AppDesignSystemTheme {
                PhotoFilmstrip(photos = photos, currentPage = 1, onPageSelected = {})
            }
        }
        compose.onNodeWithContentDescription("sunset.jpg").assertIsSelected()
        compose.onNodeWithContentDescription("dawn.jpg").assertIsNotSelected()
        compose.onNodeWithContentDescription("sunset.jpg").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Selected")
        )
    }

    @Test
    fun blankName_fallsBackToLocalizedPhotoLabel() {
        val photos = listOf(photo("", 1), photo("sunset.jpg", 2))
        compose.setContent {
            AppDesignSystemTheme {
                PhotoFilmstrip(photos = photos, currentPage = 0, onPageSelected = {})
            }
        }
        compose.onNodeWithContentDescription("Photo 1").assertExists().assertIsSelected()
    }

    @Test
    fun tappingThumbnail_reportsItsIndex() {
        var selected = -1
        val photos = listOf(photo("dawn.jpg", 1), photo("sunset.jpg", 2))
        compose.setContent {
            AppDesignSystemTheme {
                PhotoFilmstrip(photos = photos, currentPage = 0, onPageSelected = { selected = it })
            }
        }
        compose.onNodeWithContentDescription("sunset.jpg").performClick()
        assertEquals(1, selected)
    }
}