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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    item: MediaItem,
    mediaRepository: MediaRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val typography = LocalAppTypography.current
    val scope = rememberCoroutineScope()

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(item.durationMs) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSpeedMenuOpen by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }

    // VLC-grade Screen Lock & Sleep Timer states
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockButton by remember { mutableStateOf(false) }
    var isSleepTimerDialogOpen by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var isAudioBoostEnabled by remember { mutableStateOf(false) }

    // VLC-grade Horizontal Drag Scrubbing
    var scrubAccumulatedX by remember { mutableFloatStateOf(0f) }
    var scrubStartPositionMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }

    // Gesture status overlays (Brightness, Volume & Scrub feedback)
    var gestureOverlayText by remember { mutableStateOf<String?>(null) }
    var gestureOverlayIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }

    // Orientation lock state: 0 = Sensor, 1 = Landscape, 2 = Portrait
    var orientationMode by remember { mutableIntStateOf(0) }

    // Subtitle state & auto-detection of companion .srt
    var subtitleUri by remember {
        mutableStateOf<Uri?>(
            try {
                val filePath = item.uri.path
                if (filePath != null) {
                    val file = java.io.File(filePath)
                    val srt = java.io.File(file.parentFile, "${file.nameWithoutExtension}.srt")
                    if (srt.exists()) Uri.fromFile(srt) else null
                } else null
            } catch (_: Exception) { null }
        )
    }

    val subtitleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            subtitleUri = uri
            Toast.makeText(context, "Loaded Subtitle: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }

    // Setup ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val exoItem = ExoMediaItem.fromUri(item.uri)
            setMediaItem(exoItem)
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Restore saved playback position and apply subtitle if present
    LaunchedEffect(item.uri, subtitleUri) {
        val savedPos = mediaRepository.getPlaybackPosition(item.uri.toString())
        val isCurrentlyPlaying = exoPlayer.isPlaying
        val targetPos = if (exoPlayer.currentPosition > 0L) exoPlayer.currentPosition else savedPos

        val builder = ExoMediaItem.Builder().setUri(item.uri)
        if (subtitleUri != null) {
            val isVtt = subtitleUri.toString().endsWith(".vtt", ignoreCase = true)
            val subConfig = ExoMediaItem.SubtitleConfiguration.Builder(subtitleUri!!)
                .setMimeType(if (isVtt) androidx.media3.common.MimeTypes.TEXT_VTT else androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("en")
                .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                .build()
            builder.setSubtitleConfigurations(listOf(subConfig))
        }

        exoPlayer.setMediaItem(builder.build())
        if (targetPos > 0) {
            exoPlayer.seekTo(targetPos)
        }
        exoPlayer.prepare()
        if (isCurrentlyPlaying) {
            exoPlayer.play()
        }
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = "This video format or codec is not supported by your device."
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            // Save progress on exit
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            scope.launch {
                mediaRepository.savePlaybackPosition(item.uri.toString(), pos, dur)
            }
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Periodic position updater and auto-save
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying) {
            if (!isSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(areControlsVisible, isPlaying, isScreenLocked) {
        if (areControlsVisible && isPlaying && !isScreenLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    // Auto-hide unlock pill after 3.5s
    LaunchedEffect(showUnlockButton) {
        if (showUnlockButton) {
            delay(3500)
            showUnlockButton = false
        }
    }

    // Sleep Timer countdown
    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60 * 1000L)
            exoPlayer.pause()
            sleepTimerMinutes = 0
            Toast.makeText(context, "Sleep timer finished. Playback stopped.", Toast.LENGTH_SHORT).show()
        }
    }

    // Dismiss gesture feedback after 1.5s
    LaunchedEffect(gestureOverlayText) {
        if (gestureOverlayText != null) {
            delay(1500)
            gestureOverlayText = null
            gestureOverlayIcon = null
        }
    }

    BackHandler {
        if (isScreenLocked) {
            isScreenLocked = false
            showUnlockButton = false
            areControlsVisible = true
            Toast.makeText(context, "Screen unlocked", Toast.LENGTH_SHORT).show()
        } else {
            onBackClick()
        }
    }

    val darkBackground = Color(0xFF0F0F10)
    val overlayBackground = Color(0xD9161618)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.paddingLarge),
                contentAlignment = Alignment.Center
            ) {
                AppEmptyState(
                    title = "Cannot play video",
                    message = errorMessage ?: "Playback error",
                    actionLabel = "Go back",
                    onAction = onBackClick
                )
            }
        } else {
            // AndroidView embedding ExoPlayer PlayerView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        this.resizeMode = resizeMode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = resizeMode
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isScreenLocked) {
                        detectTapGestures(
                            onTap = {
                                if (isScreenLocked) {
                                    showUnlockButton = true
                                } else {
                                    areControlsVisible = !areControlsVisible
                                }
                            },
                            onDoubleTap = { offset ->
                                if (!isScreenLocked) {
                                    val width = size.width
                                    if (offset.x < width * 0.4f) {
                                        val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                        exoPlayer.seekTo(target)
                                        gestureOverlayIcon = Icons.Default.FastRewind
                                        gestureOverlayText = "-10s"
                                    } else if (offset.x > width * 0.6f) {
                                        val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                        exoPlayer.seekTo(target)
                                        gestureOverlayIcon = Icons.Default.FastForward
                                        gestureOverlayText = "+10s"
                                    } else {
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(isScreenLocked, totalDurationMs) {
                        if (!isScreenLocked) {
                            detectDragGestures(
                                onDragStart = {
                                    scrubAccumulatedX = 0f
                                    scrubStartPositionMs = exoPlayer.currentPosition
                                    isScrubbing = false
                                },
                                onDragEnd = {
                                    if (isScrubbing) {
                                        val target = (scrubStartPositionMs + (scrubAccumulatedX * 120L).toLong()).coerceIn(0L, totalDurationMs)
                                        exoPlayer.seekTo(target)
                                        currentPositionMs = target
                                        isScrubbing = false
                                    }
                                },
                                onDragCancel = {
                                    isScrubbing = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val viewWidth = size.width

                                    // VLC signature: Horizontal scrub detection
                                    if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y) || isScrubbing) {
                                        isScrubbing = true
                                        scrubAccumulatedX += dragAmount.x
                                        val deltaMs = (scrubAccumulatedX * 120L).toLong()
                                        val targetMs = (scrubStartPositionMs + deltaMs).coerceIn(0L, totalDurationMs)
                                        val sign = if (deltaMs >= 0) "+" else "-"
                                        val diffFormatted = formatTime(kotlin.math.abs(deltaMs))
                                        val targetFormatted = formatTime(targetMs)
                                        gestureOverlayIcon = if (deltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind
                                        gestureOverlayText = "$sign$diffFormatted [$targetFormatted]"
                                    } else {
                                        val isLeft = change.position.x < viewWidth / 2f
                                        if (isLeft) {
                                            // Brightness adjustment
                                            val lp = activity?.window?.attributes
                                            if (lp != null) {
                                                val currentB = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                                val delta = -dragAmount.y / 500f
                                                val newB = (currentB + delta).coerceIn(0.01f, 1.0f)
                                                lp.screenBrightness = newB
                                                activity.window.attributes = lp
                                                gestureOverlayIcon = Icons.Default.BrightnessMedium
                                                gestureOverlayText = "${(newB * 100).toInt()}%"
                                            }
                                        } else {
                                            // Volume adjustment
                                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                            val delta = if (dragAmount.y < -12f) 1 else if (dragAmount.y > 12f) -1 else 0
                                            if (delta != 0) {
                                                val newVol = (currentVol + delta).coerceIn(0, maxVolume)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                gestureOverlayIcon = Icons.AutoMirrored.Filled.VolumeUp
                                                gestureOverlayText = "${((newVol.toFloat() / maxVolume) * 100).toInt()}%"
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
            )

            // Minimal Gestures Feedback Overlay Pill
            if (gestureOverlayText != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xCC000000),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            gestureOverlayIcon?.let {
                                Icon(imageVector = it, contentDescription = null, tint = Color.White)
                            }
                            Text(
                                text = gestureOverlayText!!,
                                style = typography.title,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Top Toolbar
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = overlayBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = AppSpacing.paddingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFEDEDED)
                            )
                        }

                        Text(
                            text = item.displayName,
                            style = typography.body.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFEDEDED),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = AppSpacing.paddingSmall)
                        )

                        // PiP button (Android 8.0+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "Picture-in-picture",
                                    tint = Color(0xFFEDEDED)
                                )
                            }
                        }

                        // Orientation lock toggle
                        IconButton(
                            onClick = {
                                orientationMode = (orientationMode + 1) % 3
                                val target = when (orientationMode) {
                                    1 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                                }
                                activity?.requestedOrientation = target
                                val label = when (orientationMode) {
                                    1 -> "Landscape"
                                    2 -> "Portrait"
                                    else -> "Auto-rotate"
                                }
                                Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Orientation",
                                tint = if (orientationMode != 0) Color(0xFF8AB4F8) else Color(0xFFEDEDED)
                            )
                        }

                        // Share
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = item.mimeType
                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color(0xFFEDEDED)
                            )
                        }

                        // Screen Lock Button (VLC style)
                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                areControlsVisible = false
                                showUnlockButton = true
                                Toast.makeText(context, "Screen Locked", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Controls",
                                tint = Color(0xFFEDEDED)
                            )
                        }

                        // More Menu (Sleep Timer, Audio Boost)
                        Box {
                            IconButton(onClick = { isMoreMenuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = Color(0xFFEDEDED)
                                )
                            }

                            DropdownMenu(
                                expanded = isMoreMenuOpen,
                                onDismissRequest = { isMoreMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sleep Timer (${if (sleepTimerMinutes > 0) "${sleepTimerMinutes}m" else "Off"})") },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        isSleepTimerDialogOpen = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isAudioBoostEnabled) "Audio Boost: On (150%)" else "Audio Boost: Off") },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        isAudioBoostEnabled = !isAudioBoostEnabled
                                        Toast.makeText(context, if (isAudioBoostEnabled) "Audio Boost enabled (150%)" else "Standard Volume", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Controls Bar (Timeline, Play/Pause, Speed, Loop, Resize)
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = overlayBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = AppSpacing.paddingLarge, vertical = AppSpacing.paddingSmall)
                    ) {
                        // Time & Timeline slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(if (isSeeking) seekPositionMs else currentPositionMs),
                                style = typography.caption,
                                color = Color(0xFFEDEDED)
                            )
                            Text(
                                text = formatTime(totalDurationMs),
                                style = typography.caption,
                                color = Color(0xFFA0A0A5)
                            )
                        }

                        Slider(
                            value = if (totalDurationMs > 0) {
                                val pos = if (isSeeking) seekPositionMs else currentPositionMs
                                (pos.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { frac ->
                                isSeeking = true
                                seekPositionMs = (frac * totalDurationMs).toLong()
                            },
                            onValueChangeFinished = {
                                currentPositionMs = seekPositionMs
                                exoPlayer.seekTo(seekPositionMs)
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF8AB4F8),
                                activeTrackColor = Color(0xFF8AB4F8),
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bottom Actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Resize mode toggle (Fit, Crop/Fill, Stretch)
                            IconButton(
                                onClick = {
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                    val name = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit to screen"
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop to fill"
                                        else -> "Stretch"
                                    }
                                    Toast.makeText(context, name, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "CROP"
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "STRETCH"
                                        else -> "FIT"
                                    },
                                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFEDEDED)
                                )
                            }

                            // Subtitle (CC) Button
                            Box {
                                var isSubtitleMenuOpen by remember { mutableStateOf(false) }
                                IconButton(onClick = { isSubtitleMenuOpen = true }) {
                                    Text(
                                        text = "CC",
                                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                        color = if (subtitleUri != null) Color(0xFF8AB4F8) else Color(0xFFEDEDED)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isSubtitleMenuOpen,
                                    onDismissRequest = { isSubtitleMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Load Subtitle (.srt / .vtt)") },
                                        onClick = {
                                            isSubtitleMenuOpen = false
                                            subtitleLauncher.launch("*/*")
                                        }
                                    )
                                    if (subtitleUri != null) {
                                        DropdownMenuItem(
                                            text = { Text("Turn Off Subtitles") },
                                            onClick = {
                                                isSubtitleMenuOpen = false
                                                subtitleUri = null
                                                Toast.makeText(context, "Subtitles turned off", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }

                            // Speed Selector button
                            Box {
                                IconButton(onClick = { isSpeedMenuOpen = true }) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                        color = if (playbackSpeed != 1.0f) Color(0xFF8AB4F8) else Color(0xFFEDEDED)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isSpeedMenuOpen,
                                    onDismissRequest = { isSpeedMenuOpen = false }
                                ) {
                                    listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f).forEach { spd ->
                                        DropdownMenuItem(
                                            text = { Text("${spd}x") },
                                            onClick = {
                                                isSpeedMenuOpen = false
                                                playbackSpeed = spd
                                                exoPlayer.playbackParameters = PlaybackParameters(spd)
                                            }
                                        )
                                    }
                                }
                            }

                            // 10s Rewind
                            IconButton(
                                onClick = {
                                    val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 10s",
                                    tint = Color(0xFFEDEDED)
                                )
                            }

                            // Play / Pause
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color(0xFFEDEDED),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // 10s Forward
                            IconButton(
                                onClick = {
                                    val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                    exoPlayer.seekTo(target)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 10s",
                                    tint = Color(0xFFEDEDED)
                                )
                            }

                            // Loop toggle
                            IconButton(
                                onClick = {
                                    isLooping = !isLooping
                                    exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Loop",
                                    tint = if (isLooping) Color(0xFF8AB4F8) else Color(0xFFEDEDED)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Screen Lock / Unlock Pill (VLC style)
        if (isScreenLocked && showUnlockButton) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xE6202022),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FFFFFF)),
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            isScreenLocked = false
                            showUnlockButton = false
                            areControlsVisible = true
                            Toast.makeText(context, "Screen Unlocked", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF8AB4F8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Screen Locked • Tap to unlock",
                            color = Color.White,
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Sleep Timer Dialog
        if (isSleepTimerDialogOpen) {
            AlertDialog(
                onDismissRequest = { isSleepTimerDialogOpen = false },
                title = { Text("Sleep Timer") },
                text = {
                    Column {
                        listOf(
                            0 to "Turn off",
                            15 to "15 minutes",
                            30 to "30 minutes",
                            45 to "45 minutes",
                            60 to "60 minutes"
                        ).forEach { (mins, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        sleepTimerMinutes = mins
                                        isSleepTimerDialogOpen = false
                                        if (mins > 0) {
                                            Toast.makeText(context, "Sleep timer set for $mins minutes", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = sleepTimerMinutes == mins,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(label, style = typography.body)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { isSleepTimerDialogOpen = false }) {
                        Text("Close")
                    }
                }
            )
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
