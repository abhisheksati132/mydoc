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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics

/**
 * New Shelf Creation Bottom Sheet.
 *
 * Rules:
 * - Sheet top corners 32dp.
 * - Name field.
 * - Six colour swatches (44dp, selected = ring).
 * - Optional "Fill from a folder" row with a Choose button.
 * - Primary "Create shelf" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewShelfSheet(
    onDismiss: () -> Unit,
    onCreateShelf: (name: String, colorIndex: Int, folderUri: String?) -> Unit,
    onPickFolder: () -> Unit,
    chosenFolderUri: String? = null,
    chosenFolderName: String? = null
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var shelfName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

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
                .padding(horizontal = AppSpacing.screenPadding)
                .padding(top = 24.dp, bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New shelf",
                    style = typography.title,
                    color = colors.ink
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = colors.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Name Field
            Text(
                text = "SHELF NAME",
                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                if (shelfName.isEmpty()) {
                    Text(
                        text = "e.g. Computer Networks",
                        style = typography.body,
                        color = colors.inkMuted
                    )
                }
                BasicTextField(
                    value = shelfName,
                    onValueChange = { shelfName = it },
                    singleLine = true,
                    textStyle = typography.body.copy(color = colors.ink, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Six Colour Swatches (44dp, selected = ring)
            Text(
                text = "SHELF COLOUR",
                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0..5) {
                    val isSelected = selectedColorIndex == index
                    val shelfColor = colors.getShelfColor(index)

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(shelfColor.color)
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, colors.ink, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                haptics.select()
                                selectedColorIndex = index
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = shelfColor.onColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Optional "Fill from a folder" row
            Text(
                text = "AUTO-IMPORT",
                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(AppSpacing.cardRadius),
                color = colors.surfaceMuted,
                border = BorderStroke(AppSpacing.hairline, colors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = colors.inkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = chosenFolderName ?: "Fill from a folder",
                            style = typography.body.copy(
                                fontWeight = if (chosenFolderName != null) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (chosenFolderName != null) colors.ink else colors.inkMuted,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.select()
                            onPickFolder()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(AppSpacing.hairline, colors.line)
                    ) {
                        Text(
                            text = if (chosenFolderName != null) "Change" else "Choose",
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Primary "Create shelf" button
            Button(
                onClick = {
                    if (shelfName.isNotBlank()) {
                        haptics.confirm()
                        onCreateShelf(shelfName.trim(), selectedColorIndex, chosenFolderUri)
                    }
                },
                enabled = shelfName.isNotBlank(),
                shape = RoundedCornerShape(AppSpacing.cardRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.ink,
                    contentColor = colors.background,
                    disabledContainerColor = colors.surfaceMuted,
                    disabledContentColor = colors.inkMuted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Create shelf",
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
