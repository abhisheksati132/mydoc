package com.abhisheksati.mydoc.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.ui.theme.Spacing
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DocumentDetailsDialog(
    document: DocumentFile?,
    onDismiss: () -> Unit,
    onOpen: (DocumentFile) -> Unit,
    onShare: (DocumentFile) -> Unit,
    onRemove: (DocumentFile) -> Unit
) {
    if (document == null) return
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "Document Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
                AppDivider()
                Spacer(modifier = Modifier.height(Spacing.small))

                DetailItem(label = "Format", value = "${document.typeBadge} (${document.extension.uppercase().ifEmpty { "N/A" }})")
                DetailItem(
                    label = "File Size",
                    value = "${document.formattedSize} (${NumberFormat.getNumberInstance(Locale.US).format(document.sizeBytes)} bytes)"
                )
                if (document.pageCount > 0) {
                    DetailItem(label = "Pages", value = "${document.pageCount} pages")
                }
                if (document.formattedLastOpened.isNotBlank()) {
                    DetailItem(label = "Last Opened", value = document.formattedLastOpened)
                }
                DetailItem(label = "MIME Type", value = document.mimeType.ifEmpty { "Unknown" })

                Spacer(modifier = Modifier.height(Spacing.small))
                AppDivider()
                Spacer(modifier = Modifier.height(Spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onShare(document)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                val mime = document.mimeType.ifEmpty { "application/pdf" }
                                setDataAndType(Uri.parse(document.uri), mime)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No app available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open With")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpen(document)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Open")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onRemove(document)
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Remove")
            }
        },
        shape = RoundedCornerShape(8.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
