package com.abhisheksati.mydoc.ui.files

import android.text.format.Formatter
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.archive.*
import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.domain.archive.ArchiveProblem

internal fun archiveProblemText(problem: ArchiveProblem?): Int = when (problem) {
    ArchiveProblem.PASSWORD_REQUIRED -> R.string.arc_password_missing
    ArchiveProblem.WRONG_PASSWORD -> R.string.arc_password_wrong
    ArchiveProblem.MISSING_VOLUMES -> R.string.arc_missing_parts
    ArchiveProblem.CORRUPT -> R.string.arc_corrupt
    ArchiveProblem.UNSUPPORTED -> R.string.arc_unsupported
    ArchiveProblem.NO_SPACE -> R.string.arc_no_space
    ArchiveProblem.MEMORY -> R.string.arc_memory
    ArchiveProblem.CHANGED -> R.string.arc_changed
    else -> R.string.arc_engine_error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveJobsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val center = remember { ArchiveJobs.get(context) }
    val jobs by center.state.collectAsState()
    var notificationsAllowed by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsAllowed = it }
    AppModalSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(stringResource(R.string.arc_jobs), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.arc_background), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.arc_pause_help), style = MaterialTheme.typography.bodySmall)
                if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed) OutlinedButton(onClick = {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.arc_enable_notifications)) }
                TextButton(onClick = center::clearFinished, enabled = jobs.any { !it.active }) { Text(stringResource(R.string.transfers_clear)) }
            }
            if (jobs.isEmpty()) item { Text(stringResource(R.string.arc_no_jobs)) }
            items(jobs, key = { it.id }) { job ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(job.title, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(when (job.status) {
                            ArchiveJobStatus.QUEUED -> R.string.arc_queued
                            ArchiveJobStatus.RUNNING -> R.string.arc_running
                            ArchiveJobStatus.PAUSED -> R.string.arc_paused
                            ArchiveJobStatus.COMPLETED -> R.string.arc_completed
                            ArchiveJobStatus.FAILED -> R.string.arc_failed
                            ArchiveJobStatus.CANCELLED -> R.string.arc_cancelled
                            ArchiveJobStatus.INTERRUPTED -> R.string.arc_interrupted
                        }))
                        if (job.active) {
                            if (job.progress.percent != null) LinearProgressIndicator(progress = { job.progress.percent / 100f }, modifier = Modifier.fillMaxWidth())
                            else LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (job.progress.currentName.isNotEmpty()) Text(job.progress.currentName, style = MaterialTheme.typography.bodySmall)
                            job.remainingSeconds?.let { Text(stringResource(R.string.arc_speed, Formatter.formatShortFileSize(context, job.bytesPerSecond), it), style = MaterialTheme.typography.bodySmall) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (job.status == ArchiveJobStatus.RUNNING) TextButton(onClick = { center.pause(job.id) }, enabled = job.canPause) { Text(stringResource(R.string.arc_pause)) }
                                if (job.status == ArchiveJobStatus.PAUSED) TextButton(onClick = { center.resume(job.id) }) { Text(stringResource(R.string.arc_resume)) }
                                TextButton(onClick = { center.cancel(job.id) }) { Text(stringResource(R.string.action_cancel)) }
                            }
                        }
                        if (job.status == ArchiveJobStatus.FAILED) Text(stringResource(archiveProblemText(job.problem)), color = MaterialTheme.colorScheme.error)
                        job.outputs.forEach { Text(it.absolutePath, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tools_close)) } }
        }
    }
}
