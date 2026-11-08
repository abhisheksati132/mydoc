package com.abhisheksati.mydoc.ui.viewer.controls

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import android.view.ViewTreeObserver
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import kotlin.coroutines.resume

internal fun preferLightInk(luminance: Float,previous: Boolean?): Boolean = when {
    luminance<0.42f -> true
    luminance>0.58f -> false
    else -> previous ?: (luminance<0.5f)
}
private class InkSamples {
    val bounds=mutableMapOf<Any,Rect>()
    val light=mutableStateMapOf<Any,Boolean>()
    var dirty=true
}
private val LocalInkSamples=staticCompositionLocalOf<InkSamples?> { null }
private fun Context.activity(): Activity? = when(this) { is Activity->this;is ContextWrapper->baseContext.takeIf { it!==this }?.activity();else->null }

/** One small, dirty-frame-only PixelCopy shared by all visible viewer toolbar buttons (API 26+). */
@Composable
internal fun AdaptiveInkProvider(enabled: Boolean,content: @Composable ()->Unit) {
    val window=LocalContext.current.activity()?.window
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val samples=remember { InkSamples() }
    DisposableEffect(window) {
        val observer=window?.decorView?.viewTreeObserver
        val listener=ViewTreeObserver.OnDrawListener { samples.dirty=true }
        observer?.addOnDrawListener(listener)
        onDispose { if(observer?.isAlive==true)observer.removeOnDrawListener(listener) }
    }
    LaunchedEffect(window,enabled,lifecycle) {
        if(window==null || !enabled)return@LaunchedEffect
        samples.dirty=true
        while(isActive) {
            delay(200)
            if(!samples.dirty || samples.bounds.isEmpty() || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))continue
            samples.dirty=false
            val bounds=samples.bounds.toMap()
            val measured=copyInk(window,bounds)
            measured.forEach { (id,luminance)->if(id in samples.bounds) samples.light[id]=preferLightInk(luminance,samples.light[id]) }
        }
    }
    CompositionLocalProvider(LocalInkSamples provides samples,content=content)
}
private suspend fun copyInk(window: Window,bounds: Map<Any,Rect>): Map<Any,Float> = suspendCancellableCoroutine { continuation ->
    val width=window.decorView.width
    val height=window.decorView.height
    if(width<=0 || height<=0) { continuation.resume(emptyMap());return@suspendCancellableCoroutine }
    val bitmap=Bitmap.createBitmap((width/6).coerceAtLeast(1),(height/6).coerceAtLeast(1),Bitmap.Config.ARGB_8888)
    fun complete(status: Int) {
        try {
            val values=if(status==PixelCopy.SUCCESS) bounds.mapValues { (_,r)->
                // Sample the button's surrounding surface, away from the central glyph.
                val points=listOf(r.left+3 to r.top+3,r.right-3 to r.top+3,r.left+3 to r.bottom-3,r.right-3 to r.bottom-3)
                points.map { (x,y)->
                    val p=bitmap.getPixel((x*bitmap.width/width).toInt().coerceIn(0,bitmap.width-1),(y*bitmap.height/height).toInt().coerceIn(0,bitmap.height-1))
                    (0.2126f*android.graphics.Color.red(p)+0.7152f*android.graphics.Color.green(p)+0.0722f*android.graphics.Color.blue(p))/255f
                }.average().toFloat()
            } else emptyMap()
            if(continuation.isActive)continuation.resume(values)
        } finally { bitmap.recycle() }
    }
    try { PixelCopy.request(window,null,bitmap,::complete,Handler(Looper.getMainLooper())) }
    catch(_: IllegalArgumentException) { complete(PixelCopy.ERROR_SOURCE_INVALID) }
}

@Composable
internal fun AdaptiveIconButton(onClick: ()->Unit,modifier: Modifier=Modifier,content: @Composable ()->Unit) {
    val samples=LocalInkSamples.current
    val id=remember { Any() }
    val fallback=com.abhisheksati.mydoc.designsystem.tokens.AppTheme.colors.inkPrimary
    DisposableEffect(samples,id) {
        onDispose { samples?.bounds?.remove(id);samples?.light?.remove(id) }
    }
    val ink=samples?.light?.get(id)?.let { if(it)Color.White else Color.Black } ?: fallback
    IconButton(onClick=onClick,modifier=modifier.onGloballyPositioned {
        val rect=it.boundsInWindow()
        if(samples?.bounds?.get(id)!=rect) { samples?.bounds?.set(id,rect);samples?.dirty=true }
    }) { CompositionLocalProvider(LocalContentColor provides ink,content=content) }
}
