package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.LoadFailure
import org.junit.Rule
import org.junit.Test

class DocumentProblemScreenScrollTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun problemExplanation_isScrollable() {
        compose.setContent {
            AppDesignSystemTheme {
                DocumentProblemScreen(failure = LoadFailure.UNSUPPORTED_FORMAT, extension = "pdf")
            }
        }
        compose.onNodeWithText("Format not supported").assertExists()
        compose.onNode(hasScrollAction()).assertExists()
    }
}