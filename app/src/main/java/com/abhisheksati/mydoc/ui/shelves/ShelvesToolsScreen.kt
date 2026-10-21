package com.abhisheksati.mydoc.ui.shelves

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.tools.model.PdfTool

data class ToolItem(
    val tool: PdfTool,
    val title: String,
    val description: String,
    val icon: ImageVector
)

enum class ToolGroup(val title: String, val colorIndex: Int) {
    ORGANIZE("Organize", 0),
    OPTIMIZE("Optimize", 1),
    CONVERT("Convert", 2),
    SECURE_AND_SIGN("Secure & sign", 5)
}

val ShelvesToolCatalog: Map<ToolGroup, List<ToolItem>> = mapOf(
    ToolGroup.ORGANIZE to listOf(
        ToolItem(PdfTool.MERGE, "Merge PDF", "Combine multiple documents into one single file.", Icons.Outlined.FolderZip),
        ToolItem(PdfTool.SPLIT, "Split PDF", "Extract page ranges or separate pages.", Icons.Outlined.PictureAsPdf),
        ToolItem(PdfTool.REORDER_PAGES, "Reorder & Rotate", "Rearrange or rotate specific pages.", Icons.Outlined.Transform)
    ),
    ToolGroup.OPTIMIZE to listOf(
        ToolItem(PdfTool.COMPRESS, "Compress PDF", "Reduce file size with lossless optimization.", Icons.Outlined.Compress),
        ToolItem(PdfTool.REPAIR, "Repair PDF", "Rebuild damaged tables and clean corrupt streams.", Icons.Outlined.Build),
        ToolItem(PdfTool.PDF_TO_PDFA, "PDF/A Standardize", "Convert to legal long-term archiving standard.", Icons.Outlined.PictureAsPdf)
    ),
    ToolGroup.CONVERT to listOf(
        ToolItem(PdfTool.IMAGE_TO_PDF, "Images to PDF", "Turn photo scans into a PDF document.", Icons.Outlined.Image),
        ToolItem(PdfTool.PDF_TO_IMAGES, "PDF to Images", "Export pages as crisp JPG or PNG images.", Icons.Outlined.Image),
        ToolItem(PdfTool.PDF_TO_TEXT_MD, "Extract Text", "Pull readable text or markdown from pages.", Icons.Outlined.Description)
    ),
    ToolGroup.SECURE_AND_SIGN to listOf(
        ToolItem(PdfTool.SIGN_PDF, "Sign Document", "Place drawn signatures onto pages.", Icons.Outlined.Draw),
        ToolItem(PdfTool.PROTECT, "Password Protect", "Secure with AES encryption.", Icons.Outlined.Lock),
        ToolItem(PdfTool.UNLOCK, "Remove Password", "Remove password restrictions.", Icons.Outlined.LockOpen),
        ToolItem(PdfTool.STRIP_METADATA, "Strip Metadata", "Delete author and edit history.", Icons.Outlined.VisibilityOff)
    )
)

/**
 * "TOOLS" Destination Screen ("Shelves" Design System).
 *
 * Rules:
 * - Title "Tools".
 * - Card: "For the file you just opened" with file name & 4 suggested actions in a 2x2 grid.
 * - "Scan to PDF" full-width ink-coloured button (72dp, 24dp radius).
 * - "All tools": 2x2 tiles by group (Organize blue, Optimize orange, Convert mint, Secure & sign outlined) with count.
 *   Tapping a tile opens that group as a clean list (icon, name, ONE short line).
 */
