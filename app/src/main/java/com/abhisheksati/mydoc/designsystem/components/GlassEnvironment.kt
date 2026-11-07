package com.abhisheksati.mydoc.designsystem.components

import android.app.ActivityManager
import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal data class GlassEnvironment(
    val sdk: Int = Build.VERSION.SDK_INT,
    val powerSave: Boolean = false,
    val lowRam: Boolean = false,
    val increasedContrast: Boolean = false
)
internal val LocalGlassEnvironment = staticCompositionLocalOf { GlassEnvironment() }

internal fun shouldUseLiveGlass(mode: GlassEffectMode, environment: GlassEnvironment): Boolean =
    environment.sdk >= 31 && mode != GlassEffectMode.OFF && !environment.increasedContrast &&
        (mode == GlassEffectMode.ON || (!environment.powerSave && !environment.lowRam))

private fun Context.glassEnvironment(): GlassEnvironment {
    val power = getSystemService(PowerManager::class.java)
    val activity = getSystemService(ActivityManager::class.java)
    val contrast = Settings.Secure.getInt(contentResolver, "high_text_contrast_enabled", 0) != 0 ||
        (Build.VERSION.SDK_INT >= 34 && (getSystemService(UiModeManager::class.java)?.contrast ?: 0f) > 0f)
    return GlassEnvironment(powerSave = power?.isPowerSaveMode == true,
        lowRam = activity?.isLowRamDevice == true, increasedContrast = contrast)
}

/** One observer set for the theme, including settings changed while the app is visible. */
@Composable
internal fun rememberGlassEnvironment(): GlassEnvironment {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var environment by remember(context) { mutableStateOf(context.glassEnvironment()) }
    DisposableEffect(context, lifecycle) {
        fun refresh() { environment = context.glassEnvironment() }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        ContextCompat.registerReceiver(context, receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("high_text_contrast_enabled"), false, observer)
        val listener = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycle.addObserver(listener)
        val manager = context.getSystemService(UiModeManager::class.java)
        val contrastListener = if (Build.VERSION.SDK_INT >= 34) {
            UiModeManager.ContrastChangeListener { refresh() }.also {
                manager?.addContrastChangeListener(ContextCompat.getMainExecutor(context), it)
            }
        } else null
        onDispose {
            context.unregisterReceiver(receiver)
            context.contentResolver.unregisterContentObserver(observer)
            lifecycle.removeObserver(listener)
            if (Build.VERSION.SDK_INT >= 34 && contrastListener != null) {
                manager?.removeContrastChangeListener(contrastListener)
            }
        }
    }
    return environment
}
