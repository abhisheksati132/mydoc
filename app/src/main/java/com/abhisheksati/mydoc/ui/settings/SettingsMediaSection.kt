package com.abhisheksati.mydoc.ui.settings

import com.abhisheksati.mydoc.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun SettingsMediaSection(
    autoResumeVideo: Boolean,
    videoAutoLoop: Boolean,
    onAutoResumeVideoChange: (Boolean) -> Unit,
    onVideoAutoLoopChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        showHeader = false,
        title = stringResource(R.string.settings_media_title),
        icon = Icons.Outlined.PlayCircle,
        modifier = modifier
    ) {
        SettingToggleRow(
            title = stringResource(R.string.settings_autoresume_title),
            subtitle = stringResource(R.string.settings_autoresume_subtitle),
            icon = Icons.Outlined.Speed,
            checked = autoResumeVideo,
            onCheckedChange = onAutoResumeVideoChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = stringResource(R.string.settings_autoloop_title),
            subtitle = stringResource(R.string.settings_autoloop_subtitle),
            icon = Icons.Outlined.Repeat,
            checked = videoAutoLoop,
            onCheckedChange = onVideoAutoLoopChange
        )
    }
}
