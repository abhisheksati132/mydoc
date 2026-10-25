package com.abhisheksati.mydoc.tools.scan

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Enterprise offline document scanner launcher.
 * Dispatches cleanly to the built-in offline CameraScanSheet (CameraX + edge cropper)
 * without requiring Google Play Services or downloading ML models over the network.
 */
@Composable
fun rememberDocumentScanner(
    onPages: (List<Uri>) -> Unit,
    onFinished: () -> Unit = {},
    onError: (String) -> Unit = {}
): () -> Unit {
    val error by rememberUpdatedState(onError)
    return {
        // Triggers the built-in offline CameraScanSheet in HomeScreen
        error("open_builtin_scanner")
    }
}