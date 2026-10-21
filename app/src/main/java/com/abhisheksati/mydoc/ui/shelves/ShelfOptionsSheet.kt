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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.Shelf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfOptionsSheet(
    shelf: Shelf,
    onDismiss: () -> Unit,
    onUpdateShelf: (Shelf) -> Unit,
    onDeleteShelf: (Long) -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember(shelf.name) { mutableStateOf(shelf.name) }
    var selectedColorIndex by remember(shelf.colorIndex) { mutableIntStateOf(shelf.colorIndex) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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

            // Header: Color swatch and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.getShelfColor(selectedColorIndex).color)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shelf.name,
                        style = typography.title.copy(fontSize = 18.sp),
                        color = colors.ink
                    )
                    Text(
                        text = "${shelf.fileCount} documents on shelf",
                        style = typography.caption.withTabularFigures(),
                        color = colors.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rename Section
            if (isEditingName) {
                Column {
                    Text(
                        text = "SHELF NAME",
                        style = typography.label,
                        color = colors.inkMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(AppSpacing.inputRadius))
                            .background(colors.surfaceMuted)
                            .border(AppSpacing.hairline, colors.line, RoundedCornerShape(AppSpacing.inputRadius))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            singleLine = true,
                            textStyle = typography.body.copy(color = colors.ink, fontWeight = FontWeight.Medium),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                isEditingName = false
                                editedName = shelf.name
                            },
                            shape = RoundedCornerShape(AppSpacing.buttonRadius)
                        ) {
                            Text("Cancel", style = typography.body)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (editedName.isNotBlank()) {
                                    haptics.confirm()
                                    onUpdateShelf(shelf.copy(name = editedName.trim(), colorIndex = selectedColorIndex))
                                    isEditingName = false
                                }
                            },
                            shape = RoundedCornerShape(AppSpacing.buttonRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.ink,
                                contentColor = colors.background
                            )
                        ) {
                            Text("Save", style = typography.body)
                        }
                    }
                }
            } else {
                // Rename button
                Surface(
                    onClick = {
                        haptics.select()
                        isEditingName = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceMuted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Rename shelf",
                            style = typography.body.copy(fontWeight = FontWeight.Medium),
                            color = colors.ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color picker
            Text(
                text = "SHELF COLOR",
                style = typography.label,
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val swatchColor = colors.getShelfColor(i)
                    val isSelected = selectedColorIndex == i
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatchColor.color)
                            .clickable {
                                haptics.select()
                                selectedColorIndex = i
                                onUpdateShelf(shelf.copy(colorIndex = i, name = editedName.trim()))
                            }
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) colors.ink else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = swatchColor.onColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete shelf button
            Surface(
                onClick = {
                    haptics.select()
                    showDeleteConfirmDialog = true
                },
                shape = RoundedCornerShape(14.dp),
                color = colors.error.copy(alpha = 0.08f),
                border = BorderStroke(AppSpacing.hairline, colors.error.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Delete shelf",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Confirmation dialog before deleting shelf
    AppDialog(
        isOpen = showDeleteConfirmDialog,
        title = "Delete shelf '${shelf.name}'?",
        message = "This will remove the shelf category. All documents will remain safe in your library and phone storage.",
        confirmLabel = "Delete shelf",
        dismissLabel = "Cancel",
        onConfirm = {
            showDeleteConfirmDialog = false
            onDeleteShelf(shelf.id)
            onDismiss()
        },
        onDismiss = { showDeleteConfirmDialog = false }
    )
}
