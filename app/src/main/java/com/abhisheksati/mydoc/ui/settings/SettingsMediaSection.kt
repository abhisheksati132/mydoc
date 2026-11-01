package com.abhisheksati.mydoc.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhisheksati.mydoc.designsystem.tokens.VividGradients

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
    SettingsCard(
        title = "Video & audio",
        icon = Icons.Outlined.PlayCircle,
        family = VividGradients.Family.SUNSET,
        modifier = modifier
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
