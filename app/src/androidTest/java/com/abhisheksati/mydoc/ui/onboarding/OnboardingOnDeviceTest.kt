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
        com.abhisheksati.mydoc.DeviceCaptures.save("onboarding-access")
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Find what is inside.").assertIsDisplayed()
        com.abhisheksati.mydoc.DeviceCaptures.save("onboarding-search")
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Organise without losing control.").assertIsDisplayed()
        com.abhisheksati.mydoc.DeviceCaptures.save("onboarding-collections")
        compose.onNodeWithText("Explore collections").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true,explored) }
    }
}
