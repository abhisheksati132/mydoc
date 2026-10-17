package com.abhisheksati.mydoc.tools.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.tools.model.formatByteSize
import com.abhisheksati.mydoc.tools.model.CompressionLevel
import com.abhisheksati.mydoc.tools.model.PageMargin
import com.abhisheksati.mydoc.tools.model.PageNumberFormat
import com.abhisheksati.mydoc.tools.model.PageNumberPosition
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolExecutionResult
import com.abhisheksati.mydoc.tools.model.WorkflowPresetType
import com.abhisheksati.mydoc.tools.viewmodel.ToolExecutionUiState
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import java.io.File

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

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

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

    // SAF Save Result Launcher
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(tool.outputMimeType)
    ) { destUri ->
        if (destUri != null) {
            viewModel.saveResultToSaf(
                context = context,
                destinationUri = destUri,
                onSuccess = {
                    Toast.makeText(context, "Saved successfully.", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

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
            // Header: Tool title and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
                    Text(
                        text = tool.title,
                        style = typography.title,
                        color = colors.textPrimary
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.closeTool()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            Text(
                text = tool.description,
                style = typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = AppSpacing.paddingMedium)
            )

            AppDivider()
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // State-based content
            when (val state = executionState) {
                is ToolExecutionUiState.Idle -> {
                    // Input files section
                    InputFilesSection(
                        tool = tool,
                        uris = inputUris,
                        context = context,
                        onPickSingle = {
                            val types = if (tool.acceptsNonPdfInput) arrayOf("*/*") else arrayOf("application/pdf")
                            singlePickerLauncher.launch(types)
                        },
                        onPickMultiple = {
                            val types = if (tool.acceptsNonPdfInput) arrayOf("*/*") else arrayOf("application/pdf")
                            multiplePickerLauncher.launch(types)
                        },
                        onRemoveUri = { viewModel.removeInputUri(it) }
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

                    // Contextual Options Section
                    ToolOptionsSection(
                        tool = tool,
                        options = toolOptions,
                        onOptionsChange = { transform -> viewModel.updateOptions(transform) }
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

                    // Action Button (Min 48dp touch target)
                    val canRun = inputUris.isNotEmpty() || tool == PdfTool.WORKFLOW
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppSpacing.minTouchTarget)
                            .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                            .background(if (canRun) colors.accent else colors.surfaceVariant)
                            .clickable(enabled = canRun) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.runTool()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Run ${tool.title}",
                            style = typography.body,
                            color = if (canRun) colors.background else colors.textSecondary
                        )
                    }
                }

                is ToolExecutionUiState.Running -> {
                    // Running progress state
                    RunningProgressSection(
                        progress = state.progress,
                        onCancel = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.cancelExecution()
                        }
                    )
                }

                is ToolExecutionUiState.Success -> {
                    // Success & Before/After Verification Section
                    SuccessVerificationSection(
                        result = state.result,
                        onSaveSaf = {
                            val defaultName = "${tool.defaultSuffix}_${System.currentTimeMillis()}.${tool.outputExtension}"
                            saveDocumentLauncher.launch(defaultName)
                        },
                        onOpen = {
                            val f = state.result.outputFile
                            if (f != null && f.exists()) {
                                onOpenResultFile(f, tool.outputMimeType)
                            }
                        },
                        onShare = {
                            val f = state.result.outputFile
                            if (f != null && f.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    f
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = tool.outputMimeType
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share result"))
                            }
                        },
                        onDone = {
                            viewModel.closeTool()
                        }
                    )
                }

                is ToolExecutionUiState.Error -> {
                    // Error state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.paddingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                        Text(
                            text = state.message,
                            style = typography.body,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppSpacing.minTouchTarget)
                                .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                                .background(colors.accent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectTool(tool)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Try again",
                                style = typography.body,
                                color = colors.background
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputFilesSection(
    tool: PdfTool,
    uris: List<Uri>,
    context: Context,
    onPickSingle: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveUri: (Uri) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (tool.requiresMultipleInputFiles) "INPUT DOCUMENTS" else "INPUT DOCUMENT",
            style = typography.label,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

        if (uris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget)
                    .border(
                        width = 1.dp,
                        color = colors.divider,
                        shape = RoundedCornerShape(AppSpacing.chipCornerRadius)
                    )
                    .clickable {
                        if (tool.requiresMultipleInputFiles) onPickMultiple() else onPickSingle()
                    }
                    .padding(horizontal = AppSpacing.paddingMedium),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
                    Text(
                        text = if (tool.requiresMultipleInputFiles) "Select 2 or more files" else "Select file to process",
                        style = typography.body,
                        color = colors.accent
                    )
                }
            }
        } else {
            uris.forEach { uri ->
                val fileName = getFileNameFromUri(context, uri) ?: uri.lastPathSegment ?: "document.pdf"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacing.minTouchTarget)
                        .background(colors.surfaceVariant, RoundedCornerShape(AppSpacing.chipCornerRadius))
                        .padding(horizontal = AppSpacing.paddingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMiddleTruncated(fileName, 32),
                        style = typography.body,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onRemoveUri(uri) },
                        modifier = Modifier.size(AppSpacing.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove file",
                            tint = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
            }

            if (tool.requiresMultipleInputFiles) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacing.minTouchTarget)
                        .border(1.dp, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
                        .clickable { onPickMultiple() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add more files",
                        style = typography.body,
                        color = colors.accent
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolOptionsSection(
    tool: PdfTool,
    options: com.abhisheksati.mydoc.tools.model.ToolOptions,
    onOptionsChange: ((com.abhisheksati.mydoc.tools.model.ToolOptions) -> com.abhisheksati.mydoc.tools.model.ToolOptions) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxWidth()) {
        when (tool) {
            PdfTool.COMPRESS -> {
                Text(text = "COMPRESSION LEVEL", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                ) {
                    CompressionLevel.entries.forEach { lvl ->
                        AppFilterChip(
                            label = lvl.label,
                            isSelected = options.compressionLevel == lvl,
                            onClick = { onOptionsChange { it.copy(compressionLevel = lvl) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(
                    text = options.compressionLevel.description,
                    style = typography.caption,
                    color = colors.textSecondary
                )
            }

            PdfTool.ROTATE -> {
                Text(text = "ROTATION ANGLE", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    listOf(90, 180, 270).forEach { deg ->
                        AppFilterChip(
                            label = "$deg°",
                            isSelected = options.rotateDegrees == deg,
                            onClick = { onOptionsChange { it.copy(rotateDegrees = deg) } }
                        )
                    }
                }
            }

            PdfTool.ADD_WATERMARK -> {
                Text(text = "WATERMARK TEXT", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.watermarkText,
                    onValueChange = { t -> onOptionsChange { it.copy(watermarkText = t) } },
                    placeholder = { Text("CONFIDENTIAL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }

            PdfTool.ADD_PAGE_NUMBERS -> {
                Text(text = "POSITION", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                ) {
                    PageNumberPosition.entries.forEach { pos ->
                        AppFilterChip(
                            label = pos.label,
                            isSelected = options.pageNumberPosition == pos,
                            onClick = { onOptionsChange { it.copy(pageNumberPosition = pos) } }
                        )
                    }
                }
            }

            PdfTool.PROTECT -> {
                Text(text = "DOCUMENT PASSWORD", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.userPassword,
                    onValueChange = { p -> onOptionsChange { it.copy(userPassword = p) } },
                    placeholder = { Text("Enter password to open file") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }

            PdfTool.UNLOCK -> {
                Text(text = "CURRENT PASSWORD", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.userPassword,
                    onValueChange = { p -> onOptionsChange { it.copy(userPassword = p) } },
                    placeholder = { Text("Enter password to unlock") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }

            PdfTool.REDACT -> {
                Text(text = "TEXT TO REDACT PERMANENTLY", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.redactQuery,
                    onValueChange = { q -> onOptionsChange { it.copy(redactQuery = q) } },
                    placeholder = { Text("Search word or phrase (e.g. Secret)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }

            PdfTool.WORKFLOW -> {
                Text(text = "PRESET WORKFLOW", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                WorkflowPresetType.entries.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.paddingSmall)
                            .clickable { onOptionsChange { it.copy(workflowPreset = p) } },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppFilterChip(
                            label = p.title,
                            isSelected = options.workflowPreset == p,
                            onClick = { onOptionsChange { it.copy(workflowPreset = p) } }
                        )
                    }
                    Text(text = p.stepsSummary, style = typography.caption, color = colors.accent)
                    Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                }
            }

            PdfTool.IMAGE_TO_PDF -> {
                Text(text = "PAGE SIZE", style = typography.label, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    PageSizeFormat.entries.forEach { fmt ->
                        AppFilterChip(
                            label = fmt.label,
                            isSelected = options.pageSizeFormat == fmt,
                            onClick = { onOptionsChange { it.copy(pageSizeFormat = fmt) } }
                        )
                    }
                }
            }

            else -> {
                // Default options or no additional parameters required
            }
        }
    }
}

@Composable
private fun RunningProgressSection(
    progress: com.abhisheksati.mydoc.tools.model.ToolProgress,
    onCancel: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (progress.isIndeterminate) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                trackColor = colors.surfaceVariant
            )
        } else {
            LinearProgressIndicator(
                progress = { progress.percent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                trackColor = colors.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
        Text(
            text = progress.message.ifEmpty { "Processing document..." },
            style = typography.body,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.minTouchTarget)
                .border(1.dp, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Cancel", style = typography.body, color = colors.textPrimary)
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
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Verification Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
            Text(
                text = "Operation Completed",
                style = typography.title,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

        // Before / After Verification Card (Plain layout, thin border)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceVariant, RoundedCornerShape(AppSpacing.chipCornerRadius))
                .padding(AppSpacing.paddingMedium)
        ) {
            Text(text = "VERIFICATION", style = typography.label, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "File size", style = typography.body, color = colors.textSecondary)
                Text(
                    text = "${formatByteSize(result.inputSizeBytes)} → ${formatByteSize(result.outputSizeBytes)}",
                    style = typography.body,
                    color = colors.textPrimary
                )
            }

            if (result.inputPages > 0) {
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Pages", style = typography.body, color = colors.textSecondary)
                    Text(
                        text = "${result.inputPages} → ${result.outputPages}",
                        style = typography.body,
                        color = colors.textPrimary
                    )
                }
            }

            if (result.warningNote != null) {
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                AppDivider()
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(
                    text = result.warningNote,
                    style = typography.caption,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

        // SAF "Save file" Action (Primary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.minTouchTarget)
                .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                .background(colors.accent)
                .clickable { onSaveSaf() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Save, contentDescription = null, tint = colors.background, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
                Text(text = "Save to device", style = typography.body, color = colors.background)
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

        // Secondary Actions: Open and Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppSpacing.minTouchTarget)
                    .border(1.dp, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
                    .clickable { onOpen() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
                    Text(text = "Open result", style = typography.body, color = colors.textPrimary)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppSpacing.minTouchTarget)
                    .border(1.dp, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
                    .clickable { onShare() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Share, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
                    Text(text = "Share", style = typography.body, color = colors.textPrimary)
                }
            }
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
