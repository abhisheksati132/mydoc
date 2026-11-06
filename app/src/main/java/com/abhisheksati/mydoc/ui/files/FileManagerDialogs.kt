package com.abhisheksati.mydoc.ui.files

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.FolderProperties
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import com.abhisheksati.mydoc.designsystem.components.showMessage

@Composable
fun NewFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val colors = LocalAppColors.current
    val isOled = LocalIsOled.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
            AppRadius.modalShape
        ),
        title = { Text(stringResource(R.string.dialog_new_folder), color = colors.textPrimary) },
        text = {
            OverlaySystemBars()
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.dialog_folder_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onConfirm(name.trim())
                    }
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.action_create), color = if (name.trim().isNotEmpty()) colors.accent else colors.textSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun NewFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("New Document.txt") }
    val focusRequester = remember { FocusRequester() }
    val colors = LocalAppColors.current
    val isOled = LocalIsOled.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
            AppRadius.modalShape
        ),
        title = { Text(stringResource(R.string.dialog_new_file), color = colors.textPrimary) },
        text = {
            OverlaySystemBars()
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.dialog_file_name_hint)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onConfirm(name.trim())
                    }
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.action_create), color = if (name.trim().isNotEmpty()) colors.accent else colors.textSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun RenameDialog(
    file: File,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(file.name) }
    val focusRequester = remember { FocusRequester() }
    val colors = LocalAppColors.current
    val isOled = LocalIsOled.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
            AppRadius.modalShape
        ),
        title = { Text(stringResource(R.string.action_rename), color = colors.textPrimary) },
        text = {
            OverlaySystemBars()
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isNotEmpty() && name.trim() != file.name) {
                        onConfirm(name.trim())
                    }
                },
                enabled = name.trim().isNotEmpty() && name.trim() != file.name
            ) {
                Text(stringResource(R.string.action_rename), color = if (name.trim().isNotEmpty()) colors.accent else colors.textSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    count: Int,
    fileNamePreview: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalAppColors.current
    val isOled = LocalIsOled.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
            AppRadius.modalShape
        ),
        title = { Text(stringResource(R.string.dialog_move_to_bin), color = colors.textPrimary) },
        text = {
            OverlaySystemBars()
            // The operation moves the selection to the Recycle Bin; calling it a permanent delete
            // told the reader the opposite of what would happen.
            val message = if (count == 1 && fileNamePreview != null) {
                stringResource(R.string.dialog_move_file_to_bin, fileNamePreview)
            } else {
                stringResource(R.string.dialog_move_items_to_bin, count)
            }
            Text(message, color = colors.textSecondary)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_move_to_bin), color = colors.error, style = AppTheme.typography.bodyStrong)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        }
    )
}

@SuppressLint("NonObservableLocale")
@Composable
fun FilePropertiesDialog(
    properties: FolderProperties,
    checksum: String?,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val file = properties.file

    fun copyToClipboard(label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        context.showMessage("$label copied")
    }

    val isOled = LocalIsOled.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
            AppRadius.modalShape
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (file.isDirectory) stringResource(R.string.details_folder) else stringResource(R.string.details_file),
                    color = colors.textPrimary,
                    style = typography.titleSmall
                )
            }
        },
        text = {
            OverlaySystemBars()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Name
                PropertyItem(title = "Name", value = file.name)

                // Path
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        PropertyItem(title = stringResource(R.string.details_location), value = file.parent ?: "")
                    }
                    IconButton(
                        onClick = { copyToClipboard("Path", file.absolutePath) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.details_copy_path),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Size
                val formattedSize = formatBytes(properties.totalSizeBytes)
                val exactSize = "${properties.totalSizeBytes} bytes"
                PropertyItem(title = "Size", value = "$formattedSize ($exactSize)")

                // Folder Contents
                if (file.isDirectory) {
                    PropertyItem(
                        title = stringResource(R.string.details_contains),
                        value = "${properties.totalFilesCount} files, ${properties.totalFoldersCount} sub-folders"
                    )
                }

                // Last Modified — the locale comes from the configuration so a rotation or a
                // locale change re-composes with the right format instead of keeping a stale one.
                val df = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.MEDIUM,
                    LocalConfiguration.current.locales[0]
                )
                PropertyItem(title = stringResource(R.string.details_last_modified), value = df.format(Date(file.lastModified())))

                // Permissions
                val readable = stringResource(if (properties.isReadable) R.string.details_readable else R.string.details_unreadable)
                val writable = stringResource(if (properties.isWritable) R.string.details_writable else R.string.details_read_only)
                PropertyItem(
                    title = stringResource(R.string.details_permissions),
                    value = stringResource(R.string.details_permissions_value, readable, writable)
                )

                // Checksum
                if (!checksum.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.details_checksum),
                                style = typography.micro,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = checksum,
                                style = typography.micro.copy(fontFamily = FontFamily.Monospace),
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { copyToClipboard("SHA-256", checksum) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.details_copy_checksum),
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = colors.accent)
            }
        }
    )
}

@Composable
private fun PropertyItem(title: String, value: String) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = typography.micro,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = typography.label,
            color = colors.textPrimary
        )
    }
}

private fun formatBytes(bytes: Long): String = com.abhisheksati.mydoc.domain.model.StorageVolume.formatBytes(bytes)

@Composable
fun CompressDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    val focusRequester = remember { FocusRequester() }
    val colors = LocalAppColors.current
    val isOled = LocalIsOled.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
            AppRadius.modalShape
        ),
        title = { Text(stringResource(R.string.dialog_compress_zip), color = colors.textPrimary) },
        text = {
            OverlaySystemBars()
            Column {
                Text(
                    text = stringResource(R.string.dialog_archive_name),
                    style = LocalAppTypography.current.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Archive.zip") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onConfirm(name.trim())
                    }
                }
            ) {
                Text("Compress", color = colors.accent, style = AppTheme.typography.bodyStrong)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun ExtractConfirmDialog(
    archiveName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalAppColors.current
    val isOled = LocalIsOled.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
            AppRadius.modalShape
        ),
        title = { Text("Extract Archive", color = colors.textPrimary) },
        text = {
            OverlaySystemBars()
            Text(
                "Extract '$archiveName' into a dedicated folder in the current directory?",
                color = colors.textPrimary,
                style = LocalAppTypography.current.body
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Extract", color = colors.accent, style = AppTheme.typography.bodyStrong)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        }
    )
}

