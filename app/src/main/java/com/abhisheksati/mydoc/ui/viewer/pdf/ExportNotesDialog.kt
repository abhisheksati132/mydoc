package com.abhisheksati.mydoc.ui.viewer.pdf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog

@Composable
fun ExportNotesDialog(
    highlightCount: Int,
    onDismiss: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportPdf: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notes_export_title)) },
        text = {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.notes_export_message, highlightCount))
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExportMarkdown) {
                        Text(stringResource(R.string.notes_export_markdown))
                    }
                    Button(onClick = onExportPdf) {
                        Text(stringResource(R.string.notes_export_pdf))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
