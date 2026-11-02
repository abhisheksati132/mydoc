package com.abhisheksati.mydoc.ui.files

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import com.abhisheksati.mydoc.designsystem.components.showMessage

@Composable
fun StoragePermissionBanner(
    onRequestPermission: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Storage access required",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = "Grant All Files Access to browse and manage files across internal storage and SD cards.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Grant access")
            }
        }
    }
}

fun shareFiles(context: Context, files: List<File>) {
    if (files.isEmpty()) return
    try {
        val sharedDir = File(context.cacheDir, "shared").apply { if (!exists()) mkdirs() }
        val uris = files.map { file ->
            val shareFile = if (file.parentFile?.absolutePath == sharedDir.absolutePath) {
                file
            } else {
                val dest = File(sharedDir, file.name)
                file.copyTo(dest, overwrite = true)
                dest
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        context.startActivity(Intent.createChooser(intent, "Share via"))
    } catch (e: Exception) {
        context.showMessage("Share error: ${e.message}")
    }
}
