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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppSlider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val accentColor = colors.accent
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
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

    // Unified Settings Bottom Sheet state
    var isSettingsSheetOpen by remember { mutableStateOf(false) }
    var settingsSheetTab by remember { mutableIntStateOf(0) }

    // VLC-grade Screen Lock & Sleep Timer states
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockButton by remember { mutableStateOf(false) }
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

            // Top Toolbar (Sleek, Minimalist, YouTube/Google Photos style)
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

                        // Subtitle Quick Indicator / Switch
                        val hasSubtitles = subtitleUri != null || (mediaPlayer.spuTrack != -1 && mediaPlayer.spuTrack != 0)
                        IconButton(onClick = {
                            settingsSheetTab = 0
                            isSettingsSheetOpen = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (hasSubtitles) accentColor else Color(0xFFEDEDED)
                            )
                        }

                        // Screen Lock Button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

                        // Player Settings Gear (Unified controls)
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isSettingsSheetOpen = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Player Settings",
                                tint = Color(0xFFEDEDED)
                            )
                        }
                    }
                }
            }

            // Center Transport Controls (Translucent glass -10s, 64dp Play/Pause, +10s)
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
                            .size(48.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Play / Pause (Large 64dp frosted glass button)
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0x99000000),
                        border = BorderStroke(1.5.dp, accentColor),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Forward 10s
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0x66000000),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Controls Bar (YouTube-style full-width seekbar + concise action dock)
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
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        // Full width Seek Slider
                        AppSlider(
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
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            thumbSize = 13.dp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bottom Actions row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Current time / Total time (tabular figures for zero-jitter seeking)
                            Text(
                                text = "${formatTime(if (isSeeking) seekPositionMs else currentPositionMs)} / ${formatTime(totalDurationMs)}",
                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                                color = Color(0xFFEDEDED)
                            )

                            // Right: Quick tools (Aspect pill, Speed pill, Snapshot, Rotate)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Aspect Ratio Pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x22FFFFFF),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                // Speed Pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (playbackSpeed != 1.0f) accentColor.copy(alpha = 0.25f) else Color(0x22FFFFFF),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.5f, 0.75f)
                                            val nextIndex = (speeds.indexOf(playbackSpeed) + 1).let { if (it in speeds.indices) it else 0 }
                                            playbackSpeed = speeds[nextIndex]
                                            mediaPlayer.rate = playbackSpeed
                                            Toast.makeText(context, "${playbackSpeed}x Speed", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = typography.caption.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                                        color = if (playbackSpeed != 1.0f) accentColor else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                // Snapshot Button
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        takeVideoSnapshot(context, vlcLayoutRef, item.displayName)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Take Snapshot",
                                        tint = Color(0xFFEDEDED),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Screen Rotation Toggle
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "Rotate Screen",
                                        tint = if (orientationMode != 0) accentColor else Color(0xFFEDEDED),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x55FFFFFF)),
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Screen Locked â€¢ Tap to unlock",
                            color = Color.White,
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Unified Video Player Settings Bottom Sheet (Modern, Google Photos / YouTube style)
        if (isSettingsSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isSettingsSheetOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = Color(0xFF141720),
                tonalElevation = 6.dp,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .background(Color(0x44FFFFFF), RoundedCornerShape(2.dp))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                ) {
                    // Sheet Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Player Settings",
                            style = typography.title.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        IconButton(onClick = { isSettingsSheetOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9EA3AE))
                        }
                    }

                    // Segmented Navigation Pill Tabs
                    val tabs = listOf(
                        "Audio & CC" to Icons.Default.Audiotrack,
                        "Sound & EQ" to Icons.Default.GraphicEq,
                        "Playback & Tools" to Icons.Default.Tune
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(Color(0xFF1E222D), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabs.forEachIndexed { index, (label, icon) ->
                            val selected = settingsSheetTab == index
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        settingsSheetTab = index
                                    },
                                color = if (selected) accentColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (selected) Color.White else Color(0xFF9EA3AE),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = typography.caption.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                                        color = if (selected) Color.White else Color(0xFF9EA3AE),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tab Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        when (settingsSheetTab) {
                            0 -> {
                                // Tab 0: Audio & Subtitles
                                Text(
                                    text = "Audio Track",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val audioTracks = mediaPlayer.audioTracks ?: emptyArray()
                                var currentAudioTrack by remember(isSettingsSheetOpen) { mutableIntStateOf(mediaPlayer.audioTrack) }
                                if (audioTracks.isEmpty()) {
                                    Text("No alternate audio tracks found", style = typography.caption, color = Color(0xFF9EA3AE))
                                } else {
                                    audioTracks.forEach { track ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    currentAudioTrack = track.id
                                                    mediaPlayer.setAudioTrack(track.id)
                                                    Toast.makeText(context, "Audio: ${track.name}", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 6.dp, horizontal = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = track.id == currentAudioTrack,
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color(0xFF6B7280))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (track.id == -1) "Disable Audio" else track.name,
                                                style = typography.body,
                                                color = if (track.id == currentAudioTrack) Color.White else Color(0xFFD1D5DB),
                                                fontWeight = if (track.id == currentAudioTrack) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Subtitles
                                Text(
                                    text = "Subtitles",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E222D),
                                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            isSettingsSheetOpen = false
                                            subtitleLauncher.launch("*/*")
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (subtitleUri != null) "Loaded: ${subtitleUri?.lastPathSegment}" else "Load External Subtitle (.srt / .vtt)",
                                            style = typography.body.copy(fontWeight = FontWeight.Medium),
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val spuTracks = mediaPlayer.spuTracks ?: emptyArray()
                                var currentSpuTrack by remember(isSettingsSheetOpen) { mutableIntStateOf(mediaPlayer.spuTrack) }

                                // Disable Subtitles
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            currentSpuTrack = -1
                                            subtitleUri = null
                                            mediaPlayer.spuTrack = -1
                                            Toast.makeText(context, "Subtitles disabled", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = currentSpuTrack == -1 && subtitleUri == null,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color(0xFF6B7280))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Disable Subtitles", style = typography.body, color = Color(0xFFD1D5DB))
                                }

                                // Embedded Subtitle Tracks
                                spuTracks.filter { it.id != -1 }.forEach { track ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                currentSpuTrack = track.id
                                                mediaPlayer.setSpuTrack(track.id)
                                                Toast.makeText(context, "Subtitle: ${track.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = track.id == currentSpuTrack,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color(0xFF6B7280))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = track.name,
                                            style = typography.body,
                                            color = if (track.id == currentSpuTrack) Color.White else Color(0xFFD1D5DB),
                                            fontWeight = if (track.id == currentSpuTrack) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Delay Synchronization
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Delay Synchronization",
                                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                    if (audioDelayMs != 0L || spuDelayMs != 0L) {
                                        TextButton(onClick = {
                                            audioDelayMs = 0L
                                            spuDelayMs = 0L
                                            mediaPlayer.setAudioDelay(0L)
                                            mediaPlayer.setSpuDelay(0L)
                                            Toast.makeText(context, "Sync delays reset", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Text("Reset", color = accentColor, style = typography.caption.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Audio Delay Slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Audio Delay", style = typography.caption, color = Color(0xFF9EA3AE))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = {
                                            audioDelayMs = (audioDelayMs - 50L).coerceAtLeast(-5000L)
                                            mediaPlayer.setAudioDelay(audioDelayMs * 1000L)
                                        }) { Text("-50ms", color = Color(0xFFD1D5DB)) }
                                        Text(
                                            text = "${if (audioDelayMs > 0) "+$audioDelayMs" else "$audioDelayMs"} ms",
                                            style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                            color = if (audioDelayMs != 0L) accentColor else Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        TextButton(onClick = {
                                            audioDelayMs = (audioDelayMs + 50L).coerceAtMost(5000L)
                                            mediaPlayer.setAudioDelay(audioDelayMs * 1000L)
                                        }) { Text("+50ms", color = Color(0xFFD1D5DB)) }
                                    }
                                }
                                AppSlider(
                                    value = audioDelayMs.toFloat(),
                                    onValueChange = {
                                        audioDelayMs = it.toLong()
                                        mediaPlayer.setAudioDelay(audioDelayMs * 1000L)
                                    },
                                    valueRange = -5000f..5000f,
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Subtitle Delay Slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Subtitle Delay", style = typography.caption, color = Color(0xFF9EA3AE))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = {
                                            spuDelayMs = (spuDelayMs - 50L).coerceAtLeast(-5000L)
                                            mediaPlayer.setSpuDelay(spuDelayMs * 1000L)
                                        }) { Text("-50ms", color = Color(0xFFD1D5DB)) }
                                        Text(
                                            text = "${if (spuDelayMs > 0) "+$spuDelayMs" else "$spuDelayMs"} ms",
                                            style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                            color = if (spuDelayMs != 0L) accentColor else Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        TextButton(onClick = {
                                            spuDelayMs = (spuDelayMs + 50L).coerceAtMost(5000L)
                                            mediaPlayer.setSpuDelay(spuDelayMs * 1000L)
                                        }) { Text("+50ms", color = Color(0xFFD1D5DB)) }
                                    }
                                }
                                AppSlider(
                                    value = spuDelayMs.toFloat(),
                                    onValueChange = {
                                        spuDelayMs = it.toLong()
                                        mediaPlayer.setSpuDelay(spuDelayMs * 1000L)
                                    },
                                    valueRange = -5000f..5000f,
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                )
                            }
                            1 -> {
                                // Tab 1: Sound & Equalizer
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("VLC 10-Band Equalizer", style = typography.body.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                        Text("Hardware-accelerated frequency tuning", style = typography.caption, color = Color(0xFF9EA3AE))
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
                                                Toast.makeText(context, "Equalizer enabled", Toast.LENGTH_SHORT).show()
                                            } else {
                                                mediaPlayer.setEqualizer(null)
                                                Toast.makeText(context, "Equalizer disabled", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                    )
                                }

                                if (isEqualizerEnabled) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Pre-Amplification: ${String.format(Locale.US, "%.1f", eqPreAmp)} dB",
                                        style = typography.caption,
                                        color = Color(0xFFD1D5DB)
                                    )
                                    AppSlider(
                                        value = eqPreAmp,
                                        onValueChange = {
                                            eqPreAmp = it
                                            val eq = MediaPlayer.Equalizer.createFromPreset(selectedEqPresetIndex).apply {
                                                setPreAmp(it)
                                            }
                                            mediaPlayer.setEqualizer(eq)
                                        },
                                        valueRange = -20f..20f,
                                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("Equalizer Presets", style = typography.body.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val presetCount = remember { MediaPlayer.Equalizer.getPresetCount() }
                                    val presets = remember { (0 until presetCount).map { i -> MediaPlayer.Equalizer.getPresetName(i) } }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        presets.forEachIndexed { index, name ->
                                            val isSelected = selectedEqPresetIndex == index
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) accentColor else Color(0xFF1E222D),
                                                border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0x22FFFFFF)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        selectedEqPresetIndex = index
                                                        val eq = MediaPlayer.Equalizer.createFromPreset(index).apply {
                                                            setPreAmp(eqPreAmp)
                                                        }
                                                        mediaPlayer.setEqualizer(eq)
                                                        Toast.makeText(context, "Preset: $name", Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                Text(
                                                    text = name,
                                                    style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                    color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Audio Boost
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Audio Boost (Up to 200%)", style = typography.body.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                        Text("Allows volume slider to amplify sound beyond standard Android 100% ceiling", style = typography.caption, color = Color(0xFF9EA3AE))
                                    }
                                    Switch(
                                        checked = isAudioBoostEnabled,
                                        onCheckedChange = { enabled ->
                                            isAudioBoostEnabled = enabled
                                            if (enabled) {
                                                mediaPlayer.volume = 150
                                                Toast.makeText(context, "Audio boost enabled (150%)", Toast.LENGTH_SHORT).show()
                                            } else {
                                                mediaPlayer.volume = 100
                                                Toast.makeText(context, "Standard volume (100%)", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                    )
                                }
                            }
                            2 -> {
                                // Tab 2: Playback & Tools
                                Text("Playback Speed", style = typography.body.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                val speedList = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    speedList.forEach { spd ->
                                        val isSelected = playbackSpeed == spd
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) accentColor else Color(0xFF1E222D),
                                            border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0x22FFFFFF)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    playbackSpeed = spd
                                                    mediaPlayer.rate = spd
                                                }
                                        ) {
                                            Text(
                                                text = "${spd}x",
                                                style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text("Aspect Ratio", style = typography.body.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VlcAspectMode.entries.forEachIndexed { index, mode ->
                                        val isSelected = aspectModeIndex == index
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) accentColor else Color(0xFF1E222D),
                                            border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0x22FFFFFF)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    aspectModeIndex = index
                                                    mediaPlayer.aspectRatio = mode.ratioStr
                                                    mediaPlayer.scale = mode.scale
                                                    Toast.makeText(context, mode.label, Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Text(
                                                text = mode.buttonText,
                                                style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text("A-B Repeat", style = typography.body.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (abRepeatA != null) accentColor.copy(alpha = 0.25f) else Color(0xFF1E222D),
                                        border = BorderStroke(1.dp, if (abRepeatA != null) accentColor else Color(0x22FFFFFF)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                abRepeatA = currentPositionMs
                                                Toast.makeText(context, "Point A: ${formatTime(currentPositionMs)}", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Text(
                                            text = if (abRepeatA != null) "A: ${formatTime(abRepeatA!!)}" else "Set Point A",
                                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (abRepeatA != null) accentColor else Color.White,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (abRepeatB != null) accentColor.copy(alpha = 0.25f) else Color(0xFF1E222D),
                                        border = BorderStroke(1.dp, if (abRepeatB != null) accentColor else Color(0x22FFFFFF)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (abRepeatA != null && currentPositionMs > abRepeatA!!) {
                                                    abRepeatB = currentPositionMs
                                                    Toast.makeText(context, "Point B: ${formatTime(currentPositionMs)}. Looping A-B", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Point B must be after Point A", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = if (abRepeatB != null) "B: ${formatTime(abRepeatB!!)}" else "Set Point B",
                                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (abRepeatB != null) accentColor else Color.White,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    if (abRepeatA != null || abRepeatB != null) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1E222D),
                                            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    abRepeatA = null
                                                    abRepeatB = null
                                                    Toast.makeText(context, "A-B Repeat cleared", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Text(
                                                text = "Clear",
                                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text("Sleep Timer", style = typography.body.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                val timerOptions = listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    timerOptions.forEach { (mins, label) ->
                                        val isSelected = sleepTimerMinutes == mins
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) accentColor else Color(0xFF1E222D),
                                            border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0x22FFFFFF)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    sleepTimerMinutes = mins
                                                    if (mins > 0) {
                                                        Toast.makeText(context, "Sleep timer: $mins minutes", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = label,
                                                style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Loop & Hardware Acceleration toggles
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Loop Single Video", style = typography.body, color = Color.White)
                                    Switch(
                                        checked = isLooping,
                                        onCheckedChange = { isLooping = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Hardware Acceleration", style = typography.body, color = Color.White)
                                    Switch(
                                        checked = hwDecoderEnabled,
                                        onCheckedChange = {
                                            hwDecoderEnabled = it
                                            retryAttempt++
                                            Toast.makeText(context, if (it) "Hardware decoding enabled" else "Software decoding enabled", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Action Buttons Row: PiP, Audio Extraction, Share
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1E222D),
                                            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    isSettingsSheetOpen = false
                                                    val params = PictureInPictureParams.Builder()
                                                        .setAspectRatio(Rational(16, 9))
                                                        .build()
                                                    activity?.enterPictureInPictureMode(params)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.PictureInPicture, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("PiP", style = typography.caption.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E222D),
                                        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                isSettingsSheetOpen = false
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
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Extract M4A", style = typography.caption.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E222D),
                                        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                isSettingsSheetOpen = false
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = item.mimeType
                                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share", style = typography.caption.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                            Handler(Looper.getMainLooper()).post {
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
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Snapshot saved: Pictures/MyDoc/$filename", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            Handler(Looper.getMainLooper()).post {
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
