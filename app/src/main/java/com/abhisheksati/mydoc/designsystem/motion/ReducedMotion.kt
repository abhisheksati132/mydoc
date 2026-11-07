package com.abhisheksati.mydoc.designsystem.motion

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.awaitCancellation

val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Android exposes "remove animations" (accessibility) by zeroing the global
 * animation scales; either scale at 0 means the user asked for stillness.
 */
fun Context.isReducedMotion(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f ||
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
fun rememberReducedMotion(): State<Boolean> {
    val context = LocalContext.current
    val reduced = remember(context) { mutableStateOf(context.isReducedMotion()) }
    LaunchedEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced.value = context.isReducedMotion()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE),
            false,
            observer
        )
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer
        )
        try {
            awaitCancellation()
        } finally {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    return reduced
}
