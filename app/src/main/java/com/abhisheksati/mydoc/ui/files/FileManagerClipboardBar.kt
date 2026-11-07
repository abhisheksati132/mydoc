package com.abhisheksati.mydoc.ui.files

import com.abhisheksati.mydoc.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import com.abhisheksati.mydoc.domain.model.ClipboardState

@Composable
fun FileManagerClipboardBar(
    clipboardState: ClipboardState?,
    onCancel: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
    isTransferring: Boolean = false,
    onShowTransfers: () -> Unit = {},
    onHeightChanged: (Int) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = clipboardState != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        clipboardState?.let { clip ->
            val isCopy = clip.operation == ClipboardOperation.COPY
            GlassSurface(
                shape = AppRadius.largeShape,
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth().onSizeChanged { onHeightChanged(it.height) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isCopy) Icons.Outlined.ContentCopy else Icons.Outlined.ContentCut,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTransferring) stringResource(R.string.transfers_active) else if (clip.items.size == 1) {
                                "1 item ready to ${if (isCopy) "copy" else "move"}"
                            } else {
                                "${clip.items.size} items ready to ${if (isCopy) "copy" else "move"}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(if (isTransferring) R.string.transfers_details_hint else R.string.clipboard_paste_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = if (isTransferring) onShowTransfers else onCancel,
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(stringResource(if (isTransferring) R.string.transfers_details else R.string.action_cancel), color = scheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onPaste,
                        enabled = !isTransferring,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_paste))
                    }
                }
            }
        }
    }
}
