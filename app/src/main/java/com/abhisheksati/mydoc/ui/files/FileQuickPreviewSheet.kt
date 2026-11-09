package com.abhisheksati.mydoc.ui.files

import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.StorageItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FileQuickPreviewSheet(
    item: StorageItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onMore: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val previewDescription = stringResource(R.string.quick_preview_thumbnail)
    AppModalSheet(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Row(
                Modifier.fillMaxWidth().padding(start = AppSpacing.screenPadding, end = AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.quick_preview_title), style = AppTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).semantics { heading() })
                if (onShare != null) IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.quick_preview_close))
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(AppSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                item(key = "name") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        SelectionContainer {
                            Text(item.name, style = AppTheme.typography.title,
                                modifier = Modifier.semantics { heading() })
                        }
                        Text(stringResource(R.string.quick_preview_hint), style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant)
                    }
                }
                item(key = "format") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        Surface(shape = AppRadius.pillShape, color = scheme.primaryContainer) {
                            Text(
                                when {
                                    item.isDirectory -> stringResource(R.string.archive_folder)
                                    item.extension.isNotBlank() -> item.extension.uppercase(Locale.ROOT)
                                    else -> stringResource(R.string.quick_preview_file)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small)
                            )
                        }
                        Text(item.formattedSize, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
                item(key = "thumbnail") {
                    Box(
                        Modifier.fillMaxWidth().height(220.dp).clip(AppRadius.cardShape)
                            .background(AppTheme.colors.surfaceMuted)
                            .clearAndSetSemantics { contentDescription = previewDescription },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isDirectory) FolderPlate(size = 72)
                        else StorageThumbnail(item, Modifier.fillMaxSize())
                    }
                }
                if (item.formattedLastModified.isNotBlank()) item(key = "modified") {
                    PreviewDetail(stringResource(R.string.details_last_modified), item.formattedLastModified)
                }
                item(key = "location") {
                    PreviewDetail(stringResource(R.string.details_location), item.path)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(AppSpacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onOpen, modifier = Modifier.weight(1f), shape = AppRadius.pillShape) {
                    Text(stringResource(R.string.action_open))
                    Spacer(Modifier.width(AppSpacing.small))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                TextButton(onClick = onMore, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(AppSpacing.small))
                    Text(stringResource(R.string.cd_more_actions))
                }
            }
        }
    }
}

@Composable
private fun PreviewDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xSmall)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(value, style = MaterialTheme.typography.bodyMedium) }
    }
}
