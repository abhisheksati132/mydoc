package com.abhisheksati.mydoc.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsMediaSection(
    autoResumeVideo: Boolean,
    videoAutoLoop: Boolean,
    onAutoResumeVideoChange: (Boolean) -> Unit,
    onVideoAutoLoopChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        title = "Video & audio",
        icon = Icons.Outlined.PlayCircle,
        modifier = modifier
    ) {
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
