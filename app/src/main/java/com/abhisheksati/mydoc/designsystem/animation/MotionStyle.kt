package com.abhisheksati.mydoc.designsystem.animation

import androidx.compose.runtime.staticCompositionLocalOf

enum class MotionStyle { CALM, EXPRESSIVE }
val LocalMotionStyle = staticCompositionLocalOf { MotionStyle.EXPRESSIVE }
