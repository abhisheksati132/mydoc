package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.domain.model.*
import com.abhisheksati.mydoc.util.UserMessages
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExplorerTransferCard(
    transfer: ExplorerTransfer,
    onStop: () -> Unit,
    onOpenDestination: ((File) -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val operation = if (transfer.operation == ClipboardOperation.COPY) R.string.transfers_copy else R.string.transfers_move
    val status = when (transfer.status) {
        TransferStatus.RUNNING -> R.string.transfers_running
        TransferStatus.STOPPING -> R.string.transfers_stopping
        TransferStatus.COMPLETED -> R.string.transfers_completed
        TransferStatus.PARTIAL -> R.string.transfers_partial
        TransferStatus.FAILED -> R.string.transfers_failed
        TransferStatus.CANCELLED -> R.string.transfers_cancelled
    }
    val statusIcon = when (transfer.status) {
        TransferStatus.COMPLETED -> Icons.Outlined.CheckCircle
        TransferStatus.PARTIAL, TransferStatus.FAILED -> Icons.Outlined.ErrorOutline
        TransferStatus.CANCELLED -> Icons.Outlined.StopCircle
        else -> Icons.Outlined.Sync
    }
    val statusColor = when (transfer.status) {
        TransferStatus.PARTIAL, TransferStatus.FAILED -> scheme.error
        TransferStatus.COMPLETED -> scheme.primary
        else -> scheme.onSurfaceVariant
    }
    val failureMessages = remember(transfer.failures, operation) {
        transfer.failures.map { it.name to UserMessages.actionFailed(operation, it.cause) }
    }
    Surface(shape = AppRadius.cardShape, color = scheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(stringResource(operation), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() })
                Icon(statusIcon, contentDescription = null, tint = statusColor)
            }
            Text(stringResource(status), color = statusColor, style = MaterialTheme.typography.bodyMedium)
            Text(if (transfer.failed > 0) stringResource(R.string.transfers_counts, transfer.completed, transfer.total, transfer.failed)
                else pluralStringResource(R.plurals.transfers_completed_count, transfer.total, transfer.completed, transfer.total),
                style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.transfers_destination), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(transfer.destination.absolutePath, style = MaterialTheme.typography.bodySmall)
            if (transfer.running) {
                if (transfer.currentName.isNotBlank()) Text(transfer.currentName, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { ((transfer.completed + transfer.failed).toFloat() / transfer.total.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (transfer.failed > 0) {
                failureMessages.forEach { (name, message) ->
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Text(message.resolve(LocalContext.current), style = MaterialTheme.typography.bodySmall)
                }
                Text(stringResource(R.string.transfers_failure_help), style = MaterialTheme.typography.bodySmall, color = scheme.error)
            } else if (transfer.status == TransferStatus.CANCELLED) {
                Text(stringResource(R.string.transfers_continue_help), style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onOpenDestination != null) TextButton(onClick = { onOpenDestination(transfer.destination) }) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.transfers_open_destination))
                }
                if (transfer.running) TextButton(onClick = onStop, enabled = transfer.status != TransferStatus.STOPPING) {
                    Text(stringResource(R.string.transfers_stop))
                }
            }
        }
    }
}
