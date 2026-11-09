package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ChevronRight
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
    DIAGNOSTICS(R.string.diagnostics_title, R.string.diagnostics_summary, Icons.Outlined.Info),
    ABOUT(R.string.settings_about_title, R.string.settings_about_summary, Icons.Outlined.Info)
}

@Composable
internal fun SettingsCategoryIndex(onSelect: (SettingsCategory) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        SettingsCategory.entries.forEach { category ->
            ListItem(
                headlineContent = { Text(stringResource(category.title)) },
                supportingContent = { Text(stringResource(category.summary)) },
                leadingContent = { Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.clickable(role = Role.Button) { onSelect(category) }.padding(vertical = 4.dp)
            )
        }
    }
}
