package com.abhisheksati.mydoc.ui.shelves

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToShelfSheet(
    document: DocumentFile,
    shelves: List<Shelf>,
    onDismiss: () -> Unit,
    onAssignToShelf: (shelfId: Long) -> Unit,
    onUnassignFromShelf: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = AppSpacing.sheetTopRadius,
            topEnd = AppSpacing.sheetTopRadius
        ),
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Drag indicator handle
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.inkMuted.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Move to Shelf",
                style = typography.title.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                color = colors.ink
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = document.displayName,
                style = typography.caption,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Option: Unfiled (Remove from shelf)
                item {
                    val isUnfiled = document.shelfId == null
                    Surface(
                        onClick = {
                            haptics.select()
                            onUnassignFromShelf()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isUnfiled) colors.surfaceMuted else Color.Transparent,
                        border = BorderStroke(
                            AppSpacing.hairline,
                            if (isUnfiled) colors.ink.copy(alpha = 0.3f) else colors.line
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOff,
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unfiled",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.ink
                                )
                                Text(
                                    text = "Keep in general library without shelf",
                                    style = typography.caption,
                                    color = colors.inkMuted
                                )
                            }

                            if (isUnfiled) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Currently unfiled",
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Options: All existing shelves
                items(shelves, key = { it.id }) { shelf ->
                    val isAssigned = document.shelfId == shelf.id
                    val shelfColor = colors.getShelfColor(shelf.colorIndex)

                    Surface(
                        onClick = {
                            haptics.select()
                            onAssignToShelf(shelf.id)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isAssigned) shelfColor.color.copy(alpha = 0.12f) else Color.Transparent,
                        border = BorderStroke(
                            if (isAssigned) 1.5.dp else AppSpacing.hairline,
                            if (isAssigned) shelfColor.color else colors.line
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(shelfColor.color)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shelf.name,
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.ink
                                )
                                Text(
                                    text = "${shelf.fileCount} documents",
                                    style = typography.caption.withTabularFigures(),
                                    color = colors.inkMuted
                                )
                            }

                            if (isAssigned) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Assigned",
                                    tint = shelfColor.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
