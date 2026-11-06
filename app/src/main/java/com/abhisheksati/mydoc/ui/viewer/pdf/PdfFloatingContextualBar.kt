package com.abhisheksati.mydoc.ui.viewer.pdf

import com.abhisheksati.mydoc.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.domain.model.ReadingMode

/**
 * Floating contextual action pill shown at the top of the PDF viewer
 * when text is highlighted or a page is selected.
 */
@Composable
fun PdfFloatingContextualBar(
    pageNum: Int,
    activeSelectionText: String?,
    readingMode: ReadingMode,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit,
    onSpeak: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 64.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FloatingPillItem(
                    icon = Icons.Outlined.ContentCopy,
                    label = stringResource(if (activeSelectionText != null) R.string.ctx_copy else R.string.ctx_copy_page),
                    onClick = onCopy
                )
                FloatingPillItem(
                    icon = Icons.Outlined.Share,
                    label = stringResource(R.string.action_share),
                    onClick = onShare
                )
                FloatingPillItem(
                    icon = Icons.Outlined.TextFields,
                    label = stringResource(R.string.ctx_search),
                    onClick = onSearch
                )
                FloatingPillItem(
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    label = stringResource(R.string.action_speak),
                    onClick = onSpeak
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_close_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingPillItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
