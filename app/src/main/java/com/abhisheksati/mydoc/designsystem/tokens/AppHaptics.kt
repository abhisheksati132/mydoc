package com.abhisheksati.mydoc.designsystem.tokens

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/** Mirrors the "Haptic feedback" setting so every tactile cue can be switched off. */
val LocalHapticsEnabled = compositionLocalOf { true }

/** Direct Compose haptics, silenced when the user turns haptic feedback off. */
@Composable
fun rememberGatedHaptics(): HapticFeedback {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(haptic, enabled) { if (enabled) haptic else NoOpHaptics }
}

private object NoOpHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

/**
 * Clean, subtle haptic feedback engine.
 * Only triggers tactile pulses on physical milestones (taking a photo, corner snapping, drag finish).
 * Does not vibrate on ordinary passive taps.
 */
class AppHaptics(private val view: View, private val enabled: Boolean = true) {

    private fun perform(constant: Int) {
        if (enabled) view.performHapticFeedback(constant)
    }

    fun click() {
        tick()
    }

    /**
     * Subtle tick when scrubbing through sliders or page numbers.
     */
    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            perform(HapticFeedbackConstants.SEGMENT_TICK)
        } else {
            perform(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Snap feel when a crop corner or guide locks into place.
     */
    fun snap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.GESTURE_END)
        }
    }

    /**
     * Confirmation feedback when completing a major action (e.g. shutter snap, PDF created).
     */
    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.CONFIRM)
        }
    }

    /**
     * Reject buzz on error or denied permission.
     */
    fun reject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.REJECT)
        }
    }

    fun select() {
        // Reserved for deliberate long-press selection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            perform(HapticFeedbackConstants.SEGMENT_TICK)
        }
    }

    /**
     * Regular taps use visual touch ripple and do not vibrate the motor.
     */
    fun tap() {
        // Quiet by design
    }

    fun heavy() {
        confirm()
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { AppHaptics(view, enabled) }
}
