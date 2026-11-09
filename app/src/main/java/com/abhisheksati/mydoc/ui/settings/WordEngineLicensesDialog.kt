package com.abhisheksati.mydoc.ui.settings

import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.abhisheksati.mydoc.R

/** Static, bundled legal text. Scripts, network loads and navigation stay disabled. */
@Composable
internal fun WordEngineLicensesDialog(onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.word_engine_licenses)) },
        text = {
            AndroidView(
                modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 480.dp),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.blockNetworkLoads = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                        }
                        val html = context.assets.open("licences/libreoffice/LICENSE.html")
                            .bufferedReader().use { it.readText() }
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { it.destroy() }
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}
