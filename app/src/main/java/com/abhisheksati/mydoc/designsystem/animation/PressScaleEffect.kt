package com.abhisheksati.mydoc.designsystem.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * Crisp press physics: a 0.972x depression that returns without overshoot.
 * The app has one press behaviour; anything that springs past 1.0x reads as toy-like.
 */
fun Modifier.pressScaleEffect(
    targetScaleOnPress: Float = 0.972f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScaleOnPress else 1.0f,
        animationSpec = spring(
            dampingRatio = 1f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScaleEffect"
    )

    this
        .scale(scale)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Press physics only. For rows that need [combinedClickable] (long-press selection)
 * or any custom gesture handling: share the same [interactionSource] with your gesture
 * modifier and add this for the visual depression.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    targetScaleOnPress: Float = 0.972f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScaleOnPress else 1.0f,
        animationSpec = spring(
            dampingRatio = 1f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    scale(scale)
}
