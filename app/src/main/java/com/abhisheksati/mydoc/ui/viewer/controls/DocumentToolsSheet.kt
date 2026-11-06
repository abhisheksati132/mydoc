package com.abhisheksati.mydoc.ui.viewer.controls

import com.abhisheksati.mydoc.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.components.horizontalScrollWithFade
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup

private data class ToolFilter(
    val group: ToolGroup?,
    val icon: ImageVector,
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Only plain-text formats have a working conversion path
            listOfNotNull(
                if (ext in setOf("txt", "md", "csv", "json", "xml", "log")) PdfTool.TEXT_TO_PDF else null
            )
        }
    }

    // Labels are read in composition; the chip list is rebuilt only when they change with the
    // configuration, not on every recomposition.
    val allLabel = stringResource(R.string.tools_filter_all)
    val organizeLabel = stringResource(R.string.tools_filter_organize)
    val editLabel = stringResource(R.string.tools_filter_edit_sign)
    val compressLabel = stringResource(R.string.tools_filter_compress)
    val convertLabel = stringResource(R.string.tools_filter_convert)
    val secureLabel = stringResource(R.string.tools_filter_secure)
    val filters = remember(allLabel, organizeLabel, editLabel, compressLabel, convertLabel, secureLabel) {
        listOf(
            ToolFilter(null, Icons.Outlined.AllInclusive, allLabel),
            ToolFilter(ToolGroup.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, organizeLabel),
            ToolFilter(ToolGroup.EDIT_SIGN, Icons.Outlined.Edit, editLabel),
            ToolFilter(ToolGroup.COMPRESS, Icons.Outlined.Compress, compressLabel),
            ToolFilter(ToolGroup.CONVERT, Icons.Outlined.Difference, convertLabel),
            ToolFilter(ToolGroup.SECURE, Icons.Outlined.Security, secureLabel)
        )
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        OverlaySystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.tools_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = formatMiddleTruncated(document.displayName, maxChars = 40),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.tools_search_actions)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScrollWithFade(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (filter in filters) {
                    FilterChip(
                        selected = selectedGroup == filter.group,
                        onClick = { selectedGroup = filter.group },
                        label = { Text(filter.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .padding(top = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredTools, key = { it.id }) { tool ->
                    ToolRow(
                        tool = tool,
                        onClick = {
                            onDismiss()
                            onSelectTool(tool)
                        }
                    )
                }
                if (filteredTools.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.tools_none_for_format),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolRow(
    tool: PdfTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
