package com.abhisheksati.mydoc.tools.scan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Enterprise Google ML Kit Document Scanner integration.
 * Performs real-time edge detection, auto-cropping, shadow removal, perspective correction,
 * and multi-page batch scanning.
 *
 * Runs as an independent system activity intent so pressing back returns cleanly without
 * closing the application.
 */
@Composable
fun rememberDocumentScanner(
    onPages: (List<Uri>) -> Unit,
    onFinished: () -> Unit = {},
    onError: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val pages by rememberUpdatedState(onPages)
    val finished by rememberUpdatedState(onFinished)
    val error by rememberUpdatedState(onError)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uris = scanResult?.pages?.mapNotNull { it.imageUri }.orEmpty()
            if (uris.isNotEmpty()) {
                pages(uris)
            }
        }
        finished()
    }

    return start@{
        val activity = context.findActivity() ?: run {
            error("Activity context not found")
            finished()
            return@start
        }

        try {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(100)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()

            GmsDocumentScanning.getClient(options)
                .getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { e ->
                    error(e.localizedMessage ?: "Scanner failed to launch")
                    finished()
                }
        } catch (t: Throwable) {
            error(t.localizedMessage ?: "ML Kit scanner unavailable")
            finished()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
