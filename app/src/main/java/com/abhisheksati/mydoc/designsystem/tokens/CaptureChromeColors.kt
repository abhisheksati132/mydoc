package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Camera, scanner, and cropper chrome.
 *
 * Capture surfaces are always dark regardless of app theme — the sensor preview
 * needs a neutral surround — so these sit outside [AppColors] rather than
 * duplicating a theme branch in every overlay.
 */
object CaptureChromeColors {
    val backdrop = Color(0xFF0F0F12)
    val cameraBase = Color(0xFF0D0D0D)
    val panel = Color(0xFF1E1E1E)
    val panelVeil = Color(0x991E1E1E)
    val surface = Color(0xFF212121)

    val scrim = Color(0x99000000)
    val scrimStrong = Color(0xCC000000)
    val controlsScrim = Color(0xB3121212)
    val sheetScrim = Color(0xF20D0D0D)

    val onChrome = Color.White
    val onChromeMuted = Color(0xAAFFFFFF)
    val onChromeFaint = Color(0x40FFFFFF)
    val onChromeGhost = Color(0x33FFFFFF)
    val onChromeWash = Color(0x28FFFFFF)

    val torchOn = Color(0xFFFFD54F)
    val capture = Color(0xCCFF5252)
    val thumbInactive = Color(0x6618181C)
    val gridLine = Color(0xFF69F0AE)
}
