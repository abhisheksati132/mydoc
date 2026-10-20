package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.ui.theme.Spacing

@Composable
fun BrightnessDialog(
    isOpen: Boolean,
    currentBrightness: Float,
    onBrightnessChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var isAuto by remember { mutableStateOf(currentBrightness < 0f) }
    var brightnessValue by remember {
        mutableFloatStateOf(if (currentBrightness < 0f) 0.5f else currentBrightness.coerceIn(0.01f, 1f))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.BrightnessMedium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "Screen Brightness Dimmer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dim the display below system limits for comfortable night reading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
                AppDivider()
                Spacer(modifier = Modifier.height(Spacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Match System Brightness",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = isAuto,
                        onCheckedChange = { checked ->
                            isAuto = checked
                            if (checked) {
                                onBrightnessChanged(-1f)
                            } else {
                                onBrightnessChanged(brightnessValue)
                            }
                        }
                    )
                }

                if (!isAuto) {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Level",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(brightnessValue * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    com.abhisheksati.mydoc.designsystem.components.AppSlider(
                        value = brightnessValue,
                        onValueChange = { newValue ->
                            brightnessValue = newValue
                            onBrightnessChanged(newValue)
                        },
                        valueRange = 0.01f..1.0f,
                        thumbSize = 13.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.small))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        FilterChip(
                            selected = (brightnessValue * 100).toInt() == 10,
                            onClick = {
                                brightnessValue = 0.10f
                                onBrightnessChanged(0.10f)
                            },
                            label = { Text("Night 10%") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = (brightnessValue * 100).toInt() == 30,
                            onClick = {
                                brightnessValue = 0.30f
                                onBrightnessChanged(0.30f)
                            },
                            label = { Text("Dim 30%") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = (brightnessValue * 100).toInt() == 70,
                            onClick = {
                                brightnessValue = 0.70f
                                onBrightnessChanged(0.70f)
                            },
                            label = { Text("Day 70%") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
