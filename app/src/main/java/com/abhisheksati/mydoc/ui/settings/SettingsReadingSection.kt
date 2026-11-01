package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    SettingsCard(title = "READING & DOCUMENTS", modifier = modifier) {
        Text(
            text = "Default Reading Mode",
            style = typography.captionStrong,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeChip(
                label = "Normal",
                icon = null,
                selected = currentReadingMode == ReadingMode.NORMAL,
                onClick = { onReadingModeChange(ReadingMode.NORMAL) }
            )
            ThemeChip(
                label = "Dark",
                icon = Icons.Outlined.DarkMode,
                selected = currentReadingMode == ReadingMode.INVERTED_DARK,
                onClick = { onReadingModeChange(ReadingMode.INVERTED_DARK) }
            )
            ThemeChip(
                label = "Sepia",
                icon = null,
                selected = currentReadingMode == ReadingMode.SEPIA,
                onClick = { onReadingModeChange(ReadingMode.SEPIA) }
            )
        }

        SettingsDivider()

        Text(
            text = "PDF Page Scaling",
            style = typography.captionStrong,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeChip(
                label = "Fit Width",
                icon = null,
                selected = pdfFitMode == "FIT_WIDTH",
                onClick = { onPdfFitModeChange("FIT_WIDTH") }
            )
            ThemeChip(
                label = "Fit Whole Page",
                icon = null,
                selected = pdfFitMode == "FIT_PAGE",
                onClick = { onPdfFitModeChange("FIT_PAGE") }
            )
        }

        SettingsDivider()

        SettingToggleRow(
            title = "High-Quality Vector PDF",
            subtitle = "Supersample text and vectors for ultra-crisp reading",
            icon = Icons.Outlined.PictureAsPdf,
            checked = highQualityPdf,
            onCheckedChange = onHighQualityPdfChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Double-Tap Quick Zoom",
            subtitle = "Double tap document page to jump to 2.5x zoom",
            icon = Icons.Outlined.Speed,
            checked = doubleTapZoom,
            onCheckedChange = onDoubleTapZoomChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Keep Screen Awake While Reading",
            subtitle = "Prevents display timeout while reading documents",
            icon = Icons.Outlined.LightMode,
            checked = keepScreenOn,
            onCheckedChange = onKeepScreenOnChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Volume Button Page Turning",
            subtitle = "Use volume keys to flip pages in book view",
            icon = Icons.Outlined.Vibration,
            checked = volumeKeyTurning,
            onCheckedChange = onVolumeKeyTurningChange
        )
    }
}
