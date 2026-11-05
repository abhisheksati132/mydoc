package com.abhisheksati.mydoc.tools.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                Text(text = "COMPRESSION LEVEL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = "ROTATION ANGLE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = "WATERMARK TEXT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.watermarkText,
                    onValueChange = { t -> onOptionsChange { it.copy(watermarkText = t) } },
                    placeholder = { Text("CONFIDENTIAL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                Text(text = "ROTATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = "POSITION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = "DOCUMENT PASSWORD", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.userPassword,
                    onValueChange = { p -> onOptionsChange { it.copy(userPassword = p) } },
                    placeholder = { Text("Enter password to open file") },
                    singleLine = true,
                    isError = passwordHasInvalidChar,
                    supportingText = {
                        Text(if (passwordHasInvalidChar) "Commas are not allowed" else "Required")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                Text(text = "PERMISSIONS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Allow Printing", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
                    Text(text = "Allow Copying text & media", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = options.allowCopying,
                        onCheckedChange = { allowed -> onOptionsChange { it.copy(allowCopying = allowed) } }
                    )
                }
                Text(
                    text = "Permissions are a request that other readers honour. They do not stop someone who can open the document from printing a picture of a page or re-rendering it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = AppSpacing.paddingSmall)
                )
            }

            PdfTool.UNLOCK -> {
                Text(text = "CURRENT PASSWORD", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.userPassword,
                    onValueChange = { p -> onOptionsChange { it.copy(userPassword = p) } },
                    placeholder = { Text("Enter password to unlock") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.REDACT -> {
                Text(text = "TEXT TO REDACT PERMANENTLY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.redactQuery,
                    onValueChange = { q -> onOptionsChange { it.copy(redactQuery = q) } },
                    placeholder = { Text("Search word or phrase (e.g. Secret)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.IMAGE_TO_PDF -> {
                Text(text = "PAGE SIZE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = "VISUAL PAGE ORGANIZER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Open Visual Page Organizer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Normal)
                    }
                    if (options.reorderedPageIndices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Organized order: ${options.reorderedPageIndices.map { it + 1 }.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text("Select a PDF document above to view and organize its pages.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            PdfTool.PDF_TO_IMAGES -> {
                Text(text = "EXPORT FORMAT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    AppFilterChip(
                        label = "ZIP of Images",
                        isSelected = !options.exportAsLongImage,
                        onClick = { onOptionsChange { it.copy(exportAsLongImage = false) } }
                    )
                    AppFilterChip(
                        label = "Single Long Image (Vertical Stitch)",
                        isSelected = options.exportAsLongImage,
                        onClick = { onOptionsChange { it.copy(exportAsLongImage = true) } }
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    AppFilterChip(
                        label = "JPEG",
                        isSelected = options.exportFormatJpg,
                        onClick = { onOptionsChange { it.copy(exportFormatJpg = true) } }
                    )
                    AppFilterChip(
                        label = "PNG",
                        isSelected = !options.exportFormatJpg,
                        onClick = { onOptionsChange { it.copy(exportFormatJpg = false) } }
                    )
                }
            }

            PdfTool.ANNOTATE -> {
                Text(text = "ANNOTATION MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    AppFilterChip(
                        label = "Stamp Badge",
                        isSelected = options.annotationType.equals("STAMP", ignoreCase = true),
                        onClick = { onOptionsChange { it.copy(annotationType = "STAMP") } }
                    )
                    AppFilterChip(
                        label = "Highlight Keyword",
                        isSelected = options.annotationType.equals("HIGHLIGHT", ignoreCase = true),
                        onClick = { onOptionsChange { it.copy(annotationType = "HIGHLIGHT") } }
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                if (options.annotationType.equals("STAMP", ignoreCase = true)) {
                    Text(text = "PRESET STAMPS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = "SIGNATURE STAMP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Applies a visual signature stamp to pages. For cryptographic integrity verification, use official certificates.",
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
                            SignatureStatus.NONE -> "Draw Signature Now"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasSignature) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(text = "PLACEMENT PAGE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    listOf("First Page" to 0, "Last Page" to 999).forEach { (label, pg) ->
                        AppFilterChip(
                            label = label,
                            isSelected = options.signaturePage == pg,
                            onClick = { onOptionsChange { it.copy(signaturePage = pg) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(text = "POSITION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    listOf("Bottom Right" to Pair(0.75f, 0.85f), "Bottom Center" to Pair(0.5f, 0.85f), "Bottom Left" to Pair(0.25f, 0.85f)).forEach { (label, coords) ->
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
                Text(text = "PAGE RANGE TO EXTRACT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.pageRange,
                    onValueChange = { r -> onOptionsChange { it.copy(pageRange = r) } },
                    placeholder = { Text("e.g. 1-3, 5, 8-10") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(
                    text = "Specify individual pages or page ranges separated by commas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PdfTool.REMOVE_PAGES -> {
                Text(text = "PAGES TO REMOVE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.pageRange,
                    onValueChange = { r -> onOptionsChange { it.copy(pageRange = r) } },
                    placeholder = { Text("e.g. 2, 4-6") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Text(
                    text = "Specified pages will be pruned while preserving all remaining pages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PdfTool.SPLIT -> {
                Text(text = "SPLIT EVERY N PAGES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)) {
                    listOf(1, 2, 5, 10).forEach { n ->
                        AppFilterChip(
                            label = "$n ${if (n == 1) "page" else "pages"}",
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
                Text(text = "OR CUSTOM PAGE RANGE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.pageRange,
                    onValueChange = { r -> onOptionsChange { it.copy(pageRange = r) } },
                    placeholder = { Text("Optional: e.g. 1-4, 5-10") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.CROP -> {
                Text(text = "CROP MARGIN (ALL SIDES)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = "PAGE RANGE (BLANK FOR ALL)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))
                OutlinedTextField(
                    value = options.pageRange,
                    onValueChange = { r -> onOptionsChange { it.copy(pageRange = r) } },
                    placeholder = { Text("All pages or e.g. 1-5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PdfTool.MERGE -> {
                if (uris.size < 2) {
                    Text(
                        text = "Add at least 2 PDF documents above to combine them into one file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "${uris.size} documents will be combined in the order listed above.",
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

