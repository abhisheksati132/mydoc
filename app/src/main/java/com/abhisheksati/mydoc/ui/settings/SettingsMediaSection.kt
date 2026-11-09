package com.abhisheksati.mydoc.ui.settings

import com.abhisheksati.mydoc.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun SettingsMediaSection(
    autoResumeVideo: Boolean,
    videoAutoLoop: Boolean,
    onAutoResumeVideoChange: (Boolean) -> Unit,
    onVideoAutoLoopChange: (Boolean) -> Unit,
    videoSeekSeconds: Int,
    onVideoSeekSecondsChange: (Int) -> Unit,
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

        SettingsDivider()
        var showSeekOptions by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        androidx.compose.foundation.layout.Box {
            SettingActionRow(
                title = stringResource(R.string.video_seek_interval),
                subtitle = stringResource(R.string.video_seek_seconds, videoSeekSeconds),
                icon = Icons.Outlined.Speed,
                actionLabel = stringResource(R.string.video_change_seek_interval),
                onAction = { showSeekOptions = true }
            )
            com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu(
                expanded = showSeekOptions,
                onDismissRequest = { showSeekOptions = false }
            ) {
                listOf(5, 10, 15, 30).forEach { seconds ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.video_seek_seconds, seconds)) },
                        onClick = { onVideoSeekSecondsChange(seconds); showSeekOptions = false }
                    )
                }
            }
        }
    }
}
