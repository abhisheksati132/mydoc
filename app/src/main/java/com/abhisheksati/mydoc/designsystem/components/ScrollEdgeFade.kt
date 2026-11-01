package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing

/**
 * Horizontal scroller that fades out at either edge while there is more content to reach, so a
 * clipped chip reads as scrollable instead of truncated.
 */
@Composable
fun Modifier.horizontalScrollWithFade(state: ScrollState): Modifier {
    val fadePx = with(LocalDensity.current) { AppSpacing.large.toPx() }
    val atStart by remember(state) { derivedStateOf { !state.canScrollBackward } }
    val atEnd by remember(state) { derivedStateOf { !state.canScrollForward } }

    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (!atStart) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = 0f,
                        endX = fadePx
                    ),
                    blendMode = BlendMode.DstOut
                )
            }
            if (!atEnd) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startX = size.width - fadePx,
                        endX = size.width
                    ),
                    blendMode = BlendMode.DstOut
                )
            }
        }
        .horizontalScroll(state)
}
