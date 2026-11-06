package com.abhisheksati.mydoc.tools.ui

import com.abhisheksati.mydoc.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolExecutionResult
import com.abhisheksati.mydoc.tools.model.formatByteSize
import com.abhisheksati.mydoc.tools.ui.components.ToolOptionsSection
import com.abhisheksati.mydoc.tools.viewmodel.ToolExecutionUiState
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import java.io.File
import com.abhisheksati.mydoc.designsystem.components.showMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolRunnerSheet(
    viewModel: ToolsViewModel,
    onOpenResultFile: (File, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTool by viewModel.selectedTool.collectAsState()
    val tool = selectedTool ?: return

    val inputUris by viewModel.inputUris.collectAsState()
    val toolOptions by viewModel.toolOptions.collectAsState()
    val executionState by viewModel.executionState.collectAsState()

    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val haptics = rememberAppHaptics()
    val context = LocalContext.current

    var showVisualPageOrganizer by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }

    // SAF Document Pickers
    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.setInputUris(listOf(uri))
        }
    }

    val multiplePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.setInputUris(uris)
        }
    }

    val addMorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { viewModel.addInputUri(it) }
    }

    fun saveResultTo(destUri: Uri?) {
        if (destUri == null) return
        viewModel.saveResultToSaf(
            context = context,
            destinationUri = destUri,
            onSuccess = {
                context.showMessage("Saved successfully.")
            },
            onError = { err ->
                context.showMessage(err, long = true)
            }
        )
    }

    // The result type is decided by the engine (a split may be one PDF or a ZIP,
    // a long-image export is an image), so each type gets its own SAF contract.
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { saveResultTo(it) }
    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { saveResultTo(it) }
    val saveImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/*")
    ) { saveResultTo(it) }
    val saveMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { saveResultTo(it) }
    val saveDocxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { saveResultTo(it) }

    AppBottomSheet(
        isOpen = true,
        onDismiss = { viewModel.closeTool() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = AppSpacing.paddingLarge)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPlate(icon = tool.icon)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tool.title,
                        style = typography.titleLarge,
                        color = scheme.onSurface
                    )
                }

                IconButton(onClick = { viewModel.closeTool() }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = scheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = tool.description,
                style = typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            HorizontalDivider(color = scheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // State-based content
            when (val state = executionState) {
                is ToolExecutionUiState.Idle -> {
                    // Input files section
                    InputFilesSection(
                        tool = tool,
                        uris = inputUris,
                        context = context,
                        onPickSingle = {
                            singlePickerLauncher.launch(pickerTypes(tool))
                        },
                        onPickMultiple = {
                            multiplePickerLauncher.launch(pickerTypes(tool))
                        },
                        onAddMore = {
                            addMorePickerLauncher.launch(pickerTypes(tool))
                        },
                        onRemoveUri = { viewModel.removeInputUri(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contextual Options Section
                    ToolOptionsSection(
                        tool = tool,
                        options = toolOptions,
                        uris = inputUris,
                        onOpenOrganizer = { showVisualPageOrganizer = true },
                        onOpenSignPad = { showSignaturePad = true },
                        onOptionsChange = { transform -> viewModel.updateOptions(transform) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val canRun = when (tool) {
                        PdfTool.EXTRACT_PAGES -> inputUris.isNotEmpty() && toolOptions.pageRange.isNotBlank()
                        PdfTool.REMOVE_PAGES -> inputUris.isNotEmpty() && toolOptions.pageRange.isNotBlank()
                        // Crop applies to every page when the range is left blank - that is what the
                        // field's helper text promises and what the engine does; requiring a range
                        // made the advertised default impossible to run.
                        PdfTool.CROP -> inputUris.isNotEmpty()
                        PdfTool.REDACT -> inputUris.isNotEmpty() && toolOptions.redactQuery.isNotBlank()
                        PdfTool.MERGE -> inputUris.size >= 2
                        PdfTool.PROTECT -> inputUris.isNotEmpty() &&
                            toolOptions.userPassword.isNotBlank() &&
                            toolOptions.userPassword.none { it == ',' || it.isISOControl() } &&
                            toolOptions.ownerPassword.none { it == ',' || it.isISOControl() }
                        else -> inputUris.isNotEmpty()
                    }
                    Button(
                        onClick = {
                            haptics.confirm()
                            viewModel.runTool()
                        },
                        enabled = canRun,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = AppRadius.cardShape
                    ) {
                        Text(text = stringResource(R.string.run_tool, tool.title), style = typography.titleSmall)
                    }
                }

                is ToolExecutionUiState.Running -> {
                    RunningProgressSection(
                        progress = state.progress,
                        onCancel = { viewModel.cancelExecution() }
                    )
                }

                is ToolExecutionUiState.Success -> {
                    val resultFile = state.result.outputFile
                    val resultExt = resultFile?.extension?.lowercase()
                        ?.takeIf { it.isNotBlank() } ?: tool.outputExtension
                    val resultMime = resultFile?.let(::mimeTypeFor) ?: tool.outputMimeType
                    val sourceDocName = inputUris.firstOrNull()?.let { uri ->
                        runCatching {
                            var name: String? = null
                            if (uri.scheme == "content") {
                                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                        if (idx != -1) name = cursor.getString(idx)
                                    }
                                }
                            } else if (uri.scheme == "file") {
                                name = File(uri.path ?: "").name
                            }
                            name ?: uri.lastPathSegment
                        }.getOrNull()
                    }?.substringBeforeLast('.')

                    val defaultName = if (!sourceDocName.isNullOrBlank()) {
                        "${sourceDocName}_${tool.defaultSuffix}.$resultExt"
                    } else {
                        "${tool.defaultSuffix}_${System.currentTimeMillis()}.$resultExt"
                    }

                    SuccessVerificationSection(
                        result = state.result,
                        onSaveSaf = {
                            when (resultExt) {
                                "pdf" -> savePdfLauncher.launch(defaultName)
                                "zip" -> saveZipLauncher.launch(defaultName)
                                "jpg", "jpeg", "png", "webp" -> saveImageLauncher.launch(defaultName)
                                "md", "txt" -> saveMarkdownLauncher.launch(defaultName)
                                "docx" -> saveDocxLauncher.launch(defaultName)
                                else -> savePdfLauncher.launch(defaultName)
                            }
                        },
                        onOpen = {
                            if (resultFile != null && resultFile.exists()) {
                                onOpenResultFile(resultFile, resultMime)
                            }
                        },
                        onShare = {
                            if (resultFile != null && resultFile.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    resultFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = resultMime
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share result"))
                            }
                        },
                        onDone = { viewModel.closeTool() }
                    )
                }

                is ToolExecutionUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = scheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            style = typography.bodyLarge,
                            color = scheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.selectTool(tool) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = AppRadius.cardShape
                        ) {
                            Text(text = stringResource(R.string.action_try_again), style = typography.titleSmall)
                        }
                    }
                }
            }
        }
    }

    if (showVisualPageOrganizer && inputUris.isNotEmpty()) {
        com.abhisheksati.mydoc.tools.ui.components.ReorderPagesSheet(
            pdfUri = inputUris.first(),
            onDismiss = { showVisualPageOrganizer = false },
            onApplyOrder = { newOrder ->
                showVisualPageOrganizer = false
                viewModel.updateOptions { it.copy(reorderedPageIndices = newOrder) }
            }
        )
    }

    if (showSignaturePad) {
        com.abhisheksati.mydoc.tools.ui.components.SignPdfSheet(
            onDismiss = { showSignaturePad = false },
            onSignatureSaved = { showSignaturePad = false }
        )
    }
}

private fun pickerTypes(tool: PdfTool): Array<String> = when (tool) {
    PdfTool.IMAGE_TO_PDF, PdfTool.SCAN_TO_PDF -> arrayOf("image/*")
    PdfTool.TEXT_TO_PDF -> arrayOf("text/*", "text/csv", "application/csv", "application/json")
    else -> if (tool.acceptsNonPdfInput) arrayOf("*/*") else arrayOf("application/pdf")
}

