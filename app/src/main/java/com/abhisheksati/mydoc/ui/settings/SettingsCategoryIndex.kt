package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.remember
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R

internal enum class SettingsCategory(val title: Int, val summary: Int, val icon: ImageVector) {
    APPEARANCE(R.string.appearance_title, R.string.settings_appearance_summary, Icons.Outlined.Palette),
    READING(R.string.settings_reading_title, R.string.settings_reading_summary, Icons.AutoMirrored.Outlined.MenuBook),
    MEDIA(R.string.settings_media_title, R.string.settings_media_summary, Icons.Outlined.VideoLibrary),
    STORAGE(R.string.settings_storage_title, R.string.settings_storage_summary, Icons.Outlined.Storage),
    PRIVACY(R.string.settings_privacy_security, R.string.settings_privacy_summary, Icons.Outlined.Security),
    ABOUT(R.string.settings_about_title, R.string.settings_about_summary, Icons.Outlined.Info)
}

@Composable
internal fun SettingsCategoryIndex(onSelect: (SettingsCategory) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val groups = listOf(
        R.string.settings_group_experience to listOf(SettingsCategory.APPEARANCE, SettingsCategory.READING, SettingsCategory.MEDIA),
        R.string.settings_group_device to listOf(SettingsCategory.STORAGE, SettingsCategory.PRIVACY, SettingsCategory.ABOUT)
    )
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        groups.forEach { (label, categories) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(label), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                Surface(
                    shape = AppRadius.xLargeShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        categories.forEachIndexed { index, category ->
                            val tone = when (category) {
                                SettingsCategory.APPEARANCE -> colors.accentPink
                                SettingsCategory.READING -> colors.accent
                                SettingsCategory.MEDIA -> colors.accentCyan
                                SettingsCategory.STORAGE -> colors.accentAmber
                                SettingsCategory.PRIVACY -> colors.accentGreen
                                SettingsCategory.ABOUT -> colors.inkMuted
                            }
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            val interactions = remember(category) { MutableInteractionSource() }
                            Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).pressScale(interactions)
                                .clickable(role = Role.Button, interactionSource = interactions,
                                    indication = ripple(), onClick = { onSelect(category) }).padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp).background(tone.copy(alpha = 0.12f), AppRadius.insetShape),
                                    contentAlignment = Alignment.Center) {
                                    Icon(category.icon, contentDescription = null, tint = colors.ink)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(category.title), style = AppTheme.typography.bodyLargeStrong, color = colors.ink)
                                    Text(stringResource(category.summary), style = AppTheme.typography.caption, color = colors.inkSecondary)
                                }
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null,
                                    tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}