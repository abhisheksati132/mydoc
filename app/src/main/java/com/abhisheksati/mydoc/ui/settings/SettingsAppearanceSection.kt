package com.abhisheksati.mydoc.ui.settings

import com.abhisheksati.mydoc.R
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.designsystem.components.GlassEffectMode

@Composable
fun SettingsAppearanceSection(
    currentTheme: AppThemeMode,
    amoledBlack: Boolean,
    hapticFeedbackEnabled: Boolean,
    glassEffectMode: GlassEffectMode,
    onThemeChange: (AppThemeMode) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onGlassEffectModeChange: (GlassEffectMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    SettingsCard(
        title = stringResource(R.string.appearance_title),
        icon = Icons.Outlined.Palette,
        modifier = modifier
    ) {
        SettingsSectionLabel(text = stringResource(R.string.settings_theme))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeChip(
                label = stringResource(R.string.settings_system),
                icon = Icons.Outlined.BrightnessAuto,
                selected = currentTheme == AppThemeMode.SYSTEM,
                onClick = { onThemeChange(AppThemeMode.SYSTEM) }
            )
            ThemeChip(
                label = stringResource(R.string.theme_light),
                icon = Icons.Outlined.LightMode,
                selected = currentTheme == AppThemeMode.LIGHT,
                onClick = { onThemeChange(AppThemeMode.LIGHT) }
            )
            ThemeChip(
                label = stringResource(R.string.theme_dark),
                icon = Icons.Outlined.DarkMode,
                selected = currentTheme == AppThemeMode.DARK || currentTheme == AppThemeMode.BLACK,
                onClick = { onThemeChange(AppThemeMode.DARK) }
            )
        }

        SettingsDivider(modifier = Modifier.padding(top = 12.dp))

        SettingsSectionLabel(text = stringResource(R.string.settings_glass_effects))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeChip(
                label = stringResource(R.string.theme_auto),
                icon = Icons.Outlined.BrightnessAuto,
                selected = glassEffectMode == GlassEffectMode.AUTO,
                onClick = { onGlassEffectModeChange(GlassEffectMode.AUTO) }
            )
            ThemeChip(
                label = stringResource(R.string.toggle_on),
                icon = Icons.Outlined.Palette,
                selected = glassEffectMode == GlassEffectMode.ON,
                onClick = { onGlassEffectModeChange(GlassEffectMode.ON) }
            )
            ThemeChip(
                label = stringResource(R.string.toggle_off),
                icon = Icons.Outlined.Contrast,
                selected = glassEffectMode == GlassEffectMode.OFF,
                onClick = { onGlassEffectModeChange(GlassEffectMode.OFF) }
            )
        }

        SettingsDivider(modifier = Modifier.padding(top = 12.dp))

        SettingToggleRow(
            title = stringResource(R.string.settings_amoled_title),
            subtitle = stringResource(R.string.settings_amoled_subtitle),
            icon = Icons.Outlined.Contrast,
            checked = amoledBlack,
            onCheckedChange = onAmoledBlackChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = stringResource(R.string.settings_haptics_title),
            subtitle = stringResource(R.string.settings_haptics_subtitle),
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
