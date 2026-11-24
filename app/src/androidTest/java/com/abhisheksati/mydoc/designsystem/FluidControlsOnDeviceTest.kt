package com.abhisheksati.mydoc.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.designsystem.animation.LocalMotionStyle
import com.abhisheksati.mydoc.designsystem.animation.MotionStyle
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import com.abhisheksati.mydoc.designsystem.components.GlassAppearance
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FluidControlsOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun menuSelectionActsOnceAndRemovesPopupForBothMotionModes() {
        var open by mutableStateOf(false)
        var reduced by mutableStateOf(false)
        var chosen = 0
        compose.setContent {
            AppDesignSystemTheme {
                CompositionLocalProvider(LocalReducedMotion provides reduced,
                    LocalMotionStyle provides MotionStyle.EXPRESSIVE) {
                    Box {
                        TextButton(onClick = { open = true }) { Text("Open actions") }
                        AppDropdownMenu(open, { open = false }) {
                            DropdownMenuItem(text = { Text("Choose file") }, onClick = { chosen++; open = false })
                        }
                    }
                }
            }
        }
        repeat(2) { index ->
            compose.runOnIdle { reduced = index == 1 }
            compose.onNodeWithText("Open actions").performClick()
            compose.onNodeWithText("Choose file").assertIsDisplayed().performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Choose file").assertDoesNotExist()
            compose.runOnIdle { assertEquals(index + 1, chosen) }
        }
    }

    @Test fun visualPreferencesSurviveRepositoryRecreation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val original = SettingsRepository(context)
        val previousGlass = original.glassAppearance.value
        val previousMotion = original.motionStyle.value
        try {
            original.setGlassAppearance(GlassAppearance.FROSTED)
            original.setMotionStyle(MotionStyle.CALM)
            val restored = SettingsRepository(context)
            assertEquals(GlassAppearance.FROSTED, restored.glassAppearance.value)
            assertEquals(MotionStyle.CALM, restored.motionStyle.value)
        } finally {
            original.setGlassAppearance(previousGlass)
            original.setMotionStyle(previousMotion)
        }
    }
}
