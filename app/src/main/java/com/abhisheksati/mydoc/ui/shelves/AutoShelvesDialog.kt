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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures

data class DetectedFolder(
    val name: String,
    val path: String,
    val uri: String,
    val fileCount: Int,
    val colorIndex: Int,
    val isRecommended: Boolean = true
)

/**
 * Auto-Shelves Dialog.
 *
 * Rules:
 * - "We found N folders."
 * - One row per detected folder: colour square, folder name, "N files · path", round checkbox.
 * - Primary "Create N shelves", text button "Make one myself".
 */
@Composable
fun AutoShelvesDialog(
    folders: List<DetectedFolder>,
    onConfirm: (List<DetectedFolder>) -> Unit,
    onMakeMyself: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    // Pre-select recommended folders (exclude camera / screenshots)
    val selectionMap = remember(folders) {
        mutableStateMapOf<String, Boolean>().apply {
            folders.forEach { folder ->
                this[folder.uri] = folder.isRecommended
            }
        }
    }

    val selectedCount = selectionMap.values.count { it }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(AppSpacing.cardRadius),
            color = colors.surface,
            border = BorderStroke(AppSpacing.hairline, colors.line),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "We found ${folders.size} folders",
                    style = typography.title,
                    color = colors.ink
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "MyDoc detected these course folders. Select which ones to turn into shelves:",
                    style = typography.body,
                    color = colors.inkMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders, key = { it.uri }) { folder ->
                        val isChecked = selectionMap[folder.uri] == true
                        val shelfColor = colors.getShelfColor(folder.colorIndex)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceMuted)
                                .clickable {
                                    haptics.select()
                                    selectionMap[folder.uri] = !isChecked
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Colour square (14dp radius)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(AppSpacing.fileIconSquareRadius))
                                    .background(shelfColor.color)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val countLabel = if (folder.fileCount == 1) "1 file" else "${folder.fileCount} files"
                                Text(
                                    text = "$countLabel · ${folder.path}",
                                    style = typography.caption.withTabularFigures(),
                                    color = colors.inkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Round checkbox
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) colors.ink else Color.Transparent)
                                    .border(
                                        width = if (isChecked) 0.dp else AppSpacing.hairline,
                                        color = if (isChecked) Color.Transparent else colors.line,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = colors.background,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary "Create N shelves"
                Button(
                    onClick = {
                        val chosen = folders.filter { selectionMap[it.uri] == true }
                        haptics.confirm()
                        onConfirm(chosen)
                    },
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(AppSpacing.cardRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.ink,
                        contentColor = colors.background,
                        disabledContainerColor = colors.surfaceMuted,
                        disabledContentColor = colors.inkMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    val btnLabel = if (selectedCount == 1) "Create 1 shelf" else "Create $selectedCount shelves"
                    Text(
                        text = btnLabel,
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Text Button "Make one myself"
                TextButton(
                    onClick = {
                        haptics.select()
                        onMakeMyself()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Make one myself",
                        style = typography.body.copy(fontWeight = FontWeight.Medium),
                        color = colors.inkMuted
                    )
                }
            }
        }
    }
}
