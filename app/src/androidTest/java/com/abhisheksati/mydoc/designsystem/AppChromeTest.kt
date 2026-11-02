package com.abhisheksati.mydoc.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.abhisheksati.mydoc.designsystem.components.AppMessages
import com.abhisheksati.mydoc.designsystem.components.AppSnackbarHost
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppChromeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun topBarShowsTitleSubtitleAndInvokesBack() {
        var backCalls = 0
        compose.setContent {
            MaterialTheme {
                AppTopBar(
                    title = "Files",
                    subtitle = "5 recent",
                    onBack = { backCalls++ }
                )
            }
        }

        compose.onNodeWithText("Files").assertIsDisplayed()
        compose.onNodeWithText("5 recent").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backCalls)
    }

    @Test
    fun messageBusShowsTheLastMessageOnTheHost() {
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppSnackbarHost()
                }
            }
        }

        compose.runOnIdle { AppMessages.show("Cache cleared") }

        compose.onNodeWithText("Cache cleared").assertIsDisplayed()
    }
}
