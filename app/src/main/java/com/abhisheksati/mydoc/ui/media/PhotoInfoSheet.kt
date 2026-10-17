package com.abhisheksati.mydoc.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.ExifMetadata
import com.abhisheksati.mydoc.domain.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoInfoSheet(
    item: MediaItem,
    exif: ExifMetadata?,
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.paddingLarge, vertical = AppSpacing.paddingLarge)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Details",
                style = typography.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = AppSpacing.paddingMedium)
            )

            AppDivider()
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // File Information
            AppSectionHeader(title = "FILE INFORMATION")
            InfoRow(label = "Name", value = item.displayName)
            InfoRow(label = "Size", value = item.formattedSize)
            val dims = if (exif != null && exif.width > 0 && exif.height > 0) {
                "${exif.width} × ${exif.height} px"
            } else if (item.width > 0 && item.height > 0) {
                "${item.width} × ${item.height} px"
            } else "Unknown"
            InfoRow(label = "Dimensions", value = dims)
            InfoRow(label = "Date", value = item.formattedDate.ifEmpty { "Unknown" })
            InfoRow(label = "Format", value = item.mimeType)

            // EXIF Camera Information
            if (exif != null) {
                val hasCamera = !exif.cameraModel.isNullOrBlank() || !exif.cameraMake.isNullOrBlank()
                val hasShot = !exif.aperture.isNullOrBlank() || !exif.exposureTime.isNullOrBlank() || !exif.iso.isNullOrBlank()

                if (hasCamera || hasShot || exif.locationText != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                    AppDivider()
                    Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                    AppSectionHeader(title = "CAMERA & CAPTURE")

                    if (hasCamera) {
                        val cameraName = listOfNotNull(exif.cameraMake, exif.cameraModel).distinct().joinToString(" ")
                        InfoRow(label = "Camera", value = cameraName)
                    }

                    if (!exif.focalLength.isNullOrBlank()) {
                        InfoRow(label = "Focal length", value = exif.focalLength)
                    }
                    if (!exif.aperture.isNullOrBlank()) {
                        InfoRow(label = "Aperture", value = exif.aperture)
                    }
                    if (!exif.exposureTime.isNullOrBlank()) {
                        InfoRow(label = "Exposure", value = exif.exposureTime)
                    }
                    if (!exif.iso.isNullOrBlank()) {
                        InfoRow(label = "ISO", value = exif.iso)
                    }
                    if (!exif.dateTimeOriginal.isNullOrBlank()) {
                        InfoRow(label = "Captured", value = exif.dateTimeOriginal)
                    }

                    if (exif.locationText != null) {
                        InfoRow(label = "Coordinates", value = exif.locationText!!)
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.paddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = typography.body,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = typography.body.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary
        )
    }
}
