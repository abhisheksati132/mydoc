package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.PaperSurface
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.PaperShape
import com.abhisheksati.mydoc.designsystem.tokens.rememberSheetPalette

/**
 * A settings section printed on one sheet: flat icon plate, heading, and the
 * section's rows inside the paper card.
 */
@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val typography = LocalAppTypography.current
    val paper = rememberSheetPalette()

    PaperSurface(
        palette = paper,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(AppSpacing.medium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(PaperShape)
                        .background(paper.sheetEdge)
                        .border(1.dp, paper.rule, PaperShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = paper.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = typography.bodyLargeStrong,
                    color = paper.ink
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.xSmall),
                content = content
            )
        }
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
            tint = colors.accent,
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
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                checkedBorderColor = colors.accent,
                uncheckedThumbColor = colors.inkMuted,
                uncheckedTrackColor = colors.background,
                uncheckedBorderColor = colors.line
            )
        )
    }
}
