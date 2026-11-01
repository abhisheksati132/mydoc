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
    modifier: Modifier = Modifier,
    index: String? = null,
    showRule: Boolean = true
) {
    SettingsCard(
        title = "Video & audio",
        modifier = modifier,
        index = index,
        showRule = showRule
    ) {
        SettingToggleRow(
            title = "Hardware Video Acceleration",
            subtitle = "GPU decoding for smooth 4K",
            icon = Icons.Outlined.Videocam,
            checked = videoHwAccel,
            onCheckedChange = onVideoHwAccelChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Auto-Resume Video",
            subtitle = "Resume where you left off",
            icon = Icons.Outlined.Speed,
            checked = autoResumeVideo,
            onCheckedChange = onAutoResumeVideoChange
        )

        SettingsDivider()

        SettingToggleRow(
            title = "Video Auto-Loop",
            subtitle = "Loop videos when they end",
            icon = Icons.Outlined.Repeat,
            checked = videoAutoLoop,
            onCheckedChange = onVideoAutoLoopChange
        )
    }
}
