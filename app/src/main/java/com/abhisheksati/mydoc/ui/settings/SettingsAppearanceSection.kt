package com.abhisheksati.mydoc.ui.settings

import com.abhisheksati.mydoc.designsystem.components.AppChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

@Composable
fun SettingsAppearanceSection(
    currentTheme: AppThemeMode,
    amoledBlack: Boolean,
    hapticFeedbackEnabled: Boolean,
    onThemeChange: (AppThemeMode) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    index: String? = null,
    showRule: Boolean = true
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

    SettingsCard(
        title = "Appearance",
        modifier = modifier,
        index = index,
        showRule = showRule
    ) {
        Text(
            text = "Theme",
            style = typography.caption,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = AppSpacing.small, bottom = AppSpacing.small)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppChip(
                label = "Light",
                leadingIcon = Icons.Outlined.LightMode,
                selected = currentTheme == AppThemeMode.LIGHT,
                onClick = { onThemeChange(AppThemeMode.LIGHT) }
            )
            AppChip(
                label = "Dark",
                leadingIcon = Icons.Outlined.DarkMode,
                selected = currentTheme == AppThemeMode.DARK,
                onClick = { onThemeChange(AppThemeMode.DARK) }
            )
            AppChip(
                label = "Black",
                leadingIcon = Icons.Outlined.Contrast,
                selected = currentTheme == AppThemeMode.BLACK,
                onClick = { onThemeChange(AppThemeMode.BLACK) }
            )
        }

        SettingsDivider()

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
