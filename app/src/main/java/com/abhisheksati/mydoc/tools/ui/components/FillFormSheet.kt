package com.abhisheksati.mydoc.tools.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.tools.pdf.form.PdfFormService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FillFormSheet(
    sourceUri: Uri,
    onDismiss: () -> Unit,
    onSaveValues: (values: Map<String, String>, flatten: Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    var isLoading by remember { mutableStateOf(true) }
    var fields by remember { mutableStateOf<List<PdfFormService.FormField>>(emptyList()) }
    val fieldValues = remember { mutableStateMapOf<String, String>() }
    var flattenForm by remember { mutableStateOf(false) }

    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            val loaded = runCatching {
                PdfFormService.readFields(context, sourceUri)
            }.getOrDefault(emptyList())
            fields = loaded
            loaded.forEach { field ->
                fieldValues[field.name] = field.value
            }
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = AppSpacing.paddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fill PDF Form",
                        style = typography.title,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isLoading) "Reading interactive fields..." else "${fields.size} editable fields found",
                        style = typography.caption,
                        color = colors.textSecondary
                    )
                }
            }

            AppDivider()

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (fields.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(AppSpacing.paddingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Interactive Fields Detected",
                            style = typography.title,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This document does not contain digital AcroForm fields. It may be a scanned or flattened document.",
                            style = typography.body,
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AppSpacing.paddingMedium)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                        // Flatten option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                                .background(colors.surfaceVariant)
                                .padding(AppSpacing.paddingMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.accent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Flatten form upon saving",
                                    style = typography.body,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Permanently bakes fields so values cannot be altered.",
                                    style = typography.caption,
                                    color = colors.textSecondary
                                )
                            }
                            Switch(
                                checked = flattenForm,
                                onCheckedChange = { flattenForm = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = colors.accent
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                    }

                    items(fields, key = { it.name }) { field ->
                        val currentValue = fieldValues[field.name] ?: ""
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.paddingSmall)
                        ) {
                            Text(
                                text = field.name.ifEmpty { "Field" },
                                style = typography.label,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            when (field.type) {
                                PdfFormService.FieldType.CHECKBOX -> {
                                    val isChecked = currentValue.equals("true", ignoreCase = true) || currentValue == "1"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.surfaceVariant)
                                            .clickable {
                                                fieldValues[field.name] = if (isChecked) "false" else "true"
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isChecked) "Checked" else "Unchecked",
                                            style = typography.body,
                                            color = colors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = isChecked,
                                            onCheckedChange = { chk ->
                                                fieldValues[field.name] = if (chk) "true" else "false"
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.Black,
                                                checkedTrackColor = colors.accent
                                            )
                                        )
                                    }
                                }

                                PdfFormService.FieldType.CHOICE -> {
                                    var expanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedTextField(
                                            value = currentValue,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expanded = true },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = colors.accent,
                                                unfocusedBorderColor = colors.divider,
                                                focusedTextColor = colors.textPrimary,
                                                unfocusedTextColor = colors.textPrimary
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            field.options.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = { Text(opt) },
                                                    onClick = {
                                                        fieldValues[field.name] = opt
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                PdfFormService.FieldType.TEXT -> {
                                    OutlinedTextField(
                                        value = currentValue,
                                        onValueChange = { newVal ->
                                            fieldValues[field.name] = newVal
                                        },
                                        singleLine = false,
                                        maxLines = 4,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.accent,
                                            unfocusedBorderColor = colors.divider,
                                            focusedTextColor = colors.textPrimary,
                                            unfocusedTextColor = colors.textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom apply bar
                AppDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.paddingMedium),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                            .background(colors.accent)
                            .clickable {
                                onSaveValues(fieldValues.toMap(), flattenForm)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apply Form Values",
                                style = typography.body,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
