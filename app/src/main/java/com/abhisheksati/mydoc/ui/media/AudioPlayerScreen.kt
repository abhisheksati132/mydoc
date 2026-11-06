package com.abhisheksati.mydoc.ui.media

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.util.UserMessages
import android.media.MediaPlayer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.data.local.entity.MediaPlaybackEntity
import com.abhisheksati.mydoc.designsystem.components.AppSlider
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.kindColor
import com.abhisheksati.mydoc.designsystem.tokens.kindContainer
import com.abhisheksati.mydoc.designsystem.tokens.onKindColor
import com.abhisheksati.mydoc.designsystem.tokens.onKindContainer
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage
import com.abhisheksati.mydoc.util.shareFile

@Composable
fun AudioPlayerScreen(
    item: MediaItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()

    val mintAccent = kindColor(MediaKind.AUDIO, AppTheme.isDark)
    val onMint = onKindColor(MediaKind.AUDIO, AppTheme.isDark)
    val mintContainer = kindContainer(MediaKind.AUDIO, AppTheme.isDark)
    val onMintContainer = onKindContainer(MediaKind.AUDIO, AppTheme.isDark)

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(item.durationMs) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer() }
    val app = context.applicationContext as? MyDocApp
    // Writes are checkpoints, not every frame; see PlaybackCheckpoints for the rule and what it costs.
    val checkpoints = remember(item.uri, app) {
        PlaybackCheckpointWriter { value, duration ->
            val uri = item.uri.toString()
            app?.mediaRepository?.savePlaybackPosition(uri, value, duration)
        }
    }

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
            if (app != null) {
                scope.launch {
                    val saved = app.database.mediaPlaybackDao().getPlaybackPosition(item.uri.toString())
                    val resume = saved?.let { PlaybackCheckpoints.valueToSave(it.positionMs, totalDurationMs) }
                    if (resume != null) {
                        mediaPlayer.seekTo(resume.toInt())
                        currentPositionMs = resume
                        checkpoints.seed(resume)
                    }
                    mediaPlayer.start()
                    isPlaying = true
                }
            } else {
                mediaPlayer.start()
                isPlaying = true
            }
        } catch (e: Exception) {
            context.showMessage(resources.getString(R.string.audio_cant_play))
        }

        onDispose {
            // No save is attempted from here: this composition's scope is being cancelled, which is
            // why the old write from onDispose usually never reached the database. The last
            // checkpoint, and the flush written when playback stopped or the app was backgrounded,
            // are what survive.
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // Progress polling loop, and the checkpoint writes that make "resume where you left off" real.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isSeeking && mediaPlayer.isPlaying) {
                val position = mediaPlayer.currentPosition.toLong()
                val duration = totalDurationMs
                currentPositionMs = position
                checkpoints.checkpoint(position, duration)
            }
            delay(250L)
        }
        // Paused, stopped or left: the position the listener actually reached.
        val position = runCatching { mediaPlayer.currentPosition.toLong() }.getOrDefault(0L)
        checkpoints.checkpoint(position, totalDurationMs, force = true)
    }

    // Backgrounding is a lifecycle transition, not a disposal: the scope is still alive here, so the
    // checkpoint written now is the one that survives a phone call.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, app) {
        val owner = lifecycleOwner
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && app != null) {
                val position = runCatching { mediaPlayer.currentPosition.toLong() }.getOrDefault(0L)
                scope.launch { checkpoints.checkpoint(position, totalDurationMs, force = true) }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
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
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.ink
                )
            }

            Text(
                text = stringResource(R.string.audio_player_title),
                style = typography.titleSmall,
                color = colors.ink
            )

            IconButton(
                onClick = {
                    haptics.tap()
                    if (!shareFile(context, item.uri, item.mimeType.ifEmpty { resources.getString(R.string.audio_mime_fallback) }, resources.getString(R.string.audio_share_label))) {
                        context.showMessage(resources.getString(R.string.audio_no_share_app))
                    }
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
                shape = AppRadius.xLargeShape,
                color = mintContainer,
                modifier = Modifier
                    .size(220.dp)
                    .clip(AppRadius.xLargeShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Audiotrack,
                        contentDescription = stringResource(R.string.cd_audio_track),
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
                style = typography.heading,
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
                style = typography.label,
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
                    style = typography.caption.withTabularFigures(),
                    color = colors.inkMuted
                )
                Text(
                    text = formatAudioTime(totalDurationMs),
                    style = typography.caption.withTabularFigures(),
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
                        contentDescription = stringResource(R.string.cd_rewind),
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
                        contentDescription = stringResource(R.string.cd_forward),
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
                shape = AppRadius.cardShape,
                color = if (playbackSpeed != 1.0f) mintContainer else colors.surfaceMuted,
                modifier = Modifier
                    .clip(AppRadius.cardShape)
                    .clickable {
                        haptics.tap()
                        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
                        val nextIdx = (speeds.indexOf(playbackSpeed) + 1).let { if (it in speeds.indices) it else 0 }
                        playbackSpeed = speeds[nextIdx]
                        try {
                            mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
                        } catch (_: Exception) {}
                    }
            ) {
                Text(
                    text = "${playbackSpeed}x Speed",
                    style = (if (playbackSpeed != 1.0f) typography.captionStrong else typography.caption)
                        .withTabularFigures(),
                    color = if (playbackSpeed != 1.0f) onMintContainer else colors.ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            // Loop Toggle
            Surface(
                shape = AppRadius.cardShape,
                color = if (isLooping) mintContainer else colors.surfaceMuted,
                modifier = Modifier
                    .clip(AppRadius.cardShape)
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
                        contentDescription = stringResource(R.string.cd_auto_loop),
                        tint = if (isLooping) onMintContainer else colors.ink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(if (isLooping) R.string.audio_loop_on else R.string.audio_loop_off),
                        style = if (isLooping) typography.captionStrong else typography.caption,
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
