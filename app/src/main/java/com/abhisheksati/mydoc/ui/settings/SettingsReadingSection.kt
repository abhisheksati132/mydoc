package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.ReadingMode

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
        title = stringResource(R.string.settings_reading_title),
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        modifier = modifier
    ) {
        SettingsSectionLabel(text = stringResource(R.string.settings_default_reading_mode))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceChip(
                label = stringResource(R.string.reading_mode_normal),
                selected = currentReadingMode == ReadingMode.NORMAL,
                onClick = { onReadingModeChange(ReadingMode.NORMAL) }
            )
            ChoiceChip(
                label = stringResource(R.string.theme_dark),
                icon = Icons.Outlined.DarkMode,
                selected = currentReadingMode == ReadingMode.INVERTED_DARK,
                onClick = { onReadingModeChange(ReadingMode.INVERTED_DARK) }
            )
            ChoiceChip(
                label = stringResource(R.string.reading_mode_sepia),
                selected = currentReadingMode == ReadingMode.SEPIA,
                onClick = { onReadingModeChange(ReadingMode.SEPIA) }
            )
        }

        SettingsDivider(modifier = Modifier.padding(top = 12.dp))

        SettingsSectionLabel(text = stringResource(R.string.settings_pdf_page_scaling))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceChip(
                label = stringResource(R.string.settings_fit_width),
                selected = pdfFitMode == "FIT_WIDTH",
                onClick = { onPdfFitModeChange("FIT_WIDTH") }
            )
            ChoiceChip(
                label = stringResource(R.string.settings_fit_page),
                selected = pdfFitMode == "FIT_PAGE",
                onClick = { onPdfFitModeChange("FIT_PAGE") }
            )
        }

        SettingsDivider(modifier = Modifier.padding(top = 12.dp))

        SettingToggleRow(
            title = stringResource(R.string.settings_double_tap_title),
            subtitle = stringResource(R.string.settings_double_tap_subtitle),
            icon = Icons.Outlined.Speed,
            checked = doubleTapZoom,
            onCheckedChange = onDoubleTapZoomChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = stringResource(R.string.settings_keep_awake_title),
            subtitle = stringResource(R.string.settings_keep_awake_subtitle),
            icon = Icons.Outlined.LightMode,
            checked = keepScreenOn,
            onCheckedChange = onKeepScreenOnChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = stringResource(R.string.settings_volume_keys_title),
            subtitle = stringResource(R.string.settings_volume_keys_subtitle),
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
