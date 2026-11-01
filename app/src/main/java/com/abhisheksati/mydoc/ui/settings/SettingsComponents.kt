package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * A settings section: a numbered heading and its rows on the page margin. There is
 * no card, so the section is separated by the rule above the heading and by white
 * space, exactly like a section in a printed manual.
 */
@Composable
fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    index: String? = null,
    showRule: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AppSectionHeader(
            title = title,
            index = index,
            showRule = showRule
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding),
            content = content
        )
    }
}

@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    HorizontalDivider(
        modifier = modifier,
        thickness = AppSpacing.hairline,
        color = colors.line
    )
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .defaultMinSize(minHeight = AppSpacing.listRowHeight)
            .padding(vertical = AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier
                .padding(end = AppSpacing.medium)
                .size(20.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = AppSpacing.medium)
        ) {
            Text(
                text = title,
                style = typography.bodyLargeStrong,
                color = colors.ink
            )
            Text(
                text = subtitle,
                style = typography.caption,
                color = colors.inkMuted,
                maxLines = 2
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.ink,
                checkedBorderColor = colors.ink,
                uncheckedThumbColor = colors.inkMuted,
                uncheckedTrackColor = colors.surface,
                uncheckedBorderColor = colors.line
            )
        )
    }
}
