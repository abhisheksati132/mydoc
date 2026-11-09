package com.abhisheksati.mydoc.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.abhisheksati.mydoc.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abhisheksati.mydoc.rendering.engine.OfficeEngineManager
import com.abhisheksati.mydoc.util.LocalApkInstaller
import kotlinx.coroutines.*

@Composable
fun OfficeEngineSettings(onInstallApk: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    var installed by remember { mutableStateOf(OfficeEngineManager.isInstalled(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var download by remember { mutableStateOf<Job?>(null) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                installed = OfficeEngineManager.isInstalled(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); download?.cancel() }
    }
    LaunchedEffect(installed) {
        if (installed) withContext(Dispatchers.IO) { LocalApkInstaller.clearOfficeEngineStaging(context) }
    }
    val downloadFailed = stringResource(R.string.office_simple_failed)
    val removeFailed = stringResource(R.string.office_simple_remove_failed)
    SettingsCard(title = stringResource(R.string.office_simple_title), icon = Icons.AutoMirrored.Outlined.MenuBook) {
        Text(stringResource(if (installed) R.string.office_simple_active else R.string.office_simple_builtin),
            style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.office_simple_summary),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        progress?.let { (done, total) ->
            if (total > 0) LinearProgressIndicator(progress = { (done.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            else LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(if (total > 0) "${done / (1024 * 1024)} / ${total / (1024 * 1024)} MB" else "${done / (1024 * 1024)} MB downloaded",
                style = MaterialTheme.typography.bodySmall)
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (download != null) TextButton(enabled = download?.isActive == true,
                onClick = { download?.cancel(); progress = null }) {
                Text(stringResource(R.string.action_cancel))
            }
            else if (installed) TextButton(onClick = {
                runCatching { installer.launch(Intent(Intent.ACTION_DELETE, Uri.parse("package:${OfficeEngineManager.packageName(context)}"))) }
                    .onFailure { message = removeFailed }
            }) { Text(stringResource(R.string.office_simple_remove)) }
            else Button(onClick = {
                message = null
                progress = 0L to 0L
                download = scope.launch {
                    try {
                        val file = OfficeEngineManager.download(context) { done, total -> scope.launch { progress = done to total } }
                        val uri = LocalApkInstaller.stage(context, Uri.fromFile(file), prefix = "office-engine")
                        withContext(Dispatchers.IO) { file.delete() }
                        onInstallApk(uri)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { message = downloadFailed }
                    finally { progress = null; download = null }
                }
            }) { Text(stringResource(R.string.office_simple_install)) }
        }
    }
}
