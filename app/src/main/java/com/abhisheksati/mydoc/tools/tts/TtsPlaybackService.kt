package com.abhisheksati.mydoc.tools.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps read-aloud alive with the screen off: a media-playback foreground service with a
 * lock-screen media session. All audio state lives in [TtsPlaybackController]; this service
 * only mirrors it into the notification and forwards transport buttons back.
 */
class TtsPlaybackService : Service() {

    private lateinit var ttsController: TtsPlaybackController
    private lateinit var session: MediaSession
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observers: Job? = null
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()
        ttsController = TtsPlaybackController.get(this)
        createChannel()
        session = MediaSession(this, "MyDocReadAloud").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = ttsController.resume()
                override fun onPause() = ttsController.pause()
                override fun onStop() = ttsController.stop()
            })
            isActive = true
        }
        updateSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> ttsController.pause()
            ACTION_RESUME -> ttsController.resume()
            ACTION_STOP -> {
                ttsController.stop()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForegroundCompat()
        if (observers == null) observe()
        return START_NOT_STICKY
    }

    private fun observe() {
        observers = scope.launch {
            launch {
                // The controller is IDLE while the first page's text is still loading, so a bare
                // IDLE must not tear the service down before it has ever played; only an IDLE
                // after real playback (or an engine error) ends the session.
                var hasPlayed = false
                ttsController.playbackState.collectLatest { state ->
                    updateSession()
                    if (state == TtsPlaybackState.PLAYING || state == TtsPlaybackState.PAUSED) {
                        hasPlayed = true
                    }
                    if (state == TtsPlaybackState.ERROR || (state == TtsPlaybackState.IDLE && hasPlayed)) {
                        stopForegroundCompat()
                        stopSelf()
                    } else {
                        notify(buildNotification(state))
                    }
                }
            }
            launch {
                ttsController.title.collectLatest {
                    updateSession()
                }
            }
        }
    }

    private fun startForegroundCompat() {
        val notification = buildNotification(ttsController.playbackState.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun stopForegroundCompat() {
        if (!foregroundStarted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        foregroundStarted = false
    }

    private fun updateSession() {
        val state = when (ttsController.playbackState.value) {
            TtsPlaybackState.PLAYING -> PlaybackState.STATE_PLAYING
            TtsPlaybackState.PAUSED -> PlaybackState.STATE_PAUSED
            TtsPlaybackState.INITIALIZING -> PlaybackState.STATE_BUFFERING
            else -> PlaybackState.STATE_STOPPED
        }
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_STOP or PlaybackState.ACTION_PLAY_PAUSE
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
        )
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, ttsController.title.value.ifBlank { getString(R.string.app_name) })
                .putString(MediaMetadata.METADATA_KEY_ARTIST, getString(R.string.tts_reading))
                .build()
        )
    }

    private fun buildNotification(state: TtsPlaybackState): Notification {
        val isPlaying = state == TtsPlaybackState.PLAYING
        val transport = if (isPlaying) {
            action(ACTION_PAUSE, getString(R.string.tts_pause), android.R.drawable.ic_media_pause)
        } else {
            action(ACTION_RESUME, getString(R.string.tts_resume), android.R.drawable.ic_media_play)
        }
        val publicVersion = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_scanner)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.tts_reading))
            .build()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_scanner)
            .setContentTitle(ttsController.title.value.ifBlank { getString(R.string.app_name) })
            .setContentText(
                when (state) {
                    TtsPlaybackState.PAUSED -> getString(R.string.tts_paused)
                    TtsPlaybackState.INITIALIZING -> getString(R.string.tts_initializing)
                    else -> ttsController.currentUtterance.value.take(120)
                }
            )
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .addAction(transport)
            .addAction(action(ACTION_STOP, getString(R.string.action_stop), android.R.drawable.ic_menu_close_clear_cancel))
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .build()
    }

    private fun action(action: String, title: String, icon: Int): Notification.Action {
        val intent = Intent(this, TtsPlaybackService::class.java).setAction(action)
        val pending = PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Action.Builder(icon, title, pending).build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun notify(notification: Notification) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tts_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.tts_notification_channel_description)
                setShowBadge(false)
            }
        )
    }

    override fun onDestroy() {
        observers?.cancel()
        session.isActive = false
        session.release()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "tts_playback"
        private const val NOTIFICATION_ID = 4001
        private const val ACTION_PAUSE = "com.abhisheksati.mydoc.action.TTS_PAUSE"
        private const val ACTION_RESUME = "com.abhisheksati.mydoc.action.TTS_RESUME"
        private const val ACTION_STOP = "com.abhisheksati.mydoc.action.TTS_STOP"

        fun ensureStarted(context: Context) {
            val intent = Intent(context, TtsPlaybackService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stopSession(context: Context) {
            runCatching { context.stopService(Intent(context, TtsPlaybackService::class.java)) }
        }
    }
}