private fun mimeTypeFor(file: File): String = when (file.extension.lowercase()) {
    "pdf" -> "application/pdf"
    "zip" -> "application/zip"
    "md" -> "text/markdown"
    "txt" -> "text/plain"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "webp" -> "image/webp"
    else -> "application/octet-stream"
}

@Composable
private fun IconPlate(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(AppRadius.insetShape)
            .background(scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun InputFilesSection(
    tool: PdfTool,
    uris: List<Uri>,
    context: Context,
    onPickSingle: () -> Unit,
    onPickMultiple: () -> Unit,
    onAddMore: () -> Unit,
    onRemoveUri: (Uri) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = when {
                tool == PdfTool.IMAGE_TO_PDF -> "Input images"
                tool.requiresMultipleInputFiles -> "Input documents"
                else -> "Input document"
            },
            style = typography.labelLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(AppRadius.cardShape)
                    .background(scheme.surfaceContainerHigh)
                    .clickable {
                        if (tool.acceptsMultipleInputFiles) onPickMultiple() else onPickSingle()
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            tool.requiresMultipleInputFiles -> "Select 2 or more files"
                            tool.acceptsMultipleInputFiles -> "Select one or more files"
                            else -> "Select file to process"
                        },
                        style = typography.bodyLarge,
                        color = scheme.onSurface
                    )
                }
            }
        } else {
            uris.forEach { uri ->
                val fileName = getFileNameFromUri(context, uri) ?: uri.lastPathSegment ?: "document.pdf"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacing.minTouchTarget + 8.dp)
                        .clip(AppRadius.cardShape)
                        .background(scheme.surfaceContainerHigh)
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(AppRadius.insetShape)
                                .background(scheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = formatMiddleTruncated(fileName, 32),
                            style = typography.bodyLarge,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    IconButton(onClick = { onRemoveUri(uri) }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.cd_remove_file),
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (tool.acceptsMultipleInputFiles) {
                OutlinedButton(
                    onClick = onAddMore,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppRadius.cardShape
                ) {
                    Text(text = stringResource(R.string.action_add_more_files), style = typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun RunningProgressSection(
    progress: com.abhisheksati.mydoc.tools.model.ToolProgress,
    onCancel: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (progress.isIndeterminate) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(AppRadius.pillShape),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest
            )
        } else {
            LinearProgressIndicator(
                progress = { progress.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(AppRadius.pillShape),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = progress.message.ifEmpty { "Processing document..." },
            style = typography.bodyLarge,
            color = scheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = AppRadius.cardShape
        ) {
            Text(text = stringResource(R.string.action_cancel), style = typography.titleSmall)
        }
    }
}

@Composable
private fun SuccessVerificationSection(
    result: ToolExecutionResult,
    onSaveSaf: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPlate(icon = Icons.Outlined.Check)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.tool_result_completed),
                style = typography.titleLarge,
                color = scheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppRadius.cardShape)
                .background(scheme.surfaceContainerHigh)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.tool_result_verification),
                style = typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.tool_result_file_size), style = typography.bodyMedium, color = scheme.onSurfaceVariant)
                val diffBytes = result.outputSizeBytes - result.inputSizeBytes
                val pct = if (result.inputSizeBytes > 0) {
                    kotlin.math.round((diffBytes.toDouble() / result.inputSizeBytes) * 100.0).toInt()
                } else 0
                val deltaChip = when {
                    pct < 0 -> " ($pct%)"
                    pct > 0 -> " (+$pct%)"
                    else -> ""
                }
                Text(
                    text = "${formatByteSize(result.inputSizeBytes)} → ${formatByteSize(result.outputSizeBytes)}$deltaChip",
                    style = typography.bodyMedium,
                    color = if (pct < 0) scheme.primary else scheme.onSurface
                )
            }

            if (result.inputPages > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.tool_result_pages), style = typography.bodyMedium, color = scheme.onSurfaceVariant)
                    Text(
                        text = "${result.inputPages} → ${result.outputPages}",
                        style = typography.bodyMedium,
                        color = scheme.onSurface
                    )
                }
            }

            if (result.warningNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = scheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.warningNote,
                    style = typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSaveSaf,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = AppRadius.cardShape
        ) {
            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.action_save_to_device), style = typography.titleSmall)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = AppRadius.cardShape,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_open), style = typography.titleSmall)
            }

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = AppRadius.cardShape,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_share), style = typography.titleSmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.material3.TextButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.action_done), style = typography.titleSmall)
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else null
        }
    } catch (_: Exception) {
        null
    }
}
