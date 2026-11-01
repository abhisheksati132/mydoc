package com.abhisheksati.mydoc.ui.filemanager

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import java.io.File

@Composable
fun StorageVolumeSummary(
    volumes: List<StorageVolume>,
    onVolumeClick: (StorageVolume) -> Unit,
    onStorageDetailsClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    if (volumes.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        for (volume in volumes) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.98f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "volume_card_scale"
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        if (onStorageDetailsClick != null) {
                            onStorageDetailsClick()
                        } else {
                            onVolumeClick(volume)
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = volume.title,
                                        style = typography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = colors.accent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Analyzer",
                                            style = typography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = colors.accent,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${volume.formattedUsed} of ${volume.formattedTotal} • ${volume.formattedFree} free",
                                    style = typography.caption.copy(fontSize = 11.sp),
                                    color = colors.textSecondary
                                )
                            }
                        }

                        val pct = (volume.usedRatio * 100).toInt().coerceIn(0, 100)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$pct%",
                                style = typography.title.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = colors.textPrimary
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "View Details",
                                tint = colors.textSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { volume.usedRatio },
                        color = colors.accent,
                        trackColor = colors.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun StoragePermissionBanner(
    onRequestPermission: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = colors.background == DarkAppColors.background

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
                    style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Grant All Files Access to browse and manage files across internal storage and SD cards without restrictions.",
                style = typography.caption.copy(fontSize = 12.sp, lineHeight = 18.sp),
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
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
        Triple(FileCategory.IMAGES, Icons.Outlined.Image, DocumentFormatColors.excelTint),
        Triple(FileCategory.VIDEOS, Icons.Outlined.Videocam, DocumentFormatColors.mediaTint),
        Triple(FileCategory.AUDIO, Icons.Outlined.AudioFile, DocumentFormatColors.zipTint),
        Triple(FileCategory.APKS, Icons.Outlined.Android, Color(0xFF10B981)),
        Triple(FileCategory.ARCHIVES, Icons.Outlined.FolderZip, DocumentFormatColors.pptTint),
        Triple(FileCategory.LARGE_FILES, Icons.Outlined.DataUsage, DocumentFormatColors.pdfTint)
    )

    val haptic = LocalHapticFeedback.current

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
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        style = typography.caption.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) colors.textPrimary else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PinnedFoldersStrip(
    folders: List<File>,
    onFolderClick: (File) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Pinned Folders",
                style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = colors.textSecondary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (folder in folders) {
                Surface(
                    color = colors.surface,
                    shape = AppRadius.pillShape,
                    border = BorderStroke(0.5.dp, colors.line),
                    modifier = Modifier
                        .clip(AppRadius.pillShape)
                        .clickable { onFolderClick(folder) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = folder.name,
                            style = typography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
