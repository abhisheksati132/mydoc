package com.abhisheksati.mydoc.ui.filemanager

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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
        shape = RoundedCornerShape(18.dp),
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
                shape = RoundedCornerShape(999.dp),
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
    val typography = LocalAppTypography.current

    val categories = listOf(
        Triple(FileCategory.DOWNLOADS, Icons.Outlined.Download, colors.accent),
        Triple(FileCategory.DOCUMENTS, Icons.Outlined.Description, DocumentFormatColors.wordTint),
        Triple(FileCategory.IMAGES, Icons.Outlined.Image, DocumentFormatColors.mediaTint),
        Triple(FileCategory.VIDEOS, Icons.Outlined.Videocam, DocumentFormatColors.mediaTint),
        Triple(FileCategory.AUDIO, Icons.Outlined.AudioFile, DocumentFormatColors.mediaTint),
        Triple(FileCategory.APKS, Icons.Outlined.Android, colors.accent),
        Triple(FileCategory.ARCHIVES, Icons.Outlined.FolderZip, DocumentFormatColors.zipTint),
        Triple(FileCategory.LARGE_FILES, Icons.Outlined.DataUsage, DocumentFormatColors.pdfTint)
    )


    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((cat, icon, color) in categories) {
            val isSelected = activeCategory == cat
            val chipBg = if (isSelected) color.copy(alpha = 0.16f) else colors.surfaceContainer
            val chipBorderColor = if (isSelected) color.copy(alpha = 0.4f) else colors.line

            Surface(
                shape = AppRadius.pillShape,
                color = chipBg,
                border = BorderStroke(0.5.dp, chipBorderColor),
                modifier = Modifier
                    .clip(AppRadius.pillShape)
                    .clickable {
                        onCategorySelected(cat)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = cat.displayName,
                        tint = if (isSelected) color else color.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.displayName,
                        style = if (isSelected) typography.captionStrong else typography.caption,
                        color = if (isSelected) colors.textPrimary else colors.textSecondary
                    )
                }
            }
        }
    }
}
