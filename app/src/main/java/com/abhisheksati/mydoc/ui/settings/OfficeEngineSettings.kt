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
    var bytes by remember { mutableLongStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var download by remember { mutableStateOf<Job?>(null) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                installed = OfficeEngineManager.isInstalled(context)
                scope.launch { bytes = withContext(Dispatchers.IO) { OfficeEngineManager.installedBytes(context) } }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); download?.cancel() }
    }
    LaunchedEffect(installed) {
        bytes = withContext(Dispatchers.IO) {
            if (installed) LocalApkInstaller.clearOfficeEngineStaging(context)
            OfficeEngineManager.installedBytes(context)
        }
    }
    SettingsCard(title = "Enhanced Office support", icon = Icons.AutoMirrored.Outlined.MenuBook) {
        Text(if (installed) "LibreOffice ${OfficeEngineManager.VERSION} · Installed" else "Built-in readers are active",
            style = MaterialTheme.typography.titleSmall)
        Text("Add LibreOffice for Word, Excel and PowerPoint page layouts, password-protected Office reading and Office content indexing. Documents are processed on your device. Without it, built-in readers offer simpler layouts and fewer supported features.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (installed && bytes > 0) Text("Package and native libraries: ${bytes / (1024 * 1024)} MB. Android Settings shows total storage, including engine data.",
            style = MaterialTheme.typography.bodySmall)
        if (!installed) Text("This is a large, separate download; progress shows the transferred size. Android confirms installation. You can remove the engine at any time.",
            style = MaterialTheme.typography.bodySmall)
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
                Text(if (download?.isActive == true) "Cancel" else "Cancelling…")
            }
            else if (installed) TextButton(onClick = {
                runCatching { installer.launch(Intent(Intent.ACTION_DELETE, Uri.parse("package:${OfficeEngineManager.packageName(context)}"))) }
                    .onFailure { message = "Open Android Settings to remove MyDoc Office Engine." }
            }) { Text("Remove engine") }
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
                    catch (error: Exception) { message = error.message ?: "Unable to download Office support. Try again." }
                    finally { progress = null; download = null }
                }
            }) { Text("Download & install") }
        }
    }
}
