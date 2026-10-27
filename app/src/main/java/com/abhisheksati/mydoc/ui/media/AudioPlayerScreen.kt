package com.abhisheksati.mydoc.ui.media

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.abhisheksati.mydoc.designsystem.components.AppSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.entity.MediaPlaybackEntity
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.kindColor
import com.abhisheksati.mydoc.designsystem.tokens.kindContainer
import com.abhisheksati.mydoc.designsystem.tokens.onKindColor
import com.abhisheksati.mydoc.designsystem.tokens.onKindContainer
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AudioPlayerScreen(
    item: MediaItem,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()

    val mintAccent = kindColor(MediaKind.AUDIO, isDarkTheme)
    val onMint = onKindColor(MediaKind.AUDIO, isDarkTheme)
    val mintContainer = kindContainer(MediaKind.AUDIO, isDarkTheme)
    val onMintContainer = onKindContainer(MediaKind.AUDIO, isDarkTheme)

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(item.durationMs) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer() }

    BackHandler {
        onBackClick()
    }

    // Audio Player setup
    DisposableEffect(item.uri) {
        try {
            mediaPlayer.setDataSource(context, item.uri)
            mediaPlayer.prepare()
            totalDurationMs = mediaPlayer.duration.toLong().coerceAtLeast(item.durationMs)
            mediaPlayer.isLooping = isLooping

            // Auto-resume from database if exists
            val app = context.applicationContext as? MyDocApp
            if (app != null) {
                scope.launch {
                    val saved = app.database.mediaPlaybackDao().getPlaybackPosition(item.uri.toString())
                    if (saved != null && saved.positionMs > 1000L && saved.positionMs < totalDurationMs - 3000L) {
                        mediaPlayer.seekTo(saved.positionMs.toInt())
                        currentPositionMs = saved.positionMs
                    }
                    mediaPlayer.start()
                    isPlaying = true
                }
            } else {
                mediaPlayer.start()
                isPlaying = true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot play audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        onDispose {
            try {
                // Save position on close
                val app = context.applicationContext as? MyDocApp
                if (app != null && mediaPlayer.currentPosition > 1000) {
                    val pos = mediaPlayer.currentPosition.toLong()
                    val dur = mediaPlayer.duration.toLong()
                    scope.launch {
                        app.database.mediaPlaybackDao().savePlaybackPosition(
                            MediaPlaybackEntity(
                                uri = item.uri.toString(),
                                positionMs = pos,
                                durationMs = dur,
                                updatedTimestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // Progress polling loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isSeeking && mediaPlayer.isPlaying) {
                currentPositionMs = mediaPlayer.currentPosition.toLong()
            }
            delay(250L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = AppSpacing.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    haptics.tap()
                    onBackClick()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.ink
                )
            }

            Text(
                text = "Audio Player",
                style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                color = colors.ink
            )

            IconButton(
                onClick = {
                    haptics.tap()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = item.mimeType.ifEmpty { "audio/*" }
                        putExtra(Intent.EXTRA_STREAM, item.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Audio"))
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = colors.ink
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.6f))

        // Large Mint Cover Placeholder (28dp rounded box)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = mintContainer,
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Audiotrack,
                        contentDescription = "Audio track",
                        tint = onMintContainer,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title and Folder/Format Meta
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.displayName,
                style = typography.headline.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = buildString {
                    if (!item.bucketName.isNullOrBlank()) append("${item.bucketName} • ")
                    append(item.mimeType.substringAfterLast('/').uppercase(Locale.ROOT))
                },
                style = typography.caption.copy(fontSize = 13.sp),
                color = colors.inkMuted
            )
        }

        Spacer(modifier = Modifier.weight(0.8f))

        // Scrubber Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            val progressFrac = if (totalDurationMs > 0) {
                val p = if (isSeeking) seekPositionMs else currentPositionMs
                (p.toFloat() / totalDurationMs).coerceIn(0f, 1f)
            } else 0f

            AppSlider(
                value = progressFrac,
                onValueChange = { frac ->
                    isSeeking = true
                    seekPositionMs = (frac * totalDurationMs).toLong()
                },
                onValueChangeFinished = {
                    currentPositionMs = seekPositionMs
                    try {
                        mediaPlayer.seekTo(seekPositionMs.toInt())
                    } catch (_: Exception) {}
                    isSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = mintAccent,
                    activeTrackColor = mintAccent,
                    inactiveTrackColor = colors.surfaceMuted
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayPos = if (isSeeking) seekPositionMs else currentPositionMs
                Text(
                    text = formatAudioTime(displayPos),
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = colors.inkMuted
                )
                Text(
                    text = formatAudioTime(totalDurationMs),
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = colors.inkMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transport Controls (-10s, Play/Pause, +10s)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rewind 10s
            Surface(
                shape = CircleShape,
                color = colors.surfaceMuted,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.tap()
                        val target = (currentPositionMs - 10000L).coerceAtLeast(0L)
                        try {
                            mediaPlayer.seekTo(target.toInt())
                            currentPositionMs = target
                        } catch (_: Exception) {}
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10 seconds",
                        tint = colors.ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Play / Pause (64dp circular mint button)
            Surface(
                shape = CircleShape,
                color = mintAccent,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.heavy()
                        if (isPlaying) {
                            try { mediaPlayer.pause() } catch (_: Exception) {}
                            isPlaying = false
                        } else {
                            try { mediaPlayer.start() } catch (_: Exception) {}
                            isPlaying = true
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = onMint,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Forward 10s
            Surface(
                shape = CircleShape,
                color = colors.surfaceMuted,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.tap()
                        val target = (currentPositionMs + 10000L).coerceAtMost(totalDurationMs)
                        try {
                            mediaPlayer.seekTo(target.toInt())
                            currentPositionMs = target
                        } catch (_: Exception) {}
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10 seconds",
                        tint = colors.ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Aux Controls: Speed Chip & Loop Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (playbackSpeed != 1.0f) mintContainer else colors.surfaceMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptics.tap()
                        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
                        val nextIdx = (speeds.indexOf(playbackSpeed) + 1).let { if (it in speeds.indices) it else 0 }
                        playbackSpeed = speeds[nextIdx]
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
                            }
                        } catch (_: Exception) {}
                    }
            ) {
                Text(
                    text = "${playbackSpeed}x Speed",
                    style = typography.caption.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = if (playbackSpeed != 1.0f) onMintContainer else colors.ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            // Loop Toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLooping) mintContainer else colors.surfaceMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptics.tap()
                        isLooping = !isLooping
                        try { mediaPlayer.isLooping = isLooping } catch (_: Exception) {}
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Auto Loop",
                        tint = if (isLooping) onMintContainer else colors.ink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLooping) "Looping" else "Loop Off",
                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (isLooping) onMintContainer else colors.ink
                    )
                }
            }
        }
    }
}

private fun formatAudioTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
