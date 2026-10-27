package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors

/**
 * Modern tactile slider with a sleek circular thumb pointer.
 * Replaces Material 3's default tall vertical bar pointer with a refined, tactile circular dot
 * and a streamlined 4dp media playback track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors? = null,
    thumbSize: Dp = 14.dp,
    trackHeight: Dp = 4.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val appColors = LocalAppColors.current
    val haptic = LocalHapticFeedback.current
    val sliderColors = colors ?: SliderDefaults.colors(
        thumbColor = appColors.accent,
        activeTrackColor = appColors.accent,
        inactiveTrackColor = appColors.divider
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onValueChangeFinished?.invoke()
        },
        colors = sliderColors,
        interactionSource = interactionSource,
        thumb = {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .background(color = sliderColors.thumbColor, shape = CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = sliderColors,
                enabled = enabled,
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 0.dp,
                modifier = Modifier.height(trackHeight)
            )
        }
    )
}
