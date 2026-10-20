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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
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
    RATIO_21_9("21:9 cinema", "21:9", "21:9", 0f),
    CROP_FILL("Crop to fill", "CROP", null, 1.35f),
    ORIGINAL("Original 100%", "1:1", null, 1.0f)
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
                addOption(":file-caching=2000")
                addOption(":network-caching=2000")
                addOption(":clock-jitter=0")
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
                            addOption(":file-caching=2000")
                            addOption(":network-caching=2000")
                            addOption(":clock-jitter=0")
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
                    addOption(":file-caching=2000")
                    addOption(":network-caching=2000")
                    addOption(":clock-jitter=0")
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
                    addOption(":file-caching=2000")
                    addOption(":network-caching=2000")
                    addOption(":clock-jitter=0")
                }
                return VlcMediaResult(media, afd.parcelFileDescriptor)
            }
        } catch (_: Exception) {}
    }

    // 5. Fallback to standard Uri
    val media = Media(libVLC, uri).apply {
        setHWDecoderEnabled(enableHwDecoder, false)
        addOption(":file-caching=2000")
        addOption(":network-caching=2000")
        addOption(":clock-jitter=0")
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
    val app = context.applicationContext as? com.abhisheksati.mydoc.MyDocApp
    var isLooping by remember { mutableStateOf(app?.settingsRepository?.videoAutoLoop?.value ?: false) }
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

    var pendingResumePosition by remember { mutableLongStateOf(-1L) }
    var hasResumedPosition by remember { mutableStateOf(false) }

    var activePfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var activeVlcMedia by remember { mutableStateOf<Media?>(null) }
    var vlcLayoutRef by remember { mutableStateOf<android.view.View?>(null) }
    var hwDecoderEnabled by remember { mutableStateOf(app?.settingsRepository?.videoHardwareAcceleration?.value ?: true) }
    var retryAttempt by remember { mutableIntStateOf(0) }

    // Full VLC Feature States
    var isAudioTrackDialogOpen by remember { mutableStateOf(false) }
    var isSubtitleTrackDialogOpen by remember { mutableStateOf(false) }
    var isSyncDelayDialogOpen by remember { mutableStateOf(false) }
    var isEqualizerDialogOpen by remember { mutableStateOf(false) }

    var audioDelayMs by remember { mutableLongStateOf(0L) }
    var spuDelayMs by remember { mutableLongStateOf(0L) }

    var isEqualizerEnabled by remember { mutableStateOf(false) }
    var selectedEqPresetIndex by remember { mutableIntStateOf(0) }
    var eqPreAmp by remember { mutableFloatStateOf(0f) }

    var abRepeatA by remember { mutableStateOf<Long?>(null) }
    var abRepeatB by remember { mutableStateOf<Long?>(null) }

    // Keep screen awake throughout entire video playback session (LIF-03)
    DisposableEffect(activity) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Initialize LibVLC native engine with optimized performance flags
    val libVLC = remember {
        val options = arrayListOf(
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--audio-time-stretch",
            "--file-caching=2000",
            "--network-caching=2000",
            "--sout-mux-caching=2000"
        )
        LibVLC(context, options)
    }

    // Initialize VLC MediaPlayer
    val mediaPlayer = remember {
        MediaPlayer(libVLC)
    }

    // Load media item into LibVLC, restore saved position, and configure hardware decoding
    LaunchedEffect(item.uri, hwDecoderEnabled, retryAttempt) {
        try {
            val savedPos = mediaRepository.getPlaybackPosition(item.uri.toString())
            if (savedPos > 2000L) {
                pendingResumePosition = savedPos
            }
            activeVlcMedia?.release()
            activeVlcMedia = null
            activePfd?.close()
            activePfd = null

            val result = createVlcMedia(context, libVLC, item.uri, enableHwDecoder = hwDecoderEnabled)
            activePfd = result.pfd
            activeVlcMedia = result.media
            mediaPlayer.media = result.media

            if (subtitleUri != null) {
                try {
                    mediaPlayer.addSlave(0 /* Subtitle */, subtitleUri!!, true)
                } catch (_: Exception) {}
            }

            mediaPlayer.play()
            isPlaying = true
        } catch (e: Exception) {
            android.util.Log.e("VideoPlayerScreen", "LibVLC open failed", e)
            errorMessage = "Playback failed: ${e.message}"
        }
    }

    // Dynamic subtitle update
    LaunchedEffect(subtitleUri) {
        val uri = subtitleUri
        if (uri != null) {
            try {
                mediaPlayer.addSlave(0 /* Subtitle */, uri, true)
            } catch (_: Exception) {}
        }
    }

    // Periodic time auto-save while playing (every 3 seconds)
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(3000)
            val pos = mediaPlayer.time
            val dur = mediaPlayer.length
            if (pos > 1000L && dur > 0L) {
                mediaRepository.savePlaybackPosition(item.uri.toString(), pos, dur)
            }
        }
    }

    // LibVLC Event Listener & Lifecycle management
    DisposableEffect(mediaPlayer) {
        val listener = MediaPlayer.EventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    isPlaying = true
                    val targetPos = pendingResumePosition
                    if (!hasResumedPosition && targetPos > 0L) {
                        mediaPlayer.time = targetPos
                        currentPositionMs = targetPos
                        hasResumedPosition = true
                        pendingResumePosition = -1L
                        Toast.makeText(context, "Resumed from ${formatTime(targetPos)}", Toast.LENGTH_SHORT).show()
                    }
                }
                MediaPlayer.Event.Paused -> {
                    isPlaying = false
                }
                MediaPlayer.Event.Stopped -> {
                    isPlaying = false
                }
                MediaPlayer.Event.EndReached -> {
                    isPlaying = false
                    if (isLooping) {
                        scope.launch {
                            mediaPlayer.time = 0L
                            mediaPlayer.play()
                        }
                    }
                }
                MediaPlayer.Event.TimeChanged -> {
                    val targetPos = pendingResumePosition
                    if (!hasResumedPosition && targetPos > 0L && event.timeChanged < targetPos) {
                        mediaPlayer.time = targetPos
                        currentPositionMs = targetPos
                        hasResumedPosition = true
                        pendingResumePosition = -1L
                        Toast.makeText(context, "Resumed from ${formatTime(targetPos)}", Toast.LENGTH_SHORT).show()
                    } else if (!isSeeking && !isScrubbing) {
                        currentPositionMs = event.timeChanged
                        val b = abRepeatB
                        val a = abRepeatA
                        if (a != null && b != null && event.timeChanged >= b) {
                            mediaPlayer.time = a
                        }
                    }
                }
                MediaPlayer.Event.LengthChanged -> {
                    if (event.lengthChanged > 0L) {
                        totalDurationMs = event.lengthChanged
                        val targetPos = pendingResumePosition
                        if (!hasResumedPosition && targetPos > 0L && targetPos < event.lengthChanged) {
                            mediaPlayer.time = targetPos
                            currentPositionMs = targetPos
                            hasResumedPosition = true
                            pendingResumePosition = -1L
                            Toast.makeText(context, "Resumed from ${formatTime(targetPos)}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                MediaPlayer.Event.EncounteredError -> {
                    if (hwDecoderEnabled && retryAttempt == 0) {
                        hwDecoderEnabled = false
                        retryAttempt = 1
                        Toast.makeText(context, "Retrying with software decoding...", Toast.LENGTH_SHORT).show()
                    } else {
                        errorMessage = "VLC encountered an error playing this media."
                    }
                }
            }
        }
        mediaPlayer.setEventListener(listener)

        onDispose {
            val pos = mediaPlayer.time
            val dur = mediaPlayer.length
            val app = context.applicationContext as? com.abhisheksati.mydoc.MyDocApp
            val saveScope = app?.applicationScope ?: scope
            saveScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val saveTime = if (dur > 0L && pos >= dur - 3000L) 0L else pos
                    mediaRepository.savePlaybackPosition(item.uri.toString(), saveTime, dur)
                } catch (_: Exception) {}
            }
            mediaPlayer.setEventListener(null)
            mediaPlayer.stop()
            mediaPlayer.detachViews()
            mediaPlayer.media = null
            activeVlcMedia?.release()
            activeVlcMedia = null
            activePfd?.close()
            activePfd = null
            mediaPlayer.release()
            libVLC.release()
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Audio Focus & Noisy (headphone disconnect) management
    DisposableEffect(Unit) {
        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    }
                }
            }
        }

        val noisyReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                        Toast.makeText(context, "Headphones disconnected - playback paused", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val intentFilter = android.content.IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(noisyReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(noisyReceiver, intentFilter)
        }

        val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build().also {
                    audioManager.requestAudioFocus(it)
                }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            null
        }

        onDispose {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
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
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
                                val current = mediaPlayer.time
                                if (offset.x < width * 0.4f) {
                                    val target = (current - 10000L).coerceAtLeast(0L)
                                    mediaPlayer.time = target
                                    currentPositionMs = target
                                    gestureOverlayIcon = Icons.Default.FastRewind
                                    gestureOverlayText = "-10s"
                                } else if (offset.x > width * 0.6f) {
                                    val target = (current + 10000L).coerceAtMost(totalDurationMs)
                                    mediaPlayer.time = target
                                    currentPositionMs = target
                                    gestureOverlayIcon = Icons.Default.FastForward
                                    gestureOverlayText = "+10s"
                                } else {
                                    if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                                }
                            }
                        }
                    )
                }
                .pointerInput(isScreenLocked, totalDurationMs, isAudioBoostEnabled) {
                    if (!isScreenLocked) {
                        detectDragGestures(
                            onDragStart = {
                                scrubAccumulatedX = 0f
                                scrubStartPositionMs = mediaPlayer.time
                                isScrubbing = false
                            },
                            onDragEnd = {
                                if (isScrubbing) {
                                    val target = (scrubStartPositionMs + (scrubAccumulatedX * 120L).toLong()).coerceIn(0L, totalDurationMs)
                                    mediaPlayer.time = target
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
                                        if (isAudioBoostEnabled) {
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

            AndroidView(
                factory = { ctx ->
                    VLCVideoLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        keepScreenOn = true
                        mediaPlayer.attachViews(this, null, true, false)
                        vlcLayoutRef = this
                    }
                },
                update = { view ->
                    view.keepScreenOn = isPlaying
                    vlcLayoutRef = view
                },
                modifier = gestureModifier
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
                                text = "LibVLC Engine",
                                style = typography.caption,
                                color = Color(0xFFFF9800)
                            )
                        }

                        // Audio Tracks (VLC)
                        IconButton(onClick = { isAudioTrackDialogOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.Audiotrack,
                                contentDescription = "Audio Tracks",
                                tint = Color(0xFFEDEDED)
                            )
                        }

                        // Subtitle Track (VLC)
                        IconButton(onClick = { isSubtitleTrackDialogOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (subtitleUri != null || mediaPlayer.spuTrack != -1) Color(0xFFFF9800) else Color(0xFFEDEDED)
                            )
                        }

                        // Delay Sync (VLC)
                        IconButton(onClick = { isSyncDelayDialogOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "Audio/Subtitle Sync",
                                tint = if (audioDelayMs != 0L || spuDelayMs != 0L) Color(0xFFFF9800) else Color(0xFFEDEDED)
                            )
                        }

                        // Equalizer (VLC)
                        IconButton(onClick = { isEqualizerDialogOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = "VLC Equalizer",
                                tint = if (isEqualizerEnabled) Color(0xFFFF9800) else Color(0xFFEDEDED)
                            )
                        }

                        // Video Snapshot
                        IconButton(onClick = {
                            takeVideoSnapshot(context, vlcLayoutRef, item.displayName)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Take Snapshot",
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

                        // More Menu (PiP, Orientation, Sleep Timer, Audio Boost, Extract Audio, Share)
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    DropdownMenuItem(
                                        text = { Text("Picture-in-picture (PiP)") },
                                        leadingIcon = {
                                            Icon(Icons.Default.PictureInPicture, contentDescription = null)
                                        },
                                        onClick = {
                                            isMoreMenuOpen = false
                                            val params = PictureInPictureParams.Builder()
                                                .setAspectRatio(Rational(16, 9))
                                                .build()
                                            activity?.enterPictureInPictureMode(params)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (orientationMode) {
                                                1 -> "Orientation: Landscape"
                                                2 -> "Orientation: Portrait"
                                                else -> "Orientation: Auto-rotate"
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ScreenRotation, contentDescription = null)
                                    },
                                    onClick = {
                                        isMoreMenuOpen = false
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
                                        isAudioBoostEnabled = !isAudioBoostEnabled
                                        if (isAudioBoostEnabled) {
                                            mediaPlayer.volume = 150
                                            Toast.makeText(context, "LibVLC Audio Boost enabled (150%)", Toast.LENGTH_SHORT).show()
                                        } else {
                                            mediaPlayer.volume = 100
                                            Toast.makeText(context, "Standard Volume (100%)", Toast.LENGTH_SHORT).show()
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
                                DropdownMenuItem(
                                    text = { Text("Share Video") },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = item.mimeType
                                            putExtra(Intent.EXTRA_STREAM, item.uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Center Transport Controls (Rewind 10s, Large Play/Pause, Forward 10s)
            AnimatedVisibility(
                visible = areControlsVisible && !isScreenLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Rewind 10s
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0x66000000),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                val target = (mediaPlayer.time - 10000L).coerceAtLeast(0L)
                                mediaPlayer.time = target
                                currentPositionMs = target
                                gestureOverlayIcon = Icons.Default.FastRewind
                                gestureOverlayText = "-10s"
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Play / Pause (Large 68dp button)
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0xCC000000),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF9800)),
                        modifier = Modifier
                            .size(68.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Forward 10s
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0x66000000),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                val target = (mediaPlayer.time + 10000L).coerceAtMost(totalDurationMs)
                                mediaPlayer.time = target
                                currentPositionMs = target
                                gestureOverlayIcon = Icons.Default.FastForward
                                gestureOverlayText = "+10s"
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Controls Bar (Timeline, Time Labels, Bottom Navigation Actions)
            AnimatedVisibility(
                visible = areControlsVisible && !isScreenLocked,
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
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        // Time labels & Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(if (isSeeking) seekPositionMs else currentPositionMs),
                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFEDEDED)
                            )
                            Text(
                                text = formatTime(totalDurationMs),
                                style = typography.caption,
                                color = Color(0xFFA0A0A5)
                            )
                        }

                        // Seek Slider
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
                                mediaPlayer.time = seekPositionMs
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF9800),
                                activeTrackColor = Color(0xFFFF9800),
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bottom Actions row (Spacious, elegant, uncluttered)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Aspect Ratio
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = Color(0x22FFFFFF),
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    .clickable {
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
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            // Subtitle (CC) Button
                            IconButton(onClick = { isSubtitleTrackDialogOpen = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Subtitles,
                                    contentDescription = "Subtitles",
                                    tint = if (subtitleUri != null || mediaPlayer.spuTrack != -1) Color(0xFFFF9800) else Color(0xFFEDEDED)
                                )
                            }

                            // Speed Selector button
                            Box {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                    color = if (playbackSpeed != 1.0f) Color(0x33FF9800) else Color(0x22FFFFFF),
                                    modifier = Modifier
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                        .clickable { isSpeedMenuOpen = true }
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                        color = if (playbackSpeed != 1.0f) Color(0xFFFF9800) else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isSpeedMenuOpen,
                                    onDismissRequest = { isSpeedMenuOpen = false }
                                ) {
                                    listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f).forEach { spd ->
                                        DropdownMenuItem(
                                            text = { Text("${spd}x") },
                                            onClick = {
                                                isSpeedMenuOpen = false
                                                playbackSpeed = spd
                                                mediaPlayer.rate = spd
                                            }
                                        )
                                    }
                                }
                            }

                            // A-B Repeat Loop button
                            IconButton(
                                onClick = {
                                    if (abRepeatA == null) {
                                        abRepeatA = currentPositionMs
                                        Toast.makeText(context, "Point A set at ${formatTime(currentPositionMs)}", Toast.LENGTH_SHORT).show()
                                    } else if (abRepeatB == null) {
                                        if (currentPositionMs > abRepeatA!!) {
                                            abRepeatB = currentPositionMs
                                            Toast.makeText(context, "Point B set at ${formatTime(currentPositionMs)}. Looping A-B", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Point B must be after Point A", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        abRepeatA = null
                                        abRepeatB = null
                                        Toast.makeText(context, "A-B Repeat cleared", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(
                                    text = when {
                                        abRepeatA != null && abRepeatB != null -> "A-B"
                                        abRepeatA != null -> "A"
                                        else -> "A-B"
                                    },
                                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        abRepeatA != null && abRepeatB != null -> Color(0xFFFF9800)
                                        abRepeatA != null -> Color(0xFF8AB4F8)
                                        else -> Color(0xFF888888)
                                    }
                                )
                            }

                            // Repeat / Loop Single Video
                            IconButton(onClick = { isLooping = !isLooping }) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Loop",
                                    tint = if (isLooping) Color(0xFFFF9800) else Color(0xFFEDEDED)
                                )
                            }

                            // Screen Orientation Toggle
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
                                    contentDescription = "Rotate Screen",
                                    tint = if (orientationMode != 0) Color(0xFFFF9800) else Color(0xFFEDEDED)
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

        // VLC Audio Tracks Dialog
        if (isAudioTrackDialogOpen) {
            val audioTracks = remember(isAudioTrackDialogOpen) {
                mediaPlayer.audioTracks ?: emptyArray()
            }
            var selectedTrackId by remember(isAudioTrackDialogOpen) {
                mutableIntStateOf(mediaPlayer.audioTrack)
            }

            AlertDialog(
                onDismissRequest = { isAudioTrackDialogOpen = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audio Tracks", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = "No alternate audio tracks found in this video.",
                            style = typography.body
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(audioTracks.toList()) { track ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTrackId = track.id
                                            mediaPlayer.setAudioTrack(track.id)
                                            Toast.makeText(context, "Audio: ${track.name}", Toast.LENGTH_SHORT).show()
                                            isAudioTrackDialogOpen = false
                                        }
                                        .padding(vertical = 10.dp)
                                ) {
                                    RadioButton(
                                        selected = track.id == selectedTrackId,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (track.id == -1) "Disable Audio" else track.name,
                                        style = typography.body,
                                        fontWeight = if (track.id == selectedTrackId) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { isAudioTrackDialogOpen = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // VLC Subtitle Tracks Dialog
        if (isSubtitleTrackDialogOpen) {
            val spuTracks = remember(isSubtitleTrackDialogOpen) {
                mediaPlayer.spuTracks ?: emptyArray()
            }
            var selectedSpuId by remember(isSubtitleTrackDialogOpen) {
                mutableIntStateOf(mediaPlayer.spuTrack)
            }

            AlertDialog(
                onDismissRequest = { isSubtitleTrackDialogOpen = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Subtitles, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subtitles", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Option to load external subtitle file
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSubtitleTrackDialogOpen = false
                                    subtitleLauncher.launch("*/*")
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Load External Subtitle (.srt / .vtt / .ass)",
                                style = typography.body.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            // Turn Off Subtitles option
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSpuId = -1
                                            subtitleUri = null
                                            mediaPlayer.spuTrack = -1
                                            Toast.makeText(context, "Subtitles turned off", Toast.LENGTH_SHORT).show()
                                            isSubtitleTrackDialogOpen = false
                                        }
                                        .padding(vertical = 10.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedSpuId == -1 && subtitleUri == null,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Disable Subtitles", style = typography.body)
                                }
                            }

                            items(spuTracks.filter { it.id != -1 }) { track ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSpuId = track.id
                                            mediaPlayer.setSpuTrack(track.id)
                                            Toast.makeText(context, "Subtitle: ${track.name}", Toast.LENGTH_SHORT).show()
                                            isSubtitleTrackDialogOpen = false
                                        }
                                        .padding(vertical = 10.dp)
                                ) {
                                    RadioButton(
                                        selected = track.id == selectedSpuId,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = track.name,
                                        style = typography.body,
                                        fontWeight = if (track.id == selectedSpuId) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { isSubtitleTrackDialogOpen = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // VLC Audio & Subtitle Sync Delay Dialog
        if (isSyncDelayDialogOpen) {
            AlertDialog(
                onDismissRequest = { isSyncDelayDialogOpen = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Sync, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VLC Delay Sync", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Audio Delay section
                        Text("Audio Delay", fontWeight = FontWeight.SemiBold, style = typography.body)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                audioDelayMs = (audioDelayMs - 50L).coerceAtLeast(-5000L)
                                mediaPlayer.setAudioDelay(audioDelayMs * 1000L)
                            }) {
                                Text("-50 ms")
                            }
                            Text(
                                text = "${if (audioDelayMs > 0) "+$audioDelayMs" else "$audioDelayMs"} ms",
                                style = typography.body.copy(fontWeight = FontWeight.Bold),
                                color = if (audioDelayMs != 0L) Color(0xFFFF9800) else Color.White
                            )
                            TextButton(onClick = {
                                audioDelayMs = (audioDelayMs + 50L).coerceAtMost(5000L)
                                mediaPlayer.setAudioDelay(audioDelayMs * 1000L)
                            }) {
                                Text("+50 ms")
                            }
                        }
                        Slider(
                            value = audioDelayMs.toFloat(),
                            onValueChange = {
                                audioDelayMs = it.toLong()
                                mediaPlayer.setAudioDelay(audioDelayMs * 1000L)
                            },
                            valueRange = -5000f..5000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF9800),
                                activeTrackColor = Color(0xFFFF9800)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Subtitle Delay section
                        Text("Subtitle Delay", fontWeight = FontWeight.SemiBold, style = typography.body)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                spuDelayMs = (spuDelayMs - 50L).coerceAtLeast(-5000L)
                                mediaPlayer.setSpuDelay(spuDelayMs * 1000L)
                            }) {
                                Text("-50 ms")
                            }
                            Text(
                                text = "${if (spuDelayMs > 0) "+$spuDelayMs" else "$spuDelayMs"} ms",
                                style = typography.body.copy(fontWeight = FontWeight.Bold),
                                color = if (spuDelayMs != 0L) Color(0xFFFF9800) else Color.White
                            )
                            TextButton(onClick = {
                                spuDelayMs = (spuDelayMs + 50L).coerceAtMost(5000L)
                                mediaPlayer.setSpuDelay(spuDelayMs * 1000L)
                            }) {
                                Text("+50 ms")
                            }
                        }
                        Slider(
                            value = spuDelayMs.toFloat(),
                            onValueChange = {
                                spuDelayMs = it.toLong()
                                mediaPlayer.setSpuDelay(spuDelayMs * 1000L)
                            },
                            valueRange = -5000f..5000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF9800),
                                activeTrackColor = Color(0xFFFF9800)
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        audioDelayMs = 0L
                        spuDelayMs = 0L
                        mediaPlayer.setAudioDelay(0L)
                        mediaPlayer.setSpuDelay(0L)
                        Toast.makeText(context, "Sync delays reset to 0", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Reset All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isSyncDelayDialogOpen = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // VLC Equalizer Dialog (All 18 Presets + Pre-amp)
        if (isEqualizerDialogOpen) {
            val presetCount = remember { MediaPlayer.Equalizer.getPresetCount() }
            val presets = remember {
                (0 until presetCount).map { i -> MediaPlayer.Equalizer.getPresetName(i) }
            }

            AlertDialog(
                onDismissRequest = { isEqualizerDialogOpen = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VLC Equalizer", fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = isEqualizerEnabled,
                            onCheckedChange = { enabled ->
                                isEqualizerEnabled = enabled
                                if (enabled) {
                                    val eq = MediaPlayer.Equalizer.createFromPreset(selectedEqPresetIndex).apply {
                                        setPreAmp(eqPreAmp)
                                    }
                                    mediaPlayer.setEqualizer(eq)
                                    Toast.makeText(context, "Equalizer: ${presets.getOrNull(selectedEqPresetIndex) ?: "Flat"}", Toast.LENGTH_SHORT).show()
                                } else {
                                    mediaPlayer.setEqualizer(null)
                                    Toast.makeText(context, "Equalizer disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFF9800),
                                checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.4f)
                            )
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Pre-amp slider
                        Text(
                            text = "Pre-Amplification: ${String.format(Locale.US, "%.1f", eqPreAmp)} dB",
                            style = typography.caption,
                            color = if (isEqualizerEnabled) Color.White else Color.Gray
                        )
                        Slider(
                            value = eqPreAmp,
                            onValueChange = {
                                eqPreAmp = it
                                if (isEqualizerEnabled) {
                                    val eq = MediaPlayer.Equalizer.createFromPreset(selectedEqPresetIndex).apply {
                                        setPreAmp(it)
                                    }
                                    mediaPlayer.setEqualizer(eq)
                                }
                            },
                            valueRange = -20f..20f,
                            enabled = isEqualizerEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF9800),
                                activeTrackColor = Color(0xFFFF9800)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Audio Presets",
                            fontWeight = FontWeight.SemiBold,
                            style = typography.body,
                            color = if (isEqualizerEnabled) Color.White else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.height(240.dp)) {
                            items(presets.indices.toList()) { index ->
                                val name = presets[index]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isEqualizerEnabled) {
                                            selectedEqPresetIndex = index
                                            val eq = MediaPlayer.Equalizer.createFromPreset(index).apply {
                                                setPreAmp(eqPreAmp)
                                            }
                                            mediaPlayer.setEqualizer(eq)
                                            Toast.makeText(context, "Preset: $name", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedEqPresetIndex == index,
                                        onClick = null,
                                        enabled = isEqualizerEnabled
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = name,
                                        style = typography.body,
                                        fontWeight = if (selectedEqPresetIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isEqualizerEnabled) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { isEqualizerDialogOpen = false }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

private fun takeVideoSnapshot(
    context: Context,
    view: android.view.View?,
    title: String
) {
    if (view == null) {
        Toast.makeText(context, "Player view not ready", Toast.LENGTH_SHORT).show()
        return
    }

    var surfaceView: SurfaceView? = null
    var textureView: TextureView? = null

    fun findVideoView(v: android.view.View) {
        if (v is SurfaceView) {
            surfaceView = v
        } else if (v is TextureView) {
            textureView = v
        } else if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                findVideoView(v.getChildAt(i))
            }
        }
    }

    findVideoView(view)

    val currentTexture = textureView
    val currentSurface = surfaceView

    if (currentTexture != null) {
        val bmp = currentTexture.bitmap
        if (bmp != null) {
            saveBitmapToGallery(context, bmp, title)
            return
        }
    }

    if (currentSurface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val width = currentSurface.width
        val height = currentSurface.height
        if (width > 0 && height > 0) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                PixelCopy.request(
                    currentSurface,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            saveBitmapToGallery(context, bitmap, title)
                        } else {
                            (context as? Activity)?.runOnUiThread {
                                Toast.makeText(context, "Could not capture frame", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
                return
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerScreen", "PixelCopy failed", e)
            }
        }
    }

    try {
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        saveBitmapToGallery(context, bitmap, title)
    } catch (e: Exception) {
        Toast.makeText(context, "Snapshot failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, videoTitle: String) {
    val cleanName = videoTitle.substringBeforeLast('.').replace("[^a-zA-Z0-9]".toRegex(), "_")
    val filename = "VLC_Snapshot_${cleanName}_${System.currentTimeMillis()}.jpg"
    val contentValues = android.content.ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyDoc")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Snapshot saved: Pictures/MyDoc/$filename", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Failed to save snapshot: ${e.message}", Toast.LENGTH_SHORT).show()
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
