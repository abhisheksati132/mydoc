package com.abhisheksati.mydoc.ui.settings

import android.os.Build
import android.os.StatFs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.abhisheksati.mydoc.BuildConfig
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.rendering.engine.OfficeEngineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun DeviceDiagnosticsSection() {
    val context = LocalContext.current
    val snapshot by produceState<Pair<Long?, Boolean>?>(null, context) {
        value = withContext(Dispatchers.IO) {
            val free = runCatching { StatFs(context.filesDir.path).availableBytes }.getOrNull()
            free to OfficeEngineManager.isInstalled(context)
        }
    }
    val unavailable = stringResource(R.string.diagnostics_unavailable)
    val rows = listOf(
        stringResource(R.string.diagnostics_version) to BuildConfig.VERSION_NAME,
        stringResource(R.string.diagnostics_android) to stringResource(
            R.string.diagnostics_android_value, Build.VERSION.RELEASE, Build.VERSION.SDK_INT
        ),
        stringResource(R.string.diagnostics_abi) to Build.SUPPORTED_ABIS.joinToString().ifBlank { unavailable },
        stringResource(R.string.diagnostics_free) to
            (snapshot?.first?.let(StorageVolume::formatBytes) ?: unavailable),
        stringResource(R.string.diagnostics_office) to when (snapshot?.second) {
            true -> stringResource(R.string.diagnostics_installed)
            false -> stringResource(R.string.diagnostics_not_installed)
            null -> unavailable
        },
        stringResource(R.string.diagnostics_conversion_failure) to
            (com.abhisheksati.mydoc.rendering.engine.OfficeConversionDiagnostics.lastFailure?.name
                ?: stringResource(R.string.diagnostics_no_failure))
    )
    SelectionContainer {
        Column {
            rows.forEach { (label, value) ->
                ListItem(headlineContent = { Text(label) }, supportingContent = { Text(value) })
            }
        }
    }
}
