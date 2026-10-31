package com.abhisheksati.mydoc.ui.media

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import com.abhisheksati.mydoc.designsystem.components.AppSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@androidx.annotation.OptIn(UnstableApi::class)
enum class VideoResizeMode(val label: String, val mode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    FIXED_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)
}

/**
 * Modern, high-performance video player powered by AndroidX Media3 ExoPlayer.
 * Features:
 * - 100% Kotlin / platform MediaCodec hardware acceleration (zero JNI/LibVLC crash risk)
 * - Gesture volume (right swipe), brightness (left swipe), and double-tap seek (10s)
 * - Resize modes (Fit, Zoom, Fill, 16:9), playback speeds (0.5x - 2.0x), looping
 * - Picture-in-Picture (PiP), rotation locking, persistent playback position resume
 * - AMOLED Pure Black theme with smooth 3-second auto-hiding controls
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    item: MediaItem,
    mediaRepository: MediaRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val scope = rememberCoroutineScope()

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(item.durationMs) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var orientationMode by remember { mutableIntStateOf(0) } // 0: Auto, 1: Landscape, 2: Portrait
    var isSpeedSheetOpen by remember { mutableStateOf(false) }

    // Gesture feedback overlay state
    var gestureOverlayText by remember { mutableStateOf<String?>(null) }
    var gestureOverlayIcon by remember { mutableStateOf<ImageVector?>(null) }

    // Keep screen on while player is active
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Initialize Media3 ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                val mediaItem = ExoMediaItem.fromUri(item.uri)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    }

    // Restore saved playback position
    LaunchedEffect(item.uri) {
        val savedPos = mediaRepository.getPlaybackPosition(item.uri.toString())
        if (savedPos > 2000L) {
            exoPlayer.seekTo(savedPos)
        }
    }

    // Track playback updates and auto-save position
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val dur = exoPlayer.duration
                    if (dur > 0L) totalDurationMs = dur
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 1000L && dur > 0L) {
                val saveTime = if (pos >= dur - 3000L) 0L else pos
                scope.launch {
                    mediaRepository.savePlaybackPosition(item.uri.toString(), saveTime, dur)
                }
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Periodic position polling for timeline scrubber
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition
            delay(250)
        }
    }

    // Auto-hide controls after 3.5 seconds of inactivity
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying && !isScreenLocked) {
            delay(3500)
            areControlsVisible = false
        }
    }

    // Handle orientation switching
    LaunchedEffect(orientationMode) {
        activity?.requestedOrientation = when (orientationMode) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        if (isScreenLocked) {
            isScreenLocked = false
        } else {
            onBackClick()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isScreenLocked) {
                detectTapGestures(
                    onTap = {
                        areControlsVisible = !areControlsVisible
                    },
                    onDoubleTap = { offset ->
                        if (!isScreenLocked) {
                            if (offset.x < size.width / 2f) {
                                // Double tap left: rewind 10s
                                exoPlayer.seekBack()
                                gestureOverlayText = "-10s"
                                gestureOverlayIcon = Icons.Default.FastRewind
                            } else {
                                // Double tap right: fast-forward 10s
                                exoPlayer.seekForward()
                                gestureOverlayText = "+10s"
                                gestureOverlayIcon = Icons.Default.FastForward
                            }
                            scope.launch {
                                delay(700)
                                gestureOverlayText = null
                                gestureOverlayIcon = null
                            }
                        }
                    }
                )
            }
            .pointerInput(isScreenLocked) {
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {
                        scope.launch {
                            delay(600)
                            gestureOverlayText = null
                            gestureOverlayIcon = null
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!isScreenLocked) {
                            change.consume()
                            totalDragY += dragAmount.y
                            val isLeftSide = change.position.x < size.width / 2f

                            if (abs(totalDragY) > 20f) {
                                if (isLeftSide) {
                                    // Brightness control
                                    val window = activity?.window
                                    val currentBrightness = window?.attributes?.screenBrightness ?: 0.5f
                                    val safeBrightness = if (currentBrightness < 0f) 0.5f else currentBrightness
                                    val delta = -dragAmount.y / (size.height * 0.7f)
                                    val newBrightness = (safeBrightness + delta).coerceIn(0.01f, 1.0f)
                                    window?.attributes = window?.attributes?.apply { screenBrightness = newBrightness }
                                    gestureOverlayText = "${(newBrightness * 100).toInt()}%"
                                    gestureOverlayIcon = Icons.Default.BrightnessMedium
                                } else {
                                    // Volume control
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val delta = -dragAmount.y / (size.height * 0.6f)
                                    val volStep = if (delta > 0) 1 else -1
                                    val newVol = (currentVol + volStep).coerceIn(0, maxSystemVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    gestureOverlayText = "${((newVol.toFloat() / maxSystemVolume) * 100).toInt()}%"
                                    gestureOverlayIcon = Icons.AutoMirrored.Filled.VolumeUp
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // Media3 ExoPlayer Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = VideoResizeMode.entries[resizeModeIndex].mode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = VideoResizeMode.entries[resizeModeIndex].mode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center Gesture Feedback Overlay (Brightness, Volume, Seek)
        if (gestureOverlayText != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gestureOverlayIcon?.let { icon ->
                        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = gestureOverlayText!!,
                        style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Screen Lock Indicator (When Locked)
        if (isScreenLocked) {
            IconButton(
                onClick = { isScreenLocked = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = colors.accent)
            }
        }

        // Top and Bottom Controls Overlay
        AnimatedVisibility(
            visible = areControlsVisible && !isScreenLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Text(
                        text = item.displayName,
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Aspect Ratio Button
                    IconButton(onClick = {
                        resizeModeIndex = (resizeModeIndex + 1) % VideoResizeMode.entries.size
                        Toast.makeText(context, VideoResizeMode.entries[resizeModeIndex].label, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                    }

                    // Screen Rotation Lock Toggle
                    IconButton(onClick = {
                        orientationMode = (orientationMode + 1) % 3
                        val modeName = when (orientationMode) {
                            1 -> "Landscape"
                            2 -> "Portrait"
                            else -> "Auto Rotate"
                        }
                        Toast.makeText(context, modeName, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = "Rotation", tint = Color.White)
                    }

                    // Picture-in-Picture Button
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(onClick = {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            activity?.enterPictureInPictureMode(params)
                        }) {
                            Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White)
                        }
                    }

                    // Share Video
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = item.mimeType
                            putExtra(Intent.EXTRA_STREAM, item.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }

                    // Lock Screen Controls
                    IconButton(onClick = {
                        isScreenLocked = true
                        areControlsVisible = false
                    }) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock", tint = Color.White)
                    }
                }

                // Center Play/Pause & Fast-Seek Buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekBack() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(colors.accent, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { exoPlayer.seekForward() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Bottom Timeline and Playback Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Timeline Slider & Time Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            style = typography.caption.copy(color = Color.White, fontWeight = FontWeight.Medium),
                            fontSize = 12.sp
                        )

                        AppSlider(
                            value = if (totalDurationMs > 0L) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f,
                            onValueChange = { ratio ->
                                currentPositionMs = (ratio * totalDurationMs).toLong()
                                exoPlayer.seekTo(currentPositionMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            ),
                            thumbSize = 14.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )

                        Text(
                            text = formatTime(totalDurationMs),
                            style = typography.caption.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium),
                            fontSize = 12.sp
                        )
                    }

                    // Bottom Quick Options: Loop, Speed, Subtitle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Loop Toggle
                        IconButton(onClick = {
                            isLooping = !isLooping
                            exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                            Toast.makeText(context, if (isLooping) "Repeat: ON" else "Repeat: OFF", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isLooping) colors.accent else Color.White
                            )
                        }

                        // Playback Speed Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { isSpeedSheetOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${playbackSpeed}x",
                                    style = typography.caption.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Playback Speed Selector Modal Bottom Sheet
    if (isSpeedSheetOpen) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        ModalBottomSheet(
            onDismissRequest = { isSpeedSheetOpen = false },
            containerColor = Color(0xFF141414),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Playback Speed",
                    style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                speeds.forEach { spd ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                playbackSpeed = spd
                                exoPlayer.playbackParameters = PlaybackParameters(spd)
                                isSpeedSheetOpen = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (spd == 1.0f) "1.0x (Normal)" else "${spd}x",
                            color = if (playbackSpeed == spd) colors.accent else Color.White,
                            fontWeight = if (playbackSpeed == spd) FontWeight.Bold else FontWeight.Normal
                        )
                        if (playbackSpeed == spd) {
                            Text("Active", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
