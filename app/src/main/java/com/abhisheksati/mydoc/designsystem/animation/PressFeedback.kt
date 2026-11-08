package com.abhisheksati.mydoc.designsystem.animation

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Event-based timing independent of frame coalescing and of the drawing implementation. */
internal suspend fun animatePressFeedback(
    source: InteractionSource,
    now: () -> Long,
    animateIn: suspend () -> Unit,
    animateOut: suspend () -> Unit,
    reset: suspend () -> Unit
): Unit = coroutineScope {
    val presses = mutableSetOf<PressInteraction.Press>()
    var pressedAt = 0L
    var animation: Job? = null
    source.interactions.collect { interaction ->
        when (interaction) {
            is PressInteraction.Press -> {
                presses.add(interaction)
                pressedAt = now()
                animation?.cancel()
                animation = launch { animateIn() }
            }
            is PressInteraction.Release -> {
                if (presses.remove(interaction.press) && presses.isEmpty()) {
                    val dip = animation
                    animation = launch {
                        delay((90L - (now() - pressedAt).coerceAtLeast(0L)).coerceAtLeast(0L))
                        dip?.cancel()
                        animateOut()
                    }
                }
            }
            is PressInteraction.Cancel -> {
                if (presses.remove(interaction.press) && presses.isEmpty()) {
                    animation?.cancel()
                    animation = launch { reset() }
                }
            }
        }
    }
}
