package com.abhisheksati.mydoc.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentFile
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.components.showMessage

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
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = LocalIsOled.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(colors.glassRimTop, colors.glassRimBottom))
            ),
            shape = AppRadius.modalShape
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
                Text(
                    text = "Document details",
                    style = typography.title,
                    color = colors.textPrimary
                )
            }
        },
        text = {
            OverlaySystemBars()
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = document.displayName,
                    style = typography.body,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                AppDivider()
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

                DetailItem(label = "Format", value = "${document.typeBadge} (${document.extension.uppercase().ifEmpty { "N/A" }})")
                DetailItem(
                    label = "File size",
                    value = "${document.formattedSize} (${NumberFormat.getNumberInstance(Locale.US).format(document.sizeBytes)} bytes)"
                )
                if (document.pageCount > 0) {
                    DetailItem(label = "Pages", value = "${document.pageCount} pages")
                }
                if (document.formattedLastOpened.isNotBlank()) {
                    DetailItem(label = "Last opened", value = document.formattedLastOpened)
                }
                DetailItem(label = "MIME type", value = document.mimeType.ifEmpty { "Unknown" })

                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                AppDivider()
                Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                ) {
                    DialogActionButton(
                        icon = Icons.Outlined.Share,
                        label = "Share",
                        onClick = {
                            onDismiss()
                            onShare(document)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DialogActionButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        label = "Open with",
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
                                context.showMessage("No app available")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .height(AppSpacing.minTouchTarget)
                    .clip(AppRadius.roundShape)
                    .background(colors.accent)
                    .clickable {
                        onDismiss()
                        onOpen(document)
                    }
                    .padding(horizontal = AppSpacing.paddingLarge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open",
                    style = typography.label,
                    color = colors.background
                )
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .height(AppSpacing.minTouchTarget)
                    .clip(AppRadius.roundShape)
                    .border(
                        width = AppSpacing.dividerThickness,
                        color = colors.error,
                        shape = AppRadius.roundShape
                    )
                    .clickable {
                        onDismiss()
                        onRemove(document)
                    }
                    .padding(horizontal = AppSpacing.paddingMedium),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remove",
                        style = typography.label,
                        color = colors.error
                    )
                }
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = typography.caption,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = typography.caption,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DialogActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Box(
        modifier = modifier
            .height(AppSpacing.minTouchTarget)
            .clip(AppRadius.roundShape)
            .border(
                width = AppSpacing.dividerThickness,
                color = colors.divider,
                shape = AppRadius.roundShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
            Text(
                text = label,
                style = typography.label,
                color = colors.textPrimary
            )
        }
    }
}
