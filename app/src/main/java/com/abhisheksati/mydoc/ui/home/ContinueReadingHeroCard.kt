package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.animation.documentHero
import com.abhisheksati.mydoc.designsystem.animation.documentHeroKey
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.ui.files.LibraryThumbnail

/** One real document is the focal point; large text stacks without squeezing its title. */
@Composable
internal fun ContinueReadingHeroCard(item: LibraryItem, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val largeType = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth().pressScaleEffect(onClick = onClick)
        .clip(AppRadius.xLargeShape).background(colors.surfaceMuted)
        .border(1.dp, colors.cardBorder, AppRadius.xLargeShape).padding(20.dp)) {
        val cover: @Composable () -> Unit = {
            Box(Modifier.size(108.dp, 148.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(88.dp, 124.dp).graphicsLayer { rotationZ = 7f }
                    .background(colors.accent.copy(alpha = 0.10f), AppRadius.smallShape))
                Box(Modifier.size(88.dp, 124.dp).graphicsLayer { rotationZ = -5f }
                    .background(colors.surface, AppRadius.smallShape)
                    .border(1.dp, colors.cardBorder, AppRadius.smallShape))
                Box(Modifier.size(92.dp, 130.dp).shadow(5.dp, AppRadius.smallShape)
                    .clip(AppRadius.smallShape).background(colors.surface)
                    .documentHero(documentHeroKey(item.uri))) {
                    LibraryThumbnail(item, Modifier.fillMaxSize())
                }
            }
        }
        val information: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.home_continue_reading), style = typography.overline, color = colors.accent)
                Text(item.name, style = typography.heading, color = colors.ink,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(if (item.pageCount > 0) stringResource(R.string.home_page_of, item.lastPage + 1, item.pageCount)
                    else item.formattedSize, style = typography.metadata, color = colors.inkSecondary)
                val progress = if (item.pageCount > 0) ((item.lastPage + 1f) / item.pageCount).coerceIn(0f, 1f)
                    else item.progressFraction.coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = colors.accent, trackColor = colors.cardBorder)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_resume), style = typography.bodyStrong, color = colors.accent)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (largeType || maxWidth < 280.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { cover(); information() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                cover()
                Box(Modifier.weight(1f)) { information() }
            }
        }
    }
}
