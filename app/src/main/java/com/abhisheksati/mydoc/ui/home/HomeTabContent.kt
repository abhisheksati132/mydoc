package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.animation.documentHero
import com.abhisheksati.mydoc.designsystem.animation.documentHeroKey
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.components.AuroraBackdrop
import com.abhisheksati.mydoc.designsystem.components.DocumentFilterChips
import com.abhisheksati.mydoc.designsystem.components.EditorialAction
import com.abhisheksati.mydoc.designsystem.components.VividIconTile
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.VividGradients
import com.abhisheksati.mydoc.designsystem.tokens.glow
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.tools.model.PdfTool

/**
 * The home page, vivid premium: an aurora-lit canvas, a glowing gradient hero
 * for scanning, a trio of gradient action tiles, and soft cards with colored
 * format badges for everything else.
 */
@Composable
fun HomeTabContent(
    continueItems: List<LibraryItem>,
    recentItems: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onToolClick: (PdfTool) -> Unit = {},
    onSettingsClick: () -> Unit,
    onToggleTheme: () -> Unit = {},
    onClearRecent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var showClearRecentDialog by remember { mutableStateOf(false) }
    var selectedDocumentFilter by remember { mutableStateOf(DocumentFilter.ALL) }

    // Lazy items do not invalidate on palette changes alone; folding the palette
    // into their keys forces a rebuild when the theme flips.
    val paletteKey = if (AppTheme.isDark) if (AppTheme.isOled) "b" else "d" else "l"

    val filteredRecentItems = remember(recentItems, selectedDocumentFilter) {
        recentItems.filter { selectedDocumentFilter.matches(it.extension) }
    }
    val continueItem = continueItems.firstOrNull()

    if (showClearRecentDialog) {
        AppDialog(
            isOpen = true,
            title = stringResource(R.string.clear_recent_history),
            message = stringResource(R.string.clear_recent_history_desc),
            confirmLabel = stringResource(R.string.clear_cache),
            dismissLabel = "Cancel",
            onConfirm = {
                showClearRecentDialog = false
                onClearRecent()
            },
            onDismiss = { showClearRecentDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AuroraBackdrop(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            AppTabHeader(
                title = "MyDoc",
                titleContent = { GradientWordmark() },
                onSearchClick = onSearchClick,
                onToggleTheme = onToggleTheme,
                onSettingsClick = onSettingsClick
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = AppSpacing.large)
            ) {
                item(key = "hero_$paletteKey") {
                    HeroScanCard(onClick = onScanClick)
                }

                item(key = "action_tiles_$paletteKey") {
                    ActionTiles(onToolClick = onToolClick)
                }

                if (continueItem != null) {
                    item(key = "continue_header_$paletteKey") {
                        AppSectionHeader(title = "Continue reading", showRule = false)
                    }
                    item(key = "continue_row_$paletteKey") {
                        ContinueReadingCard(
                            item = continueItem,
                            onClick = { onItemClick(continueItem) }
                        )
                    }
                }

                item(key = "recent_header_$paletteKey") {
                    AppSectionHeader(
                        title = "Recent documents",
                        showRule = false,
                        action = if (recentItems.isNotEmpty()) {
                            {
                                EditorialAction(
                                    label = "Clear",
                                    onClick = { showClearRecentDialog = true }
                                )
                            }
                        } else null
                    )
                }

                item(key = "filter_chips_$paletteKey") {
                    DocumentFilterChips(
                        selected = selectedDocumentFilter,
                        onSelected = { selectedDocumentFilter = it },
                        modifier = Modifier.padding(
                            start = AppSpacing.screenPadding,
                            end = AppSpacing.screenPadding,
                            bottom = AppSpacing.small
                        )
                    )
                }

                if (filteredRecentItems.isEmpty()) {
                    item(key = "empty_recents_$paletteKey") {
                        if (selectedDocumentFilter == DocumentFilter.ALL) {
                            AppEmptyState(
                                title = "Nothing here yet",
                                message = "Scan a page or open a file and it will be listed here.",
                                actionLabel = "Scan a document",
                                onActionClick = onScanClick
                            )
                        } else {
                            AppEmptyState(
                                title = "No ${selectedDocumentFilter.label} files",
                                message = "This page of the index is empty.",
                                actionLabel = "Show all",
                                onActionClick = { selectedDocumentFilter = DocumentFilter.ALL }
                            )
                        }
                    }
                } else {
                    items(
                        count = filteredRecentItems.size,
                        key = { index -> "${paletteKey}_${filteredRecentItems[index].uri}" }
                    ) { index ->
                        val item = filteredRecentItems[index]
                        FileCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = if (item.uri != continueItem?.uri) {
                                Modifier.documentHero(documentHeroKey(item.uri))
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GradientWordmark() {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "My",
            style = typography.wordmark,
            color = colors.ink
        )
        Text(
            text = "Doc",
            style = typography.wordmark.copy(brush = VividGradients.wordmark(colors)),
            color = Color.Unspecified
        )
    }
}

/**
 * The glowing brand-gradient hero. Decorative light circles give it depth;
 * the frosted camera disc is the affordance.
 */
@Composable
private fun HeroScanCard(onClick: () -> Unit) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val shape = AppRadius.largeShape

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.medium,
                bottom = AppSpacing.small
            )
            .glow(colors.accent, shape, elevation = 18.dp)
            .clip(shape)
            .background(VividGradients.brand)
            .pressScaleEffect(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = size.height * 1.1f,
                center = Offset(size.width * 0.88f, size.height * 0.1f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.09f),
                radius = size.height * 0.7f,
                center = Offset(size.width * 1.02f, size.height * 1.0f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = size.height * 0.9f,
                center = Offset(size.width * 0.02f, size.height * 1.2f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QUICK ACTION",
                    style = typography.overline,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scan a document",
                    style = typography.title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Camera to PDF in seconds",
                    style = typography.caption,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.medium))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Three gradient tiles for the everyday tools. Each owns a hue family, so the
 * row is colorful but every color still means something.
 */
@Composable
private fun ActionTiles(onToolClick: (PdfTool) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.small,
                bottom = AppSpacing.small
            ),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        ActionTile(
            label = "Merge",
            icon = Icons.AutoMirrored.Outlined.MergeType,
            family = VividGradients.Family.OCEAN,
            onClick = { onToolClick(PdfTool.MERGE) },
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            label = "Compress",
            icon = Icons.Outlined.Compress,
            family = VividGradients.Family.GOLD,
            onClick = { onToolClick(PdfTool.COMPRESS) },
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            label = "Sign",
            icon = Icons.Outlined.Draw,
            family = VividGradients.Family.SUNSET,
            onClick = { onToolClick(PdfTool.SIGN_PDF) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    family: VividGradients.Family,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val shape = AppRadius.cardShape

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .pressScaleEffect(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        VividIconTile(
            icon = icon,
            family = family,
            size = 44.dp,
            iconSize = 22.dp,
            shape = AppRadius.cardShape
        )
        Text(
            text = label,
            style = typography.captionStrong,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The document in progress: a soft card with a gradient progress bar —
 * the only moving color in the list.
 */
@Composable
private fun ContinueReadingCard(
    item: LibraryItem,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val shape = AppRadius.cardShape
    val geometryModifier = Modifier.documentHero(documentHeroKey(item.uri))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.xSmall)
            .then(geometryModifier)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .pressScaleEffect(onClick = onClick)
            .padding(AppSpacing.medium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            VividIconTile(
                icon = getDocumentFormatIcon(item.extension),
                family = familyForExtension(item.extension),
                size = 44.dp,
                iconSize = 22.dp,
                shape = AppRadius.cardShape
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatMiddleTruncated(item.name, 40),
                    style = typography.bodyLargeStrong,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = itemMeta(item),
                    style = typography.caption.withTabularFigures(),
                    color = colors.inkMuted,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        if (item.progressFraction > 0f) {
            Spacer(modifier = Modifier.height(AppSpacing.small))
            ProgressGauge(fraction = item.progressFraction)
        }
    }
}

@Composable
private fun ProgressGauge(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(AppRadius.roundShape)
            .background(colors.track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(AppRadius.roundShape)
                .background(VividGradients.brand)
        )
    }
}

/** One recent document: soft card, colored format badge, quiet chevron. */
@Composable
private fun FileCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val shape = AppRadius.cardShape
    val tint = DocumentFormatColors.getTint(item.extension)
    val badgeShape = AppRadius.cardShape

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                bottom = AppSpacing.small
            )
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .pressScaleEffect(onClick = onClick)
            .padding(AppSpacing.small + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(badgeShape)
                .background(tint.copy(alpha = if (AppTheme.isDark) 0.22f else 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getDocumentFormatIcon(item.extension),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatMiddleTruncated(item.name, 40),
                style = typography.bodyStrong,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = itemMeta(item),
                style = typography.caption.withTabularFigures(),
                color = colors.inkMuted,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun familyForExtension(extension: String): VividGradients.Family =
    when (extension.lowercase()) {
        "pdf" -> VividGradients.Family.FIRE
        "doc", "docx", "dot", "dotx", "odt", "rtf" -> VividGradients.Family.OCEAN
        "xls", "xlsx", "xlsm", "csv", "ods" -> VividGradients.Family.FOREST
        "ppt", "pptx", "pps", "odp" -> VividGradients.Family.GOLD
        "jpg", "jpeg", "png", "webp", "gif", "svg" -> VividGradients.Family.SUNSET
        "mp4", "mkv", "mov", "avi" -> VividGradients.Family.SUNSET
        "mp3", "flac", "wav", "m4a", "ogg" -> VividGradients.Family.FOREST
        "zip", "rar", "7z", "tar", "gz" -> VividGradients.Family.GRAPE
        else -> VividGradients.Family.BRAND
    }

private fun itemMeta(item: LibraryItem): String {
    val extension = item.extension.uppercase()
    val percent = (item.progressFraction * 100).toInt()
    return if (percent > 0) {
        "$extension · ${item.formattedSize} · $percent%"
    } else {
        "$extension · ${item.formattedSize}"
    }
}

/**
 * Returns authentic document format icon for the given extension.
 */
private fun getDocumentFormatIcon(extension: String): ImageVector {
    return when (extension.lowercase()) {
        "pdf" -> Icons.Outlined.PictureAsPdf
        "doc", "docx", "dot", "dotx", "odt", "rtf" -> Icons.Outlined.Description
        "xls", "xlsx", "xlsm", "csv", "ods" -> Icons.Outlined.TableChart
        "ppt", "pptx", "pps", "odp" -> Icons.Outlined.Slideshow
        "txt", "log", "md", "json", "xml" -> Icons.AutoMirrored.Outlined.Article
        "jpg", "jpeg", "png", "webp", "gif", "svg" -> Icons.Outlined.Image
        "mp4", "mkv", "mov", "avi" -> Icons.Outlined.Videocam
        "mp3", "flac", "wav", "m4a", "ogg" -> Icons.Outlined.AudioFile
        "zip", "rar", "7z", "tar", "gz" -> Icons.Outlined.FolderZip
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}
