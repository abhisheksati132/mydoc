package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxSlide
import com.abhisheksati.mydoc.ui.viewer.presentation.VisualSlideCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SlideCanvasOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun slidePaintsItsCornersWithoutRoundedFramesOrInsets() {
        compose.setContent {
            AppDesignSystemTheme {
                Box(Modifier.size(320.dp, 180.dp).background(Color.Red).testTag("slide")) {
                    VisualSlideCard(
                        slide = PptxSlide(1, "Title", emptyList(), backgroundColor = Color.White),
                        cardBgColor = Color.Gray, defaultTextColor = Color.Black
                    )
                }
            }
        }
        val pixels = compose.onNodeWithTag("slide").captureToImage().toPixelMap()
        for ((x, y) in listOf(1 to 1, pixels.width - 2 to 1, 1 to pixels.height - 2)) {
            assertEquals(1f, pixels[x, y].red, 0.02f)
            assertEquals(1f, pixels[x, y].green, 0.02f)
            assertEquals(1f, pixels[x, y].blue, 0.02f)
        }
    }
}
