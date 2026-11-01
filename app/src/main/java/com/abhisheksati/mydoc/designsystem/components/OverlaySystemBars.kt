package com.abhisheksati.mydoc.designsystem.components

import android.app.Activity
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

/**
 * Dialogs and sheets own a separate window that AppTheme's activity pass never reaches, so their
 * bar appearance would keep following the platform theme. Call this inside dialog content.
 */
@Composable
fun OverlaySystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val dark = AppTheme.isDark

    SideEffect {
        val window = view.dialogWindow() ?: (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private fun View.dialogWindow(): Window? {
    var current: ViewParent? = parent
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent
    }
    return null
}
