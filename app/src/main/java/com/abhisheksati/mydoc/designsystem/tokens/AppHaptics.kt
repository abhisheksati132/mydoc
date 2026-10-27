package com.abhisheksati.mydoc.designsystem.tokens

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Clean, subtle haptic feedback engine.
 * Only triggers tactile pulses on physical milestones (taking a photo, corner snapping, drag finish).
 * Does not vibrate on ordinary passive taps.
 */
class AppHaptics(private val view: View) {

    /**
     * Subtle tick when scrubbing through sliders or page numbers.
     */
    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Snap feel when a crop corner or guide locks into place.
     */
    fun snap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        }
    }

    /**
     * Confirmation feedback when completing a major action (e.g. shutter snap, PDF created).
     */
    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    /**
     * Reject buzz on error or denied permission.
     */
    fun reject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    fun select() {
        // Reserved for deliberate long-press selection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
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
    return remember(view) { AppHaptics(view) }
}
