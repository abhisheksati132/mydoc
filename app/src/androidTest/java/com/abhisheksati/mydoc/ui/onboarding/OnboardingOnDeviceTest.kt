package com.abhisheksati.mydoc.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class OnboardingOnDeviceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun guideExplainsAccessAndCompletesTheThreeStepFlow() {
        var explored: Boolean?=null
        compose.setContent { AppDesignSystemTheme { OnboardingGuide { explored=it } } }
        compose.onNodeWithText("Your files. Your choice.").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Find what is inside.").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Organise without losing control.").assertIsDisplayed()
        compose.onNodeWithText("Explore collections").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true,explored) }
    }
}
