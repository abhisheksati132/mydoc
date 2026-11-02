package com.abhisheksati.mydoc.tools.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.tools.pdf.form.PdfFormService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillFormSheet(
    sourceUri: Uri,
    onDismiss: () -> Unit,
    onSaveValues: (values: Map<String, String>, flatten: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

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

    AppBottomSheet(
        isOpen = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppRadius.insetShape)
                        .background(scheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fill PDF Form",
                        style = typography.titleLarge,
                        color = scheme.onSurface
                    )
                    Text(
                        text = if (isLoading) "Reading interactive fields..." else "${fields.size} editable fields found",
                        style = typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = scheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = scheme.outlineVariant)

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (fields.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(AppRadius.largeShape)
                            .background(scheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = scheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No interactive fields",
                        style = typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This document does not contain digital AcroForm fields. It may be a scanned or flattened document.",
                        style = typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppRadius.cardShape)
                                .background(scheme.surfaceContainerLow)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = scheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Flatten form upon saving",
                                    style = typography.titleSmall,
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = "Permanently bakes fields so values cannot be altered.",
                                    style = typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = flattenForm,
                                onCheckedChange = { flattenForm = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(fields, key = { it.name }) { field ->
                        val currentValue = fieldValues[field.name] ?: ""
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = field.name.ifEmpty { "Field" },
                                style = typography.labelLarge,
                                color = scheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            when (field.type) {
                                PdfFormService.FieldType.CHECKBOX -> {
                                    val isChecked = currentValue.equals("true", ignoreCase = true) || currentValue == "1"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(AppRadius.cardShape)
                                            .background(scheme.surfaceContainerLow)
                                            .clickable {
                                                fieldValues[field.name] = if (isChecked) "false" else "true"
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isChecked) "Checked" else "Unchecked",
                                            style = typography.bodyMedium,
                                            color = scheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = isChecked,
                                            onCheckedChange = { chk ->
                                                fieldValues[field.name] = if (chk) "true" else "false"
                                            }
                                        )
                                    }
                                }

                                PdfFormService.FieldType.CHOICE -> {
                                    var expanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = currentValue,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        )
                                        ExposedDropdownMenu(
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
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                HorizontalDivider(color = scheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveValues(fieldValues.toMap(), flattenForm)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = AppRadius.cardShape
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Form Values")
                    }
                }
            }
        }
    }
}
