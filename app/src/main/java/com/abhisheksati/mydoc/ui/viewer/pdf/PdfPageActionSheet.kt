package com.abhisheksati.mydoc.ui.viewer.pdf

import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars

/**
 * Bottom action sheet displaying contextual tools for a specific PDF page:
 * copy text, read aloud with TTS, share text, and perform OCR text extraction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPageActionSheet(
    isOpen: Boolean,
    pageNum: Int,
    onDismiss: () -> Unit,
    onCopyPageText: () -> Unit,
    onReadPageAloud: () -> Unit,
    onSharePageText: () -> Unit,
    onExtractOcr: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppModalSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        OverlaySystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.page_label, pageNum + 1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            PageAction(
                icon = Icons.Outlined.ContentCopy,
                title = stringResource(R.string.page_copy_title),
                supporting = stringResource(R.string.page_copy_supporting),
                onClick = onCopyPageText
            )
            PageAction(
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                title = stringResource(R.string.page_read_title),
                supporting = stringResource(R.string.page_read_supporting),
                onClick = onReadPageAloud
            )
            PageAction(
                icon = Icons.Outlined.Share,
                title = stringResource(R.string.page_share_title),
                supporting = stringResource(R.string.page_share_supporting),
                onClick = onSharePageText
            )
            PageAction(
                icon = Icons.Outlined.TextFields,
                title = stringResource(R.string.page_ocr_title),
                supporting = stringResource(R.string.page_ocr_supporting),
                onClick = onExtractOcr
            )
        }
    }
}

@Composable
private fun PageAction(
    icon: ImageVector,
    title: String,
    supporting: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
