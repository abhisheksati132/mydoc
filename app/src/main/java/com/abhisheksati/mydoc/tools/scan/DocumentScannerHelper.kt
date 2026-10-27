package com.abhisheksati.mydoc.tools.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Enterprise offline document scanner launcher helper.
 * Launches the built-in offline CameraScanSheet (CameraX + edge cropper)
 * safely with lifecycle state preservation.
 */
@Composable
fun rememberDocumentScanner(
    onLaunchScanner: () -> Unit
): () -> Unit {
    val launcher by rememberUpdatedState(onLaunchScanner)
    return {
        launcher()
    }
}
