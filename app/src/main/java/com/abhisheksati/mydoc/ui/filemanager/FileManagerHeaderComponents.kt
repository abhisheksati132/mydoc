package com.abhisheksati.mydoc.ui.filemanager

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.designsystem.components.AppChip
import com.abhisheksati.mydoc.designsystem.components.horizontalScrollWithFade
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.FileCategory
import java.io.File

@Composable
fun StoragePermissionBanner(
    onRequestPermission: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = LocalIsOled.current

    Surface(
        shape = AppRadius.largeShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Storage Access Required",
                    style = typography.bodyStrong,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Grant All Files Access to browse and manage files across internal storage and SD cards without restrictions.",
                style = typography.caption,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = if (isOled) Color.Black else Color.White
                ),
                shape = AppRadius.roundShape,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Grant Storage Access",
                        style = AppTheme.typography.captionStrong
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun FileManagerCategoryChips(
    activeCategory: FileCategory?,
    onCategorySelected: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    val categories = listOf(
        Triple(FileCategory.DOWNLOADS, Icons.Outlined.Download, colors.accent),
        Triple(FileCategory.DOCUMENTS, Icons.Outlined.Description, DocumentFormatColors.wordTint),
        Triple(FileCategory.IMAGES, Icons.Outlined.Image, DocumentFormatColors.mediaTint),
        Triple(FileCategory.VIDEOS, Icons.Outlined.Videocam, DocumentFormatColors.pptTint),
        Triple(FileCategory.AUDIO, Icons.Outlined.AudioFile, DocumentFormatColors.excelTint),
        Triple(FileCategory.APKS, Icons.Outlined.Android, colors.accent),
        Triple(FileCategory.ARCHIVES, Icons.Outlined.FolderZip, DocumentFormatColors.zipTint),
        Triple(FileCategory.LARGE_FILES, Icons.Outlined.DataUsage, DocumentFormatColors.pdfTint)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScrollWithFade(rememberScrollState())
            .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        for ((cat, icon, tint) in categories) {
            AppChip(
                label = cat.displayName,
                selected = activeCategory == cat,
                onClick = { onCategorySelected(cat) },
                leadingIcon = icon,
                accentTint = tint
            )
        }
    }
}
