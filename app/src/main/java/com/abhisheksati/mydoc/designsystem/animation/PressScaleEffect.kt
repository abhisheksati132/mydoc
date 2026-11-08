package com.abhisheksati.mydoc.designsystem.animation

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion

/** Click semantics and indication stay with the control; the shared transform adds press feedback. */
fun Modifier.pressScaleEffect(
    targetScaleOnPress: Float = 0.972f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactions = remember { MutableInteractionSource() }
    pressScale(interactions, targetScaleOnPress).then(
        if (onClick != null) Modifier.clickable(
            interactionSource = interactions,
            indication = ripple(),
            onClick = onClick
        ) else Modifier
    )
}

/**
 * Collect individual press events so a quick Press/Release in one frame still produces feedback.
 * Cancel (e.g. a scroll taking ownership) resets immediately; it never delays scrolling or clicks.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    targetScaleOnPress: Float = 0.972f
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(interactionSource, targetScaleOnPress, reduced) {
        scale.snapTo(1f)
        if (reduced) return@LaunchedEffect
        animatePressFeedback(interactionSource, SystemClock::uptimeMillis,
            animateIn = { scale.animateTo(targetScaleOnPress, AppMotion.pressIn()) },
            animateOut = { scale.animateTo(1f, AppMotion.pressOut()) },
            reset = { scale.snapTo(1f) })
    }
    graphicsLayer {
        val value = if (reduced) 1f else scale.value
        scaleX = value
        scaleY = value
    }
}
