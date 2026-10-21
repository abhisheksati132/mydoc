package com.abhisheksati.mydoc.ui.shelves

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.Shelf

/**
 * "NOW" Destination Screen ("Shelves" Design System).
 *
 * Rules:
 * - Top row: wordmark "mydoc" (lowercase, 22sp bold) left, search icon button right.
 * - Title "Your shelves" (display style: 38sp, weight 600, -1.4sp).
 * - Hero card (28dp radius, filled with the shelf colour of the file being resumed, 196dp tall):
 *   shelf name + "resume", file title (28sp), thin progress bar, "Page X of Y", and a 52dp circular arrow button.
 * - If nothing to resume, show FirstRunShelvesHero.
 * - Shelf grid (2 columns, 10dp gap): coloured shelf tiles (124dp tall) with shelf name & count.
 *   Outlined "Unfiled" tile and "+ New shelf" tile.
 * - "Recent" list: shelf-colour dot, file name, "TYPE · when".
 */
@Composable
fun NowScreen(
    shelves: List<Shelf>,
    continueDoc: DocumentFile?,
    recentDocs: List<DocumentFile>,
    unfiledCount: Int,
    onDocumentClick: (DocumentFile) -> Unit,
    onShelfClick: (Shelf) -> Unit,
    onUnfiledClick: () -> Unit,
    onNewShelfClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChooseFolder: () -> Unit,
    onOpenSingleFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(
            start = AppSpacing.screenPadding,
            end = AppSpacing.screenPadding,
            top = 16.dp,
            bottom = AppSpacing.navBarHeight + 36.dp
        )
    ) {
        // 1. Top Bar: Wordmark "mydoc" & Search Icon
        item(key = "now_top_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "mydoc",
                    style = typography.heading.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colors.ink
                )

                IconButton(
                    onClick = {
                        haptics.select()
                        onSearchClick()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search documents",
                        tint = colors.ink
                    )
                }
            }
        }

        // 2. Title "Your shelves"
        item(key = "now_title") {
            Text(
                text = "Your shelves",
                style = typography.display,
                color = colors.ink,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
            )
        }

        // 3. Hero Card OR First Run State
        if (continueDoc != null) {
            item(key = "resume_hero_card") {
                val shelfColor = colors.getShelfColor(continueDoc.shelfColorIndex ?: 0)
                val heroBg = shelfColor.color
                val heroText = shelfColor.onColor

                Surface(
                    shape = RoundedCornerShape(AppSpacing.heroCardRadius),
                    color = heroBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .clip(RoundedCornerShape(AppSpacing.heroCardRadius))
                        .clickable {
                            haptics.select()
                            onDocumentClick(continueDoc)
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top label: SHELF NAME · RESUME
                        val shelfLabel = continueDoc.shelfName?.uppercase() ?: "DOCUMENT"
                        Text(
                            text = "$shelfLabel · RESUME",
                            style = typography.caption.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = heroText.copy(alpha = 0.82f)
                        )

                        // Middle: File Title (28sp bold)
                        Text(
                            text = continueDoc.displayName,
                            style = typography.heading.copy(
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = heroText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Bottom row: Progress & Circular Arrow
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                val progress = if (continueDoc.pageCount > 0) {
                                    (continueDoc.lastPage.toFloat() / continueDoc.pageCount).coerceIn(0f, 1f)
                                } else 0.1f

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = heroText,
                                    trackColor = heroText.copy(alpha = 0.25f)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                val pageLabel = if (continueDoc.pageCount > 0) {
                                    "Page ${continueDoc.lastPage + 1} of ${continueDoc.pageCount}"
                                } else "Continue reading"

                                Text(
                                    text = pageLabel,
                                    style = typography.caption.withTabularFigures(),
                                    color = heroText.copy(alpha = 0.85f)
                                )
                            }

                            // 52dp circular action button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(heroText),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Resume reading",
                                    tint = heroBg,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        } else if (shelves.isEmpty() && recentDocs.isEmpty()) {
            item(key = "first_run_hero") {
                FirstRunShelvesHero(
                    onChooseFolder = onChooseFolder,
                    onOpenSingleFile = onOpenSingleFile
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // 4. Shelves 2-Column Grid
        if (shelves.isNotEmpty() || continueDoc != null) {
            item(key = "shelves_grid_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Pair shelves into rows of 2
                    val chunked = shelves.chunked(2)
                    for (row in chunked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (shelf in row) {
                                val shelfColor = colors.getShelfColor(shelf.colorIndex)
                                Surface(
                                    shape = RoundedCornerShape(AppSpacing.shelfTileRadius),
                                    color = shelfColor.color,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(124.dp)
                                        .clip(RoundedCornerShape(AppSpacing.shelfTileRadius))
                                        .clickable {
                                            haptics.select()
                                            onShelfClick(shelf)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = shelf.name,
                                            style = typography.shelf,
                                            color = shelfColor.onColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val countText = if (shelf.fileCount == 1) "1 file" else "${shelf.fileCount} files"
                                        Text(
                                            text = countText,
                                            style = typography.body.withTabularFigures(),
                                            color = shelfColor.onColor.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Row with Unfiled tile & New Shelf tile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Unfiled tile
                        Surface(
                            shape = RoundedCornerShape(AppSpacing.shelfTileRadius),
                            color = colors.surfaceMuted,
                            border = BorderStroke(AppSpacing.hairline, colors.line),
                            modifier = Modifier
                                .weight(1f)
                                .height(124.dp)
                                .clip(RoundedCornerShape(AppSpacing.shelfTileRadius))
                                .clickable {
                                    haptics.select()
                                    onUnfiledClick()
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Unfiled",
                                    style = typography.shelf,
                                    color = colors.ink
                                )

                                val unfiledText = if (unfiledCount == 1) "1 file" else "$unfiledCount files"
                                Text(
                                    text = unfiledText,
                                    style = typography.body.withTabularFigures(),
                                    color = colors.inkMuted
                                )
                            }
                        }

                        // Dashed New Shelf tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(124.dp)
                                .clip(RoundedCornerShape(AppSpacing.shelfTileRadius))
                                .border(
                                    width = AppSpacing.hairline,
                                    color = colors.line,
                                    shape = RoundedCornerShape(AppSpacing.shelfTileRadius)
                                )
                                .clickable {
                                    haptics.select()
                                    onNewShelfClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "New shelf",
                                    tint = colors.inkMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "New shelf",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.inkMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // 5. Recent List
        if (recentDocs.isNotEmpty()) {
            item(key = "recent_header") {
                Text(
                    text = "RECENT",
                    style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = colors.inkMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(
                items = recentDocs.take(10),
                key = { "recent_${it.uri}" }
            ) { doc ->
                val shelfColor = colors.getShelfColor(doc.shelfColorIndex ?: 0)
                val dotColor = if (doc.shelfId != null) shelfColor.color else colors.line

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppSpacing.cardRadius))
                        .clickable {
                            haptics.select()
                            onDocumentClick(doc)
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shelf-colour 8dp dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.displayName,
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val shelfTag = doc.shelfName?.let { "$it · " } ?: ""
                        Text(
                            text = "$shelfTag${doc.typeBadge} · ${doc.formattedLastOpened}".trim(),
                            style = typography.caption.withTabularFigures(),
                            color = colors.inkMuted
                        )
                    }
                }

                // 1dp hairline divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacing.hairline)
                        .background(colors.line.copy(alpha = 0.7f))
                )
            }
        }
    }
}
