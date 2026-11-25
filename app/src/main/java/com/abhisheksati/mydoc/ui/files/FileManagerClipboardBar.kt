package com.abhisheksati.mydoc.ui.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.components.GlassMaterial
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import com.abhisheksati.mydoc.domain.model.ClipboardState

/** A measured paste card: large text moves its actions below the destination. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileManagerClipboardBar(
    clipboardState: ClipboardState?,
    onCancel: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
    isTransferring: Boolean = false,
    onShowTransfers: () -> Unit = {},
    onHeightChanged: (Int) -> Unit = {},
    destinationName: String? = null
) {
    val reduced = LocalReducedMotion.current
    AnimatedVisibility(
        visible = clipboardState != null,
        enter = if (reduced) EnterTransition.None else slideInVertically(AppMotion.snappy()) { it / 3 } + fadeIn(AppMotion.fadeFast),
        exit = if (reduced) ExitTransition.None else slideOutVertically(AppMotion.snappy()) { it / 3 } + fadeOut(AppMotion.fadeFast),
        modifier = modifier
    ) {
        clipboardState?.let { clip ->
            GlassSurface(shape = AppRadius.largeShape, material = GlassMaterial.PANEL,
                modifier = Modifier.fillMaxWidth().onSizeChanged { onHeightChanged(it.height) }) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
                    val stack = maxWidth < 336.dp || LocalDensity.current.fontScale > 1.3f
                    val actions: @Composable () -> Unit = {
                        TextButton(onClick = if (isTransferring) onShowTransfers else onCancel) {
                            Text(stringResource(if (isTransferring) R.string.transfers_details else R.string.action_cancel))
                        }
                        Button(onClick = onPaste, enabled = !isTransferring && clip.items.isNotEmpty(), shape = AppRadius.pillShape) {
                            Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_paste))
                        }
                    }
                    if (stack) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ClipboardDetails(clip, isTransferring, destinationName)
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ClipboardDetails(clip, isTransferring, destinationName, Modifier.weight(1f))
                            actions()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipboardDetails(clip: ClipboardState, transferring: Boolean, destination: String?, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val copy = clip.operation == ClipboardOperation.COPY
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(if (copy) Icons.Outlined.ContentCopy else Icons.Outlined.ContentCut, null,
            tint = colors.accent, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (transferring) stringResource(R.string.transfers_active) else pluralStringResource(
                if (copy) R.plurals.clipboard_copy_ready else R.plurals.clipboard_move_ready, clip.items.size, clip.items.size),
                style = AppTheme.typography.bodyStrong, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (transferring) stringResource(R.string.transfers_details_hint)
                else destination?.let { stringResource(R.string.clipboard_destination, it) }
                    ?: stringResource(R.string.clipboard_paste_hint),
                style = AppTheme.typography.caption, color = colors.inkSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
