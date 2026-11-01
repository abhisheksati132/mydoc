package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Page palettes for the document readers.
 *
 * These are content surfaces, not app chrome: a sepia page stays sepia in light,
 * dark, and OLED modes. Reader screens must read these instead of declaring their
 * own paper colours — the same sepia was previously written out in four files.
 */
object DocumentCanvasColors {
    // Page surfaces
    val sepiaPage = Color(0xFFF4ECD8)
    val eyeComfortPage = Color(0xFFF7F3E9)
    val sepiaSpreadPage = Color(0xFFEADBBE)
    val eyeComfortSpreadPage = Color(0xFFECE4D0)
    val invertedPage = Color(0xFF0A0A0C)
    val blackPage = Color(0xFF000000)
    val pdfPage = Color(0xFFFAFAFC)

    // Ink on those surfaces
    val sepiaInk = Color(0xFF3A3125)
    val eyeComfortInk = Color(0xFF2C261F)
    val invertedInk = Color(0xFFEDEDED)
    val onDarkInk = Color(0xFFF1F5F9)
    val onLightInk = Color(0xFF1E293B)

    val sepiaMuted = Color(0xFF5A4E3A)
    val eyeComfortMuted = Color(0xFF5C5245)
    val invertedMuted = Color(0xFFA0A0A5)

    // Highlights and annotations
    val searchHighlight = Color(0x66FFEB3B)
    val activeHighlight = Color(0x99FFEB3B)
    val annotationOrange = Color(0xCCFF9800)
    val annotationRed = Color(0xFFFF5722)
    val annotationAccent = Color(0x555B4FE8)

    // Slide elements that arrive without their own fill or stroke
    val elementFill = Color(0x22888888)
    val elementBorder = Color(0x44888888)
    val elementHairline = Color(0x33888888)
    val elementShadow = Color(0x11000000)

    // Signature ink
    val inkBlack = Color(0xFF000000)
    val inkNavy = Color(0xFF1565C0)
    val inkCrimson = Color(0xFFD32F2F)
}
