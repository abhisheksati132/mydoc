package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem
import java.util.Locale

/** Format color is a secondary cue; the label keeps full-strength ink in every theme. */
@Composable
internal fun FileFormatBadge(item: StorageItem) {
    if (item.isDirectory || item.extension.isBlank()) return
    val colors = AppTheme.colors
    val tone = when (item.fileType) {
        StorageFileType.PDF -> colors.accentPink
        StorageFileType.WORD -> colors.accentCyan
        StorageFileType.EXCEL, StorageFileType.APK -> colors.accentGreen
        StorageFileType.PPT, StorageFileType.ARCHIVE -> colors.accentAmber
        StorageFileType.IMAGE -> colors.photoKind
        StorageFileType.VIDEO -> colors.videoKind
        StorageFileType.AUDIO -> colors.audioKind
        else -> colors.inkMuted
    }
    Text(item.extension.uppercase(Locale.ROOT).take(8), style = AppTheme.typography.metadataStrong,
        color = colors.ink, modifier = Modifier.clearAndSetSemantics { }
            .background(tone.copy(alpha = 0.12f), AppRadius.microShape)
            .padding(horizontal = 6.dp, vertical = 3.dp))
}
