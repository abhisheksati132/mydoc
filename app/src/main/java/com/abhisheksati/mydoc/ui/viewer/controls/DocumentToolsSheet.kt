package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.components.horizontalScrollWithFade
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsDark
import com.abhisheksati.mydoc.designsystem.tokens.ToolGroupColors
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup

private data class FilterCategory(
    val group: ToolGroup?,
    val icon: ImageVector,
    val color: Color,
    val label: String
)

/**
 * Contextual bottom sheet displayed when the user triggers Tools from inside the document viewer.
 * Pre-selects and filters tools relevant to the active document (PDF, Word, Excel, PPT, Text).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentToolsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    document: DocumentFile,
    onSelectTool: (PdfTool) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val isDark = LocalIsDark.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<ToolGroup?>(null) }

    val ext = document.extension.lowercase()
    val isPdf = ext == "pdf" || document.category == DocumentCategory.PDF

    // Curate applicable tools for this document
    val applicableTools = remember(isPdf, ext) {
        if (isPdf) {
            // PDF supports all tools except image_to_pdf
            PdfTool.entries.filter { it != PdfTool.IMAGE_TO_PDF }
        } else {
            // Non-PDF documents: offer conversion to PDF first, plus text export
            listOfNotNull(
                if (ext in setOf("docx", "doc", "xlsx", "xls", "pptx", "ppt")) PdfTool.OFFICE_TO_PDF else null,
                if (ext in setOf("txt", "md", "csv", "json", "xml", "log")) PdfTool.TEXT_TO_PDF else null,
                PdfTool.COMPRESS,
                PdfTool.PROTECT
            )
        }
    }

    val filteredTools = remember(applicableTools, searchQuery, selectedGroup) {
        applicableTools.filter { tool ->
            val matchesGroup = selectedGroup == null || tool.group == selectedGroup
            val matchesSearch = searchQuery.isBlank() ||
                tool.title.contains(searchQuery, ignoreCase = true) ||
                tool.description.contains(searchQuery, ignoreCase = true) ||
                tool.group.title.contains(searchQuery, ignoreCase = true)
            matchesGroup && matchesSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppRadius.sheetShape,
        containerColor = colors.surfaceContainerLow,
        tonalElevation = AppSpacing.elevation,
        scrimColor = colors.background.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .background(colors.divider, AppRadius.roundShape)
            )
        },
        modifier = modifier
    ) {
        OverlaySystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Document Tools",
                            style = typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = formatMiddleTruncated(document.displayName, maxChars = 30),
                            style = typography.caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = {
                        onDismiss()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            // Quick Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AppSearchField(
                    query = searchQuery,
                    onQueryChanged = { searchQuery = it },
                    placeholder = "Search actions (Sign, Compress, Protect...)",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Filter Pills
            val categories = listOf(
                FilterCategory(null, Icons.Outlined.AllInclusive, colors.accent, "All"),
                FilterCategory(ToolGroup.EDIT, Icons.Outlined.Edit, ToolGroupColors.edit.tint, "Edit & Sign"),
                FilterCategory(ToolGroup.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, ToolGroupColors.organize.tint, "Organize"),
                FilterCategory(ToolGroup.OPTIMIZE, Icons.Outlined.Compress, ToolGroupColors.optimize.tint, "Optimize"),
                FilterCategory(ToolGroup.CONVERT_FROM, Icons.Outlined.Difference, ToolGroupColors.convertFrom.tint, "Export"),
                FilterCategory(ToolGroup.SECURITY, Icons.Outlined.Security, ToolGroupColors.security.tint, "Security")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScrollWithFade(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (cat in categories) {
                    val isSelected = selectedGroup == cat.group
                    val chipBg = if (isSelected) cat.color.copy(alpha = 0.18f) else colors.surfaceContainer
                    val chipBorder = if (isSelected) cat.color else colors.cardBorder

                    Surface(
                        shape = AppRadius.cardShape,
                        color = chipBg,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, chipBorder),
                        modifier = Modifier
                            .clip(AppRadius.cardShape)
                            .clickable {
                                selectedGroup = cat.group
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.label,
                                tint = if (isSelected) cat.color else cat.color.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.label,
                                style = if (isSelected) typography.captionStrong else typography.caption,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            AppDivider()

            // List of filtered tools
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTools, key = { it.id }) { tool ->
                    DocumentToolCard(
                        tool = tool,
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onSelectTool(tool)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentToolCard(
    tool: PdfTool,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val (groupTint, groupContainer) = when (tool.group) {
        ToolGroup.ORGANIZE -> ToolGroupColors.organize.tint to ToolGroupColors.container(ToolGroupColors.organize, isDark)
        ToolGroup.OPTIMIZE -> ToolGroupColors.optimize.tint to ToolGroupColors.container(ToolGroupColors.optimize, isDark)
        ToolGroup.CONVERT_TO -> ToolGroupColors.convertTo.tint to ToolGroupColors.container(ToolGroupColors.convertTo, isDark)
        ToolGroup.CONVERT_FROM -> ToolGroupColors.convertFrom.tint to ToolGroupColors.container(ToolGroupColors.convertFrom, isDark)
        ToolGroup.EDIT -> ToolGroupColors.edit.tint to ToolGroupColors.container(ToolGroupColors.edit, isDark)
        ToolGroup.SECURITY -> ToolGroupColors.security.tint to ToolGroupColors.container(ToolGroupColors.security, isDark)
        ToolGroup.SCAN -> ToolGroupColors.scan.tint to ToolGroupColors.container(ToolGroupColors.scan, isDark)
    }

    Surface(
        shape = AppRadius.cardShape,
        color = if (isDark) colors.surfaceContainer else colors.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(AppRadius.cardShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(AppRadius.cardShape)
                    .background(groupContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = groupTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tool.title,
                    style = typography.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    style = typography.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
