package com.abhisheksati.mydoc.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsMediaSection(
    videoHwAccel: Boolean,
    autoResumeVideo: Boolean,
    videoAutoLoop: Boolean,
    onVideoHwAccelChange: (Boolean) -> Unit,
    onAutoResumeVideoChange: (Boolean) -> Unit,
    onVideoAutoLoopChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = "VIDEO & AUDIO", modifier = modifier) {
        SettingToggleRow(
            title = "Hardware Video Acceleration",
            subtitle = "Use GPU hardware decoding for ultra-smooth 4K video playback",
            icon = Icons.Outlined.Videocam,
            checked = videoHwAccel,
            onCheckedChange = onVideoHwAccelChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Auto-Resume Video",
            subtitle = "Remember last playback position and resume automatically",
            icon = Icons.Outlined.Speed,
            checked = autoResumeVideo,
            onCheckedChange = onAutoResumeVideoChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Video Auto-Loop",
            subtitle = "Automatically replay videos when they reach the end",
            icon = Icons.Outlined.Repeat,
            checked = videoAutoLoop,
            onCheckedChange = onVideoAutoLoopChange
        )
    }
}
