package com.abhisheksati.mydoc.tools.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius

/** Operation errors stay visible until dismissed or a new attempt begins. */
@Composable
internal fun ImagePdfEditorFeedback(message: String, onDismiss: () -> Unit) {
    val focus = remember { BringIntoViewRequester() }
    LaunchedEffect(message) { focus.bringIntoView() }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = AppRadius.insetShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(message, modifier = Modifier.bringIntoViewRequester(focus).semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.imgpdf_dismiss_error)) }
        }
    }
}
