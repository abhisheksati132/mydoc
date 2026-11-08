package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class AdaptiveInkOnDeviceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun individualButtonsReadDifferentInkFromTheirActualWindowBackgrounds() {
        val left=AtomicReference<Color?>(null)
        val right=AtomicReference<Color?>(null)
        compose.setContent { AppDesignSystemTheme {
            AdaptiveInkProvider(enabled=true) {
                Row(Modifier.padding(top=100.dp)) {
                    Box(Modifier.size(120.dp).background(Color.Black)) {
                        AdaptiveIconButton(onClick={},modifier=Modifier.padding(24.dp)) {
                            val ink=LocalContentColor.current
                            SideEffect { left.set(ink) }
                            Text("Left")
                        }
                    }
                    Box(Modifier.size(120.dp).background(Color.White)) {
                        AdaptiveIconButton(onClick={},modifier=Modifier.padding(24.dp)) {
                            val ink=LocalContentColor.current
                            SideEffect { right.set(ink) }
                            Text("Right")
                        }
                    }
                }
            }
        } }
        compose.waitUntil(8_000) { left.get()==Color.White && right.get()==Color.Black }
    }
}
