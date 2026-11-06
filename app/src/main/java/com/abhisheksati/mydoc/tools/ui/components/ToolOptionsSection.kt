package com.abhisheksati.mydoc.tools.ui.components

import com.abhisheksati.mydoc.R
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.horizontalScrollWithFade
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.tools.model.CompressionLevel
import com.abhisheksati.mydoc.tools.model.PageNumberPosition
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.security.SignatureStatus
import com.abhisheksati.mydoc.tools.security.SignatureStorage

@Composable
internal fun ToolOptionsSection(
    tool: PdfTool,
    options: com.abhisheksati.mydoc.tools.model.ToolOptions,
    uris: List<Uri>,
    onOpenOrganizer: () -> Unit,
    onOpenSignPad: () -> Unit = {},
    onOptionsChange: ((com.abhisheksati.mydoc.tools.model.ToolOptions) -> com.abhisheksati.mydoc.tools.model.ToolOptions) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (tool) {
            PdfTool.COMPRESS -> {
                Text(text = stringResource(R.string.opt_compression_level), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScrollWithFade(rememberScrollState()),
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PdfTool.ROTATE -> {
                Text(text = stringResource(R.string.opt_rotation_angle), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    listOf(90, 180, 270).forEach { deg ->
                        AppFilterChip(
                            label = stringResource(R.string.opt_rotation_degrees, deg),
                            isSelected = options.rotateDegrees == deg,
                            onClick = { onOptionsChange { it.copy(rotateDegrees = deg) } }
                        )
                    }
                }
            }

            PdfTool.ADD_WATERMARK -> {
                Text(text = stringResource(R.string.opt_watermark_text), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.watermarkText,
                    onValueChange = { t -> onOptionsChange { it.copy(watermarkText = t) } },
                    placeholder = { Text(stringResource(R.string.opt_watermark_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                Text(text = stringResource(R.string.opt_rotation), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScrollWithFade(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                ) {
                    val angles = listOf(-45f to "Diagonal (-45°)", 0f to "Horizontal (0°)", 45f to "Diagonal (+45°)", 90f to "Vertical (90°)")
                    angles.forEach { (angle, label) ->
                        AppFilterChip(
                            label = label,
                            isSelected = options.watermarkRotation == angle,
                            onClick = { onOptionsChange { it.copy(watermarkRotation = angle) } }
                        )
                    }
                }
            }

            PdfTool.ADD_PAGE_NUMBERS -> {
                Text(text = stringResource(R.string.opt_position), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScrollWithFade(rememberScrollState()),
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
                val passwordHasInvalidChar = options.userPassword.any { it == ',' || it.isISOControl() }
                Text(text = stringResource(R.string.opt_document_password), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.userPassword,
                    onValueChange = { p -> onOptionsChange { it.copy(userPassword = p) } },
                    // A password typed into a plain text field is readable over a shoulder and
                    // lands in every screenshot of the sheet.
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text(stringResource(R.string.opt_password_open_hint)) },
                    singleLine = true,
                    isError = passwordHasInvalidChar,
                    supportingText = {
                        Text(if (passwordHasInvalidChar) "Commas are not allowed" else "Required")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                Text(text = stringResource(R.string.opt_permissions), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.opt_allow_print), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = options.allowPrinting,
                        onCheckedChange = { allowed -> onOptionsChange { it.copy(allowPrinting = allowed) } }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.opt_allow_copy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = options.allowCopying,
                        onCheckedChange = { allowed -> onOptionsChange { it.copy(allowCopying = allowed) } }
                    )
                }
                Text(
                    text = stringResource(R.string.opt_permissions_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = AppSpacing.paddingSmall)
                )
            }

            PdfTool.UNLOCK -> {
                Text(text = stringResource(R.string.opt_current_password), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.userPassword,
                    onValueChange = { p -> onOptionsChange { it.copy(userPassword = p) } },
                    // A password typed into a plain text field is readable over a shoulder and
                    // lands in every screenshot of the sheet.
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text(stringResource(R.string.opt_password_unlock_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.REDACT -> {
                Text(text = stringResource(R.string.opt_redact_text), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.redactQuery,
                    onValueChange = { q -> onOptionsChange { it.copy(redactQuery = q) } },
                    placeholder = { Text(stringResource(R.string.opt_redact_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.IMAGE_TO_PDF -> {
                Text(text = stringResource(R.string.opt_page_size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            PdfTool.REORDER_PAGES -> {
                Text(text = stringResource(R.string.opt_visual_organizer), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                if (uris.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .clickable { onOpenOrganizer() }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.opt_open_organizer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Normal)
                    }
                    if (options.reorderedPageIndices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.opt_organized_order,
                                options.reorderedPageIndices.map { it + 1 }.joinToString(", ")
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(stringResource(R.string.opt_organizer_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            PdfTool.PDF_TO_IMAGES -> {
                Text(text = stringResource(R.string.opt_export_format), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    AppFilterChip(
                        label = stringResource(R.string.opt_zip_images),
                        isSelected = !options.exportAsLongImage,
                        onClick = { onOptionsChange { it.copy(exportAsLongImage = false) } }
                    )
                    AppFilterChip(
                        label = stringResource(R.string.opt_single_long_image),
                        isSelected = options.exportAsLongImage,
                        onClick = { onOptionsChange { it.copy(exportAsLongImage = true) } }
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    AppFilterChip(
                        label = stringResource(R.string.opt_jpeg),
                        isSelected = options.exportFormatJpg,
                        onClick = { onOptionsChange { it.copy(exportFormatJpg = true) } }
                    )
                    AppFilterChip(
                        label = stringResource(R.string.opt_png),
                        isSelected = !options.exportFormatJpg,
                        onClick = { onOptionsChange { it.copy(exportFormatJpg = false) } }
                    )
                }
            }

            PdfTool.ANNOTATE -> {
                Text(text = stringResource(R.string.opt_annotation_mode), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    AppFilterChip(
                        label = stringResource(R.string.opt_stamp_badge),
                        isSelected = options.annotationType.equals("STAMP", ignoreCase = true),
                        onClick = { onOptionsChange { it.copy(annotationType = "STAMP") } }
                    )
                    AppFilterChip(
                        label = stringResource(R.string.opt_highlight_keyword),
                        isSelected = options.annotationType.equals("HIGHLIGHT", ignoreCase = true),
                        onClick = { onOptionsChange { it.copy(annotationType = "HIGHLIGHT") } }
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                if (options.annotationType.equals("STAMP", ignoreCase = true)) {
                    Text(text = stringResource(R.string.opt_preset_stamps), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScrollWithFade(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                    ) {
                        listOf("APPROVED", "CONFIDENTIAL", "REVIEWED", "DRAFT", "REJECTED").forEach { preset ->
                            AppFilterChip(
                                label = preset,
                                isSelected = options.annotationText == preset,
                                onClick = { onOptionsChange { it.copy(annotationText = preset) } }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                }
                Text(
                    text = if (options.annotationType.equals("STAMP", ignoreCase = true)) "STAMP TEXT" else "KEYWORD TO HIGHLIGHT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.annotationText,
                    onValueChange = { t -> onOptionsChange { it.copy(annotationText = t) } },
                    placeholder = { Text(if (options.annotationType.equals("STAMP", ignoreCase = true)) "APPROVED" else "Search keyword") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.SIGN_PDF -> {
                val signatureStatus = SignatureStorage.status(LocalContext.current)
                val hasSignature = signatureStatus == SignatureStatus.READY
                Text(text = stringResource(R.string.opt_signature_stamp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = stringResource(R.string.opt_signature_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (hasSignature) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .border(
                            width = 1.dp,
                            color = if (hasSignature) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { onOpenSignPad() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Draw,
                        contentDescription = null,
                        tint = if (hasSignature) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (signatureStatus) {
                            SignatureStatus.READY -> "Edit / Redraw Signature"
                            SignatureStatus.UNREADABLE -> "Saved signature can't be opened - redraw it"
                            SignatureStatus.NONE -> stringResource(R.string.opt_draw_signature)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasSignature) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(text = stringResource(R.string.opt_placement_page), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    // "Last page" is a choice, not page 999: a document with 1000 pages would have
                    // signed its 1000th and called it the last.
                    listOf("First Page" to 0, "Last Page" to -1).forEach { (label, pg) ->
                        AppFilterChip(
                            label = label,
                            isSelected = options.signaturePage == pg,
                            onClick = { onOptionsChange { it.copy(signaturePage = pg) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(text = stringResource(R.string.opt_position), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    // PDF y grows upwards, so a bottom preset is a small fraction, not a large one.
                    listOf("Bottom Right" to Pair(0.75f, 0.15f), "Bottom Center" to Pair(0.5f, 0.15f), "Bottom Left" to Pair(0.25f, 0.15f)).forEach { (label, coords) ->
                        val isSel = kotlin.math.abs(options.signatureXPercent - coords.first) < 0.05f && kotlin.math.abs(options.signatureYPercent - coords.second) < 0.05f
                        AppFilterChip(
                            label = label,
                            isSelected = isSel,
                            onClick = {
                                onOptionsChange {
                                    it.copy(signatureXPercent = coords.first, signatureYPercent = coords.second)
                                }
                            }
                        )
                    }
                }
            }

            PdfTool.EXTRACT_PAGES -> {
                Text(text = stringResource(R.string.opt_range_extract), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.pageRange,
                    onValueChange = { r -> onOptionsChange { it.copy(pageRange = r) } },
                    placeholder = { Text(stringResource(R.string.opt_range_extract_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(
                    text = stringResource(R.string.opt_range_extract_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PdfTool.REMOVE_PAGES -> {
                Text(text = stringResource(R.string.opt_pages_remove), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.pageRange,
                    onValueChange = { r -> onOptionsChange { it.copy(pageRange = r) } },
                    placeholder = { Text(stringResource(R.string.opt_pages_remove_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(
                    text = stringResource(R.string.opt_pages_remove_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PdfTool.SPLIT -> {
                Text(text = stringResource(R.string.opt_split_every), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    listOf(1, 2, 5, 10).forEach { n ->
                        AppFilterChip(
                            label = pluralStringResource(R.plurals.opt_split_pages, n, n),
                            isSelected = options.splitEveryNPages == n && options.pageRange.isBlank(),
                            onClick = {
                                onOptionsChange {
                                    it.copy(splitEveryNPages = n, pageRange = "")
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                Text(text = stringResource(R.string.opt_custom_range), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.pageRange,
                    onValueChange = { r -> onOptionsChange { it.copy(pageRange = r) } },
                    placeholder = { Text(stringResource(R.string.opt_custom_range_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.CROP -> {
                Text(text = stringResource(R.string.opt_crop_margin), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    listOf("5%" to 0.05f, "10%" to 0.10f, "15%" to 0.15f, "20%" to 0.20f).forEach { (lbl, frac) ->
                        AppFilterChip(
                            label = lbl,
                            isSelected = kotlin.math.abs(options.cropMarginPercent - frac) < 0.01f,
                            onClick = { onOptionsChange { it.copy(cropMarginPercent = frac) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                Text(text = stringResource(R.string.opt_crop_range), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.pageRange,
                    onValueChange = { r -> onOptionsChange { it.copy(pageRange = r) } },
                    placeholder = { Text(stringResource(R.string.opt_crop_range_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.MERGE -> {
                if (uris.size < 2) {
                    Text(
                        text = stringResource(R.string.opt_merge_need_two),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.opt_merge_count, uris.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                // Default options or no additional parameters required
            }
        }
    }
}

