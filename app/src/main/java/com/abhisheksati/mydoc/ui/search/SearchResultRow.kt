package com.abhisheksati.mydoc.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.ui.files.LibraryThumbnail

/** Name and content matches share the same readable cover, title and supporting text. */
@Composable
internal fun SearchResultRow(item: LibraryItem, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val colors = AppTheme.colors
    Row(modifier.fillMaxWidth().pressScale(interactions).clip(AppRadius.largeShape)
        .background(colors.surface).clickable(role = Role.Button, interactionSource = interactions,
            indication = androidx.compose.material3.ripple(), onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LibraryThumbnail(item, Modifier.size(48.dp, 64.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name, style = AppTheme.typography.bodyLargeStrong, color = colors.ink,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = AppTheme.typography.caption, color = colors.inkSecondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
