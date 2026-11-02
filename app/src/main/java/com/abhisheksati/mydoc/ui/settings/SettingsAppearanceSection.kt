package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.data.local.AppThemeMode

@Composable
fun SettingsAppearanceSection(
    currentTheme: AppThemeMode,
    amoledBlack: Boolean,
    hapticFeedbackEnabled: Boolean,
    onThemeChange: (AppThemeMode) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    SettingsCard(
        title = "Appearance",
        icon = Icons.Outlined.Palette,
        modifier = modifier
    ) {
        SettingsSectionLabel(text = "Theme")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeChip(
                label = "System",
                icon = Icons.Outlined.BrightnessAuto,
                selected = currentTheme == AppThemeMode.SYSTEM,
                onClick = { onThemeChange(AppThemeMode.SYSTEM) }
            )
            ThemeChip(
                label = "Light",
                icon = Icons.Outlined.LightMode,
                selected = currentTheme == AppThemeMode.LIGHT,
                onClick = { onThemeChange(AppThemeMode.LIGHT) }
            )
            ThemeChip(
                label = "Dark",
                icon = Icons.Outlined.DarkMode,
                selected = currentTheme == AppThemeMode.DARK,
                onClick = { onThemeChange(AppThemeMode.DARK) }
            )
            ThemeChip(
                label = "Black",
                icon = Icons.Outlined.Contrast,
                selected = currentTheme == AppThemeMode.BLACK,
                onClick = { onThemeChange(AppThemeMode.BLACK) }
            )
        }

        SettingsDivider(modifier = Modifier.padding(top = 12.dp))

        SettingToggleRow(
            title = "Pure AMOLED Black",
            subtitle = "Pitch black background, saves battery on OLED",
            icon = Icons.Outlined.Contrast,
            checked = amoledBlack,
            onCheckedChange = onAmoledBlackChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Tactile Vibration Haptics",
            subtitle = "Vibrate on taps and gestures",
            icon = Icons.Outlined.Vibration,
            checked = hapticFeedbackEnabled,
            onCheckedChange = {
                if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onHapticFeedbackChange(it)
            }
        )
    }
}

@Composable
private fun ThemeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = if (selected) Icons.Outlined.Check else icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}
