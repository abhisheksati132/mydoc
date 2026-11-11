package com.abhisheksati.mydoc.ui.media

import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.R
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.abhisheksati.mydoc.designsystem.components.AppSlider
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage
import com.abhisheksati.mydoc.util.shareFile

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
    modifier: Modifier = Modifier,
    autoResume: Boolean = true,
    autoLoop: Boolean = true,
    seekSeconds: Int = 10
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = context as? Activity
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val scope = rememberCoroutineScope()

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var isPlaying by remember { mutableStateOf(true) }
    var playbackFailed by remember(item.uri) { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var scrubPositionMs by remember { mutableStateOf<Long?>(null) }
    var totalDurationMs by remember { mutableLongStateOf(item.durationMs) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var initialDragVolume by remember { mutableIntStateOf(0) }
    var isLooping by remember { mutableStateOf(autoLoop) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var isScreenLocked by remember { mutableStateOf(false) }
    // Window bounds of the player, used as the PiP source-rect hint so the system can
    // animate the surface out of its real position instead of a full-screen snapshot.
    var playerBounds by remember { mutableStateOf<Rect?>(null) }
    var orientationMode by remember { mutableIntStateOf(0) } // 0: Auto, 1: Landscape, 2: Portrait
    var isSpeedSheetOpen by remember { mutableStateOf(false) }
    var playbackMenuOpen by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showNetworkInput by remember { mutableStateOf(false) }
    var activeUri by remember(item.uri) { mutableStateOf(item.uri) }
    var queue by remember(item.uri) { mutableStateOf(listOf(item.uri)) }
    var selectedTrackType by remember { mutableStateOf<Int?>(null) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var loopStart by remember { mutableStateOf<Long?>(null) }
    var loopEnd by remember { mutableStateOf<Long?>(null) }

    // Gesture feedback overlay state
    var gestureOverlayText by remember { mutableStateOf<String?>(null) }
    var gestureOverlayIcon by remember { mutableStateOf<ImageVector?>(null) }

    // Keep screen on while player is active
    val playerContentView = LocalView.current
    DisposableEffect(activity) {
        val window = activity?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.attributes = window?.attributes?.apply {
                screenBrightness = originalBrightness
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            // The bars were hidden for playback; the rest of the app needs them back.
            if (window != null) {
                WindowCompat.getInsetsController(window, playerContentView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // The video owns the whole screen, so the system bars are part of what it covers: they stay
    // hidden while the controls are hidden or the screen is locked, and come back with the controls
    // rather than sitting permanently over the top bar. A swipe still brings them transiently.
    LaunchedEffect(areControlsVisible, isScreenLocked) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, playerContentView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (areControlsVisible && !isScreenLocked) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Initialize Media3 ExoPlayer
    val exoPlayer = remember(item.uri) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(seekSeconds * 1000L)
            .setSeekForwardIncrementMs(seekSeconds * 1000L)
            .build().apply {
                setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, true)
                setHandleAudioBecomingNoisy(true)
                val mediaItem = ExoMediaItem.fromUri(item.uri)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    }

    LaunchedEffect(exoPlayer, isLooping) {
        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    val checkpointUri = activeUri
    val checkpoints = remember(checkpointUri, mediaRepository) {
        PlaybackCheckpointWriter { value, duration ->
            if (checkpointUri.scheme != "https") {
                mediaRepository.savePlaybackPosition(checkpointUri.toString(), value, duration)
            }
        }
    }
    var resumeReady by remember(checkpointUri) { mutableStateOf(false) }

    fun addToQueue(uri: android.net.Uri): Int {
        val existing = queue.indexOf(uri)
        if (existing >= 0) return existing
        val index = queue.size
        queue = queue + uri
        exoPlayer.addMediaItem(ExoMediaItem.fromUri(uri))
        isLooping = false
        return index
    }

    val queuePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { addToQueue(it) }
        showQueue = true
    }

    fun switchToQueueItem(index: Int) {
        if (exoPlayer.currentMediaItemIndex != index) {
            val position = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            scope.launch { checkpoints.checkpoint(position, duration, force = true) }
            exoPlayer.seekToDefaultPosition(index)
        }
        if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
        exoPlayer.play()
    }

    LaunchedEffect(checkpointUri, autoResume) {
        if (autoResume && checkpointUri.scheme != "https") {
            val saved = mediaRepository.getPlaybackPosition(checkpointUri.toString())
            if (saved > 2000L && exoPlayer.currentMediaItem?.localConfiguration?.uri == checkpointUri) {
                exoPlayer.seekTo(saved)
                checkpoints.seed(saved)
            }
        }
        resumeReady = true
    }

    // Track playback updates and auto-save position
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
            }.getOrNull().orEmpty()
            val mime = when (name.substringAfterLast('.').lowercase(Locale.ROOT)) {
                "srt" -> "application/x-subrip"
                "vtt" -> "text/vtt"
                "ttml", "dfxp" -> "application/ttml+xml"
                else -> null
            }
            if (mime == null) {
                context.showMessage(resources.getString(R.string.video_unsupported_subtitle))
            } else {
                val subtitle = ExoMediaItem.SubtitleConfiguration.Builder(uri)
                    .setMimeType(mime).setLabel(name).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
                val media = exoPlayer.currentMediaItem?.buildUpon()
                    ?.setSubtitleConfigurations(listOf(subtitle))?.build()
                if (media != null) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
                    exoPlayer.replaceMediaItem(exoPlayer.currentMediaItemIndex, media)
                    exoPlayer.prepare()
                }
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                val uri = mediaItem?.localConfiguration?.uri ?: return
                if (activeUri == uri) return
                activeUri = uri
                totalDurationMs = exoPlayer.duration.takeIf { it > 0L } ?: 0L
                currentPositionMs = exoPlayer.currentPosition
                scrubPositionMs = null
                loopStart = null
                loopEnd = null
                playbackFailed = false
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackFailed = true
                isPlaying = false
            }

            override fun onTracksChanged(current: Tracks) { tracks = current }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                currentPositionMs = newPosition.positionMs
            }

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
            // Nothing is saved from here: this scope is being cancelled. The checkpoints written
            // while playing, and the flush below that runs when playback stops, are what persist.
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Periodic position polling for the timeline scrubber, and the checkpoint it makes possible.
    LaunchedEffect(isPlaying, checkpoints, resumeReady) {
        if (!resumeReady) return@LaunchedEffect
        while (isPlaying) {
            val position = exoPlayer.currentPosition
            val start = loopStart
            val end = loopEnd
            if (start != null && end != null && position >= end) exoPlayer.seekTo(start)
            currentPositionMs = position
            checkpoints.checkpoint(position, exoPlayer.duration)
            delay(250)
        }
        checkpoints.checkpoint(exoPlayer.currentPosition, exoPlayer.duration, force = true)
    }

    val videoLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(videoLifecycleOwner, checkpoints, resumeReady) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                if (activity?.isInPictureInPictureMode != true) exoPlayer.pause()
                // The composition is still alive when the app is backgrounded, so this scope works.
                if (resumeReady) scope.launch { checkpoints.checkpoint(exoPlayer.currentPosition, exoPlayer.duration, force = true) }
            }
        }
        videoLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { videoLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-hide controls after 3.5 seconds of inactivity
    LaunchedEffect(areControlsVisible, isPlaying, playbackMenuOpen, selectedTrackType, scrubPositionMs == null) {
        if (areControlsVisible && isPlaying && !isScreenLocked && !playbackMenuOpen && selectedTrackType == null && scrubPositionMs == null) {
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
                                gestureOverlayText = "-${seekSeconds}s"
                                gestureOverlayIcon = Icons.Default.FastRewind
                            } else {
                                // Double tap right: fast-forward 10s
                                exoPlayer.seekForward()
                                gestureOverlayText = "+${seekSeconds}s"
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
                    onDragStart = {
                        totalDragY = 0f
                        initialDragVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    },
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
                                    if (window != null) {
                                        window.attributes = window.attributes.apply { screenBrightness = newBrightness }
                                    }
                                    gestureOverlayText = "${(newBrightness * 100).toInt()}%"
                                    gestureOverlayIcon = Icons.Default.BrightnessMedium
                                } else {
                                    // Volume control: calculate absolute volume relative to drag start
                                    val effectiveDragY = totalDragY - (if (totalDragY > 0f) 20f else -20f)
                                    val deltaFraction = -effectiveDragY / (size.height * 0.5f)
                                    val newVol = (initialDragVolume + kotlin.math.round(deltaFraction * maxSystemVolume).toInt())
                                        .coerceIn(0, maxSystemVolume)
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    if (newVol != currentVol) {
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    }
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
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    val bounds = coords.boundsInWindow()
                    playerBounds = Rect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt()
                    )
                }
        )

        // Center Gesture Feedback Overlay (Brightness, Volume, Seek)
        if (gestureOverlayText != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = AppRadius.largeShape,
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
                        text = gestureOverlayText ?: "",
                        style = typography.titleSmall,
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
                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.video_unlock), tint = colors.accent)
            }
        }

        // Top and Bottom Controls Overlay
        AnimatedVisibility(
            visible = areControlsVisible && !isScreenLocked && activity?.isInPictureInPictureMode != true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Action Bar — fixed dark scrim (the video surface is always black,
                // so theme-adaptive glass would flip to a light plate in light mode and
                // make the white icons invisible).
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            // In landscape the cutout and the navigation bar move to the sides, so
                            // the bars have to be padded there too or the outermost buttons land
                            // under the notch.
                            .displayCutoutPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = Color.White)
                    }

                    Text(
                        text = videoSourceLabel(item, activeUri),
                        style = typography.documentName,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    val isLandscape = orientationMode == 1 ||
                        LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                    if (isLandscape) {
                        // Aspect Ratio Button
                        IconButton(onClick = {
                            resizeModeIndex = (resizeModeIndex + 1) % VideoResizeMode.entries.size
                            context.showMessage(VideoResizeMode.entries[resizeModeIndex].label)
                        }) {
                            Icon(Icons.Outlined.AspectRatio, contentDescription = stringResource(R.string.video_aspect_ratio), tint = Color.White)
                        }

                        // Screen Rotation Lock Toggle
                        IconButton(onClick = {
                            orientationMode = (orientationMode + 1) % 3
                        }) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = stringResource(R.string.video_rotation), tint = Color.White)
                        }
                    }

                    // Picture-in-Picture Button
                    IconButton(onClick = {
                        val pipParams = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .apply {
                                playerBounds?.let { setSourceRectHint(it) }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    setAutoEnterEnabled(true)
                                }
                            }
                            .build()
                        activity?.enterPictureInPictureMode(pipParams)
                    }) {
                        Icon(Icons.Default.PictureInPicture, contentDescription = stringResource(R.string.cd_pip), tint = Color.White)
                    }

                    Box {
                        IconButton(onClick = { playbackMenuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.video_playback_options), tint = Color.White)
                        }
                        com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu(
                            expanded = playbackMenuOpen, onDismissRequest = { playbackMenuOpen = false }
                        ) {
                            VideoMenuAction(R.string.video_queue) {
                                playbackMenuOpen = false
                                showQueue = true
                            }
                            VideoMenuAction(R.string.video_network_stream) {
                                playbackMenuOpen = false
                                showNetworkInput = true
                            }
                            VideoMenuAction(R.string.video_audio_tracks) {
                                playbackMenuOpen = false
                                selectedTrackType = C.TRACK_TYPE_AUDIO
                            }
                            VideoMenuAction(R.string.video_subtitle_tracks) {
                                playbackMenuOpen = false
                                selectedTrackType = C.TRACK_TYPE_TEXT
                            }
                            VideoMenuAction(R.string.video_load_subtitle) {
                                playbackMenuOpen = false
                                subtitlePicker.launch(arrayOf("*/*"))
                            }
                            VideoMenuAction(when {
                                loopEnd != null -> R.string.video_clear_loop
                                loopStart != null -> R.string.video_mark_loop_end
                                else -> R.string.video_mark_loop_start
                            }) {
                                playbackMenuOpen = false
                                val start = loopStart
                                val position = exoPlayer.currentPosition
                                when {
                                    loopEnd != null -> { loopStart = null; loopEnd = null }
                                    start == null -> loopStart = position
                                    position > start + 500L -> loopEnd = position
                                    else -> context.showMessage(resources.getString(R.string.video_loop_end_invalid))
                                }
                            }
                            if (!isLandscape) {
                                VideoMenuAction(R.string.video_aspect_ratio) {
                                    playbackMenuOpen = false
                                    resizeModeIndex = (resizeModeIndex + 1) % VideoResizeMode.entries.size
                                    context.showMessage(VideoResizeMode.entries[resizeModeIndex].label)
                                }
                                VideoMenuAction(R.string.video_rotation) {
                                    playbackMenuOpen = false
                                    orientationMode = (orientationMode + 1) % 3
                                }
                            }
                            if (activeUri.scheme != "https") VideoMenuAction(R.string.action_share) {
                                playbackMenuOpen = false
                                if (!shareFile(context, activeUri, if (activeUri == item.uri) item.mimeType else "video/*", resources.getString(R.string.video_share_label))) {
                                    context.showMessage(resources.getString(R.string.video_no_share_app))
                                }
                            }
                        }
                    }

                    // Lock Screen Controls
                    IconButton(onClick = {
                        isScreenLocked = true
                        areControlsVisible = false
                    }) {
                        Icon(Icons.Default.LockOpen, contentDescription = stringResource(R.string.cd_lock_screen), tint = Color.White)
                    }
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
                        Icon(Icons.Default.FastRewind, contentDescription = stringResource(R.string.video_skip_back_seconds, seekSeconds), tint = Color.White, modifier = Modifier.size(28.dp))
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
                        Icon(Icons.Default.FastForward, contentDescription = stringResource(R.string.video_skip_forward_seconds, seekSeconds), tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Bottom Timeline and Playback Controls — fixed dark scrim (see top bar note).
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .displayCutoutPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                    val displayedPosition = scrubPositionMs ?: currentPositionMs
                    // Timeline Slider & Time Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(displayedPosition),
                            style = typography.caption.copy(color = Color.White)
                        )

                        AppSlider(
                            value = if (totalDurationMs > 0L) (displayedPosition.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            enabled = totalDurationMs > 0L,
                            onValueChange = { ratio -> scrubPositionMs = (ratio * totalDurationMs).toLong() },
                            onValueChangeFinished = {
                                scrubPositionMs?.let(exoPlayer::seekTo)
                                scrubPositionMs = null
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
                            style = typography.caption.copy(color = Color.White.copy(alpha = 0.8f))
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
                            context.showMessage(resources.getString(if (isLooping) R.string.video_repeat_on else R.string.video_repeat_off))
                        }) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = stringResource(R.string.cd_repeat),
                                tint = if (isLooping) colors.accent else Color.White
                            )
                        }

                        // Playback Speed Button
                        Surface(
                            shape = AppRadius.pillShape,
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
                                    style = typography.captionStrong.copy(color = Color.White)
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    // Playback Speed Selector Modal Bottom Sheet
    if (showQueue) {
        AppAlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text(stringResource(R.string.video_queue)) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    queue.forEachIndexed { index, uri ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(videoSourceLabel(item, uri)) },
                            leadingIcon = {
                                androidx.compose.material3.RadioButton(selected = uri == activeUri, onClick = null)
                            },
                            onClick = { switchToQueueItem(index); showQueue = false }
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showQueue = false
                    queuePicker.launch(arrayOf("video/*"))
                }) { Text(stringResource(R.string.video_queue_add)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showQueue = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    if (showNetworkInput) {
        NetworkVideoDialog(
            onDismiss = { showNetworkInput = false },
            onOpen = { uri ->
                switchToQueueItem(addToQueue(uri))
                showNetworkInput = false
            }
        )
    }

    if (playbackFailed) {
        AppAlertDialog(
            onDismissRequest = onBackClick,
            title = { Text(stringResource(R.string.video_cannot_play)) },
            text = { Text(stringResource(R.string.video_retry_help)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    playbackFailed = false
                    exoPlayer.prepare()
                    exoPlayer.play()
                }) { Text(stringResource(R.string.video_retry)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onBackClick) {
                    Text(stringResource(R.string.cd_back))
                }
            }
        )
    }

    selectedTrackType?.let { type ->
        VideoTrackDialog(tracks, type, onDismiss = { selectedTrackType = null }) { track ->
            val parameters = exoPlayer.trackSelectionParameters.buildUpon().clearOverridesOfType(type)
                .setTrackTypeDisabled(type, track == null && type == C.TRACK_TYPE_TEXT)
            if (track != null) parameters.setOverrideForType(TrackSelectionOverride(track.first.mediaTrackGroup, track.second))
            exoPlayer.trackSelectionParameters = parameters.build()
            selectedTrackType = null
        }
    }

    if (isSpeedSheetOpen) {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
        // Landscape gives a sheet about a fifth of the height it gets in portrait. Opening it
        // half-expanded and letting the list scroll is what keeps every speed reachable, and the
        // insets have to be paid on the sides as well as the bottom once the bars rotate.
        AppModalSheet(
            onDismissRequest = { isSpeedSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkAppColors.surface,
            contentColor = Color.White
        ) {
            OverlaySystemBars()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.video_playback_speed),
                    style = typography.bodyStrong,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                speeds.forEach { spd ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppRadius.cardShape)
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
                            style = if (playbackSpeed == spd) typography.bodyStrong else typography.body
                        )
                        if (playbackSpeed == spd) {
                            Text(stringResource(R.string.video_active), color = colors.accent, style = typography.caption)
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

@Composable
private fun VideoMenuAction(label: Int, onClick: () -> Unit) {
    androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick)
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoTrackDialog(
    tracks: Tracks,
    type: Int,
    onDismiss: () -> Unit,
    onSelect: (Pair<Tracks.Group, Int>?) -> Unit
) {
    val options = tracks.groups.filter { it.type == type }.flatMap { group ->
        (0 until group.length).filter { group.isTrackSupported(it) }.map { group to it }
    }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (type == C.TRACK_TYPE_TEXT) R.string.video_subtitle_tracks else R.string.video_audio_tracks)) },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                VideoMenuAction(if (type == C.TRACK_TYPE_TEXT) R.string.video_subtitles_off else R.string.video_track_auto) { onSelect(null) }
                options.forEachIndexed { number, (group, index) ->
                    val format = group.getTrackFormat(index)
                    val name = format.label ?: format.language?.let { Locale.forLanguageTag(it).displayLanguage }
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(name ?: stringResource(R.string.video_track_number, number + 1)) },
                        leadingIcon = {
                            androidx.compose.material3.RadioButton(selected = group.isTrackSelected(index), onClick = null)
                        },
                        onClick = { onSelect(group to index) }
                    )
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

private fun videoSourceLabel(item: MediaItem, uri: android.net.Uri): String = when {
    uri == item.uri -> item.displayName
    uri.scheme == "https" -> uri.host.orEmpty()
    else -> uri.lastPathSegment.orEmpty()
}

@Composable
private fun NetworkVideoDialog(onDismiss: () -> Unit, onOpen: (android.net.Uri) -> Unit) {
    var input by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.video_network_stream)) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = input,
                onValueChange = { input = it; invalid = false },
                label = { Text(stringResource(R.string.video_network_url)) },
                isError = invalid,
                supportingText = { if (invalid) Text(stringResource(R.string.video_network_invalid)) },
                singleLine = true
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val uri = runCatching { java.net.URI(input.trim()) }.getOrNull()
                if (uri?.scheme != "https" || uri.host.isNullOrBlank() || uri.rawUserInfo != null) {
                    invalid = true
                } else {
                    onOpen(android.net.Uri.parse(uri.toASCIIString()))
                }
            }) { Text(stringResource(R.string.action_open)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}