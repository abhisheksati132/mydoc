package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.domain.model.ReadingMode

@Composable
fun SettingsReadingSection(
    currentReadingMode: ReadingMode,
    pdfFitMode: String,
    doubleTapZoom: Boolean,
    keepScreenOn: Boolean,
    volumeKeyTurning: Boolean,
    onReadingModeChange: (ReadingMode) -> Unit,
    onPdfFitModeChange: (String) -> Unit,
    onDoubleTapZoomChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onVolumeKeyTurningChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        title = "Reading & documents",
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        modifier = modifier
    ) {
        SettingsSectionLabel(text = "Default reading mode")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceChip(
                label = "Normal",
                selected = currentReadingMode == ReadingMode.NORMAL,
                onClick = { onReadingModeChange(ReadingMode.NORMAL) }
            )
            ChoiceChip(
                label = "Dark",
                icon = Icons.Outlined.DarkMode,
                selected = currentReadingMode == ReadingMode.INVERTED_DARK,
                onClick = { onReadingModeChange(ReadingMode.INVERTED_DARK) }
            )
            ChoiceChip(
                label = "Sepia",
                selected = currentReadingMode == ReadingMode.SEPIA,
                onClick = { onReadingModeChange(ReadingMode.SEPIA) }
            )
        }

        SettingsDivider(modifier = Modifier.padding(top = 12.dp))

        SettingsSectionLabel(text = "PDF page scaling")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceChip(
                label = "Fit Width",
                selected = pdfFitMode == "FIT_WIDTH",
                onClick = { onPdfFitModeChange("FIT_WIDTH") }
            )
            ChoiceChip(
                label = "Fit Whole Page",
                selected = pdfFitMode == "FIT_PAGE",
                onClick = { onPdfFitModeChange("FIT_PAGE") }
            )
        }

        SettingsDivider(modifier = Modifier.padding(top = 12.dp))

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

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}
