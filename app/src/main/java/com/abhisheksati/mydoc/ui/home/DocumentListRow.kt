package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing as Spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentListRow(
    document: DocumentFile,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveFromRecents: () -> Unit,
    onShare: () -> Unit,
    onShowDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onShowDetails
                )
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Neutral outlined document type badge (no colorful blobs)
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = document.typeBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            // File Name and Metadata
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Metadata: size • last opened • page
                val metadataParts = buildList {
                    if (document.formattedSize.isNotBlank()) add(document.formattedSize)
                    if (document.formattedLastOpened.isNotBlank()) add(document.formattedLastOpened)
                    if (document.lastPage > 0 && document.pageCount > 0) {
                        add("Page ${document.lastPage + 1}/${document.pageCount}")
                    }
                }

                Text(
                    text = metadataParts.joinToString("  •  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite toggle button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.padding(start = Spacing.extraSmall)
            ) {
                Icon(
                    imageVector = if (document.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.toggle_favorite),
                    tint = if (document.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Overflow menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp
                ) {
                    DropdownMenuItem(
                        text = { Text("Details & Info") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Info, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onShowDetails()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove_from_recents)) },
                        onClick = {
                            menuExpanded = false
                            onRemoveFromRecents()
                        }
                    )
                }
            }
        }

        AppDivider()
    }
}
