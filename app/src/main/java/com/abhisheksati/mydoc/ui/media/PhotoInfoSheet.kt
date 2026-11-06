package com.abhisheksati.mydoc.ui.media

import com.abhisheksati.mydoc.R
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
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

    val isOled = LocalIsOled.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        OverlaySystemBars()
        GlassSurface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            elevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.paddingLarge, vertical = AppSpacing.paddingLarge)
                    .verticalScroll(rememberScrollState())
            ) {
            Text(
                text = stringResource(R.string.menu_details),
                style = typography.heading,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = AppSpacing.paddingMedium)
            )

            AppDivider()
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // File Information
            AppSectionHeader(title = stringResource(R.string.photo_file_information))
            InfoRow(label = "Name", value = item.displayName)
            InfoRow(label = "Size", value = item.formattedSize)
            val dims = if (exif != null && exif.width > 0 && exif.height > 0) {
                "${exif.width} × ${exif.height} px"
            } else if (item.width > 0 && item.height > 0) {
                "${item.width} × ${item.height} px"
            } else stringResource(R.string.photo_unknown)
            InfoRow(label = stringResource(R.string.photo_dimensions), value = dims)
            InfoRow(label = stringResource(R.string.photo_date), value = item.formattedDate.ifEmpty { stringResource(R.string.photo_unknown) })
            InfoRow(label = stringResource(R.string.photo_format), value = item.mimeType)

            // EXIF Camera Information
            if (exif != null) {
                val hasCamera = !exif.cameraModel.isNullOrBlank() || !exif.cameraMake.isNullOrBlank()
                val hasShot = !exif.aperture.isNullOrBlank() || !exif.exposureTime.isNullOrBlank() || !exif.iso.isNullOrBlank()

                if (hasCamera || hasShot || exif.locationText != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                    AppDivider()
                    Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                    AppSectionHeader(title = stringResource(R.string.photo_camera_capture))

                    if (hasCamera) {
                        val cameraName = listOfNotNull(exif.cameraMake, exif.cameraModel).distinct().joinToString(" ")
                        InfoRow(label = stringResource(R.string.photo_camera), value = cameraName)
                    }

                    if (!exif.focalLength.isNullOrBlank()) {
                        InfoRow(label = stringResource(R.string.photo_focal_length), value = exif.focalLength)
                    }
                    if (!exif.aperture.isNullOrBlank()) {
                        InfoRow(label = stringResource(R.string.photo_aperture), value = exif.aperture)
                    }
                    if (!exif.exposureTime.isNullOrBlank()) {
                        InfoRow(label = stringResource(R.string.photo_exposure), value = exif.exposureTime)
                    }
                    if (!exif.iso.isNullOrBlank()) {
                        InfoRow(label = "ISO", value = exif.iso)
                    }
                    if (!exif.dateTimeOriginal.isNullOrBlank()) {
                        InfoRow(label = stringResource(R.string.photo_captured), value = exif.dateTimeOriginal)
                    }

                    if (exif.locationText != null) {
                        InfoRow(label = stringResource(R.string.photo_coordinates), value = exif.locationText ?: "")
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))
        }
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
            style = typography.bodyMedium,
            color = colors.textPrimary
        )
    }
}
