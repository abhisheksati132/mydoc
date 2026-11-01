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
import com.abhisheksati.mydoc.designsystem.components.EditorialAction
import com.abhisheksati.mydoc.designsystem.components.EditorialRule
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding)
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.small))
            Text(
                text = "Storage access required",
                style = typography.bodyLargeStrong,
                color = colors.ink
            )
        }

        Text(
            text = "Grant All Files Access to browse and manage files across internal storage and SD cards.",
            style = typography.caption,
            color = colors.inkMuted,
            modifier = Modifier.padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.xSmall,
                bottom = AppSpacing.small
            )
        )

        EditorialAction(
            label = "Grant access",
            onClick = onRequestPermission,
            modifier = Modifier.padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                bottom = AppSpacing.small
            )
        )

        EditorialRule(
            modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
            thickness = AppSpacing.hairline,
            color = colors.line
        )
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
    val categories = listOf(
        FileCategory.DOWNLOADS to Icons.Outlined.Download,
        FileCategory.DOCUMENTS to Icons.Outlined.Description,
        FileCategory.IMAGES to Icons.Outlined.Image,
        FileCategory.VIDEOS to Icons.Outlined.Videocam,
        FileCategory.AUDIO to Icons.Outlined.AudioFile,
        FileCategory.APKS to Icons.Outlined.Android,
        FileCategory.ARCHIVES to Icons.Outlined.FolderZip,
        FileCategory.LARGE_FILES to Icons.Outlined.DataUsage
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScrollWithFade(rememberScrollState())
            .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        for ((cat, icon) in categories) {
            AppChip(
                label = cat.displayName,
                selected = activeCategory == cat,
                onClick = { onCategorySelected(cat) },
                leadingIcon = icon
            )
        }
    }
}
