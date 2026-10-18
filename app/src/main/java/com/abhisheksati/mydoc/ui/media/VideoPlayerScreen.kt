package com.abhisheksati.mydoc.ui.media

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.Locale

/**
 * Aspect ratio configurations for VLC video rendering.
 */
enum class VlcAspectMode(val label: String, val buttonText: String, val ratioStr: String?, val scale: Float) {
    FIT("Fit to screen", "FIT", null, 0f),
    RATIO_16_9("16:9 widescreen", "16:9", "16:9", 0f),
    RATIO_4_3("4:3 standard", "4:3", "4:3", 0f),
    CROP_FILL("Crop to fill", "CROP", null, 1.35f),
    ORIGINAL("Original 100%", "1:1", null, 1.0f)
}

enum class PlayerEngine {
    LIBVLC,
    EXOPLAYER
}

private data class VlcMediaResult(
    val media: Media,
    val pfd: ParcelFileDescriptor? = null
)

private fun createVlcMedia(
    context: Context,
    libVLC: LibVLC,
    uri: Uri,
    enableHwDecoder: Boolean = true
): VlcMediaResult {
    // 1. Direct file scheme
    if (uri.scheme == "file") {
        val path = uri.path
        if (path != null && File(path).canRead()) {
            val media = Media(libVLC, path).apply {
                setHWDecoderEnabled(enableHwDecoder, false)
            }
            return VlcMediaResult(media, null)
        }
    }

    // 2. Content scheme - check MediaStore.MediaColumns.DATA for direct POSIX read
    if (uri.scheme == "content") {
        try {
            val proj = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (idx != -1 && cursor.moveToFirst()) {
                    val rawPath = cursor.getString(idx)
                    if (!rawPath.isNullOrBlank() && File(rawPath).canRead()) {
                        val media = Media(libVLC, rawPath).apply {
                            setHWDecoderEnabled(enableHwDecoder, false)
                        }
                        return VlcMediaResult(media, null)
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Content scheme - open ParcelFileDescriptor (PFD)
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val media = Media(libVLC, pfd.fileDescriptor).apply {
                    setHWDecoderEnabled(enableHwDecoder, false)
                }
                return VlcMediaResult(media, pfd)
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoPlayerScreen", "Failed to open FileDescriptor for $uri", e)
        }

        // 4. Content scheme - try AssetFileDescriptor if PFD failed
        try {
            val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
            if (afd != null) {
                val media = Media(libVLC, afd).apply {
                    setHWDecoderEnabled(enableHwDecoder, false)
                }
                return VlcMediaResult(media, afd.parcelFileDescriptor)
            }
        } catch (_: Exception) {}
    }

    // 5. Fallback to standard Uri
    val media = Media(libVLC, uri).apply {
        setHWDecoderEnabled(enableHwDecoder, false)
    }
    return VlcMediaResult(media, null)
}

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
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(item.durationMs) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var aspectModeIndex by remember { mutableIntStateOf(0) }
    val currentAspectMode = VlcAspectMode.entries[aspectModeIndex]
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

    // Subtitle state & auto-detection of companion .srt / .vtt
    var subtitleUri by remember {
        mutableStateOf<Uri?>(
            try {
                val filePath = item.uri.path
                if (filePath != null) {
                    val file = java.io.File(filePath)
                    val srt = java.io.File(file.parentFile, "${file.nameWithoutExtension}.srt")
                    val vtt = java.io.File(file.parentFile, "${file.nameWithoutExtension}.vtt")
                    when {
                        srt.exists() -> Uri.fromFile(srt)
                        vtt.exists() -> Uri.fromFile(vtt)
                        else -> null
                    }
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

    var activeEngine by remember { mutableStateOf(PlayerEngine.LIBVLC) }
    var activePfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var hwDecoderEnabled by remember { mutableStateOf(true) }
    var retryAttempt by remember { mutableIntStateOf(0) }

    // Initialize LibVLC native engine with optimized performance flags
    val libVLC = remember {
        val options = arrayListOf(
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--audio-time-stretch",
            "-vvv"
        )
        LibVLC(context, options)
    }

    // Initialize VLC MediaPlayer
    val mediaPlayer = remember {
        MediaPlayer(libVLC)
    }

    // Initialize ExoPlayer as resilient fallback engine
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Load media item into LibVLC, restore saved position, and configure hardware decoding
    LaunchedEffect(item.uri, hwDecoderEnabled, retryAttempt) {
        if (activeEngine == PlayerEngine.LIBVLC) {
            val savedPos = mediaRepository.getPlaybackPosition(item.uri.toString())
            try {
                activePfd?.close()
                activePfd = null

                val result = createVlcMedia(context, libVLC, item.uri, enableHwDecoder = hwDecoderEnabled)
                activePfd = result.pfd
                mediaPlayer.media = result.media
                result.media.release()

                if (subtitleUri != null) {
                    try {
                        mediaPlayer.addSlave(0 /* Subtitle */, subtitleUri!!, true)
                    } catch (_: Exception) {}
                }

                val targetPos = if (currentPositionMs > 0L) currentPositionMs else savedPos
                if (targetPos > 0L) {
                    mediaPlayer.time = targetPos
                    currentPositionMs = targetPos
                }
                mediaPlayer.play()
                isPlaying = true
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerScreen", "LibVLC open failed, falling back to ExoPlayer", e)
                activeEngine = PlayerEngine.EXOPLAYER
            }
        }
    }

    // Dynamic subtitle update
    LaunchedEffect(subtitleUri) {
        val uri = subtitleUri
        if (uri != null && activeEngine == PlayerEngine.LIBVLC) {
            try {
                mediaPlayer.addSlave(0 /* Subtitle */, uri, true)
            } catch (_: Exception) {}
        }
    }

    // Handle switching between VLC and ExoPlayer
    LaunchedEffect(activeEngine) {
        if (activeEngine == PlayerEngine.EXOPLAYER) {
            try {
                mediaPlayer.pause()
                val exoItem = ExoMediaItem.fromUri(item.uri)
                exoPlayer.setMediaItem(exoItem)
                exoPlayer.prepare()
                if (currentPositionMs > 0L) {
                    exoPlayer.seekTo(currentPositionMs)
                }
                exoPlayer.play()
                isPlaying = true
            } catch (e: Exception) {
                errorMessage = "Playback failed: ${e.message}"
            }
        } else if (activeEngine == PlayerEngine.LIBVLC) {
            exoPlayer.pause()
            if (currentPositionMs > 0L) {
                mediaPlayer.time = currentPositionMs
            }
            mediaPlayer.play()
            isPlaying = true
        }
    }

    // Periodic time & duration sync for ExoPlayer
    LaunchedEffect(activeEngine, isPlaying) {
        if (activeEngine == PlayerEngine.EXOPLAYER) {
            while (isPlaying) {
                if (!isSeeking && !isScrubbing) {
                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration
                    if (dur > 0L) {
                        totalDurationMs = dur
                    }
                }
                delay(250)
            }
        }
    }

    // LibVLC Event Listener & Lifecycle management
    DisposableEffect(mediaPlayer) {
        val listener = MediaPlayer.EventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    if (activeEngine == PlayerEngine.LIBVLC) isPlaying = true
                }
                MediaPlayer.Event.Paused -> {
                    if (activeEngine == PlayerEngine.LIBVLC) isPlaying = false
                }
                MediaPlayer.Event.Stopped -> {
                    if (activeEngine == PlayerEngine.LIBVLC) isPlaying = false
                }
                MediaPlayer.Event.EndReached -> {
                    if (activeEngine == PlayerEngine.LIBVLC) {
                        isPlaying = false
                        if (isLooping) {
                            scope.launch {
                                mediaPlayer.time = 0L
                                mediaPlayer.play()
                            }
                        }
                    }
                }
                MediaPlayer.Event.TimeChanged -> {
                    if (activeEngine == PlayerEngine.LIBVLC && !isSeeking && !isScrubbing) {
                        currentPositionMs = event.timeChanged
                    }
                }
                MediaPlayer.Event.LengthChanged -> {
                    if (activeEngine == PlayerEngine.LIBVLC && event.lengthChanged > 0L) {
                        totalDurationMs = event.lengthChanged
                    }
                }
                MediaPlayer.Event.EncounteredError -> {
                    if (activeEngine == PlayerEngine.LIBVLC) {
                        if (hwDecoderEnabled && retryAttempt == 0) {
                            hwDecoderEnabled = false
                            retryAttempt = 1
                            Toast.makeText(context, "Retrying with software decoding...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Switching to standard player engine...", Toast.LENGTH_SHORT).show()
                            activeEngine = PlayerEngine.EXOPLAYER
                        }
                    }
                }
            }
        }
        mediaPlayer.setEventListener(listener)

        onDispose {
            val pos = if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.time else exoPlayer.currentPosition
            val dur = if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.length else exoPlayer.duration
            scope.launch {
                mediaRepository.savePlaybackPosition(item.uri.toString(), pos, dur)
            }
            mediaPlayer.setEventListener(null)
            mediaPlayer.stop()
            mediaPlayer.detachViews()
            mediaPlayer.release()
            libVLC.release()
            activePfd?.close()
            activePfd = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ExoPlayer Listener & Lifecycle
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingExo: Boolean) {
                if (activeEngine == PlayerEngine.EXOPLAYER) {
                    isPlaying = isPlayingExo
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (activeEngine == PlayerEngine.EXOPLAYER) {
                    if (state == Player.STATE_READY) {
                        val dur = exoPlayer.duration
                        if (dur > 0L) totalDurationMs = dur
                    } else if (state == Player.STATE_ENDED) {
                        isPlaying = false
                        if (isLooping) {
                            exoPlayer.seekTo(0L)
                            exoPlayer.play()
                        }
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (activeEngine == PlayerEngine.EXOPLAYER) {
                    errorMessage = "Playback error: ${error.localizedMessage ?: "Codec unsupported"}"
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
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
            mediaPlayer.pause()
            exoPlayer.pause()
            sleepTimerMinutes = 0
            Toast.makeText(context, "Sleep timer finished. Playback paused.", Toast.LENGTH_SHORT).show()
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
            mediaPlayer.stop()
            exoPlayer.stop()
            onBackClick()
        }
    }

    val darkBackground = Color(0xFF000000)
    val overlayBackground = Color(0xD9000000)

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
            val gestureModifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked, activeEngine) {
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
                                val current = if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.time else exoPlayer.currentPosition
                                if (offset.x < width * 0.4f) {
                                    val target = (current - 10000L).coerceAtLeast(0L)
                                    if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.time = target else exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                    gestureOverlayIcon = Icons.Default.FastRewind
                                    gestureOverlayText = "-10s"
                                } else if (offset.x > width * 0.6f) {
                                    val target = (current + 10000L).coerceAtMost(totalDurationMs)
                                    if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.time = target else exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                    gestureOverlayIcon = Icons.Default.FastForward
                                    gestureOverlayText = "+10s"
                                } else {
                                    if (activeEngine == PlayerEngine.LIBVLC) {
                                        if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                                    } else {
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    }
                                }
                            }
                        }
                    )
                }
                .pointerInput(isScreenLocked, totalDurationMs, isAudioBoostEnabled, activeEngine) {
                    if (!isScreenLocked) {
                        detectDragGestures(
                            onDragStart = {
                                scrubAccumulatedX = 0f
                                scrubStartPositionMs = if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.time else exoPlayer.currentPosition
                                isScrubbing = false
                            },
                            onDragEnd = {
                                if (isScrubbing) {
                                    val target = (scrubStartPositionMs + (scrubAccumulatedX * 120L).toLong()).coerceIn(0L, totalDurationMs)
                                    if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.time = target else exoPlayer.seekTo(target)
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
                                        // Volume adjustment: LibVLC audio boost or system volume
                                        if (isAudioBoostEnabled && activeEngine == PlayerEngine.LIBVLC) {
                                            val currentVlcVol = mediaPlayer.volume
                                            val delta = if (dragAmount.y < -12f) 4 else if (dragAmount.y > 12f) -4 else 0
                                            if (delta != 0) {
                                                val newVol = (currentVlcVol + delta).coerceIn(0, 200)
                                                mediaPlayer.volume = newVol
                                                gestureOverlayIcon = Icons.AutoMirrored.Filled.VolumeUp
                                                gestureOverlayText = "$newVol% [VLC Boost]"
                                            }
                                        } else {
                                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                            val delta = if (dragAmount.y < -12f) 1 else if (dragAmount.y > 12f) -1 else 0
                                            if (delta != 0) {
                                                val newVol = (currentVol + delta).coerceIn(0, maxSystemVolume)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                gestureOverlayIcon = Icons.AutoMirrored.Filled.VolumeUp
                                                gestureOverlayText = "${((newVol.toFloat() / maxSystemVolume) * 100).toInt()}%"
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

            if (activeEngine == PlayerEngine.LIBVLC) {
                AndroidView(
                    factory = { ctx ->
                        VLCVideoLayout(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            mediaPlayer.attachViews(this, null, true, false)
                        }
                    },
                    modifier = gestureModifier
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false
                            player = exoPlayer
                        }
                    },
                    update = { view ->
                        view.resizeMode = when (currentAspectMode) {
                            VlcAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            VlcAspectMode.RATIO_16_9, VlcAspectMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            VlcAspectMode.CROP_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            VlcAspectMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = gestureModifier
                )
            }

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

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = AppSpacing.paddingSmall)
                        ) {
                            Text(
                                text = item.displayName,
                                style = typography.body.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFFEDEDED),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activeEngine == PlayerEngine.LIBVLC) "LibVLC Engine" else "ExoPlayer Engine",
                                style = typography.caption,
                                color = if (activeEngine == PlayerEngine.LIBVLC) Color(0xFFFF9800) else Color(0xFF8AB4F8)
                            )
                        }

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

                        // More Menu (Sleep Timer, Audio Boost, Engine Switch)
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
                                    text = {
                                        Text(if (activeEngine == PlayerEngine.LIBVLC) "Switch to ExoPlayer Engine" else "Switch to LibVLC Engine")
                                    },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        activeEngine = if (activeEngine == PlayerEngine.LIBVLC) PlayerEngine.EXOPLAYER else PlayerEngine.LIBVLC
                                        Toast.makeText(
                                            context,
                                            if (activeEngine == PlayerEngine.LIBVLC) "Switched to LibVLC Engine" else "Switched to ExoPlayer Engine",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sleep Timer (${if (sleepTimerMinutes > 0) "${sleepTimerMinutes}m" else "Off"})") },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        isSleepTimerDialogOpen = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isAudioBoostEnabled) "Audio Boost: On (Up to 200%)" else "Audio Boost: Off") },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        if (activeEngine != PlayerEngine.LIBVLC) {
                                            Toast.makeText(context, "Audio Boost requires LibVLC engine", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isAudioBoostEnabled = !isAudioBoostEnabled
                                            if (isAudioBoostEnabled) {
                                                mediaPlayer.volume = 150
                                                Toast.makeText(context, "LibVLC Audio Boost enabled (150%)", Toast.LENGTH_SHORT).show()
                                            } else {
                                                mediaPlayer.volume = 100
                                                Toast.makeText(context, "Standard Volume (100%)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Extract Audio (M4A)") },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        scope.launch {
                                            Toast.makeText(context, "Extracting audio track...", Toast.LENGTH_SHORT).show()
                                            val extractor = com.abhisheksati.mydoc.media.audio.AudioExtractor(context)
                                            val result = extractor.extractAudioFromVideo(item.uri, item.displayName.substringBeforeLast('.'))
                                            result.onSuccess { audioFile ->
                                                Toast.makeText(context, "Audio saved: Music/${audioFile.name}", Toast.LENGTH_LONG).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Audio extraction failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
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
                                if (activeEngine == PlayerEngine.LIBVLC) {
                                    mediaPlayer.time = seekPositionMs
                                } else {
                                    exoPlayer.seekTo(seekPositionMs)
                                }
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF9800),
                                activeTrackColor = Color(0xFFFF9800),
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
                            // VLC Aspect Ratio toggle (Fit, 16:9, 4:3, Crop, 1:1)
                            IconButton(
                                onClick = {
                                    aspectModeIndex = (aspectModeIndex + 1) % VlcAspectMode.entries.size
                                    val nextMode = VlcAspectMode.entries[aspectModeIndex]
                                    mediaPlayer.aspectRatio = nextMode.ratioStr
                                    mediaPlayer.scale = nextMode.scale
                                    Toast.makeText(context, nextMode.label, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = currentAspectMode.buttonText,
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
                                        color = if (subtitleUri != null) Color(0xFFFF9800) else Color(0xFFEDEDED)
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
                                                mediaPlayer.spuTrack = -1
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
                                        color = if (playbackSpeed != 1.0f) Color(0xFFFF9800) else Color(0xFFEDEDED)
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
                                                if (activeEngine == PlayerEngine.LIBVLC) {
                                                    mediaPlayer.rate = spd
                                                } else {
                                                    exoPlayer.setPlaybackSpeed(spd)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // 10s Rewind
                            IconButton(
                                onClick = {
                                    val current = if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.time else exoPlayer.currentPosition
                                    val target = (current - 10000L).coerceAtLeast(0L)
                                    if (activeEngine == PlayerEngine.LIBVLC) {
                                        mediaPlayer.time = target
                                    } else {
                                        exoPlayer.seekTo(target)
                                    }
                                    currentPositionMs = target
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
                                    if (activeEngine == PlayerEngine.LIBVLC) {
                                        if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                                    } else {
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    }
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
                                    val current = if (activeEngine == PlayerEngine.LIBVLC) mediaPlayer.time else exoPlayer.currentPosition
                                    val target = (current + 10000L).coerceAtMost(totalDurationMs)
                                    if (activeEngine == PlayerEngine.LIBVLC) {
                                        mediaPlayer.time = target
                                    } else {
                                        exoPlayer.seekTo(target)
                                    }
                                    currentPositionMs = target
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
                                    if (activeEngine == PlayerEngine.EXOPLAYER) {
                                        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Loop",
                                    tint = if (isLooping) Color(0xFFFF9800) else Color(0xFFEDEDED)
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
                            tint = Color(0xFFFF9800),
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
