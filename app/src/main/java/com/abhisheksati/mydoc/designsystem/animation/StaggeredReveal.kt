package com.abhisheksati.mydoc.designsystem.animation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import kotlinx.coroutines.delay

/**
 * A slow, no-overshoot settle for content that appears together: each item fades and rises
 * a few dp, offset by its index. Uses the AppMotion contract, and holds completely still
 * under reduced motion.
 */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    stepMillis: Int = 30,
    content: @Composable BoxScope.() -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    var played by remember(reducedMotion) { mutableStateOf(reducedMotion) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            delay(index.toLong().coerceAtLeast(0L) * stepMillis)
            played = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = AppMotion.snappy(),
        label = "staggeredReveal"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 12.dp.toPx()
        },
        content = content
    )
}
