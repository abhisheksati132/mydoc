package com.abhisheksati.mydoc.data.archive

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.R
import kotlinx.coroutines.*

/** User-started local file processing. No boot starts or background restarts. */
class ArchiveJobService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs get() = ArchiveJobs.get(this)
    private var latestStartId = 0
    private var stoppingIdle = false
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.arc_jobs), NotificationManager.IMPORTANCE_LOW))
        val notification = notification(null)
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(ID, notification)
        scope.launch {
            jobs.state.collect { records ->
                val current = records.firstOrNull { it.status == ArchiveJobStatus.RUNNING || it.status == ArchiveJobStatus.PAUSED }
                getSystemService(NotificationManager::class.java).notify(ID, notification(current))
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        stoppingIdle = false
        intent?.getStringExtra("cancel")?.let(jobs::cancel)
        workUntilIdle()
        return START_NOT_STICKY
    }
    private fun workUntilIdle() {
        jobs.start { scope.launch {
            // Submission can race the worker's final empty-queue check.
            if (jobs.state.value.any { it.active }) workUntilIdle()
            else stoppingIdle = stopSelfResult(latestStartId)
        } }
    }
    override fun onTimeout(startId: Int, fgsType: Int) { jobs.cancelAll(); stopSelf() }
    override fun onDestroy() { if (!stoppingIdle) jobs.cancelAll(); scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun notification(record: ArchiveJobRecord?): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.arc_jobs)).setContentText(record?.title ?: getString(R.string.arc_preparing))
            .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true)
            .setProgress(100, record?.progress?.percent ?: 0, record?.progress?.percent == null)
            .apply {
                if (record != null) {
                    val cancel = PendingIntent.getService(this@ArchiveJobService, record.id.hashCode(),
                        Intent(this@ArchiveJobService, ArchiveJobService::class.java).putExtra("cancel", record.id),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    addAction(0, getString(R.string.action_cancel), cancel)
                }
            }.build()
    }
    companion object { private const val CHANNEL = "archive_jobs"; private const val ID = 731 }
}