@Composable
fun ShelvesToolsScreen(
    recentDoc: DocumentFile?,
    onToolClick: (PdfTool) -> Unit,
    onScanToPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    var activeGroup by remember { mutableStateOf<ToolGroup?>(null) }

    BackHandler(enabled = activeGroup != null) {
        activeGroup = null
    }

    if (activeGroup != null) {
        // Sub-screen: Plain list for the selected tool group
        val group = activeGroup!!
        val groupTools = ShelvesToolCatalog[group] ?: emptyList()
        val groupColor = colors.getShelfColor(group.colorIndex)

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activeGroup = null },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = group.title,
                    style = typography.title,
                    color = colors.ink
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = AppSpacing.screenPadding,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groupTools, key = { it.tool.name }) { item ->
                    Surface(
                        shape = RoundedCornerShape(AppSpacing.cardRadius),
                        color = colors.surface,
                        border = BorderStroke(AppSpacing.hairline, colors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppSpacing.cardRadius))
                            .clickable {
                                haptics.select()
                                onToolClick(item.tool)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(AppSpacing.fileIconSquareRadius))
                                    .background(groupColor.color.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = groupColor.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.ink
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.description,
                                    style = typography.caption,
                                    color = colors.inkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Main Tools Hub
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = 8.dp,
                bottom = 24.dp
            )
        ) {
            // 1. Title "Tools"
            item(key = "tools_title") {
                Text(
                    text = "Tools",
                    style = typography.display,
                    color = colors.ink,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // 2. Card: "For the file you just opened"
            if (recentDoc != null) {
                item(key = "contextual_card") {
                    Surface(
                        shape = RoundedCornerShape(AppSpacing.cardRadius),
                        color = colors.surface,
                        border = BorderStroke(AppSpacing.hairline, colors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "FOR THE FILE YOU JUST OPENED",
                                style = typography.caption.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = colors.inkMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recentDoc.displayName,
                                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2x2 Suggested actions grid
                            val suggestions = listOf(
                                Triple(PdfTool.COMPRESS, "Compress", Icons.Outlined.Compress),
                                Triple(PdfTool.SPLIT, "Extract pages", Icons.Outlined.PictureAsPdf),
                                Triple(PdfTool.REORDER_PAGES, "Reorder", Icons.Outlined.Transform),
                                Triple(PdfTool.PDF_TO_IMAGES, "Save as images", Icons.Outlined.Image)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                suggestions.chunked(2).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        row.forEach { (tool, label, icon) ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = colors.surfaceMuted,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        haptics.select()
                                                        onToolClick(tool)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = colors.ink,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = label,
                                                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                                        color = colors.ink,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. "Scan to PDF" full-width ink-coloured button (72dp, 24dp radius)
            item(key = "scan_button") {
                Button(
                    onClick = {
                        haptics.confirm()
                        onScanToPdf()
                    },
                    shape = RoundedCornerShape(AppSpacing.cardRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.ink,
                        contentColor = colors.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Scan to PDF",
                            style = typography.shelf.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 4. "All tools": 2x2 tiles by group
            item(key = "all_tools_header") {
                Text(
                    text = "ALL TOOLS",
                    style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                    color = colors.inkMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item(key = "tool_groups_grid") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        listOf(ToolGroup.ORGANIZE, ToolGroup.OPTIMIZE),
                        listOf(ToolGroup.CONVERT, ToolGroup.SECURE_AND_SIGN)
                    ).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { group ->
                                val isOutlined = group == ToolGroup.SECURE_AND_SIGN
                                val groupColor = colors.getShelfColor(group.colorIndex)
                                val toolCount = ShelvesToolCatalog[group]?.size ?: 0

                                Surface(
                                    shape = RoundedCornerShape(AppSpacing.shelfTileRadius),
                                    color = if (isOutlined) colors.surfaceMuted else groupColor.color,
                                    border = if (isOutlined) BorderStroke(AppSpacing.hairline, colors.line) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(AppSpacing.shelfTileRadius))
                                        .clickable {
                                            haptics.select()
                                            activeGroup = group
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = group.title,
                                            style = typography.shelf,
                                            color = if (isOutlined) colors.ink else groupColor.onColor
                                        )

                                        val countLabel = if (toolCount == 1) "1 tool" else "$toolCount tools"
                                        Text(
                                            text = countLabel,
                                            style = typography.caption.withTabularFigures(),
                                            color = if (isOutlined) colors.inkMuted else groupColor.onColor.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
