package com.abhisheksati.mydoc.ui.settings

import com.abhisheksati.mydoc.designsystem.components.AppChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.ReadingMode

@Composable
fun SettingsReadingSection(
    currentReadingMode: ReadingMode,
    pdfFitMode: String,
    highQualityPdf: Boolean,
    doubleTapZoom: Boolean,
    keepScreenOn: Boolean,
    volumeKeyTurning: Boolean,
    onReadingModeChange: (ReadingMode) -> Unit,
    onPdfFitModeChange: (String) -> Unit,
    onHighQualityPdfChange: (Boolean) -> Unit,
    onDoubleTapZoomChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onVolumeKeyTurningChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    SettingsCard(
        title = "Reading & documents",
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        modifier = modifier
    ) {
        Text(
            text = "Default reading mode",
            style = typography.caption,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = AppSpacing.small, bottom = AppSpacing.small)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppChip(
                label = "Normal",
                selected = currentReadingMode == ReadingMode.NORMAL,
                onClick = { onReadingModeChange(ReadingMode.NORMAL) }
            )
            AppChip(
                label = "Dark",
                leadingIcon = Icons.Outlined.DarkMode,
                selected = currentReadingMode == ReadingMode.INVERTED_DARK,
                onClick = { onReadingModeChange(ReadingMode.INVERTED_DARK) }
            )
            AppChip(
                label = "Sepia",
                selected = currentReadingMode == ReadingMode.SEPIA,
                onClick = { onReadingModeChange(ReadingMode.SEPIA) }
            )
        }

        SettingsDivider()

        Text(
            text = "PDF page scaling",
            style = typography.caption,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = AppSpacing.small, bottom = AppSpacing.small)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppChip(
                label = "Fit Width",
                selected = pdfFitMode == "FIT_WIDTH",
                onClick = { onPdfFitModeChange("FIT_WIDTH") }
            )
            AppChip(
                label = "Fit Whole Page",
                selected = pdfFitMode == "FIT_PAGE",
                onClick = { onPdfFitModeChange("FIT_PAGE") }
            )
        }

        SettingsDivider()

        SettingToggleRow(
            title = "High-Quality Vector PDF",
            subtitle = "Supersample text and vectors",
            icon = Icons.Outlined.PictureAsPdf,
            checked = highQualityPdf,
            onCheckedChange = onHighQualityPdfChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Double-Tap Quick Zoom",
            subtitle = "Double tap a page to zoom to 2.5×",
            icon = Icons.Outlined.Speed,
            checked = doubleTapZoom,
            onCheckedChange = onDoubleTapZoomChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Keep Screen Awake While Reading",
            subtitle = "Stop the screen sleeping while reading",
            icon = Icons.Outlined.LightMode,
            checked = keepScreenOn,
            onCheckedChange = onKeepScreenOnChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Volume Button Page Turning",
            subtitle = "Volume keys flip pages",
            icon = Icons.Outlined.Vibration,
            checked = volumeKeyTurning,
            onCheckedChange = onVolumeKeyTurningChange
        )
    }
}
