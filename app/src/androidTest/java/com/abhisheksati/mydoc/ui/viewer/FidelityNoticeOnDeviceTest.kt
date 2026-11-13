package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import org.junit.Rule
import org.junit.Test

class FidelityNoticeOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    private fun checkDismiss(direction: LayoutDirection) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides direction) {
                AppDesignSystemTheme {
                    FidelityNotice(FidelityDisclosure("Re-laid out for reading", listOf("Formatting may differ from the original.")))
                }
            }
        }
        compose.onNodeWithText("Re-laid out for reading").assertDoesNotExist()
        compose.onNodeWithContentDescription("Rendering information").assertIsDisplayed().performClick()
        compose.onNodeWithText("Re-laid out for reading").assertIsDisplayed()
        compose.onNodeWithContentDescription("Rendering information").performClick()
        compose.onNodeWithText("Re-laid out for reading").assertDoesNotExist()
    }

    @Test fun noticeCanBeDismissedWithLargeText() = checkDismiss(LayoutDirection.Ltr)
    @Test fun noticeCanBeDismissedWithLargeRtlText() = checkDismiss(LayoutDirection.Rtl)

    @Test fun newLimitationsRemainAvailableOnDemand() {
        val disclosure = mutableStateOf(FidelityDisclosure("Re-laid out for reading", emptyList()))
        compose.setContent { AppDesignSystemTheme { FidelityNotice(disclosure.value) } }
        compose.onNodeWithContentDescription("Rendering information").performClick()
        compose.runOnIdle { disclosure.value = FidelityDisclosure("Only part of this file was read", listOf("Some pages are missing.")) }
        compose.onNodeWithText("Only part of this file was read").assertDoesNotExist()
        compose.onNodeWithContentDescription("Rendering information").performClick()
        compose.onNodeWithText("Only part of this file was read").assertIsDisplayed()
    }

    @Test fun anotherDocumentStartsWithCollapsedInformation() {
        val document = mutableStateOf("first-document")
        compose.setContent { AppDesignSystemTheme {
            FidelityNotice(FidelityDisclosure("Re-laid out for reading", emptyList()), dismissalKey = document.value)
        } }
        compose.onNodeWithContentDescription("Rendering information").performClick()
        compose.runOnIdle { document.value = "second-document" }
        compose.onNodeWithText("Re-laid out for reading").assertDoesNotExist()
        compose.onNodeWithContentDescription("Rendering information").performClick()
        compose.onNodeWithText("Re-laid out for reading").assertIsDisplayed()
    }

    @Test fun informationAutomaticallyClosesAfterThreeSeconds() {
        compose.mainClock.autoAdvance = false
        compose.setContent { AppDesignSystemTheme {
            FidelityNotice(FidelityDisclosure("Layout information", emptyList()))
        } }
        compose.onNodeWithContentDescription("Rendering information").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("Layout information").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(3_100)
        compose.onNodeWithText("Layout information").assertDoesNotExist()
        compose.onNodeWithContentDescription("Rendering information").assertIsDisplayed()
    }
}