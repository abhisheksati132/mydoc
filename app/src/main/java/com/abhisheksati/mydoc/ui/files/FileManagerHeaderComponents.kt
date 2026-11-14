package com.abhisheksati.mydoc.ui.files

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.util.UserMessages
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import com.abhisheksati.mydoc.designsystem.components.showMessage

@Composable
fun StoragePermissionBanner(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
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
                    text = stringResource(R.string.perm_required_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_not_now),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.perm_required_message),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(stringResource(R.string.action_grant_access))
            }
        }
    }
}

fun shareFiles(context: Context, files: List<File>) {
    if (files.isEmpty()) return
    try {
        val success = com.abhisheksati.mydoc.util.shareUris(
            context,
            files.map { Uri.fromFile(it) },
            context.getString(R.string.share_via)
        )
        if (!success) {
            context.showMessage(context.getString(R.string.file_share_failed))
        }
    } catch (e: Exception) {
        context.showMessage(context.getString(R.string.file_share_failed))
    }
}
