package com.abhisheksati.mydoc.tools.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Draw
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.GlassCard
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.InterFontFamily
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel

/**
 * Apple-grade Bento Grid Tools Screen featuring Quick Action Hero Tiles and Frosted Glass Rows.
 */
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onToolClick: (PdfTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val haptic = LocalHapticFeedback.current

    var searchQuery by remember { mutableStateOf("") }

    val allTools = remember { PdfTool.entries }
    val filteredTools = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allTools
        } else {
            val q = searchQuery.trim().lowercase()
            allTools.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.group.title.lowercase().contains(q)
            }
        }
    }

    val groupedTools = remember(filteredTools) {
        filteredTools.groupBy { it.group }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Apple frosted search field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.paddingLarge, vertical = AppSpacing.paddingSmall)
        ) {
            AppSearchField(
                query = searchQuery,
                onQueryChanged = { searchQuery = it },
                placeholder = "Search 30 PDF tools...",
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Quick Actions Hero Bento Shelf (visible when not searching)
            if (searchQuery.isBlank()) {
                item(key = "quick_actions_header") {
                    AppSectionHeader(
                        title = "QUICK ACTIONS",
                        modifier = Modifier.padding(top = AppSpacing.paddingSmall)
                    )
                }

                item(key = "quick_actions_bento") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.paddingLarge),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroBentoCard(
                                title = "Scan to PDF",
                                subtitle = "Camera OCR scanner",
                                icon = Icons.Outlined.CameraAlt,
                                gradient = Brush.linearGradient(listOf(Color(0xFF34C759), Color(0xFF30D158))),
                                glowColor = Color(0xFF34C759),
                                onClick = { onToolClick(PdfTool.SCAN_TO_PDF) },
                                modifier = Modifier.weight(1f)
                            )
                            HeroBentoCard(
                                title = "Sign PDF",
                                subtitle = "Touch signature pad",
                                icon = Icons.Outlined.Draw,
                                gradient = Brush.linearGradient(listOf(Color(0xFF5856D6), Color(0xFF7D7AFF))),
                                glowColor = Color(0xFF5856D6),
                                onClick = { onToolClick(PdfTool.SIGN_PDF) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroBentoCard(
                                title = "Merge Files",
                                subtitle = "Combine documents",
                                icon = Icons.AutoMirrored.Outlined.MergeType,
                                gradient = Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF0A84FF))),
                                glowColor = Color(0xFF007AFF),
                                onClick = { onToolClick(PdfTool.MERGE) },
                                modifier = Modifier.weight(1f)
                            )
                            HeroBentoCard(
                                title = "Compress PDF",
                                subtitle = "Reduce file size",
                                icon = Icons.Outlined.Compress,
                                gradient = Brush.linearGradient(listOf(Color(0xFFFF9500), Color(0xFFFFB340))),
                                glowColor = Color(0xFFFF9500),
                                onClick = { onToolClick(PdfTool.COMPRESS) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Categorized Bento Tool Sections
            ToolGroup.entries.forEach { group ->
                val toolsInGroup = groupedTools[group]
                if (!toolsInGroup.isNullOrEmpty()) {
                    item(key = "header_${group.name}") {
                        AppSectionHeader(
                            title = group.title,
                            modifier = Modifier.padding(top = AppSpacing.paddingMedium)
                        )
                    }

                    items(
                        items = toolsInGroup,
                        key = { it.id }
                    ) { tool ->
                        BentoToolRow(
                            tool = tool,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToolClick(tool)
                            },
                            modifier = Modifier.padding(horizontal = AppSpacing.paddingLarge, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2x2 Bento Hero Card for Quick Actions.
 */
@Composable
private fun HeroBentoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    glowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    GlassCard(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(AppSpacing.cardCornerRadius),
        backgroundColor = colors.cardBackground,
        borderColor = colors.glassBorder,
        ambientGlow = glowColor.copy(alpha = 0.22f),
        elevation = 6.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(gradient)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.15f))
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Bento List Tile with Apple Glassmorphic styling.
 */
@Composable
private fun BentoToolRow(
    tool: PdfTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val groupColor = getGroupAccentColor(tool.group)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = colors.cardBackground,
        borderColor = colors.glassBorder,
        elevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle icon badge with subtle ambient drop glow
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .drawBehind {
                        drawCircle(
                            color = groupColor.copy(alpha = 0.25f),
                            radius = size.maxDimension * 0.55f,
                            center = center
                        )
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(groupColor.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = groupColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = groupColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.title,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun getGroupAccentColor(group: ToolGroup): Color {
    return when (group) {
        ToolGroup.ORGANIZE -> Color(0xFF007AFF)      // Apple Blue
        ToolGroup.OPTIMIZE -> Color(0xFFFF9500)      // Apple Orange
        ToolGroup.CONVERT_TO, ToolGroup.CONVERT_FROM -> Color(0xFF34C759) // Apple Green
        ToolGroup.EDIT -> Color(0xFF5856D6)          // Apple Purple
        ToolGroup.SECURITY -> Color(0xFFFF3B30)      // Apple Red
        ToolGroup.SCAN -> Color(0xFF00C7BE)          // Apple Teal
        ToolGroup.AUTOMATION -> Color(0xFFAF52DE)    // Apple Indigo
    }
}
