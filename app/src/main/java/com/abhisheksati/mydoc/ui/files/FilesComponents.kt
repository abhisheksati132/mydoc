package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.abhisheksati.mydoc.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Surface
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import com.abhisheksati.mydoc.designsystem.components.GlassMaterial
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.tonalSheen
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.storage.StorageCategoryStat
import com.abhisheksati.mydoc.ui.storage.StorageCategoryType

@Composable
fun FilesSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val largeType = LocalDensity.current.fontScale > 1.3f
    val labels: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = AppTheme.typography.heading, color = AppTheme.colors.ink,
                modifier = Modifier.semantics { heading() })
            if (meta != null) Text(meta, style = AppTheme.typography.metadata, color = AppTheme.colors.inkMuted)
        }
    }
    val action: @Composable () -> Unit = {
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
    val spacing = modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 4.dp)
    if (largeType) {
        Column(spacing, verticalArrangement = Arrangement.spacedBy(4.dp)) { labels(); action() }
    } else {
        Row(spacing, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { labels() }
            action()
        }
    }
}

/**
 * The Library's storage hero: used space against capacity as one segmented bar
 * (each slice is a category, so the bar already says *what* is using the disk).
 * Exact figures live in Insights; this card stays a visual entry point.
 */
@Composable
fun StorageOverviewCard(
    usedRatio: Float,
    usedBytes: Long,
    totalBytes: Long,
    categories: List<StorageCategoryStat>,
    onOpenTrash: () -> Unit,
    onBrowse: () -> Unit,
    onInsights: () -> Unit,
    modifier: Modifier = Modifier,
    onClean: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val colors = AppTheme.colors
    val accent = colors.accent
    val shape = AppRadius.xLargeShape
    val sweep by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (usedRatio > 0f) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            700,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "storageOverviewSweep"
    )
    val segments = rememberStorageBarSegments(categories, totalBytes, usedBytes)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface, shape)
            .background(
                Brush.verticalGradient(listOf(accent.copy(alpha = 0.15f), accent.copy(alpha = 0.02f))),
                shape
            )
            .tonalSheen()
            .border(1.dp, accent.copy(alpha = 0.18f), shape)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppRadius.insetShape)
                .clickable(role = Role.Button, onClick = onBrowse),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accent.copy(alpha = 0.16f), AppRadius.insetShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(
                text = stringResource(R.string.files_title_internal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        SegmentedStorageBar(
            segments = segments,
            sweep = sweep,
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StorageQuickAction(
                icon = Icons.Outlined.DonutLarge,
                label = stringResource(R.string.files_insights),
                tone = accent,
                onClick = onInsights,
                modifier = Modifier.weight(1f)
            )
            StorageQuickAction(
                icon = Icons.Outlined.DeleteOutline,
                label = stringResource(R.string.menu_recycle_bin),
                tone = colors.accentAmber,
                onClick = onOpenTrash,
                modifier = Modifier.weight(1f)
            )
            StorageQuickAction(
                icon = Icons.Outlined.AutoAwesome,
                label = stringResource(R.string.settings_clean),
                tone = colors.accentGreen,
                onClick = onClean,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StorageQuickAction(
    icon: ImageVector,
    label: String,
    tone: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppRadius.cardShape)
            .background(tone.copy(alpha = 0.10f))
            .border(1.dp, tone.copy(alpha = 0.14f), AppRadius.cardShape)
            .pressScaleEffect(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(tone.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(17.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal data class StorageBarSegment(val fraction: Float, val color: Color)

/**
 * Category slices for a capacity bar, ordered like the Insights donut: largest
 * first, Downloads (a subset of Documents) and Large Files (overlapping hits)
 * left out, and whatever the categories cannot account for drawn as one muted
 * "unclassified" slice so the bar still sums to used space.
 */
@Composable
internal fun rememberStorageBarSegments(
    categories: List<StorageCategoryStat>,
    totalBytes: Long,
    usedBytes: Long
): List<StorageBarSegment> {
    val colors = AppTheme.colors
    val palette = listOf(
        colors.accent,
        colors.accentAmber,
        colors.accentCyan,
        colors.accentGreen,
        colors.accentPink
    )
    return remember(categories, totalBytes, usedBytes) {
        if (totalBytes <= 0L) return@remember emptyList()
        val included = categories
            .filter {
                it.type != StorageCategoryType.DOWNLOADS &&
                    it.type != StorageCategoryType.LARGE_FILES &&
                    it.bytes > 0L
            }
            .sortedByDescending { it.bytes }
        val classified = included.sumOf { it.bytes }
        // Below ~2% a slice renders as a stray tick once the 3dp gaps are applied, so
        // these fold into the larger slice drawn just before them.
        val minVisibleFraction = 0.02f
        val segments = mutableListOf<StorageBarSegment>()
        included.forEachIndexed { index, stat ->
            val fraction = stat.bytes.toFloat() / totalBytes
            val last = segments.lastOrNull()
            if (last != null && fraction < minVisibleFraction) {
                segments[segments.lastIndex] = last.copy(fraction = last.fraction + fraction)
            } else {
                segments += StorageBarSegment(fraction, palette[index % palette.size])
            }
        }
        val unclassified = usedBytes - classified
        if (classified <= usedBytes && unclassified > 0L) {
            val fraction = unclassified.toFloat() / totalBytes
            val last = segments.lastOrNull()
            if (last != null && fraction < minVisibleFraction) {
                segments[segments.lastIndex] = last.copy(fraction = last.fraction + fraction)
            } else {
                segments += StorageBarSegment(fraction, colors.inkMuted)
            }
        }
        segments
    }
}

@Composable
internal fun SegmentedStorageBar(
    segments: List<StorageBarSegment>,
    sweep: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    gap: androidx.compose.ui.unit.Dp = 3.dp
) {
    androidx.compose.foundation.Canvas(modifier) {
        val radius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        drawRoundRect(trackColor, cornerRadius = radius)
        if (segments.isEmpty()) return@Canvas
        val gapPx = gap.toPx()
        var x = 0f
        segments.forEach { segment ->
            val width = size.width * segment.fraction * sweep
            if (width > 0f && x < size.width) {
                val drawn = width.coerceAtMost(size.width - x)
                drawRoundRect(
                    color = segment.color,
                    topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(drawn, size.height),
                    cornerRadius = radius
                )
            }
            x += width + gapPx
            if (x >= size.width) return@Canvas
        }
    }
}

@Composable
fun CategoryTile(
    type: StorageCategoryType,
    stat: StorageCategoryStat?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = AppTheme.colors
    val tone = when (type) {
        StorageCategoryType.DOCUMENTS -> colors.accent
        StorageCategoryType.DOWNLOADS -> colors.accentCyan
        StorageCategoryType.IMAGES -> colors.accentAmber
        StorageCategoryType.VIDEOS -> colors.accentPink
        StorageCategoryType.AUDIO -> colors.accentGreen
        StorageCategoryType.APKS -> colors.accent
        else -> colors.accent
    }
    val shape = AppRadius.xLargeShape
    Column(
        modifier = modifier
            .heightIn(min = 128.dp)
            .clip(shape)
            .background(colors.surface, shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        tone.copy(alpha = 0.14f),
                        tone.copy(alpha = 0.03f)
                    )
                ),
                shape
            )
            .tonalSheen()
            .border(1.dp, tone.copy(alpha = 0.22f), shape)
            .pressScaleEffect(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.verticalGradient(listOf(tone.copy(alpha = 0.26f), tone.copy(alpha = 0.10f))),
                        AppRadius.insetShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (stat != null && stat.fileCount > 0) {
                Surface(
                    shape = AppRadius.pillShape,
                    color = tone.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${stat.fileCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tone,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(if (type == StorageCategoryType.APKS) R.string.files_category_apps else type.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    stat == null -> stringResource(R.string.files_scanning_short)
                    stat.bytes <= 0L -> stringResource(R.string.files_empty_short)
                    else -> stat.formattedSize
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FolderPlate(modifier: Modifier = Modifier, size: Int = 44) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(AppRadius.insetShape)
            .background(scheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = scheme.onSecondaryContainer,
            modifier = Modifier.size((size * 0.48f).dp)
        )
    }
}

/** The one overflow affordance: same icon, tint and 48dp target in every list and grid. */
@Composable
internal fun FileOverflowButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FileRow(
    item: StorageItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFileSize: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberAppHaptics()
    val pressInteractions = androidx.compose.runtime.remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(AppRadius.cardShape)
            .background(if (isSelected) scheme.primaryContainer else scheme.background)
            .then(
                if (isSelectionMode) {
                    // R7: `toggleable` is what publishes a checked state to the accessibility layer.
                    // Measured on mydoc_api35: a plain `semantics { selected = … }` never reached the
                    // node, and a bare `role = Role.Checkbox` made every row checkable while the
                    // selected one still read unchecked — worse than saying nothing. Selection mode
                    // only, so a row being browsed normally is not announced as a checkbox.
                    Modifier.toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { onClick() }
                    )
                } else {
                    Modifier.pressScale(pressInteractions).combinedClickable(
                        interactionSource = pressInteractions,
                        indication = ripple(),
                        onClick = onClick,
                        onLongClick = {
                            haptics.snap()
                            onLongClick()
                        }
                    )
                }
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (item.isDirectory) {
            FolderPlate()
        } else {
            StorageThumbnail(
                item = item,
                modifier = Modifier
                    .size(width = 44.dp, height = 52.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = AppTheme.typography.bodyLargeStrong,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FileFormatBadge(item)
                Text(
                    text = if (item.isDirectory) {
                        item.formattedSize
                    } else {
                        listOf(if (showFileSize) item.formattedSize else "", item.formattedLastModified)
                            .filter { it.isNotBlank() }.joinToString(" · ")
                    },
                    style = AppTheme.typography.caption,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = stringResource(if (isSelected) R.string.cd_selected else R.string.cd_not_selected),
                tint = if (isSelected) scheme.primary else scheme.outline,
                modifier = Modifier.size(22.dp)
            )
        } else {
            FileOverflowButton(
                contentDescription = stringResource(R.string.more_options_for_file, item.name),
                onClick = onMoreClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FileGridCell(
    item: StorageItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberAppHaptics()
    val pressInteractions = androidx.compose.runtime.remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppRadius.largeShape)
            .background(if (isSelected) scheme.primaryContainer else scheme.surface)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) scheme.primary else AppTheme.colors.cardBorder, AppRadius.largeShape)
            .then(
                if (isSelectionMode) {
                    // R7: `toggleable` is what publishes a checked state to the accessibility layer.
                    // Measured on mydoc_api35: a plain `semantics { selected = … }` never reached the
                    // node, and a bare `role = Role.Checkbox` made every row checkable while the
                    // selected one still read unchecked — worse than saying nothing. Selection mode
                    // only, so a row being browsed normally is not announced as a checkbox.
                    Modifier.toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { onClick() }
                    )
                } else {
                    Modifier.pressScale(pressInteractions).combinedClickable(
                        interactionSource = pressInteractions,
                        indication = ripple(),
                        onClick = onClick,
                        onLongClick = {
                            haptics.snap()
                            onLongClick()
                        }
                    )
                }
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (LocalDensity.current.fontScale > 1.3f) 2f else 1f)
                .clip(AppRadius.insetShape)
                .background(if (isSelected) scheme.primaryContainer else scheme.surfaceContainerLow)
        ) {
            if (item.isDirectory) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = if (isSelected) scheme.onPrimaryContainer else scheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            } else {
                StorageThumbnail(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scheme.primary.copy(alpha = 0.16f))
                    )
                }
            }

            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = stringResource(if (isSelected) R.string.cd_selected else R.string.cd_not_selected),
                    tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .background(scheme.surface, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FileFormatBadge(item)
            Text(
                text = item.formattedSize,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun LocationRow(
    title: String,
    meta: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppRadius.cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(AppRadius.insetShape)
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FilesEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.abhisheksati.mydoc.designsystem.components.PaperCompanion(icon)
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilesSelectionBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: (() -> Unit)?,
    onCompress: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onMergePdfs: (() -> Unit)? = null,
    onConvertToPdf: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    var isMenuOpen by remember { mutableStateOf(false) }
    val hasSelection = selectedCount > 0

    GlassSurface(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = AppRadius.largeShape, material = GlassMaterial.PANEL, elevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cd_close_selection))
                }
                Text(pluralStringResource(R.plurals.files_selected_count_plural, selectedCount, selectedCount),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (selectedCount < totalCount) {
                    IconButton(onClick = onSelectAll) {
                        Icon(Icons.Outlined.SelectAll, contentDescription = stringResource(R.string.cd_select_all))
                    }
                }
                Box {
                    IconButton(onClick = { isMenuOpen = true }, enabled = hasSelection) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cd_more_actions))
                    }
                    AppDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                        if (onMergePdfs != null) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.library_merge_pdfs)) },
                                onClick = { isMenuOpen = false; onMergePdfs() })
                        }
                        if (onConvertToPdf != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.batch_convert_pdf)) },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    isMenuOpen = false
                                    onConvertToPdf()
                                }
                            )
                        }
                        if (onRename != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_rename)) },
                                leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
                                onClick = {
                                    isMenuOpen = false
                                    onRename()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.arc_create)) },
                            leadingIcon = { Icon(Icons.Outlined.FolderZip, contentDescription = null) },
                            onClick = {
                                isMenuOpen = false
                                onCompress()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.photo_move_to_bin_title), color = scheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = scheme.error) },
                            onClick = {
                                isMenuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onCopy, enabled = hasSelection) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_copy))
                }
                TextButton(onClick = onCut, enabled = hasSelection) {
                    Icon(Icons.Outlined.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_move))
                }
                TextButton(onClick = onShare, enabled = hasSelection) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_share))
                }
            }
        }
    }
}

@Composable
fun BreadcrumbRow(
    crumbs: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    // Labels identify navigation changes; callback instances change on recomposition.
    // A new width also reveals the destination after rotation or a text-size change.
    LaunchedEffect(crumbs.map { it.first }, scrollState.maxValue) {
        if (scrollState.maxValue != Int.MAX_VALUE) scrollState.scrollTo(scrollState.maxValue)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        crumbs.forEachIndexed { index, (label, onClick) ->
            val isLast = index == crumbs.size - 1
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isLast) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(AppRadius.microShape)
                    .background(if (isLast) scheme.secondaryContainer else Color.Transparent)
                    .semantics { selected = isLast }
                    .clickable(role = Role.Button, onClick = onClick)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = scheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
