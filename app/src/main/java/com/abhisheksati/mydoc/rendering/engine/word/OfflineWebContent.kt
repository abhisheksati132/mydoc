package com.abhisheksati.mydoc.rendering.engine.word

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import java.io.ByteArrayInputStream

/**
 * Hardens offscreen WebViews rendering local content against network access.
 * Enforces strict offline privacy: blocks network loads and responds to external URIs with 403.
 */
internal object OfflineWebContent {

    private val allowedPrefixes = listOf("file:///android_asset/", "data:", "blob:", "about:")

    fun harden(view: WebView) {
        view.settings.blockNetworkLoads = true
        view.settings.allowContentAccess = false
        @Suppress("DEPRECATION")
        view.settings.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        view.settings.allowUniversalAccessFromFileURLs = false
    }

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url?.toString().orEmpty()
        if (allowedPrefixes.any { url.startsWith(it) }) return null
        return WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
    }
}
